import groovy.json.JsonOutput
import groovy.json.JsonSlurper

plugins {
    // Remapping Loom (Mojang mappings) on obfuscated versions (1.21.x), plain Loom on 26.x.
    id("dev.kikugie.loom-back-compat")
    id("maven-publish")
}

// This script runs once per Minecraft version (versions/<mc>/). Every version-specific value comes from
// stonecutter.properties.toml.
val mc: String = sc.current.version
val modVersion = property("mod.version").toString()
val geckolib = property("deps.geckolib").toString()

/** 26.x runs on Java 25, 1.21.x on Java 21: the bytecode level follows. */
val javaVersion: Int = if (sc.current.parsed >= "26.1") 25 else 21

/**
 * Optional features: the sources that only exist where the feature does. Where a feature is missing, its
 * sources are left out, the mixin list follows, and src/stubs/<feature>/ supplies no-op versions of the
 * classes that shared code still calls. In Java, `//? if <feature>` (stonecutter.gradle.kts) and
 * dev.swiftclient.compat.Features hide what depends on them.
 */
val featureSources: Map<String, List<String>> = mapOf(
    // Custom GPU passes written for the 26.2+ render API: blur / post effects, rounded-rect shader,
    // Atmosphere sky, hand aura, jump circles, custom glint and the Stream Only overlay.
    "gpu" to listOf(
        "dev/swiftclient/renderer/PostFx.java", "dev/swiftclient/renderer/blur/**", "dev/swiftclient/renderer/rect/**",
        "dev/swiftclient/streamonly/**", "dev/swiftclient/mixin/StreamOverlayFrameMixin.java",
        "dev/swiftclient/mixin/GameRendererBlurMixin.java",
        "dev/swiftclient/visuals/Atmosphere*.java", "dev/swiftclient/visuals/SkyMesh.java", "dev/swiftclient/mixin/Atmosphere*Mixin.java",
        "dev/swiftclient/visuals/HandAura*.java", "dev/swiftclient/visuals/AuraBlur.java", "dev/swiftclient/mixin/HandAura*Mixin.java",
        "dev/swiftclient/visuals/JumpCirclesModule.java", "dev/swiftclient/mixin/JumpCircles*Mixin.java",
        "dev/swiftclient/visuals/CustomGlintModule.java", "dev/swiftclient/mixin/GlintTextureTransformMixin.java",
    ),
    // Relic / totem particle effects, drawn as custom vanilla gizmos (1.21.11+).
    "glyphs" to listOf(
        "dev/swiftclient/visuals/glyph/**", "dev/swiftclient/visuals/RelicGlyphs.java", "dev/swiftclient/visuals/FloatingRelicsModule.java",
        "dev/swiftclient/visuals/RelicParticlesModule.java", "dev/swiftclient/visuals/TotemSpiritModule.java",
    ),
    // What draws through the 1.21.9+ entity rendering (SubmitNodeCollector): capes, wings, head cosmetics,
    // name tag badge on yourself, dropped-item rendering (Item Physics, 2D Items, UHC Overlay), CS knives and
    // the 3D hand.
    "submit" to listOf(
        "dev/swiftclient/mixin/AvatarNameSelfMixin.java", "dev/swiftclient/mixin/CapeLayerMixin.java", "dev/swiftclient/mixin/WingsLayerMixin.java",
        "dev/swiftclient/mixin/HeadCosmeticLayerMixin.java", "dev/swiftclient/mixin/ItemEntityRendererMixin.java",
        "dev/swiftclient/mixin/WorldRendererMixin.java", "dev/swiftclient/mixin/EquipmentLayerRendererMixin.java", "dev/swiftclient/mixin/ItemInHandModelsMixin.java", "dev/swiftclient/mixin/ItemLayerRenderStateAccessor.java",
        "dev/swiftclient/visuals/Knife*.java", "dev/swiftclient/visuals/ModelKnife.java", "dev/swiftclient/visuals/SlashTrail.java",
        "dev/swiftclient/visuals/WeaponInspectModule.java", "dev/swiftclient/visuals/FirstPersonArm.java", "dev/swiftclient/visuals/HandModelModule.java",
        "dev/swiftclient/visuals/HeadCosmeticModule.java", "dev/swiftclient/visuals/Pharaoh*.java", "dev/swiftclient/visuals/VoxelModel.java",
    ),
    // Skin Change: the 1.21.9+ player skin API (ClientAsset textures, PlayerModelType).
    "skins" to listOf("dev/swiftclient/modules/SkinChangeModule.java"),
    // Fog Customizer: the FogData-based fog renderer of 1.21.6+.
    "fog" to listOf("dev/swiftclient/modules/FogModule.java", "dev/swiftclient/mixin/FogRendererMixin.java"),
    // GLFW window tricks (Borderless Fullscreen); 26.3 runs on SDL3 and its fullscreen is already borderless.
    "glfw" to listOf("dev/swiftclient/mixin/BorderlessWindowMixin.java"),
    // GeckoLib pets (no GeckoLib build for 1.21.9).
    "pets" to listOf("dev/swiftclient/pet/**"),
)

