package dev.swiftclient.platform;

import dev.swiftclient.core.account.AccountEntry;
import dev.swiftclient.core.account.AccountManager;
import dev.swiftclient.core.gfx.Canvas;
import dev.swiftclient.renderer.RoundedButtons;
import dev.swiftclient.ui.PlayerSkin3DRenderer;
import dev.swiftclient.ui.SkinRenderer;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.FontDescription.Resource;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public final class CanvasImpl implements Canvas {
   private final GuiGraphicsExtractor ctx;
   private final Minecraft mc = Minecraft.getInstance();
   private static final FontDescription UI_FONT = new Resource(Identifier.fromNamespaceAndPath("swiftclient", "ui"));
   private static final Style UI_STYLE = Style.EMPTY.withFont(UI_FONT);
   private static final Identifier BTN = Identifier.withDefaultNamespace("widget/button");
   private static final Identifier BTN_HOVER = Identifier.withDefaultNamespace("widget/button_highlighted");
   private static final int ICON_PX = 64;
   private static final Map<String, Identifier> ICONS = new HashMap<>();
   private static final Identifier SWIFT_LOGO = Identifier.fromNamespaceAndPath("swiftclient", "textures/gui/logo.png");
   private static final Identifier STEVE = Identifier.withDefaultNamespace("textures/entity/player/wide/steve.png");

   public CanvasImpl(GuiGraphicsExtractor ctx) {
      this.ctx = ctx;
      RoundedButtons.bind(ctx);
   }

   @Override
   public void fill(int x, int y, int x2, int y2, int argb) {
      this.ctx.fill(x, y, x2, y2, argb);
   }

   @Override
   public void gradientV(int x, int y, int w, int h, int top, int bottom) {
      this.ctx.fillGradient(x, y, x + w, y + h, top, bottom);
   }

   @Override
   public void buttonBackground(int x, int y, int w, int h, boolean hovered) {
      RoundedButtons.drawButtonBackground(x, y, w, h, hovered, true);
   }

   @Override
   public void playerModel(int x, int y, int w, int h, int mouseX, int mouseY, float delta) {
      PlayerSkin3DRenderer.render(this.ctx, x, y, w, h, mouseX, mouseY, delta);
   }

   @Override
   public void roundRect(int x, int y, int w, int h, float radius, int argb) {
      RoundedButtons.drawRoundedRect(x, y, w, h, radius, argb);
   }

   @Override
   public void card(int x, int y, int w, int h, int bg, int border, int thickness, float radius) {
      RoundedButtons.drawCard(x, y, w, h, bg, border, thickness, radius);
   }

   private static Component lc(String s) {
      return Component.literal(s).setStyle(UI_STYLE);
   }

   @Override
   public void text(String s, int x, int y, int argb, boolean shadow) {
      this.ctx.text(this.mc.font, lc(s), x, y, argb, shadow);
   }

   @Override
   public void centeredText(String s, int cx, int y, int argb, boolean shadow) {
      this.ctx.text(this.mc.font, lc(s), cx - this.textWidth(s) / 2, y, argb, shadow);
   }

   @Override
   public boolean richText(Object handle, int x, int y, int argb, boolean shadow) {
      if (handle instanceof Component comp) {
         this.ctx.text(this.mc.font, comp, x, y, argb, shadow);
         return true;
      } else {
         return false;
      }
   }

   @Override
   public int richWidth(Object handle) {
      return handle instanceof Component comp ? this.mc.font.width(comp) : -1;
   }

   @Override
   public boolean itemStack(Object handle, int x, int y) {
      if (handle instanceof ItemStack st && !st.isEmpty()) {
         this.ctx.item(st, x, y);
         return true;
      } else {
         return false;
      }
   }

   @Override
   public int textWidth(String s) {
      return this.mc.font.width(lc(s));
   }

   @Override
   public int lineHeight() {
      return 9;
   }

   @Override
   public void pushScissor(int x, int y, int w, int h) {
      this.ctx.enableScissor(x, y, x + w, y + h);
   }

   @Override
   public void popScissor() {
      this.ctx.disableScissor();
   }

   @Override
   public void pushTranslate(int dx, int dy) {
      this.ctx.pose().pushMatrix();
      this.ctx.pose().translate(dx, dy);
   }

   @Override
   public void popTranslate() {
      this.ctx.pose().popMatrix();
   }

   @Override
   public void pushScale(int ox, int oy, float scale) {
      this.ctx.pose().pushMatrix();
      this.ctx.pose().translate(ox, oy);
      this.ctx.pose().scale(scale, scale);
   }

   @Override
   public void popScale() {
      this.ctx.pose().popMatrix();
   }

   @Override
   public void vanillaButton(int x, int y, int w, int h, boolean hovered) {
      this.ctx.blitSprite(RenderPipelines.GUI_TEXTURED, hovered ? BTN_HOVER : BTN, x, y, w, h);
   }

   @Override
   public void icon(String name, int x, int y, int size, int argb) {
      this.ctx.blit(RenderPipelines.GUI_TEXTURED, iconId(name), x, y, 0.0F, 0.0F, size, size, 64, 64, 64, 64, argb);
   }

   private static Identifier iconId(String name) {
      return ICONS.computeIfAbsent(name, n -> Identifier.fromNamespaceAndPath("swiftclient", "textures/gui/icons/" + n + ".png"));
   }

   @Override
   public void logo(int x, int y, int size) {
      this.ctx.blit(RenderPipelines.GUI_TEXTURED, SWIFT_LOGO, x, y, 0.0F, 0.0F, size, size, size, size, size, size);
   }

   @Override
   public void textureRegion(Object handle, int x, int y, int w, int h, int u, int v, int regionW, int regionH, int texW, int texH) {
      if (handle instanceof Identifier id) {
         this.ctx.blit(RenderPipelines.GUI_TEXTURED, id, x, y, u, v, w, h, regionW, regionH, texW, texH);
      }
   }

   @Override
   public void playerHead(String uuid, int x, int y, int size) {
      Identifier tex = STEVE;
      Optional<AccountEntry> active = AccountManager.get().getActive();
      if (active.isPresent() && active.get().getUuid().toString().equals(uuid)) {
         Identifier cached = SkinRenderer.getSkinTextureId();
         if (cached != null) {
            tex = cached;
         }
      }

      this.ctx.blit(RenderPipelines.GUI_TEXTURED, tex, x, y, 8.0F, 8.0F, size, size, 8, 8, 64, 64);
      this.ctx.blit(RenderPipelines.GUI_TEXTURED, tex, x, y, 40.0F, 8.0F, size, size, 8, 8, 64, 64);
   }
}
