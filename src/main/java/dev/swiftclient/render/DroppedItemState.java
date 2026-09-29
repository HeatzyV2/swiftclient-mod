package dev.swiftclient.render;

/** Extra per-frame data on dropped items (added to ItemEntityRenderState by a mixin). */
public interface DroppedItemState {
   boolean swiftclient$onGround();

   void swiftclient$setOnGround(boolean onGround);

   /** UHC Overlay size multiplier, 1 for ordinary items. */
   float swiftclient$scale();

   void swiftclient$setScale(float scale);
}
