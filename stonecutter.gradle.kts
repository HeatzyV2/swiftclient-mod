plugins {
    id("dev.kikugie.stonecutter")
    // Loom for 26.x (unobfuscated) and for 1.21.x (remapped with Mojang mappings), picked per version by
    // loom-back-compat. Loom 1.18 needs Gradle 9.7+ running on JDK 25.
    id("net.fabricmc.fabric-loom") version "1.18.4" apply false
    id("net.fabricmc.fabric-loom-remap") version "1.18.4" apply false
}

// Version shown in src/ (the IDE). Switch with the "Set active project to ..." tasks, and switch back to
// the VCS version (26.2) before committing.
stonecutter active "26.2"

/** 26.2 class -> its 26.3 location (com.mojang.renderpearl.api). */
val renderpearl = listOf(
    "com.mojang.blaze3d.GpuFormat" to "com.mojang.renderpearl.api.GpuFormat",
    "com.mojang.blaze3d.PrimitiveTopology" to "com.mojang.renderpearl.api.pipeline.PrimitiveTopology",
    "com.mojang.blaze3d.buffers.GpuBuffer" to "com.mojang.renderpearl.api.buffers.GpuBuffer",
    "com.mojang.blaze3d.buffers.GpuFence" to "com.mojang.renderpearl.api.commands.GpuFence",
    "com.mojang.blaze3d.pipeline.BindGroupLayout" to "com.mojang.renderpearl.api.pipeline.BindGroupLayout",
    "com.mojang.blaze3d.pipeline.BlendFunction" to "com.mojang.renderpearl.api.pipeline.BlendFunction",
    "com.mojang.blaze3d.pipeline.ColorTargetState" to "com.mojang.renderpearl.api.pipeline.ColorTargetState",
    "com.mojang.blaze3d.pipeline.DepthStencilState" to "com.mojang.renderpearl.api.pipeline.DepthStencilState",
    "com.mojang.blaze3d.pipeline.RenderPipeline" to "com.mojang.renderpearl.api.pipeline.RenderPipeline",
    "com.mojang.blaze3d.platform.CompareOp" to "com.mojang.renderpearl.api.pipeline.CompareOp",
    "com.mojang.blaze3d.shaders.UniformType" to "com.mojang.renderpearl.api.pipeline.UniformType",
    "com.mojang.blaze3d.systems.CommandEncoder" to "com.mojang.renderpearl.api.commands.CommandEncoder",
    "com.mojang.blaze3d.systems.GpuDevice" to "com.mojang.renderpearl.api.device.GpuDevice",
    "com.mojang.blaze3d.systems.RenderPass" to "com.mojang.renderpearl.api.commands.RenderPass",
    "com.mojang.blaze3d.textures." to "com.mojang.renderpearl.api.textures.",
    "com.mojang.blaze3d.vertex.VertexFormat;" to "com.mojang.renderpearl.api.vertex.VertexFormat;",
)

/** (version that moved it, old name, name in the code): classes that changed package. */
val packageMoves = listOf(
    Triple("26.1", "net.minecraft.client.GuiMessage", "net.minecraft.client.multiplayer.chat.GuiMessage"),
    Triple("26.1", "net.minecraft.client.gui.render.state", "net.minecraft.client.renderer.state.gui"),
    Triple("26.1", "net.minecraft.client.renderer.state.CameraRenderState", "net.minecraft.client.renderer.state.level.CameraRenderState"),
    Triple("26.1", "net.minecraft.client.renderer.state.LevelRenderState", "net.minecraft.client.renderer.state.level.LevelRenderState"),
    Triple("26.1", "net.minecraft.client.renderer.state.SkyRenderState", "net.minecraft.client.renderer.state.level.SkyRenderState"),
    Triple("26.1", "net.minecraft.client.renderer.block.model.ItemTransform", "net.minecraft.client.resources.model.cuboid.ItemTransform"),
    Triple("1.21.11", "net.minecraft.client.model.PlayerModel", "net.minecraft.client.model.player.PlayerModel"),
    Triple("1.21.11", "net.minecraft.client.renderer.RenderType", "net.minecraft.client.renderer.rendertype.RenderType"),
    // 1.21.11 split RenderType: its factories moved to RenderTypes.
    Triple("1.21.11", "net.minecraft.client.renderer.RenderType", "net.minecraft.client.renderer.rendertype.RenderTypes"),
    Triple("1.21.11", "net.minecraft.Util", "net.minecraft.util.Util"),
    Triple("1.21.11", "net.minecraft.world.entity.projectile.AbstractArrow", "net.minecraft.world.entity.projectile.arrow.AbstractArrow"),
    Triple("1.21.11", "net.minecraft.world.entity.projectile.ThrownTrident", "net.minecraft.world.entity.projectile.arrow.ThrownTrident"),
    Triple("1.21.11", "net.minecraft.world.entity.projectile.ThrownEnderpearl", "net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl"),
    Triple("1.21.9", "net.minecraft.client.resources.PlayerSkin", "net.minecraft.world.entity.player.PlayerSkin"),
    Triple("1.21.6", "net.minecraft.client.renderer.FogRenderer", "net.minecraft.client.renderer.fog.FogRenderer"),
)

