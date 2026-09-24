package dev.swiftclient.core.music;

import dev.swiftclient.core.log.Log;
import org.slf4j.Logger;
import dev.swiftclient.core.cosmetics.Backoff;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Reads the focused Windows media session (Spotify, browser, etc.) via SMTC.
 * Replaces the old launcher-side {@code nowPlayingFile} bridge that Swift never shipped.
 *
 * <p>One long-lived PowerShell process runs {@link #SCRIPT}, which prints one JSON line per
 * second on stdout; a daemon thread reads that stream. The process only exists while the
 * "Now playing" module is enabled ({@link #setActive}), and exits on its own if the game dies.
 */
public final class WindowsSmtc {
   private static final Logger LOG = Log.get("Music");
   private static final Object LOCK = new Object();
   /** Throttles respawns if PowerShell keeps dying (missing WinRT, blocked by policy...). */
   private static final Backoff RESTART = new Backoff("Now Playing SMTC", 5000L, 300000L);
   private static Process process;
   private static boolean hookInstalled;

   private WindowsSmtc() {
   }

   /** Start or stop the SMTC reader. Cheap enough to call every client tick. */
   public static void setActive(boolean on) {
      if (!isWindows()) {
         return;
      }

      synchronized (LOCK) {
         if (on) {
            if (process != null && !process.isAlive()) {
               process = null;
               RESTART.failure("process PowerShell termine");
            }

            if (process == null && !RESTART.blocked()) {
               start();
            }
         } else if (process != null) {
            stop();
         }
      }
   }

   private static boolean isWindows() {
      return System.getProperty("os.name", "").toLowerCase().contains("win");
   }

   private static void start() {
      try {
         Path script = writeScript();
         ProcessBuilder pb = new ProcessBuilder(
            "powershell.exe",
            "-NoProfile",
            "-NonInteractive",
            "-ExecutionPolicy",
            "Bypass",
            "-File",
            script.toAbsolutePath().toString(),
            "-ParentPid",
            Long.toString(ProcessHandle.current().pid()),
            "-IntervalMs",
            "1000"
         );
         pb.redirectErrorStream(true);
         Process p = pb.start();
         process = p;
         Thread reader = new Thread(() -> read(p), "swiftclient-smtc");
         reader.setDaemon(true);
         reader.start();
         if (!hookInstalled) {
            hookInstalled = true;
            Runtime.getRuntime().addShutdownHook(new Thread(WindowsSmtc::shutdown, "swiftclient-smtc-shutdown"));
         }

         LOG.info("Lecteur SMTC demarre (pid {})", p.pid());
      } catch (Throwable t) {
         process = null;
         RESTART.failure(t.getClass().getSimpleName() + ": " + t.getMessage());
      }
   }

   private static void stop() {
      Process p = process;
      process = null;
      if (p != null) {
         p.destroy();
         LOG.info("Lecteur SMTC arrete");
      }
   }

   private static void shutdown() {
      synchronized (LOCK) {
         stop();
      }
   }

   private static void read(Process p) {
      try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
         String line;
         while ((line = r.readLine()) != null) {
            line = line.trim();
            if (line.startsWith("{")) {
               RESTART.success();
               MusicState.applyJson(line, true);
            } else if (line.startsWith("NO_SESSION")) {
               RESTART.success();
            }
         }
      } catch (Throwable ignored) {
      }
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
      param([int]$ParentPid = 0, [int]$IntervalMs = 1000)
      [Console]::OutputEncoding = [System.Text.Encoding]::UTF8
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
      $lastTid = ''
      $lastArt = ''
      $artTries = 0
      while ($true) {
        if ($ParentPid -gt 0 -and -not (Get-Process -Id $ParentPid -ErrorAction SilentlyContinue)) { exit 0 }
        $line = 'NO_SESSION'
        try {
          $s = $mgr.GetCurrentSession()
          if ($s) {
            $props = Await ($s.TryGetMediaPropertiesAsync()) ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionMediaProperties])
            $title = Esc $props.Title
            if (-not [string]::IsNullOrWhiteSpace($title)) {
              $pb = $s.GetPlaybackInfo()
              $tl = $s.GetTimelineProperties()
              $artist = Esc $props.Artist
              $playing = if ([int]$pb.PlaybackStatus -eq 4) { 'true' } else { 'false' }
              $pos = [int64]$tl.Position.TotalMilliseconds
              $dur = [int64](($tl.EndTime - $tl.StartTime).TotalMilliseconds)
              if ($dur -lt 0) { $dur = 0 }
              $tid = [Math]::Abs(($title + '|' + $artist).GetHashCode())
              if ($tid -ne $lastTid) { $lastTid = $tid; $lastArt = ''; $artTries = 0 }
              # Thumbnail only once per track (a few retries: SMTC often publishes it late).
              if ($lastArt -eq '' -and $artTries -lt 5) { $artTries++; $lastArt = Esc (ReadThumb $props.Thumbnail) }
              $line = '{"title":"' + $title + '","artist":"' + $artist + '","playing":' + $playing + ',"position":' + $pos + ',"duration":' + $dur + ',"trackId":"' + $tid + '","art":"' + $lastArt + '"}'
            }
          }
        } catch { $line = 'NO_SESSION' }
        [Console]::Out.WriteLine($line)
        [Console]::Out.Flush()
        Start-Sleep -Milliseconds $IntervalMs
      }
      """;
}
