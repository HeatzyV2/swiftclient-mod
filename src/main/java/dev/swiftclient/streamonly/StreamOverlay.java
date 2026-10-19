package dev.swiftclient.streamonly;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.GpuFence;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import dev.swiftclient.core.log.Log;
import dev.swiftclient.modules.HideInventoryModule;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.item.TrackingItemStackRenderState;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector4f;
import org.slf4j.Logger;

/**
 * Hide Inventory "Stream Only": the game frame (what OBS or Discord capture) shows empty or fake items,
 * while native windows excluded from screen capture draw your real items, counts, tooltips and item name
 * exactly on top of it, for you alone. Windows only.
 *
 * <p>Each frame, after the GUI is drawn: the item icons are rendered once into a scratch texture and read
 * back (cached per model), the slot backgrounds are read from the frame (the slots are still empty at that
 * point), then the fake items are drawn onto the frame and the overlay windows are recomposed on the CPU.
 */
public final class StreamOverlay {
   private static final Logger LOGGER = Log.get("StreamOnly");
   private static final int SLOTS = 41;
   private static final int CURSOR = 41;
   private static final int ENTRIES = 42;
   private static final long SLOT_BITS = (1L << SLOTS) - 1L;
   private static final int CELLS_PER_SIDE = 7;
   private static final int MAX_CELLS = CELLS_PER_SIDE * CELLS_PER_SIDE;
   private static final int CACHE_LIMIT = 4 * 1024 * 1024;
   private static final long READBACK_TIMEOUT = 1_000_000_000L;
   private static final long BACKGROUND_INTERVAL = 33_000_000L;
   private static final long ANIMATED_REFRESH_MS = 250L;
   private static final int FALLBACK_BACKGROUND = 0xFF8B8B8B;
   private static final int[] BLANK = new int[0];
   private static final ItemStack[] stack = new ItemStack[ENTRIES];
   private static final int[] guiX = new int[ENTRIES];
   private static final int[] guiY = new int[ENTRIES];
   private static long mask;
   private static int hovered = -1;
   private static boolean selectionSeen;
   private static int selectionX;
   private static int selectionY;
   private static final ItemStack[] shownStack = new ItemStack[ENTRIES];
   private static final int[] shownX = new int[ENTRIES];
   private static final int[] shownY = new int[ENTRIES];
   private static final int[] shownCount = new int[ENTRIES];
   private static final int[] shownBar = new int[ENTRIES];
   private static long shownMask;
   private static int shownScale;
   private static boolean shownOpaque;
   private static boolean shownSelection;
   private static int shownSelectionX;
   private static int shownSelectionY;
   private static int shownHover = -1;
   private static int shownWindowX;
   private static int shownWindowY;
   private static int boxX;
   private static int boxY;
   private static boolean backgroundValid;
   private static boolean dirty;
   private static ItemStack cursorStack;
   private static int cursorCount;
   private static int cursorBar;
   private static int cursorScale;
   private static int cursorX;
   private static int cursorY;
   private static final Map<Object, int[]> icons = new HashMap<>();
   private static final int[][] slotIcon = new int[ENTRIES][];
   private static final int[] pendingSlot = new int[MAX_CELLS];
   private static final Object[] pendingKey = new Object[MAX_CELLS];
   private static int cachedBytes;
   private static final int[][] background = new int[SLOTS][];
   private static final int[] region = new int[4];
   private static long backgroundAt;
   private static boolean asyncInFlight;
   private static final Canvas canvas = new Canvas();
   private static OverlayWindow slotWindow;
   private static OverlayWindow cursorWindow;
   private static GpuTexture scratch;
   private static GpuTextureView scratchView;
   private static GpuTexture scratchDepth;
   private static GpuTextureView scratchDepthView;
   private static int iconPx;
   private static boolean failed;

   private StreamOverlay() {
   }

   /** The overlay can run here: Windows, and it has not failed this session. */
   public static boolean usable() {
      return !failed && System.getProperty("os.name", "").toLowerCase(Locale.ROOT).startsWith("windows");
   }

