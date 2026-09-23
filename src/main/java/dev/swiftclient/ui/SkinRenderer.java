package dev.swiftclient.ui;

import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;

public class SkinRenderer {
   private static Identifier cachedTexture;
   private static Object cachedSkinTexturesObj;
   private static long cachedAt = 0L;
   private static boolean fetchTriggered = false;

   public static Identifier getSkinTextureId() {
      return getTexture();
   }

   public static Identifier skinIdOf(AbstractClientPlayer p) {
      try {
         return extractBodyIdentifier(p.getSkin());
      } catch (Throwable var2) {
         return null;
      }
   }

   public static Object getCachedSkinTexturesObj() {
      return cachedSkinTexturesObj;
   }

   public static void preloadSkin() {
      getTexture();
   }

   private static Identifier getTexture() {
      long now = System.currentTimeMillis();
      if (cachedTexture != null && now - cachedAt < 60000L) {
         return cachedTexture;
      } else if (fetchTriggered && cachedTexture == null) {
         return null;
      } else {
         try {
            Minecraft mc = Minecraft.getInstance();
            fetchTriggered = true;
            mc.getSkinManager().get(mc.getGameProfile()).thenAccept(opt -> opt.ifPresent(s -> {
               cachedSkinTexturesObj = s;
               Identifier id = extractBodyIdentifier(s);
               if (id != null) {
                  cachedTexture = id;
                  cachedAt = System.currentTimeMillis();
                  System.out.println("[SwiftClient] Skin chargée : " + id);
               }
            }));
         } catch (Throwable var3) {
            var3.printStackTrace();
         }

         return cachedTexture;
      }
   }

   private static Identifier extractBodyIdentifier(Object skinTextures) {
      try {
         RecordComponent[] comps = skinTextures.getClass().getRecordComponents();
         if (comps != null && comps.length != 0) {
            if (comps[0].getType() == Identifier.class) {
               return (Identifier)comps[0].getAccessor().invoke(skinTextures);
            } else {
               Object body = comps[0].getAccessor().invoke(skinTextures);
               if (body == null) {
                  return null;
               } else {
                  List<Identifier> candidates = new ArrayList<>();

                  for (Method im : body.getClass().getMethods()) {
                     if (im.getParameterCount() == 0 && im.getReturnType() == Identifier.class) {
                        Object r = im.invoke(body);
                        if (r != null) {
                           candidates.add((Identifier)r);
                        }
                     }
                  }

                  if (candidates.isEmpty()) {
                     return null;
                  } else {
                     for (Identifier id : candidates) {
                        String path = id.getPath();
                        if (path.startsWith("skins/") || path.contains("/skins/") || path.contains("textures/skin")) {
                           return id;
                        }
                     }

                     return candidates.get(candidates.size() - 1);
                  }
               }
            }
         } else {
            return null;
         }
      } catch (Throwable var9) {
         return null;
      }
   }

   public static void draw(GuiGraphicsExtractor ctx, int x, int y, int w, int h, int mouseX, int mouseY) {
      Minecraft mc = Minecraft.getInstance();
      LivingEntity entity = (LivingEntity)(mc.player != null ? mc.player : PlayerCache.get());
      if (entity == null) {
         drawFullBody(ctx, x, y, w, h);
      } else {
         try {
            int size = Math.min(w / 2, h / 4);
            InventoryScreen.extractEntityInInventoryFollowsMouse(ctx, x, y, x + w, y + h, size, 0.0F, mouseX, mouseY, entity);
         } catch (Throwable var10) {
            drawFullBody(ctx, x, y, w, h);
         }
      }
   }

   public static void drawFullBody(GuiGraphicsExtractor ctx, int x, int y, int w, int h) {
      Identifier tex = getTexture();
      if (tex == null) {
         ctx.centeredText(Minecraft.getInstance().font, "§7chargement skin...", x + w / 2, y + h / 2 - 4, -5592406);
      } else {
         int charW = 16;
         int charH = 32;
         int scale = Math.max(1, Math.min(w / charW, h / charH));
         int totalW = charW * scale;
         int totalH = charH * scale;
         int ox = x + (w - totalW) / 2;
         int oy = y + (h - totalH) / 2;
         drawPart(ctx, tex, ox + 4 * scale, oy, scale, 8, 8, 8, 8);
         drawPart(ctx, tex, ox + 4 * scale, oy, scale, 40, 8, 8, 8);
         drawPart(ctx, tex, ox + 4 * scale, oy + 8 * scale, scale, 20, 20, 8, 12);
         drawPart(ctx, tex, ox + 4 * scale, oy + 8 * scale, scale, 20, 36, 8, 12);
         drawPart(ctx, tex, ox, oy + 8 * scale, scale, 44, 20, 4, 12);
         drawPart(ctx, tex, ox, oy + 8 * scale, scale, 44, 36, 4, 12);
         drawPart(ctx, tex, ox + 12 * scale, oy + 8 * scale, scale, 36, 52, 4, 12);
         drawPart(ctx, tex, ox + 12 * scale, oy + 8 * scale, scale, 52, 52, 4, 12);
         drawPart(ctx, tex, ox + 4 * scale, oy + 20 * scale, scale, 4, 20, 4, 12);
         drawPart(ctx, tex, ox + 4 * scale, oy + 20 * scale, scale, 4, 36, 4, 12);
         drawPart(ctx, tex, ox + 8 * scale, oy + 20 * scale, scale, 20, 52, 4, 12);
         drawPart(ctx, tex, ox + 8 * scale, oy + 20 * scale, scale, 4, 52, 4, 12);
      }
   }

   private static void drawPart(GuiGraphicsExtractor ctx, Identifier tex, int sx, int sy, int scale, int srcU, int srcV, int srcW, int srcH) {
      ctx.blit(RenderPipelines.GUI_TEXTURED, tex, sx, sy, srcU, srcV, srcW * scale, srcH * scale, srcW, srcH, 64, 64);
   }
}