/** Features available on this Minecraft version (`mod.features` in stonecutter.properties.toml). */
val features: Set<String> = property("mod.features").toString().split(',').map { it.trim() }.filter { it.isNotEmpty() }.toSet()

val excludedSources: List<String> = featureSources.filterKeys { it !in features }.values.flatten()

version = "$modVersion+$mc"
group = property("mod.group").toString()

base {
    archivesName = "swiftclient-mod"
}

repositories {
    exclusiveContent {
        forRepository { maven("https://api.modrinth.com/maven") { name = "Modrinth" } }
        filter { includeGroup("maven.modrinth") }
    }
    // Test libraries only come from Maven Central.
    exclusiveContent {
        forRepository { mavenCentral() }
        filter {
            includeGroupByRegex("org\\.junit(\\..*)?")
            includeGroup("org.opentest4j")
            includeGroup("org.apiguardian")
        }
    }
}

dependencies {
    minecraft("com.mojang:minecraft:$mc")
    loomx.applyMojangMappings()
    modImplementation("net.fabricmc:fabric-loader:${property("deps.fabric_loader")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${property("deps.fabric_api")}")

    if (geckolib.isNotBlank()) {
        modImplementation("maven.modrinth:geckolib:$geckolib")
        include("maven.modrinth:geckolib:$geckolib")
    }

    testImplementation(platform("org.junit:junit-bom:${property("deps.junit")}"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // Bundled mods, shipped inside the jar and loaded by runClient too: performance, and ViaFabricPlus
    // so a Swift instance joins servers of other versions.
    for (slug in listOf("sodium", "lithium", "immediatelyfast", "ferrite-core", "viafabricplus")) {
        val v = property("deps.${slug.replace("-", "")}").toString()
        if (v.isBlank()) continue
        include("maven.modrinth:$slug:$v")
        // ViaFabricPlus keeps its own libraries in nested jars, which Loom's remapped dev copy does not load
        // (crash at startup in runClient on 1.21.x): there it is only bundled, not run in development.
        if (slug == "viafabricplus" && sc.current.parsed < "26.1") continue
        // -PauditMixins counts every injection, which the bundled mods' optional ones would fail: only ours run.
        if (project.hasProperty("auditMixins")) continue
        modRuntimeOnly("maven.modrinth:$slug:$v")
    }
}

sourceSets.main {
    // Left-out features are dropped from the code, not from their stubs.
    val stubRoot = rootProject.file("src/stubs").absolutePath
    val excluded = excludedSources.map(::glob)
    java.exclude { el -> !el.file.absolutePath.startsWith(stubRoot) && excluded.any { it.matches(el.relativePath.pathString) } }
    for (missing in featureSources.keys - features) {
        val stubs = rootProject.file("src/stubs/$missing")
        if (stubs.isDirectory) java.srcDir(stubs)
    }
}

tasks.test {
    useJUnitPlatform()
    systemProperty("swiftclient.src", rootProject.file("src").absolutePath)
    // ./gradlew test --tests '*UiPreviewTest*' -Ppreview=true : renders the main screens to build/ui-preview/*.png
    if (project.hasProperty("preview")) {
        systemProperty("swiftclient.preview", "1")
        outputs.upToDateWhen { false }
    }
    // ./gradlew test --tests '*LangTest*' -PlangDump=<file> : writes the English texts still missing
    if (project.hasProperty("langDump")) {
        systemProperty("swiftclient.langDump", project.property("langDump")!!)
    }
    // ./gradlew test --tests '*BackendE2ETest*' -Pe2eApi=http://127.0.0.1:8787 -Pe2eRelay=127.0.0.1:18600 -Pe2eGateway=127.0.0.1:18610
    // runs the mod against a real backend started with MOJANG_SESSION_URL=http://127.0.0.1:18555
    if (project.hasProperty("e2eApi")) {
        systemProperty("swiftclient.e2e.api", project.property("e2eApi")!!)
        systemProperty("swiftclient.e2e.relay", project.findProperty("e2eRelay") ?: "")
        systemProperty("swiftclient.e2e.gateway", project.findProperty("e2eGateway") ?: "")
        outputs.upToDateWhen { false }
    }
}

// Values written into fabric.mod.json for this version.
val modJsonProps = mapOf(
    "version" to modVersion,
    "minecraft" to property("mod.mc_compat").toString(),
    "java" to javaVersion.toString(),
    "fabric_api" to property("deps.fabric_api").toString().substringBefore('+'),
)

tasks.processResources {
    inputs.properties(modJsonProps)
    inputs.property("excluded", excludedSources)
    filesMatching("fabric.mod.json") { expand(modJsonProps) }

    // The mixin list drops the excluded mixins and its Java level follows the Minecraft version; without
    // GeckoLib, fabric.mod.json stops requiring it.
    val excludedMixins = excludedSources.filter { it.startsWith("dev/swiftclient/mixin/") }
        .map { glob(it.removePrefix("dev/swiftclient/mixin/").removeSuffix(".java").replace('/', '.')) }
    val level = "JAVA_$javaVersion"
    val noGeckolib = geckolib.isBlank()
    val newPackFormat = sc.current.parsed >= "1.21.9"
    val out = destinationDir
    doLast {
        rewriteJson(File(out, "swiftclient.client.mixins.json")) { json ->
            json["compatibilityLevel"] = level
            for (side in listOf("mixins", "client")) {
                val list = json[side] as? List<*> ?: continue
                json[side] = list.filter { name -> excludedMixins.none { it.matches(name.toString()) } }
            }
        }
        // Built-in resource packs: 1.21.9 replaced pack_format with min_format / max_format.
        out.resolve("resourcepacks").listFiles()?.forEach { pack ->
            val meta = File(pack, "pack.mcmeta")
            if (meta.isFile) rewriteJson(meta) { json ->
                @Suppress("UNCHECKED_CAST")
                val info = json["pack"] as MutableMap<String, Any?>
                info.keys.retainAll(setOf("description"))
                if (newPackFormat) {
                    info["min_format"] = 69
                    info["max_format"] = 999
                } else {
                    info["pack_format"] = 46
                    info["supported_formats"] = mapOf("min_inclusive" to 46, "max_inclusive" to 999)
                }
            }
        }
        if (noGeckolib) rewriteJson(File(out, "fabric.mod.json")) { json ->
            @Suppress("UNCHECKED_CAST")
            (json["depends"] as MutableMap<String, Any?>).remove("geckolib")
        }
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release = javaVersion
    options.compilerArgs.addAll(listOf("-Xmaxerrs", "5000"))
}

java {
    // Gradle and Loom need JDK 25 (Loom 1.18); 1.21.x still gets Java 21 bytecode (options.release).
    toolchain { languageVersion = JavaLanguageVersion.of(25) }
    withSourcesJar()
}

loom {
    runs {
        named("client") {
            // Dev runs stay out of the repo root: run/ for the 26.2 dev version, run/<mc>/ for the others
            // (an older version must never open a world saved by a newer one).
            runDirectory = rootProject.file(if (mc == "26.2") "run" else "run/$mc")
            // ./gradlew runClient -PauditMixins : applies every mixin at startup (see SwiftClient)
            if (project.hasProperty("auditMixins")) {
                jvmArguments.add("-Dswiftclient.auditMixins=true")
                // Every injection that misses is logged instead of crashing at the first one.
                jvmArguments.addAll(listOf("-Dmixin.ignoreRequired=true", "-Dmixin.debug.countInjections=true"))
            }
            // -PauditExit : the game closes itself right after the audit (tools/audit-mixins.sh)
            if (project.hasProperty("auditExit")) jvmArguments.add("-Dswiftclient.auditExit=true")
            // ./gradlew runClient -PdevOpen=mods (or multiplayer) : opens that screen over the title screen
            if (project.hasProperty("devOpen")) jvmArguments.add("-Dswiftclient.devOpen=${project.property("devOpen")}")
            // ./gradlew runClient -PquickPlay=<world> : joins that singleplayer world right away
            if (project.hasProperty("quickPlay")) programArgs("--quickPlaySingleplayer", project.property("quickPlay").toString())
            // ./gradlew runClient -PdevShots : screenshots of the world, then of the mods menu
            if (project.hasProperty("devShots")) jvmArguments.add("-Dswiftclient.devShots=true")
        }
    }
}

// Release jar of this version, named for the launcher: build/libs/<mod version>/swiftclient-mod-<mod>+<mc>.jar
tasks.register<Copy>("collectJar") {
    group = "build"
    description = "Builds and tests this version, then copies its jar to build/libs/<mod version>/."
    dependsOn(tasks.named("test"))
    from(loomx.modJar.flatMap { it.archiveFile })
    into(rootProject.layout.buildDirectory.dir("libs/$modVersion"))
}

// Copies the built jar into local launcher instances. Opt-in only: it deletes older
// Swift Client jars and the .fabric cache of each instance, so it must never run as a
// side effect of `build`. Usage:
//   ./gradlew :26.2:deployLocal                         (~/.swiftclient and %APPDATA%/SwiftClient)
//   ./gradlew :26.2:deployLocal -PdeployDir=<instances> (any other instances folder)
tasks.register("deployLocal") {
    group = "distribution"
    description = "Copies the built jar into every local launcher instance (destructive, opt-in)."
    dependsOn(loomx.modJar)
    doLast {
        val built = loomx.modJar.get().archiveFile.get().asFile
        val roots = if (project.hasProperty("deployDir")) listOf(file(project.property("deployDir")!!))
        else listOf(
            file("${System.getProperty("user.home")}/.swiftclient/instances"),
            file("${System.getenv("APPDATA")}/SwiftClient/instances"),
        )
        roots.filter { it.isDirectory }.forEach { root ->
            root.listFiles()?.filter { it.isDirectory }?.forEach inst@{ inst ->
                val mods = listOf(File(inst, "minecraft/mods"), File(inst, "mods")).find { it.isDirectory } ?: return@inst
                mods.listFiles()?.filter { Regex("(?i).*swiftclient.*\\.jar").matches(it.name) }?.forEach { it.delete() }
                var fabric = File(mods.parentFile, ".fabric")
                if (!fabric.exists()) fabric = File(inst, ".fabric")
                if (fabric.exists()) fabric.deleteRecursively()
                built.copyTo(File(mods, "swiftclient-mod.jar"), overwrite = true)
                println("deployed ${built.name} -> $mods/swiftclient-mod.jar")
            }
        }
    }
}

fun rewriteJson(f: File, edit: (MutableMap<String, Any?>) -> Unit) {
    @Suppress("UNCHECKED_CAST")
    val json = JsonSlurper().parse(f) as MutableMap<String, Any?>
    edit(json)
    f.writeText(JsonOutput.prettyPrint(JsonOutput.toJson(json)))
}

fun glob(pattern: String): Regex =
    Regex(pattern.split('*').joinToString(".*") { Regex.escape(it) })