   public static void retry() {
      failed = false;
   }

   // --- Recorded while the GUI is extracted (HideInventoryHudMixin, HideInventoryScreenMixin) ---

   public static void hotbar(int slot, int x, int y, ItemStack item) {
      if (hotbarVisible()) {
         record(slot, x, y, item);
      }
   }

   public static void container(int slot, int x, int y, ItemStack item, boolean isHovered) {
      record(slot, x, y, item);
      if (isHovered) {
         hovered = slot;
      }
   }

   public static void carried(ItemStack item, int x, int y) {
      record(CURSOR, x, y, item);
   }

   public static void selection(int x, int y) {
      if (hotbarVisible()) {
         selectionSeen = true;
         selectionX = x;
         selectionY = y;
      }
   }

   public static void tooltip(ItemStack item, int mouseX, int mouseY) {
      TextPanels.tooltip(item, mouseX, mouseY);
   }

   public static void itemName(ItemStack item, int alpha, int y) {
      TextPanels.itemName(item, alpha, y);
   }

   private static boolean hotbarVisible() {
      Screen screen = Minecraft.getInstance().gui.screen();
      return screen == null || screen instanceof ChatScreen;
   }

   private static void record(int entry, int x, int y, ItemStack item) {
      if (entry >= 0 && entry < ENTRIES && item != null && (entry != CURSOR || !item.isEmpty())) {
         stack[entry] = item;
         guiX[entry] = x;
         guiY[entry] = y;
         mask |= 1L << entry;
      }
   }

   // --- After the GUI was drawn (StreamOverlayFrameMixin) ---

   public static void endFrame() {
      long frame = mask;
      mask = 0L;
      int hover = hovered;
      hovered = -1;
      boolean selection = selectionSeen && HideInventoryModule.hidesSelectedSlot();
      selectionSeen = false;
      if (HideInventoryModule.overlayWanted() && usable()) {
         try {
            Minecraft mc = Minecraft.getInstance();
            Window win = mc.getWindow();
            if (win.isMinimized()) {
               hideAll();
            } else {
               int scale = win.getGuiScale();
               boolean opaque = HideInventoryModule.opaqueCells();
               frame = withoutEmpty(frame, opaque && HideInventoryModule.fillsEmpty());
               layers(mc, win, frame, scale, win.getX(), win.getY(), opaque, selection, hover);
               drawFakes(mc, frame & SLOT_BITS, scale);
               TextPanels.update(win, scale, win.getX(), win.getY());
            }
         } catch (LinkageError | RuntimeException e) {
            failed = true;
            LOGGER.warn("Hide Inventory: stream-only overlay unavailable ({}); items stay hidden", e.toString());
            release();
         }
      } else if (started()) {
         release();
      }

      TextPanels.frameDone();
   }

   public static void stop() {
      if (RenderSystem.isOnRenderThread()) {
         release();
      } else {
         Minecraft.getInstance().execute(StreamOverlay::release);
      }
   }

   private static boolean started() {
      return slotWindow != null || cursorWindow != null || scratch != null || TextPanels.started();
   }

