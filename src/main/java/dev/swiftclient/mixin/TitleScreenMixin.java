package dev.swiftclient.mixin;

import dev.swiftclient.compat.GuiPose;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.realmsclient.RealmsMainScreen;
import com.mojang.realmsclient.gui.screens.RealmsNotificationsScreen;
import dev.swiftclient.core.ModCompat;
import dev.swiftclient.core.mods.ModsScreen;
import dev.swiftclient.core.mods.ModuleManager;
import dev.swiftclient.core.mods.modules.VanillaUiModule;
import dev.swiftclient.core.platform.Platform;
import dev.swiftclient.core.screen.AccountScreen;
import dev.swiftclient.core.screen.LanguageScreen;
import dev.swiftclient.core.screen.WardrobeScreen;
import dev.swiftclient.core.theme.SwiftPanorama;
import dev.swiftclient.core.ui.AccountBadge;
import dev.swiftclient.core.ui.SwiftIntro;
import dev.swiftclient.core.ui.Px;
import dev.swiftclient.core.ui.widget.SwiftMenuButton;
import dev.swiftclient.platform.CanvasImpl;
import dev.swiftclient.platform.CoreScreenHost;
import dev.swiftclient.ui.CanvasWidget;
import java.util.Locale;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.SafetyScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
//? if >=1.21.9
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({TitleScreen.class})
public abstract class TitleScreenMixin extends Screen {
   protected TitleScreenMixin(Component title) {
      super(title);
   }

   @Unique
   private static String tr(String key) {
      try {
         return Platform.game().translate(key);
      } catch (Throwable t) {
         return key;
      }
   }

   @WrapOperation(
      method = {"extractRenderState"},
      at = {@At(
         value = "INVOKE",
         //? if >=1.21.6 {
         target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;text(Lnet/minecraft/client/gui/Font;Ljava/lang/String;III)V"
         //?} else
         /*target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;text(Lnet/minecraft/client/gui/Font;Ljava/lang/String;III)I"*/
      )}
   )
   //? if >=1.21.6 {
   private void swiftclient$customVersion(GuiGraphicsExtractor context, Font renderer, String text, int x, int y, int color, Operation<Void> original) {
   //?} else
   /*private int swiftclient$customVersion(GuiGraphicsExtractor context, Font renderer, String text, int x, int y, int color, Operation<Integer> original) {*/
      String shown = text;
      int shownColor = color;
      if (text.startsWith("Minecraft ")) {
         int slash = text.indexOf(47);
         String versionPart = slash >= 0 ? text.substring(0, slash) : text;
         shown = "Swift Client " + versionPart.substring("Minecraft ".length());
         shownColor = -10066330;
      }

      //? if >=1.21.6 {
      original.call(new Object[]{context, renderer, shown, x, y, shownColor});
      //?} else
      /*return original.call(new Object[]{context, renderer, shown, x, y, shownColor});*/
   }

   @WrapOperation(
      method = {"extractRenderState"},
      at = {@At(
         value = "INVOKE",
         target = "Lcom/mojang/realmsclient/gui/screens/RealmsNotificationsScreen;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V"
      )}
   )
   private void swiftclient$skipRealmsNotif(
      RealmsNotificationsScreen self, GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta, Operation<Void> original
   ) {
   }