/** Other names changed in 26.1 that the code uses: name in the code -> name before 26.1. */
val renames26_1 = mapOf(
    "keymapping" to "keybinding",
    "KeyMappingHelper" to "KeyBindingHelper",
    "registerKeyMapping" to "registerKeyBinding",
    "clientboundPlay" to "playS2C",
    "serverboundPlay" to "playC2S",
    "ContainerInput" to "ClickType",
    "handleContainerInput" to "handleInventoryMouseClick",
    "nonEmptyItemCopyStream" to "nonEmptyStream",
)

/** GUI methods renamed in 26.1 that the code declares or calls: name in the code -> name before 26.1. */
val guiMethodRenames = mapOf(
    "extractWidgetRenderState" to "renderWidget",
    "extractRenderStateWithTooltipAndSubtitles" to "renderWithTooltipAndSubtitles",
    "extractImage" to "renderImage",
    "extractEntityInInventoryFollowsMouse" to "renderEntityInInventoryFollowsMouse",
)

/** Vanilla GUI methods targeted by name in mixins: "extractX" since 26.1, "renderX" before. */
val guiExtractNames = listOf(
    "Slot", "SelectedItemName", "ItemHotbar", "FloatingItem", "Tooltip", "Crosshair", "BossOverlay", "Title",
    "Content", "PingIcon", "ScoreboardSidebar",
)

/** GuiGraphics (GuiGraphicsExtractor since 26.1) methods: name in the code -> name before 26.1. */
val guiGraphicsRenames = mapOf(
    "text" to "drawString",
    "centeredText" to "drawCenteredString",
    "textWithBackdrop" to "drawStringWithBackdrop",
    "item" to "renderItem",
    "itemDecorations" to "renderItemDecorations",
    "outline" to "renderOutline",
)