   private static void layers(Minecraft mc, Window win, long frame, int scale, int windowX, int windowY, boolean opaque, boolean selection, int hover) {
      long slotBits = frame & SLOT_BITS;
      boolean slotsShown = slotBits != 0L || selection;
      boolean cursorShown = (frame >>> CURSOR & 1L) != 0L;
      if (!slotsShown && !cursorShown) {
         hideSlotsAndCursor();
         return;
      }

      int px = 16 * scale;
      if (px != iconPx || cachedBytes > CACHE_LIMIT) {
         resetIcons(px);
      }

      if (scratch == null) {
         allocate(px);
      }

      if (slotWindow == null && slotsShown) {
         slotWindow = new OverlayWindow(win.handle());
      }

      if (cursorWindow == null && cursorShown) {
         cursorWindow = new OverlayWindow(win.handle());
      }

      boolean layout = slotBits != shownMask
         || scale != shownScale
         || opaque != shownOpaque
         || selection != shownSelection
         || selection && (selectionX != shownSelectionX || selectionY != shownSelectionY);
      boolean content = false;
      for (int i = 0; i < SLOTS; i++) {
         if ((slotBits >>> i & 1L) != 0L) {
            if (guiX[i] != shownX[i] || guiY[i] != shownY[i]) {
               layout = true;
            } else if (stack[i] != shownStack[i] || stack[i].getCount() != shownCount[i] || bar(stack[i]) != shownBar[i]) {
               content = true;
            }
         }
      }

      if (layout) {
         backgroundValid = false;
      }

      int pending = resolveIcons(frame, px);
      boolean wantBackground = opaque && slotBits != 0L && !backgroundValid;
      if (pending > 0 || wantBackground) {
         synchronousRead(slotBits, scale, wantBackground, pending);
         if (wantBackground) {
            shownHover = hover;
         }
      }

      if (!slotsShown) {
         if (slotWindow != null) {
            slotWindow.hide();
         }

         shownMask = 0L;
      } else if (layout || content || dirty || pending > 0 || wantBackground) {
         composeSlots(slotBits, scale, opaque, selection, windowX, windowY);
         dirty = false;
      } else if (windowX != shownWindowX || windowY != shownWindowY) {
         slotWindow.move(windowX + boxX, windowY + boxY);
         shownWindowX = windowX;
         shownWindowY = windowY;
      }

      // The slot backgrounds change (hover highlight, the world behind the hotbar): refresh them without stalling.
      if (opaque && slotBits != 0L && !asyncInFlight && backgroundValid
         && (hover != shownHover || hotbarVisible() && System.nanoTime() - backgroundAt >= BACKGROUND_INTERVAL)) {
         requestAsync(slotBits, scale);
         shownHover = hover;
      }

      cursor(frame, scale, windowX, windowY);
   }

   private static void hideSlotsAndCursor() {
      if (slotWindow != null) {
         slotWindow.hide();
      }

      if (cursorWindow != null) {
         cursorWindow.hide();
      }

      shownMask = 0L;
      cursorStack = null;
   }

   private static void hideAll() {
      hideSlotsAndCursor();
      TextPanels.hide();
   }

   /** Empty slots only matter when a fake is drawn in them (the overlay must cover it). */
   private static long withoutEmpty(long frame, boolean keepEmpty) {
      if (keepEmpty) {
         return frame;
      }

      long bits = frame;
      for (int i = 0; i < SLOTS; i++) {
         if ((bits >>> i & 1L) != 0L && stack[i].isEmpty()) {
            bits &= ~(1L << i);
         }
      }

      return bits;
   }

   private static int bar(ItemStack item) {
      return item.isBarVisible() ? item.getBarColor() << 5 | item.getBarWidth() : -1;
   }

   private static int cellX(int cell, int px) {
      return cell % CELLS_PER_SIDE * px;
   }

   private static int cellY(int cell, int px) {
      return cell / CELLS_PER_SIDE * px;
   }

   /** Icons already cached are reused; the others are drawn into the scratch cells. @return cells drawn */
   private static int resolveIcons(long bits, int px) {
      int pending = 0;
      int side = CELLS_PER_SIDE * px;
      for (int i = 0; i < ENTRIES; i++) {
         if ((bits >>> i & 1L) == 0L) {
            continue;
         }

         ItemStack item = stack[i];
         if (item != shownStack[i]) {
            slotIcon[i] = null;
         }

         if (slotIcon[i] != null) {
            continue;
         }

         TrackingItemStackRenderState state = ItemIcons.resolve(item);
         if (state == null) {
            slotIcon[i] = BLANK;
            continue;
         }

         Object key = state.isAnimated()
            ? List.of(state.getModelIdentity(), System.currentTimeMillis() / ANIMATED_REFRESH_MS)
            : new ArrayList<>((List<?>)state.getModelIdentity());
         int[] icon = icons.get(key);
         if (icon != null) {
            slotIcon[i] = icon;
         } else if (pending < MAX_CELLS) {
            if (pending == 0) {
               RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(scratch, new Vector4f(0.0F, 0.0F, 0.0F, 0.0F), scratchDepth, 0.0);
            }

            int cell = pending++;
            ItemIcons.draw(state, scratchView, scratchDepthView, side, side, cellX(cell, px), cellY(cell, px), px);
            pendingSlot[cell] = i;
            pendingKey[cell] = key;
         }
      }

      return pending;
   }

