package dev.swiftclient.mixin;

import dev.swiftclient.core.mods.ModsScreen;
import dev.swiftclient.core.mods.ModuleManager;
import dev.swiftclient.core.mods.VanillaPauseKeys;
import dev.swiftclient.core.platform.Platform;
import dev.swiftclient.core.screen.HostWorldScreen;
import dev.swiftclient.core.screen.LanguageScreen;
import dev.swiftclient.core.screen.WardrobeScreen;
import dev.swiftclient.core.ui.widget.LabelButtonWidget;
import dev.swiftclient.core.ui.widget.TextLinkWidget;
import dev.swiftclient.platform.CoreScreenHost;
import dev.swiftclient.ui.CanvasWidget;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({PauseScreen.class})
public abstract class GameMenuScreenMixin extends Screen {
   private static final int SKIN_W = 90;
   private static final int SKIN_H = 130;
   private static final int SKIN_GAP = 14;
   private int lightclient$minX = 0;
   private int lightclient$minY = 0;
   private int lightclient$maxY = 0;
   private boolean lightclient$hasBtns = false;

   protected GameMenuScreenMixin(Component title) {
      super(title);
   }

   @Inject(
      method = {"init"},
      at = {@At("TAIL")}
   )
   private void lightclient$customUI(CallbackInfo ci) {
      if (!ModuleManager.active("vanillaui")) {
         PauseScreen self = (PauseScreen)(Object)this;
         Minecraft mc = Minecraft.getInstance();
         List<Button> candidates = new ArrayList<>();

         for (GuiEventListener e : this.children()) {
            if (e instanceof Button b && !b.getMessage().getString().isBlank()) {
               candidates.add(b);
            }
         }

         boolean flooded = candidates.size() > 13;
         List<Component> msgs = new ArrayList<>();
         List<Runnable> acts = new ArrayList<>();
         List<Button> integrated = new ArrayList<>();

         for (Button b : candidates) {
            boolean take = !flooded || VanillaPauseKeys.shouldRestyle(lightclient$key(b.getMessage()), b.getMessage().getString());
            if (take) {
               Button btn = b;
               msgs.add(b.getMessage());
               acts.add(() -> ((ButtonWidgetAccessor)btn).lightclient$getOnPress().onPress(btn));
               integrated.add(btn);
            }
         }

         for (Button bx : integrated) {
            this.removeWidget(bx);
         }

         this.lightclient$layout(msgs, acts);
         this.lightclient$rangerLesRestes();

         // Text links under pause stack — no emoji icon dock
         boolean solo = mc.getSingleplayerServer() != null;
         int linkY = this.height - 28;
         int x = this.width / 2 - 120;
         this.addRenderableWidget(
            new CanvasWidget(
               x,
               linkY,
               50,
               14,
               Component.translatable("swift.menu.mods"),
               new TextLinkWidget(() -> tr("swift.menu.mods"), () -> mc.setScreenAndShow(new CoreScreenHost(new ModsScreen(), self)))
            )
         );
         this.addRenderableWidget(
            new CanvasWidget(
               x + 60,
               linkY,
               80,
               14,
               Component.translatable("swift.menu.cosmetics"),
               new TextLinkWidget(() -> tr("swift.menu.cosmetics"), () -> mc.setScreenAndShow(new CoreScreenHost(new WardrobeScreen(), self)))
            )
         );
         this.addRenderableWidget(
            new CanvasWidget(
               x + 150,
               linkY,
               60,
               14,
               Component.translatable("swift.menu.language"),
               new TextLinkWidget(() -> tr("swift.menu.language"), () -> mc.setScreenAndShow(new CoreScreenHost(new LanguageScreen(), self)))
            )
         );
         if (solo) {
            this.addRenderableWidget(
               new CanvasWidget(
                  x + 220,
                  linkY,
                  50,
                  14,
                  Component.translatable("swift.menu.host"),
                  new TextLinkWidget(() -> tr("swift.menu.host"), () -> mc.setScreenAndShow(new CoreScreenHost(new HostWorldScreen(), self)))
               )
            );
         }
      }
   }

   private static String tr(String key) {
      try {
         return Platform.game().translate(key);
      } catch (Throwable t) {
         return key;
      }
   }

   private void lightclient$rangerLesRestes() {
      if (this.lightclient$hasBtns) {
         for (GuiEventListener e : List.copyOf(this.children())) {
            if (e instanceof AbstractWidget w && !(w instanceof CanvasWidget)) {
               this.removeWidget(w);
            }
         }
      }
   }

   private static String lightclient$key(Component t) {
      return t.getContents() instanceof TranslatableContents tc ? tc.getKey() : null;
   }

   private void lightclient$layout(List<Component> msgs, List<Runnable> acts) {
      int n = msgs.size();
      this.lightclient$hasBtns = n > 0;
      if (n != 0) {
         int fullW = 220;
         int halfW = 106;
         int bh = 28;
         int gap = 8;
         int colGap = 8;
         int leftX = this.width / 2 - fullW / 2;
         int midCount = Math.max(0, n - 2);
         int midRows = (midCount + 1) / 2;
         int totalRows = 1 + midRows + (n >= 2 ? 1 : 0);
         int y0 = this.height / 2 - (totalRows * (bh + gap) - gap) / 2 - 6;
         int yMidStart = y0 + bh + gap;
         int yLast = yMidStart + midRows * (bh + gap);

         for (int i = 0; i < n; i++) {
            Runnable act = acts.get(i);
            Component msg = msgs.get(i);
            int x;
            int y;
            int w;
            if (i == 0) {
               x = leftX;
               y = y0;
               w = fullW;
            } else if (i == n - 1) {
               x = leftX;
               y = yLast;
               w = fullW;
            } else {
               int j = i - 1;
               int row = j / 2;
               int col = j % 2;
               w = halfW;
               x = leftX + col * (halfW + colGap);
               y = yMidStart + row * (bh + gap);
            }

            this.addRenderableWidget(new CanvasWidget(x, y, w, bh, msg, new LabelButtonWidget(msg::getString, act)));
         }

         this.lightclient$minX = leftX;
         this.lightclient$minY = y0;
         this.lightclient$maxY = n >= 2 ? yLast + bh : y0 + bh;
      }
   }

   @Inject(
      method = {"extractRenderState"},
      at = {@At("RETURN")}
   )
   private void lightclient$drawSkinPanel(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta, CallbackInfo ci) {
      // no-op: pause-menu 3D skin removed
   }
}
