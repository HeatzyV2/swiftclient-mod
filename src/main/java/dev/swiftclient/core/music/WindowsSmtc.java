package dev.swiftclient.core.music;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Reads the focused Windows media session (Spotify, browser, etc.) via SMTC.
 * Replaces the old launcher-side {@code nowPlayingFile} bridge that Swift never shipped.
 */
public final class WindowsSmtc {
   private static final AtomicBoolean STARTED = new AtomicBoolean(false);
   private static Path scriptPath;

   private WindowsSmtc() {
   }

   public static void ensureStarted() {
      if (!isWindows() || !STARTED.compareAndSet(false, true)) {
         return;
      }
      try {
         scriptPath = writeScript();
      } catch (Throwable t) {
         System.err.println("[SwiftClient] Now Playing SMTC script: " + t.getMessage());
         return;
      }
      ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor(r -> {
         Thread t = new Thread(r, "swiftclient-smtc");
         t.setDaemon(true);
         return t;
      });
      exec.scheduleAtFixedRate(WindowsSmtc::tickSafe, 0L, 1L, TimeUnit.SECONDS);
   }

   private static boolean isWindows() {
      return System.getProperty("os.name", "").toLowerCase().contains("win");
   }

   private static void tickSafe() {
      try {
         tick();
      } catch (Throwable ignored) {
      }
   }

   private static void tick() throws Exception {
      if (scriptPath == null || !Files.isRegularFile(scriptPath)) {
         return;
      }
      ProcessBuilder pb = new ProcessBuilder(
         "powershell.exe",
         "-NoProfile",
         "-NonInteractive",
         "-ExecutionPolicy",
         "Bypass",
         "-File",
         scriptPath.toAbsolutePath().toString()
      );
      pb.redirectErrorStream(true);
      Process p = pb.start();
      StringBuilder out = new StringBuilder();
      try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
         String line;
         while ((line = r.readLine()) != null) {
            out.append(line);
         }
      }
      p.waitFor(4L, TimeUnit.SECONDS);
      if (p.isAlive()) {
         p.destroyForcibly();
         return;
      }
      String body = out.toString().trim();
      if (body.isEmpty() || body.startsWith("NO_SESSION") || !body.startsWith("{")) {
         return;
      }
      MusicState.applyJson(body, true);
   }

   private static Path writeScript() throws Exception {
      Path dir = Path.of(System.getProperty("java.io.tmpdir"), "swiftclient");
      Files.createDirectories(dir);
      Path file = dir.resolve("nowplaying-smtc.ps1");
      Files.writeString(file, SCRIPT, StandardCharsets.UTF_8);
      return file;
   }

   // PlaybackStatus: Closed=0 Opened=1 Changing=2 Stopped=3 Playing=4 Paused=5
   // Thumbnail must be read via IInputStream.ReadAsync reflection — PowerShell cannot cast
   // the OpenReadAsync COM object to IRandomAccessStream / AsStreamForRead.
   private static final String SCRIPT = """
      Add-Type -AssemblyName System.Runtime.WindowsRuntime
      $asTask = ([System.WindowsRuntimeSystemExtensions].GetMethods() | Where-Object {
        $_.Name -eq 'AsTask' -and $_.GetParameters().Count -eq 1 -and $_.GetParameters()[0].ParameterType.Name -eq 'IAsyncOperation`1'
      })[0]
      $asTaskProg = ([System.WindowsRuntimeSystemExtensions].GetMethods() | Where-Object {
        $_.Name -eq 'AsTask' -and $_.GetParameters().Count -eq 1 -and $_.GetParameters()[0].ParameterType.Name -eq 'IAsyncOperationWithProgress`2'
      })[0]
      function Await($WinRtTask, $ResultType) {
        $netTask = $asTask.MakeGenericMethod($ResultType).Invoke($null, @($WinRtTask))
        $netTask.Wait(-1) | Out-Null
        $netTask.Result
      }
      function AwaitProg($WinRtTask, $T1, $T2) {
        $netTask = $asTaskProg.MakeGenericMethod($T1, $T2).Invoke($null, @($WinRtTask))
        $netTask.Wait(-1) | Out-Null
        $netTask.Result
      }
      function Esc([string]$s) {
        if ($null -eq $s) { return '' }
        return ($s -replace '\\\\','\\\\\\\\' -replace '"','\\\\"' -replace '[\\r\\n]',' ')
      }
      function ReadThumb($thumb) {
        if ($null -eq $thumb) { return '' }
        try {
          $ras = Await ($thumb.OpenReadAsync()) ([Windows.Storage.Streams.IRandomAccessStreamWithContentType])
          if ($null -eq $ras) { return '' }
          $readMethod = [Windows.Storage.Streams.IInputStream].GetMethod('ReadAsync')
          $cap = [uint32](2 * 1024 * 1024)
          $bytes = New-Object byte[] $cap
          $ibuf = [System.Runtime.InteropServices.WindowsRuntime.WindowsRuntimeBufferExtensions]::AsBuffer($bytes)
          $opt = [Windows.Storage.Streams.InputStreamOptions]::None
          $op = $readMethod.Invoke($ras, @($ibuf, $cap, $opt))
          $result = AwaitProg $op ([Windows.Storage.Streams.IBuffer]) ([UInt32])
          if ($null -eq $result -or $result.Length -le 0) { return '' }
          $out = New-Object byte[] $result.Length
          [System.Runtime.InteropServices.WindowsRuntime.WindowsRuntimeBufferExtensions]::CopyTo($result, $out)
          $dir = Join-Path $env:TEMP 'swiftclient'
          if (-not (Test-Path $dir)) { New-Item -ItemType Directory -Path $dir | Out-Null }
          $path = Join-Path $dir 'smtc-art.bin'
          [System.IO.File]::WriteAllBytes($path, $out)
          return ($path -replace '\\\\','/')
        } catch { return '' }
      }
      [Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager,Windows.Media.Control,ContentType=WindowsRuntime] | Out-Null
      [Windows.Storage.Streams.Buffer,Windows.Storage.Streams,ContentType=WindowsRuntime] | Out-Null
      $mgr = Await ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager]::RequestAsync()) ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager])
      $s = $mgr.GetCurrentSession()
      if (-not $s) { Write-Output 'NO_SESSION'; exit 0 }
      $props = Await ($s.TryGetMediaPropertiesAsync()) ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionMediaProperties])
      $pb = $s.GetPlaybackInfo()
      $tl = $s.GetTimelineProperties()
      $title = Esc $props.Title
      $artist = Esc $props.Artist
      if ([string]::IsNullOrWhiteSpace($title)) { Write-Output 'NO_SESSION'; exit 0 }
      $playing = if ([int]$pb.PlaybackStatus -eq 4) { 'true' } else { 'false' }
      $pos = [int64]$tl.Position.TotalMilliseconds
      $dur = [int64](($tl.EndTime - $tl.StartTime).TotalMilliseconds)
      if ($dur -lt 0) { $dur = 0 }
      $tid = [Math]::Abs(($title + '|' + $artist).GetHashCode())
      $art = Esc (ReadThumb $props.Thumbnail)
      Write-Output ('{"title":"' + $title + '","artist":"' + $artist + '","playing":' + $playing + ',"position":' + $pos + ',"duration":' + $dur + ',"trackId":"' + $tid + '","art":"' + $art + '"}')
      """;
}