   private static void resetIcons(int px) {
      icons.clear();
      cachedBytes = 0;
      Arrays.fill(slotIcon, null);
      backgroundValid = false;
      if (px != iconPx) {
         closeTextures();
         Arrays.fill(background, null);
      }
   }

   private static void allocate(int px) {
      int side = CELLS_PER_SIDE * px;
      GpuDevice device = RenderSystem.getDevice();
      scratch = device.createTexture("Swift stream overlay icons", GpuTexture.USAGE_COPY_SRC | GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_RENDER_ATTACHMENT | GpuTexture.USAGE_COPY_DST, GpuFormat.RGBA8_UNORM, side, side, 1, 1);
      scratchView = device.createTextureView(scratch);
      scratchDepth = device.createTexture("Swift stream overlay icon depth", GpuTexture.USAGE_RENDER_ATTACHMENT | GpuTexture.USAGE_COPY_DST, GpuFormat.D32_FLOAT, side, side, 1, 1);
      scratchDepthView = device.createTextureView(scratchDepth);
      iconPx = px;
   }

   /** Reads the new icons (and the slot backgrounds when needed) now, waiting for the GPU. */
   private static void synchronousRead(long slotBits, int scale, boolean wantBackground, int pending) {
      int px = 16 * scale;
      int side = CELLS_PER_SIDE * px;
      long scratchBytes = pending > 0 ? (long)side * side * 4L : 0L;
      boolean framed = wantBackground && region(slotBits, scale, region);
      long frameBytes = framed ? (long)region[2] * region[3] * 4L : 0L;
      if (wantBackground) {
         backgroundValid = true;
      }

      if (scratchBytes + frameBytes == 0L) {
         if (wantBackground) {
            unpackBackground(null, 0, region, false, slotBits, scale, guiX, guiY);
         }

         return;
      }

      GpuDevice device = RenderSystem.getDevice();
      GpuBuffer readback = device.createBuffer(() -> "Swift stream overlay readback", GpuBuffer.USAGE_MAP_READ | GpuBuffer.USAGE_COPY_DST, scratchBytes + frameBytes);
      try {
         CommandEncoder encoder = device.createCommandEncoder();
         if (pending > 0) {
            encoder.copyTextureToBuffer(scratch, readback, 0L, () -> {}, 0);
         }

         if (framed) {
            copyRegion(encoder, readback, scratchBytes, region);
         }

         try (GpuFence copied = encoder.createFence()) {
            encoder.submit();
            if (!copied.awaitCompletion(READBACK_TIMEOUT)) {
               throw new IllegalStateException("overlay read-back timed out");
            }
         }

         try (GpuBufferSlice.MappedView view = readback.slice(0L, scratchBytes + frameBytes).map(true, false)) {
            ByteBuffer data = view.data();
            for (int c = 0; c < pending; c++) {
               int[] icon = unpackIcon(data, cellX(c, px), cellY(c, px), side, px);
               icons.put(pendingKey[c], icon);
               pendingKey[c] = null;
               cachedBytes += px * px * 4;
               slotIcon[pendingSlot[c]] = icon;
            }

            if (wantBackground) {
               unpackBackground(data, (int)scratchBytes, region, framed, slotBits, scale, guiX, guiY);
               backgroundAt = System.nanoTime();
            }
         }
      } finally {
         readback.close();
      }
   }