   @WrapOperation(
      method = {"init", "createNormalMenuOptions", "createTestWorldButton", "createDemoMenuOptions"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/screens/TitleScreen;addRenderableWidget(Lnet/minecraft/client/gui/components/events/GuiEventListener;)Lnet/minecraft/client/gui/components/events/GuiEventListener;"
      )}
   )
   private GuiEventListener swiftclient$skipAllVanillaWidgets(TitleScreen self, GuiEventListener element, Operation<GuiEventListener> original) {
      return ModuleManager.get(VanillaUiModule.class).isEnabled() ? (GuiEventListener)original.call(new Object[]{self, element}) : element;
   }

   @Unique
   private int swiftclient$colX = 24;
   @Unique
   private int swiftclient$colW = 210;
   @Unique
   private int swiftclient$top = 28;
   @Unique
   private int swiftclient$logo = 40;
   @Unique
   private int swiftclient$afterLogo = 16;
   @Unique
   private int swiftclient$ctaH = 30;
   @Unique
   private int swiftclient$rowH = 22;
   @Unique
   private int swiftclient$gap = 4;
   @Unique
   private int swiftclient$smallH = 18;

   /** Left column: the menu sits on the left so the panorama stays free on the right. */
   @Unique
   private void swiftclient$computeLayout() {
      int marginL = ModCompat.essentialLoaded() ? 170 : Math.max(24, Math.round(this.width * 0.06F));
      this.swiftclient$colX = marginL;
      this.swiftclient$colW = Math.max(150, Math.min(190, this.width - marginL - 24));

      int logo = 42;
      int afterLogo = 16;
      int ctaH = 30;
      int afterCta = 8;
      int rowH = 22;
      int gap = 4;
      int afterRows = 8;
      int smallH = 18;
      int rows = 4;
      int need = logo + afterLogo + ctaH + afterCta + rows * rowH + (rows - 1) * gap + afterRows + smallH;
      int avail = Math.max(150, this.height - 44);
      if (need > avail) {
         float k = (float)avail / need;
         logo = Math.max(26, Math.round(logo * k));
         afterLogo = Math.max(8, Math.round(afterLogo * k));
         ctaH = Math.max(22, Math.round(ctaH * k));
         rowH = Math.max(16, Math.round(rowH * k));
         gap = Math.max(2, Math.round(gap * k));
         smallH = Math.max(14, Math.round(smallH * k));
         need = logo + afterLogo + ctaH + afterCta + rows * rowH + (rows - 1) * gap + afterRows + smallH;
      }

      this.swiftclient$top = Math.max(16, (this.height - need) / 2 - 6);
      this.swiftclient$logo = logo;
      this.swiftclient$afterLogo = afterLogo;
      this.swiftclient$ctaH = ctaH;
      this.swiftclient$rowH = rowH;
      this.swiftclient$gap = gap;
      this.swiftclient$smallH = smallH;
   }

   @Unique
   private CanvasWidget swiftclient$button(int x, int y, int w, int h, String key, String icon, Runnable click, SwiftMenuButton.Style style, int index) {
      return new CanvasWidget(x, y, w, h, Component.translatable(key), new SwiftMenuButton(() -> tr(key), icon, click, style, index));
   }

   @Inject(
      method = {"init"},
      at = {@At("TAIL")}
   )
   private void swiftclient$addCustomUi(CallbackInfo ci) {
      SwiftPanorama.applyWhenReady();
      if (ModuleManager.get(VanillaUiModule.class).isEnabled()) {
         return;
      }

      this.swiftclient$computeLayout();
      SwiftIntro.start();

      TitleScreen self = (TitleScreen)(Object)this;
      Minecraft mc = Minecraft.getInstance();
      int x = this.swiftclient$colX;
      int w = this.swiftclient$colW;
      int y = this.swiftclient$top;
      int logo = this.swiftclient$logo;

      y += logo + this.swiftclient$afterLogo;

      this.addRenderableWidget(this.swiftclient$button(x, y, w, this.swiftclient$ctaH, "menu.singleplayer", "play",
         () -> mc.setScreenAndShow(new SelectWorldScreen(self)), SwiftMenuButton.Style.PRIMARY, 0));
      y += this.swiftclient$ctaH + 8;

      int rowH = this.swiftclient$rowH;
      int gap = this.swiftclient$gap;
      this.addRenderableWidget(this.swiftclient$button(x, y, w, rowH, "menu.multiplayer", "globe", () -> {
         if (mc.options.skipMultiplayerWarning) {
            mc.setScreenAndShow(new JoinMultiplayerScreen(self));
         } else {
            mc.setScreenAndShow(new SafetyScreen(self));
         }
      }, SwiftMenuButton.Style.ROW, 1));
      y += rowH + gap;
      this.addRenderableWidget(this.swiftclient$button(x, y, w, rowH, "swift.menu.mods", "mods",
         () -> mc.setScreenAndShow(new CoreScreenHost(new ModsScreen(), self)), SwiftMenuButton.Style.ROW, 2));
      y += rowH + gap;
      this.addRenderableWidget(this.swiftclient$button(x, y, w, rowH, "swift.menu.cosmetics", "shirt",
         () -> mc.setScreenAndShow(new CoreScreenHost(new WardrobeScreen(), self)), SwiftMenuButton.Style.ROW, 3));
      y += rowH + gap;
      this.addRenderableWidget(this.swiftclient$button(x, y, w, rowH, "menu.options", "gear",
         () -> mc.setScreenAndShow(new OptionsScreen(self, mc.options/*? if >=26.1 <26.3 {*/, false/*?}*/)), SwiftMenuButton.Style.ROW, 4));
      y += rowH + 8;

      int sh = this.swiftclient$smallH;
      int sgap = 4;
      int sw = (w - 2 * sgap) / 3;
      this.addRenderableWidget(this.swiftclient$button(x, y, sw, sh, "swift.menu.language", "globe",
         () -> mc.setScreenAndShow(new CoreScreenHost(new LanguageScreen(), self)), SwiftMenuButton.Style.SMALL, 5));
      this.addRenderableWidget(this.swiftclient$button(x + sw + sgap, y, sw, sh, "swift.menu.realms", "cloud",
         () -> mc.setScreenAndShow(new RealmsMainScreen(self)), SwiftMenuButton.Style.SMALL, 6));
      this.addRenderableWidget(this.swiftclient$button(x + 2 * (sw + sgap), y, w - 2 * (sw + sgap), sh, "swift.menu.quit", "close",
         () -> mc.stop(), SwiftMenuButton.Style.DANGER, 7));
   }

   /** Dark veil behind the column, fading out to the right, so the menu reads on any panorama. */
   @Inject(
      method = {"extractRenderState"},
      at = {@At("HEAD")}
   )
   private void swiftclient$veil(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta, CallbackInfo ci) {
      if (ModuleManager.get(VanillaUiModule.class).isEnabled()) {
         return;
      }

      int solid = this.swiftclient$colX + this.swiftclient$colW / 2;
      int fade = this.swiftclient$colW + 120;
      ctx.fill(0, 0, solid, this.height, 0xA0060910);
      int steps = 24;
      for (int i = 0; i < steps; i++) {
         int x0 = solid + fade * i / steps;
         int x1 = solid + fade * (i + 1) / steps;
         int alpha = Math.round(0xA0 * (1.0F - (i + 0.5F) / steps));
         ctx.fill(x0, 0, x1, this.height, alpha << 24 | 0x060910);
      }
   }

   @Inject(
      method = {"extractRenderState"},
      at = {@At("RETURN")}
   )
   private void swiftclient$drawSkinPanel(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta, CallbackInfo ci) {
      CanvasImpl c = new CanvasImpl(ctx);
      SwiftIntro.tickSounds();
      if (!ModuleManager.get(VanillaUiModule.class).isEnabled()) {
         this.swiftclient$wordmark(ctx, c);
      }

      if (!ModCompat.essentialLoaded()) {
         AccountBadge.render(c, this.width, mouseX, mouseY);
      }
   }

   /** Height of a capital of the bold UI font at scale 1, and its distance below the text origin. */
   @Unique
   private static final float SWIFTCLIENT$CAP_H = 6.25F;
   @Unique
   private static final float SWIFTCLIENT$CAP_TOP = 0.85F;
   /** Slant of the S in logo.png (its edges move 0.21 px right per px up): the name leans the same way. */
   @Unique
   private static final float SWIFTCLIENT$SLANT = 0.21F;

   /**
    * SWIFT CLIENT in large bold capitals, slanted like the S of the logo, with the streak and the game version
    * underneath.
    */
   @Unique
   private void swiftclient$wordmark(GuiGraphicsExtractor ctx, CanvasImpl c) {
      float ba = SwiftIntro.brandAlpha();
      if (ba <= 0.02F) {
         return;
      }

      int head = this.swiftclient$logo;
      Font font = Minecraft.getInstance().font;
      net.minecraft.network.chat.Style bold = dev.swiftclient.compat.Fonts.style(net.minecraft.resources.Identifier.fromNamespaceAndPath("swiftclient", "ui_title"));
      Component swift = Component.literal(tr("swift.brand.swift").toUpperCase(Locale.ROOT)).setStyle(bold);
      Component client = Component.literal(tr("swift.brand.client").toUpperCase(Locale.ROOT)).setStyle(bold);
      int gap = 3;
      // As tall as half the header, narrowed if needed so the slanted name stays within the button column
      float capH = head * 0.5F;
      float k = capH / SWIFTCLIENT$CAP_H;
      float units = font.width(swift) + gap + font.width(client) + SWIFTCLIENT$SLANT * SWIFTCLIENT$CAP_H;
      if (units * k > this.swiftclient$colW) {
         k = this.swiftclient$colW / units;
         capH = k * SWIFTCLIENT$CAP_H;
      }

      float capTop = this.swiftclient$top + (head - capH) * 0.35F;
      float startX = this.swiftclient$colX + SWIFTCLIENT$SLANT * capH - (1.0F - ba) * 24.0F;
      int a = Math.round(255 * ba) << 24;

      var pose = GuiPose.of(ctx);
      pose.pushMatrix();
      pose.translate(startX, capTop - SWIFTCLIENT$CAP_TOP * k);
      pose.mul(new org.joml.Matrix3x2f(1.0F, 0.0F, -SWIFTCLIENT$SLANT, 1.0F, 0.0F, 0.0F));
      pose.scale(k, k);
      ctx.text(font, swift, 0, 0, Px.ACCENT & 0xFFFFFF | a, false);
      ctx.text(font, client, font.width(swift) + gap, 0, 0xFFFFFF | a, false);
      pose.popMatrix();

      int under = Math.round(capTop + capH + 6.0F);
      int x = Math.round(startX - SWIFTCLIENT$SLANT * (capH + 6.0F));
      Px.dashes(c, x, under, Px.ACCENT & 0xFFFFFF | a);
      String version = SharedConstants.getCurrentVersion().name();
      int tx = x + 36;
      Px.chamfer(c, tx, under - 5, c.textWidth(version) + 10, 11, 3, Math.round(0xD0 * ba) << 24 | 0x232A3A);
      c.text(version, tx + 5, under - 3, 0xB4BDCC | a, false);
   }

   @Inject(
      method = {"mouseClicked"},
      at = {@At("HEAD")},
      cancellable = true
   )
   //? if >=1.21.9 {
   private void swiftclient$openAccountSwitcher(MouseButtonEvent click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
      double mouseX = click.x();
      double mouseY = click.y();
   //?} else {
   /*private void swiftclient$openAccountSwitcher(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
   *///?}
      if (!ModCompat.essentialLoaded() && AccountBadge.isOnBadge(this.width, mouseX, mouseY)) {
         Minecraft.getInstance().setScreenAndShow(new CoreScreenHost(new AccountScreen(), (TitleScreen)(Object)this));
         cir.setReturnValue(true);
      }
   }
}
