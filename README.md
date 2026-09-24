# Swift Client Mod

Mod Fabric client de **Swift Client** pour Minecraft 26.2.

Les versions (mod, Minecraft, Loader, Fabric API, dépendances) sont définies à un seul endroit :
[`gradle.properties`](gradle.properties). La version du mod est injectée dans `fabric.mod.json` au build.

## Prérequis

* **JDK 25**. Gradle lui-même doit tourner sous Java 25 (exigence de Loom 1.18) :
  si `JAVA_HOME` pointe sur un JDK plus ancien, le build échoue dès la configuration.
* Rien d'autre : Minecraft, ses librairies et toutes les dépendances sont téléchargés par Gradle.
  Aucun launcher installé n'est nécessaire.

## Commandes

```bash
./gradlew build                    # jar dans build/libs/
./gradlew runClient                # lance le jeu en dev (dossier run/)
./gradlew runClient -PauditMixins  # idem + applique tous les mixins au démarrage
./gradlew deployLocal              # copie le jar dans les instances locales (opt-in, destructif)
```

`deployLocal` cible `~/.swiftclient/instances` et `%APPDATA%/SwiftClient/instances`,
ou un autre dossier via `-PdeployDir=<dossier instances>`.

## Configuration à l'exécution

| Propriété JVM | Effet |
|---|---|
| `-Dswiftclient.api=<url>` (ou `SWIFTCLIENT_API`) | Backend (défaut : `http://151.240.30.3:10049`, `off` pour le couper) |
| `-Dswiftclient.nowPlayingFile=<fichier>` | Pont « Now Playing » fourni par le launcher |
| `-Dswiftclient.auditMixins=true` | Applique tous les mixins au premier tick et journalise le résultat |

## Build : Fabric Loom sur MC 26.2 (validé)

MC 26.x est distribué **non obfusqué** : on utilise le plugin `net.fabricmc.fabric-loom`
(variante sans remapping, pas de mappings), comme le template officiel Fabric.

| Élément | Version | Remarque |
|---|---|---|
| Fabric Loom | 1.18.2 | exige **Gradle 9.7+** et un **JVM Gradle 25** |
| Gradle (wrapper) | 9.7.1 | |
| Fabric Loader | 0.19.5 | |
| Fabric API | 0.161.0+26.2 | déclarée dans `fabric.mod.json` (`fabric-api >= 0.161.0`) |
| GeckoLib | 5.5.5 (Modrinth `7gaQHok7`) | embarquée (Jar-in-Jar) |
| Sodium / Lithium / ImmediatelyFast / FerriteCore | 0.9.2 / 0.25.3 / 1.16.4 / 9.0.0 | embarqués (Jar-in-Jar), mêmes artefacts qu'avant |

Vérifié : `build`, `runClient`, audit des 28 mixins sans erreur (dev **et** jar de production),
jars embarqués identiques octet pour octet à l'ancien dossier `libs/`.

Loom affiche `(…) is not valid semver` pour les jars Modrinth embarqués : c'est le nom de version
Modrinth qui n'est pas en semver, les métadonnées des mods eux-mêmes sont correctes. Sans impact.

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