   /** Refreshes the slot backgrounds a frame or two later, without waiting for the GPU. */
   private static void requestAsync(long slotBits, int scale) {
      backgroundAt = System.nanoTime();
      int[] area = new int[4];
      if (!region(slotBits, scale, area)) {
         return;
      }

      long bytes = (long)area[2] * area[3] * 4L;
      GpuBuffer buffer = RenderSystem.getDevice().createBuffer(() -> "Swift stream overlay background", GpuBuffer.USAGE_MAP_READ | GpuBuffer.USAGE_COPY_DST, bytes);
      int[] xs = guiX.clone();
      int[] ys = guiY.clone();
      asyncInFlight = true;
      copyRegion(RenderSystem.getDevice().createCommandEncoder(), buffer, 0L, area, () -> {
         try {
            asyncInFlight = false;
            if (scratch != null && slotBits == shownMask && scale == shownScale && samePositions(slotBits, xs, ys)) {
               try (GpuBufferSlice.MappedView view = buffer.slice(0L, bytes).map(true, false)) {
                  if (unpackBackground(view.data(), 0, area, true, slotBits, scale, xs, ys)) {
                     dirty = true;
                  }
               }
            }
         } finally {
            buffer.close();
         }
      });
   }

   private static boolean samePositions(long slotBits, int[] xs, int[] ys) {
      for (int i = 0; i < SLOTS; i++) {
         if ((slotBits >>> i & 1L) != 0L && (xs[i] != guiX[i] || ys[i] != guiY[i])) {
            return false;
         }
      }

      return true;
   }

   /** The framebuffer rectangle covering every recorded slot: x, y (top-left origin), width, height. */
   private static boolean region(long slotBits, int scale, int[] out) {
      RenderTarget main = Minecraft.getInstance().gameRenderer.mainRenderTarget();
      int px = 16 * scale;
      int x0 = Integer.MAX_VALUE;
      int y0 = Integer.MAX_VALUE;
      int x1 = Integer.MIN_VALUE;
      int y1 = Integer.MIN_VALUE;
      for (int i = 0; i < SLOTS; i++) {
         if ((slotBits >>> i & 1L) != 0L) {
            x0 = Math.min(x0, guiX[i] * scale);
            y0 = Math.min(y0, guiY[i] * scale);
            x1 = Math.max(x1, guiX[i] * scale + px);
            y1 = Math.max(y1, guiY[i] * scale + px);
         }
      }

      x0 = Math.max(x0, 0);
      y0 = Math.max(y0, 0);
      x1 = Math.min(x1, main.width);
      y1 = Math.min(y1, main.height);
      if (x1 <= x0 || y1 <= y0) {
         return false;
      }

      out[0] = x0;
      out[1] = y0;
      out[2] = x1 - x0;
      out[3] = y1 - y0;
      return true;
   }

   private static void copyRegion(CommandEncoder encoder, GpuBuffer target, long offset, int[] area) {
      copyRegion(encoder, target, offset, area, () -> {});
   }

   private static void copyRegion(CommandEncoder encoder, GpuBuffer target, long offset, int[] area, Runnable done) {
      RenderTarget main = Minecraft.getInstance().gameRenderer.mainRenderTarget();
      // Texture rows start at the bottom of the screen.
      encoder.copyTextureToBuffer(main.getColorTexture(), target, offset, done, 0, area[0], main.height - area[1] - area[3], area[2], area[3]);
   }

   private static int[] unpackIcon(ByteBuffer data, int cellX, int cellY, int side, int px) {
      int[] out = new int[px * px];
      for (int y = 0; y < px; y++) {
         int row = ((side - 1 - (cellY + y)) * side + cellX) * 4;
         for (int x = 0; x < px; x++) {
            int at = row + x * 4;
            out[y * px + x] = (data.get(at + 3) & 255) << 24 | (data.get(at) & 255) << 16 | (data.get(at + 1) & 255) << 8 | data.get(at + 2) & 255;
         }
      }

      return out;
   }

