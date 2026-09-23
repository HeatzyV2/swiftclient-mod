# Swift Client Mod

Projet source Fabric pour **Swift Client** (Minecraft 26.2).

---

## Specs

* **Mod ID :** `swiftclient`
* **Version :** `1.0.0`
* **Minecraft :** `26.2` (>= 26.2 < 26.3)
* **Java :** `25`
* **Fabric Loader :** `>= 0.18.4` (used: `0.19.5`)
* **Entrypoint :** `dev.swiftclient.SwiftClient`
* **Homepage :** https://swiftclient.dev

---

## Structure

```text
swiftclient-mod/
├── build.gradle
├── settings.gradle
├── gradle.properties
├── libs/                     # Jar-in-Jar deps (Sodium, Lithium, GeckoLib, …)
└── src/main/
    ├── java/dev/swiftclient/ # Client, core, mixins, UI, cosmetics, …
    └── resources/
        ├── assets/swiftclient/   # textures, fonts, lang, shaders, panoramas
        ├── swiftclient/          # themes.json, partners.json
        ├── fabric.mod.json
        └── swiftclient.client.mixins.json
```

---

## Build

```powershell
.\gradlew.bat compileJava
.\gradlew.bat jar
```

Output: `build/libs/swiftclient-mod-1.0.0.jar`

Gradle resolves Minecraft libraries from `~/.swiftclient/versions/26.2/` and `~/.swiftclient/libraries`.