stonecutter parameters {
    val mc = current.parsed
    // Optional features: `//? if gpu {` in Java (`mod.features` in stonecutter.properties.toml).
    val features = node.project.property("mod.features").toString().split(',').map { it.trim() }
    // Every feature name used in featureSources (build.gradle.kts) is listed here.
    for (f in listOf("fog", "gpu", "glfw", "glyphs", "pets", "skins", "submit")) constants[f] = f in features

    replacements {
        // Classes that were only renamed or moved: replaced here instead of conditions in the code.
        // string(mc >= X) { replace(old, new) } : the code (written for 26.2) gets `old` below X, `new` from X.
        string(mc >= "1.21.11") {
            replace("ResourceLocation", "Identifier")
        }
        // Regex replacements apply their forward pattern to the code for versions >= X and their reverse
        // pattern to the code for older ones. Replacements are literal (no $1), and NEVER is a pattern that
        // matches nothing, for the direction that must leave the code as it is.
        val never = "(?!)"
        // Replacements with the same forward pattern are merged, so each reverse-only one gets its own "never".
        var uid = 0
        fun nv() = never + "#" + uid++
        // Type renames whose new name contains the old one: older versions get the old name back.
        fun rename(since: String, newName: String, oldName: String) = regex(mc >= since) {
            replace(nv(), newName, "\\b$newName\\b", oldName)
        }
        rename("26.1", "GuiGraphicsExtractor", "GuiGraphics")
        // 26.1 renamed the GUI drawing methods (renderX -> extractX, drawString -> text...).
        regex(mc >= "26.1") {
            // Declarations and calls of the screen / widget draw method (entity renderers keep extractRenderState).
            replace(nv(), "x", "\\bextractRenderState(?=\\((?:final )?GuiGraphics|\\(ctx,|\\(graphics,)", "render")
            replace(nv(), "x", ";extractRenderState\\((?=Lnet/minecraft/client/gui/GuiGraphics)", ";render(")
            replace(nv(), "x", "\"extractRenderState\"", "\"render\"")
            for ((new, old) in guiMethodRenames) replace(nv(), "x", "\\b$new\\b", old)
            for (name in guiExtractNames) replace(nv(), "x", "\"extract$name\"", "\"render$name\"")
            // GuiGraphics calls, on the names the code gives it.
            for ((new, old) in guiGraphicsRenames) {
                replace(nv(), "x", "(?<=\\b(?:ctx|g|graphics|context|CTX))\\.$new\\(", ".$old(")
                replace(nv(), "x", "(?<=GuiGraphics(?:Extractor)?;)$new\\(", "$old(")
            }
        }
        // 26.2 split the HUD out of Gui: the HUD class is `Gui` before.
        rename("26.2", "Hud", "Gui")
        // 26.1 renames (vanilla and Fabric API moved to Mojang's names).
        for ((new, old) in renames26_1) rename("26.1", new, old)
        rename("1.21.11", "RenderTypes", "RenderType")
        // 1.21.11: ResourceKey.location() became identifier() with the Identifier rename; ARGB.lerp became srgbLerp.
        rename("1.21.11", "identifier", "location")
        rename("1.21.11", "srgbLerp", "lerp")
        // 1.21.9: Minecraft.setScreenAndShow, record GameProfile (name() / id()), SkinManager.get.
        rename("1.21.9", "setScreenAndShow", "setScreen")
        // 1.21.6: the GUI draws with render pipelines (before: RenderType factories); WorldVersion.name().
        rename("1.21.6", "RenderPipelines.GUI_TEXTURED", "RenderType::guiTextured")
        regex(mc >= "1.21.6") {
            replace(nv(), "x", "import net\\.minecraft\\.client\\.renderer\\.RenderPipelines;", "import net.minecraft.client.renderer.RenderType;")
            // GUI blits take a Function<Identifier, RenderType> instead of a RenderPipeline, in mixin descriptors too.
            replace(nv(), "x", "(?<=GuiGraphics(?:Extractor)?;blit(?:Sprite)?\\()Lcom/mojang/blaze3d/pipeline/RenderPipeline;", "Ljava/util/function/Function;")
        }
        regex(mc >= "1.21.6") {
            replace(nv(), "x", "(?<=getCurrentVersion\\(\\))\\.name\\(\\)", ".getName()")
        }
        regex(mc >= "1.21.9") {
            replace(nv(), "x", "(?<=getGameProfile\\(\\)|getProfile\\(\\))\\.name\\(\\)", ".getName()")
            replace(nv(), "x", "(?<=getGameProfile\\(\\)|getProfile\\(\\))\\.id\\(\\)", ".getId()")
            replace(nv(), "x", "(?<=getSkinManager\\(\\))\\.get\\(", ".getOrLoad(")
        }
        // Classes that changed package: older versions get the old one, in Java and in mixin descriptors.
        fun moved(since: String, old: String, new: String) = regex(mc >= since) {
            replace(nv(), new, "\\b" + Regex.escape(new) + "\\b", old)
            // No leading \b: in descriptors the package follows the "L" of the type.
            replace(nv(), new.replace('.', '/'), Regex.escape(new.replace('.', '/')) + "\\b", old.replace('.', '/'))
        }
        for ((since, old, new) in packageMoves) moved(since, old, new)
        string(mc >= "26.3") {
            // 26.3 moved the GPU API to com.mojang.renderpearl.api (Vulkan backend).
            for ((old, new) in renderpearl) {
                replace(old, new)
                replace(old.replace('.', '/').removeSuffix(";"), new.replace('.', '/').removeSuffix(";"))
            }
        }
        // Renames in a version newer than the code's: only versions >= X are touched.
        fun since(version: String, pattern: String, replacement: String) = regex(mc >= version) {
            replace(pattern, replacement, never, replacement)
        }
        // 26.3: first-person hands moved to a render-state renderer.
        since("26.3", "\\bItemInHandRenderer\\b", "FirstPersonHandsAndItemsRenderer")
        // 26.3: PoseStack.mulPose(Quaternion) became rotate(Quaternion) (mulPose(Matrix4f) stays).
        since(
            "26.3",
            "(?<=\\b(?:pose|poseStack|matrices|stack))\\.mulPose\\((?=Axis\\.|camera\\.orientation|\\(new Quaternionf)",
            ".rotate("
        )
        // 26.3: the generic entity glint is gone; the shield one is the same glint texture and texturing.
        since("26.3", "RenderTypes\\.entityGlint\\(\\)", "RenderTypes.patternedShieldGlint()")
    }
}

// ./gradlew buildAll : builds and tests every version, then gathers the jars in build/libs/<mod version>/
tasks.register("buildAll") {
    group = "build"
    description = "Builds and tests every Minecraft version and collects the jars in build/libs/<version>/."
    dependsOn(stonecutter.versions.map { ":${it.project}:collectJar" })
}