   private static boolean unpackBackground(ByteBuffer data, int offset, int[] area, boolean framed, long slotBits, int scale, int[] xs, int[] ys) {
      int px = 16 * scale;
      int rx = area[0];
      int ry = area[1];
      int rw = area[2];
      int rh = area[3];
      boolean changed = false;
      for (int slot = 0; slot < SLOTS; slot++) {
         if ((slotBits >>> slot & 1L) == 0L) {
            continue;
         }

         int cx = xs[slot] * scale - rx;
         int cy = ys[slot] * scale - ry;
         if (framed && cx >= 0 && cy >= 0 && cx + px <= rw && cy + px <= rh) {
            int[] next = new int[px * px];
            for (int y = 0; y < px; y++) {
               int row = offset + ((rh - 1 - (cy + y)) * rw + cx) * 4;
               for (int x = 0; x < px; x++) {
                  int at = row + x * 4;
                  next[y * px + x] = 0xFF000000 | (data.get(at) & 255) << 16 | (data.get(at + 1) & 255) << 8 | data.get(at + 2) & 255;
               }
            }

            if (!Arrays.equals(background[slot], next)) {
               changed = true;
               background[slot] = next;
            }
         } else {
            if (background[slot] != null) {
               changed = true;
            }

            background[slot] = null;
         }
      }

      return changed;
   }

   /** Fake items drawn onto the streamed frame, in the slots the overlay covers. */
   private static void drawFakes(Minecraft mc, long slotBits, int scale) {
      if (slotBits == 0L) {
         return;
      }

      RenderTarget main = mc.gameRenderer.mainRenderTarget();
      boolean cleared = false;
      int px = 16 * scale;
      RenderSystem.backupProjectionMatrix();
      try {
         for (int slot = 0; slot < SLOTS; slot++) {
            if ((slotBits >>> slot & 1L) == 0L) {
               continue;
            }

            TrackingItemStackRenderState state = ItemIcons.resolve(HideInventoryModule.fakeFor(slot));
            if (state == null) {
               continue;
            }

            if (!cleared) {
               RenderSystem.getDevice().createCommandEncoder().clearDepthTexture(main.getDepthTexture(), 0.0);
               cleared = true;
            }

            ItemIcons.draw(state, main.getColorTextureView(), main.getDepthTextureView(), main.width, main.height, guiX[slot] * scale, guiY[slot] * scale, px);
         }
      } finally {
         RenderSystem.restoreProjectionMatrix();
      }
   }

   private static void composeSlots(long slotBits, int scale, boolean opaque, boolean selection, int windowX, int windowY) {
      int px = 16 * scale;
      OverlayArt.Image frame = selection ? OverlayArt.selection() : null;
      boolean drawSelection = frame != null;
      int minX = Integer.MAX_VALUE;
      int minY = Integer.MAX_VALUE;
      int maxX = Integer.MIN_VALUE;
      int maxY = Integer.MIN_VALUE;
      for (int i = 0; i < SLOTS; i++) {
         if ((slotBits >>> i & 1L) != 0L) {
            minX = Math.min(minX, guiX[i]);
            minY = Math.min(minY, guiY[i]);
            maxX = Math.max(maxX, guiX[i] + 17);
            maxY = Math.max(maxY, guiY[i] + 17);
         }
      }

      if (drawSelection) {
         minX = Math.min(minX, selectionX);
         minY = Math.min(minY, selectionY);
         maxX = Math.max(maxX, selectionX + frame.width());
         maxY = Math.max(maxY, selectionY + frame.height());
      }

      if (minX > maxX) {
         return;
      }

      int bx = minX * scale;
      int by = minY * scale;
      int bw = (maxX - minX) * scale;
      int bh = (maxY - minY) * scale;
      canvas.wrap(slotWindow.pixels(bw, bh), slotWindow.stride(), bw, bh);
      if (opaque) {
         for (int i = 0; i < SLOTS; i++) {
            if ((slotBits >>> i & 1L) != 0L) {
               int ox = guiX[i] * scale - bx;
               int oy = guiY[i] * scale - by;
               int[] behind = background[i];
               if (behind != null) {
                  canvas.copy(behind, px, px, ox, oy);
               } else {
                  canvas.rect(ox, oy, px, px, FALLBACK_BACKGROUND);
               }
            }
         }
      }

      if (drawSelection) {
         canvas.blend(frame.pixels(), frame.width(), frame.height(), selectionX * scale - bx, selectionY * scale - by, scale);
      }

      for (int i = 0; i < SLOTS; i++) {
         if ((slotBits >>> i & 1L) != 0L) {
            int ox = guiX[i] * scale - bx;
            int oy = guiY[i] * scale - by;
            int[] icon = slotIcon[i];
            if (icon != null && icon != BLANK) {
               canvas.blend(icon, px, px, ox, oy, 1);
            }

            if (!stack[i].isEmpty()) {
               DigitFont.decorations(canvas, stack[i], ox, oy, scale);
            }
         }
      }

      slotWindow.present(windowX + bx, windowY + by, bw, bh);
      for (int i = 0; i < SLOTS; i++) {
         if ((slotBits >>> i & 1L) != 0L) {
            shownStack[i] = stack[i];
            shownX[i] = guiX[i];
            shownY[i] = guiY[i];
            shownCount[i] = stack[i].getCount();
            shownBar[i] = bar(stack[i]);
         }
      }

      shownMask = slotBits;
      shownScale = scale;
      shownOpaque = opaque;
      shownSelection = selection;
      shownSelectionX = selectionX;
      shownSelectionY = selectionY;
      shownWindowX = windowX;
      shownWindowY = windowY;
      boxX = bx;
      boxY = by;
   }

