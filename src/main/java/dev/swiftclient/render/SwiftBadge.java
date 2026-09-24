package dev.swiftclient.render;

import dev.swiftclient.core.badges.BadgeState;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.FontDescription.Resource;
import net.minecraft.resources.Identifier;

public final class SwiftBadge {
   private static final FontDescription FONT = new Resource(Identifier.fromNamespaceAndPath("swiftclient", "badge"));

   private SwiftBadge() {
   }

   public static Component prefix(String grade) {
      if (grade == null) {
         return null;
      } else {
         boolean member = "member".equals(grade);
         String glyph = member ? "\ue000" : "\ue001";
         int rgb = member ? 16777215 : BadgeState.colorFor(grade) & 16777215;
         return Component.empty().append(Component.literal(glyph).setStyle(Style.EMPTY.withFont(FONT).withColor(rgb))).append(Component.literal(" "));
      }
   }
}
