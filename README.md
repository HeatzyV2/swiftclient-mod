# Swift Client Mod

Mod Fabric client de **Swift Client**, construit pour **toutes les versions de Minecraft de 1.21.4 à 26.3**
(1.21.4 … 1.21.11, 26.1.2, 26.2, 26.3) à partir d'**un seul code source**, avec
[Stonecutter](https://stonecutter.kikugie.dev).

## Prérequis

* **JDK 25**. Gradle lui-même doit tourner sous Java 25 (exigence de Loom 1.18) ; les versions 1.21.x sont
  compilées en bytecode Java 21 automatiquement.
* Rien d'autre : Minecraft, ses librairies et toutes les dépendances sont téléchargés par Gradle.

## Où sont les versions

| Fichier | Rôle |
|---|---|
| [`stonecutter.properties.toml`](stonecutter.properties.toml) | **toutes** les versions : mod, Fabric API, mods embarqués, fonctionnalités par version |
| [`settings.gradle.kts`](settings.gradle.kts) | la liste des versions de Minecraft, et la version du code (26.2) |
| [`stonecutter.gradle.kts`](stonecutter.gradle.kts) | les renommages entre versions (classes, packages, méthodes) appliqués automatiquement |
| [`build.gradle.kts`](build.gradle.kts) | le build d'une version, et la table des fonctionnalités optionnelles |

Le code dans `src/` est écrit pour **26.2**. Les différences entre versions passent par :

1. **les tables de renommage** de `stonecutter.gradle.kts` (une classe qui a changé de nom ou de package n'a
   besoin d'aucune condition dans le code) ;
2. **les helpers `dev.swiftclient.compat`** (`Mc`, `Input`, `GuiPose`, `Draw3D`, `Fonts`…) qui cachent les API
   qui ont bougé ;
3. **des conditions Stonecutter** là où il n'y a pas d'autre choix : `//? if >=1.21.9 {` … `//?} else {` … `//?}` ;
4. **les fonctionnalités optionnelles** (`mod.features`) : une fonctionnalité absente d'une version voit ses
   fichiers retirés du build, ses mixins retirés de la config, et son module retiré du menu.

| Fonctionnalité | Versions | Contenu |
|---|---|---|
| `gpu` | 26.2 | shaders maison : flou, PostFx, rectangles arrondis, ciel Atmosphere, Hand Aura, Jump Circles, Custom Glint, Stream Only |
| `submit` | 1.21.9+ | capes, ailes, coiffes, couteaux CS, main 3D, Item Physics / 2D Items / UHC Overlay |
| `skins` | 1.21.9+ | Skin Change |
| `glyphs` | 1.21.11+ | Floating Relics, Relic Particles, Totem Spirit |
| `pets` | 26.1+ | pets GeckoLib |
| `fog` | 1.21.6+ | Fog Customizer |
| `glfw` | jusqu'à 26.2 | Borderless Fullscreen (26.3 passe à SDL3, déjà sans bordure) |

## Commandes

```bash
./gradlew :26.2:build                     # une version : jar dans versions/26.2/build/libs/
./gradlew buildAll                        # toutes les versions, tests compris -> build/libs/<version du mod>/
./gradlew :1.21.4:runClient               # lance le jeu d'une version (dossier run/<version>, run/ pour 26.2)
./gradlew :26.2:runClient -PauditMixins   # idem + applique tous les mixins au démarrage
tools/audit-mixins.sh all                 # lance chaque version, vérifie ses mixins et ferme le jeu
./gradlew :26.2:deployLocal               # copie le jar dans les instances locales (opt-in, destructif)
```

## Sortir une mise à jour

```bash
tools/release-all.sh 2.7.0                       # version, build + tests de toutes les versions, jars rassemblés
tools/release-all.sh 2.7.0 --audit               # idem + lancement de chaque version pour vérifier les mixins
tools/release-all.sh 2.7.0 --publish notes.md    # idem + release GitHub v2.7.0 avec tous les jars
```

Les jars s'appellent `swiftclient-mod-<version>+<minecraft>.jar`. Le jar 26.2 est aussi publié sous l'ancien nom
`swiftclient-mod-<version>.jar`, pour les launchers qui ne connaissent pas encore les jars par version.
`--publish` exige un arbre git propre (version commitée et poussée) et la CLI `gh` connectée.

Ajouter une version de Minecraft : une entrée dans `settings.gradle.kts`, une section dans
`stonecutter.properties.toml`, puis `./gradlew :<version>:compileJava` et `tools/audit-mixins.sh <version>`.

## Configuration à l'exécution

| Propriété JVM | Effet |
|---|---|
| `-Dswiftclient.api=<url>` (ou `SWIFTCLIENT_API`) | Backend (défaut : `https://api.swiftclient.fr`, `off` pour le couper) |
| `-Dswiftclient.nowPlayingFile=<fichier>` | Pont « Now Playing » fourni par le launcher |
| `-Dswiftclient.auditMixins=true` | Applique tous les mixins au premier tick et journalise le résultat |

## Structure

```text
src/main/
├── java/dev/swiftclient/  # client, core, mixins, UI, cosmétiques…
└── resources/
    ├── assets/swiftclient/        # textures, polices, langues, shaders, panoramas
    ├── swiftclient/               # themes.json, partners.json
    ├── fabric.mod.json
    └── swiftclient.client.mixins.json
```