   private static void cursor(long frame, int scale, int windowX, int windowY) {
      if ((frame >>> CURSOR & 1L) == 0L) {
         if (cursorWindow != null) {
            cursorWindow.hide();
         }

         cursorStack = null;
         return;
      }

      ItemStack item = stack[CURSOR];
      int px = 16 * scale;
      int screenX = windowX + guiX[CURSOR] * scale;
      int screenY = windowY + guiY[CURSOR] * scale;
      boolean same = item == cursorStack && item.getCount() == cursorCount && bar(item) == cursorBar && scale == cursorScale && slotIcon[CURSOR] != null;
      if (same) {
         if (screenX != cursorX || screenY != cursorY) {
            cursorWindow.move(screenX, screenY);
         }
      } else {
         int size = px + scale;
         canvas.wrap(cursorWindow.pixels(size, size), cursorWindow.stride(), size, size);
         int[] icon = slotIcon[CURSOR];
         if (icon != null && icon != BLANK) {
            canvas.blend(icon, px, px, 0, 0, 1);
         }

         DigitFont.decorations(canvas, item, 0, 0, scale);
         cursorWindow.present(screenX, screenY, size, size);
         cursorStack = item;
         cursorCount = item.getCount();
         cursorBar = bar(item);
         cursorScale = scale;
      }

      cursorX = screenX;
      cursorY = screenY;
      shownStack[CURSOR] = item;
   }

   private static void closeTextures() {
      if (scratchView != null) {
         scratchView.close();
      }

      if (scratch != null) {
         scratch.close();
      }

      if (scratchDepthView != null) {
         scratchDepthView.close();
      }

      if (scratchDepth != null) {
         scratchDepth.close();
      }

      scratchView = null;
      scratch = null;
      scratchDepthView = null;
      scratchDepth = null;
      iconPx = 0;
   }

   private static void release() {
      if (slotWindow != null) {
         slotWindow.destroy();
         slotWindow = null;
      }

      if (cursorWindow != null) {
         cursorWindow.destroy();
         cursorWindow = null;
      }

      TextPanels.release();
      closeTextures();
      icons.clear();
      cachedBytes = 0;
      Arrays.fill(slotIcon, null);
      Arrays.fill(pendingKey, null);
      Arrays.fill(background, null);
      Arrays.fill(stack, null);
      Arrays.fill(shownStack, null);
      mask = 0L;
      shownMask = 0L;
      shownHover = -1;
      cursorStack = null;
      backgroundValid = false;
      dirty = false;
   }
}
