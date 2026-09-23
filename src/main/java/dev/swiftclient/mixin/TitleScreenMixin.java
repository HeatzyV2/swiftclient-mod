package dev.swiftclient.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.realmsclient.RealmsMainScreen;
import com.mojang.realmsclient.gui.screens.RealmsNotificationsScreen;
import dev.swiftclient.core.ModCompat;
import dev.swiftclient.core.mods.ModsScreen;
import dev.swiftclient.core.mods.ModuleManager;
import dev.swiftclient.core.platform.Platform;
import dev.swiftclient.core.screen.AccountScreen;
import dev.swiftclient.core.screen.LanguageScreen;
import dev.swiftclient.core.screen.WardrobeScreen;
import dev.swiftclient.core.theme.LcPanorama;
import dev.swiftclient.core.theme.ThemeManager;
import dev.swiftclient.core.theme.ThemePopup;
import dev.swiftclient.core.ui.AccountBadge;
import dev.swiftclient.core.ui.SwiftIntro;
import dev.swiftclient.core.ui.ThemeBadge;
import dev.swiftclient.core.ui.widget.NavRowWidget;
import dev.swiftclient.core.ui.widget.TextLinkWidget;
import dev.swiftclient.platform.CanvasImpl;
import dev.swiftclient.platform.CoreScreenHost;
import dev.swiftclient.ui.CanvasWidget;
import dev.swiftclient.ui.LogoButton;
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
   @Unique
   private final ThemePopup lightclient$themePopup = new ThemePopup();

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
         target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;text(Lnet/minecraft/client/gui/Font;Ljava/lang/String;III)V"
      )}
   )
   private void lightclient$customVersion(GuiGraphicsExtractor context, Font renderer, String text, int x, int y, int color, Operation<Void> original) {
      if (text.startsWith("Minecraft ")) {
         int slash = text.indexOf(47);
         String versionPart = slash >= 0 ? text.substring(0, slash) : text;
         String custom = "Swift Client " + versionPart.substring("Minecraft ".length());
         original.call(new Object[]{context, renderer, custom, x, y, -10066330});
      } else {
         original.call(new Object[]{context, renderer, text, x, y, color});
      }
   }

   @WrapOperation(
      method = {"extractRenderState"},
      at = {@At(
         value = "INVOKE",
         target = "Lcom/mojang/realmsclient/gui/screens/RealmsNotificationsScreen;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V"
      )}
   )
   private void lightclient$skipRealmsNotif(
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
   private GuiEventListener lightclient$skipAllVanillaWidgets(TitleScreen self, GuiEventListener element, Operation<GuiEventListener> original) {
      return ModuleManager.active("vanillaui") ? (GuiEventListener)original.call(new Object[]{self, element}) : element;
   }

   @Unique
   private int lightclient$railX = 0;
   @Unique
   private int lightclient$railW = 220;
   @Unique
   private int lightclient$layoutTop = 28;
   @Unique
   private int lightclient$logoSize = 52;

   @Unique
   private void lightclient$computeLayout() {
      int marginL = ModCompat.essentialLoaded() ? 170 : 24;
      int marginR = ModCompat.essentialLoaded() ? 64 : 24;
      int usable = Math.max(160, this.width - marginL - marginR);
      this.lightclient$railW = Math.min(220, usable);
      this.lightclient$railX = marginL + (usable - this.lightclient$railW) / 2;

      int topPad = 18;
      int bottomPad = 28;
      int avail = Math.max(160, this.height - topPad - bottomPad);

      int logo = 48;
      int afterLogo = 30;
      int ctaH = 32;
      int afterCta = 10;
      int rowH = 22;
      int gap = 3;
      int afterRealms = 12;
      int quitH = 16;
      int secondary = 6;
      int need = logo + afterLogo + ctaH + afterCta + secondary * rowH + (secondary - 1) * gap + afterRealms + quitH;

      if (need > avail) {
         float k = (float)avail / (float)need;
         logo = Math.max(28, Math.round(logo * k));
         afterLogo = Math.max(18, Math.round(afterLogo * k));
         ctaH = Math.max(24, Math.round(ctaH * k));
         afterCta = Math.max(6, Math.round(afterCta * k));
         rowH = Math.max(16, Math.round(rowH * k));
         gap = Math.max(1, Math.round(gap * k));
         afterRealms = Math.max(6, Math.round(afterRealms * k));
         need = logo + afterLogo + ctaH + afterCta + secondary * rowH + (secondary - 1) * gap + afterRealms + quitH;
      }

      // Bias upward: small top breathing room, never pin Quit to the bezel
      int slack = Math.max(0, avail - need);
      this.lightclient$layoutTop = topPad + Math.min(slack / 4, 24);
      this.lightclient$logoSize = logo;
      this.lightclient$afterLogo = afterLogo;
      this.lightclient$ctaH = ctaH;
      this.lightclient$afterCta = afterCta;
      this.lightclient$rowH = rowH;
      this.lightclient$gap = gap;
      this.lightclient$afterRealms = afterRealms;
   }

   @Unique
   private int lightclient$afterLogo = 30;
   @Unique
   private int lightclient$ctaH = 32;
   @Unique
   private int lightclient$afterCta = 10;
   @Unique
   private int lightclient$rowH = 22;
   @Unique
   private int lightclient$gap = 3;
   @Unique
   private int lightclient$afterRealms = 12;

   @Inject(
      method = {"init"},
      at = {@At("TAIL")}
   )
   private void lightclient$addCustomUi(CallbackInfo ci) {
      LcPanorama.ensurePersistedApplied();
      if (ModuleManager.active("vanillaui")) {
         return;
      }

      this.lightclient$computeLayout();
      SwiftIntro.start();

      TitleScreen self = (TitleScreen)(Object)this;
      Minecraft mc = Minecraft.getInstance();

      int railX = this.lightclient$railX;
      int railW = this.lightclient$railW;
      int logo = this.lightclient$logoSize;
      int y = this.lightclient$layoutTop;

      this.addRenderableWidget(new LogoButton(railX + (railW - logo) / 2, y, logo));
      y += logo + this.lightclient$afterLogo;

      int rowH = this.lightclient$rowH;
      int gap = this.lightclient$gap;
      int ctaH = this.lightclient$ctaH;
      int idx = 0;

      this.addRenderableWidget(
         new CanvasWidget(
            railX,
            y,
            railW,
            ctaH,
            Component.translatable("menu.singleplayer"),
            new NavRowWidget(() -> tr("menu.singleplayer"), () -> mc.setScreenAndShow(new SelectWorldScreen(self)), true, idx++)
         )
      );
      y += ctaH + this.lightclient$afterCta;

      this.addRenderableWidget(
         new CanvasWidget(
            railX,
            y,
            railW,
            rowH,
            Component.translatable("menu.multiplayer"),
            new NavRowWidget(() -> tr("menu.multiplayer"), () -> {
               if (mc.options.skipMultiplayerWarning) {
                  mc.setScreenAndShow(new JoinMultiplayerScreen(self));
               } else {
                  mc.setScreenAndShow(new SafetyScreen(self));
               }
            }, false, idx++)
         )
      );
      y += rowH + gap;
      this.addRenderableWidget(
         new CanvasWidget(
            railX,
            y,
            railW,
            rowH,
            Component.translatable("swift.menu.mods"),
            new NavRowWidget(() -> tr("swift.menu.mods"), () -> mc.setScreenAndShow(new CoreScreenHost(new ModsScreen(), self)), false, idx++)
         )
      );
      y += rowH + gap;
      this.addRenderableWidget(
         new CanvasWidget(
            railX,
            y,
            railW,
            rowH,
            Component.translatable("swift.menu.cosmetics"),
            new NavRowWidget(() -> tr("swift.menu.cosmetics"), () -> mc.setScreenAndShow(new CoreScreenHost(new WardrobeScreen(), self)), false, idx++)
         )
      );
      y += rowH + gap;
      this.addRenderableWidget(
         new CanvasWidget(
            railX,
            y,
            railW,
            rowH,
            Component.translatable("menu.options"),
            new NavRowWidget(() -> tr("menu.options"), () -> mc.setScreenAndShow(new OptionsScreen(self, mc.options, false)), false, idx++)
         )
      );
      y += rowH + gap;
      this.addRenderableWidget(
         new CanvasWidget(
            railX,
            y,
            railW,
            rowH,
            Component.translatable("swift.menu.language"),
            new NavRowWidget(() -> tr("swift.menu.language"), () -> mc.setScreenAndShow(new CoreScreenHost(new LanguageScreen(), self)), false, idx++)
         )
      );
      y += rowH + gap;
      this.addRenderableWidget(
         new CanvasWidget(
            railX,
            y,
            railW,
            rowH,
            Component.translatable("menu.online"),
            new NavRowWidget(() -> tr("menu.online"), () -> mc.setScreenAndShow(new RealmsMainScreen(self)), false, idx++)
         )
      );
      y += rowH + this.lightclient$afterRealms;
      this.addRenderableWidget(
         new CanvasWidget(
            railX + (railW - 70) / 2,
            y,
            70,
            16,
            Component.translatable("swift.menu.quit"),
            new TextLinkWidget(() -> tr("swift.menu.quit"), () -> mc.stop(), idx)
         )
      );
   }

   @Inject(
      method = {"extractRenderState"},
      at = {@At("RETURN")}
   )
   private void lightclient$drawSkinPanel(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta, CallbackInfo ci) {
      CanvasImpl c = new CanvasImpl(ctx);
      SwiftIntro.tickSounds();
      if (!ModuleManager.active("vanillaui")) {
         int railX = this.lightclient$railX;
         int railW = this.lightclient$railW;
         int logo = this.lightclient$logoSize;
         int brandY = this.lightclient$layoutTop + logo + Math.max(6, this.lightclient$afterLogo / 3);
         float ba = SwiftIntro.brandAlpha();
         if (ba > 0.02F) {
            String swift = tr("swift.brand.swift");
            String client = tr("swift.brand.client");
            int wordW = c.textWidth(swift) + 6 + c.textWidth(client);
            int bx = railX + (railW - wordW) / 2 + Math.round((1.0F - ba) * 24.0F);
            int a = Math.round(255 * ba) << 24;
            c.text(swift, bx, brandY, -12877066 & 16777215 | a, false);
            c.text(client, bx + c.textWidth(swift) + 6, brandY, -1 & 16777215 | a, false);
            c.fill(bx, brandY + 12, bx + wordW, brandY + 13, 1715176182 & 16777215 | a);
         }

         // Speed streaks behind the logo during Swift In
         float sa = SwiftIntro.streakAlpha();
         if (sa > 0.02F) {
            int logoCx = railX + railW / 2 + SwiftIntro.logoOffsetX();
            int logoCy = this.lightclient$layoutTop + logo / 2;
            int streakA = Math.round(180 * sa) << 24;
            int col = -12877066 & 16777215 | streakA;
            for (int i = 0; i < 5; i++) {
               int ly = logoCy - 14 + i * 7;
               int len = 40 + i * 18;
               int lx = logoCx - len - 20 - i * 8;
               c.fill(lx, ly, lx + len, ly + 1, col);
            }
         }
      }

      if (!ModCompat.essentialLoaded()) {
         AccountBadge.render(c, this.width, mouseX, mouseY);
         ThemeBadge.render(c, this.width, mouseX, mouseY);
      }
      this.lightclient$themePopup.draw(c, this.width, mouseX, mouseY);
      ThemeManager.tick();
      ThemeManager.drawCurtain(c, this.width, this.height);
   }

   @Inject(
      method = {"mouseClicked"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void lightclient$openAccountSwitcher(MouseButtonEvent click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
      if (!ModCompat.essentialLoaded() && AccountBadge.isOnBadge(this.width, click.x(), click.y())) {
         Minecraft.getInstance().setScreenAndShow(new CoreScreenHost(new AccountScreen(), (TitleScreen)(Object)this));
         cir.setReturnValue(true);
      } else if (!ModCompat.essentialLoaded() && ThemeBadge.isOnBadge(this.width, click.x(), click.y())) {
         this.lightclient$themePopup.toggle();
         cir.setReturnValue(true);
      } else if (this.lightclient$themePopup.isOpen() && this.lightclient$themePopup.clickAt(this.width, click.x(), click.y())) {
         cir.setReturnValue(true);
      }
   }
}
