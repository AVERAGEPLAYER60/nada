# Base Finder (Fabric, client-side) - Minecraft 26.3

Targets **Minecraft 26.3 / Fabric Loader 0.19.5+ / Fabric API 0.165.0+26.3 / Java 25**.
26.3 is the latest release; 26.4 is still snapshot-only, so there is no stable 26.4 build to target yet.
To retarget later, change `minecraft_version` and `fabric_version` in `gradle.properties`.

## STATUS: NOT COMPILED OR RUN
This was ported to 26.3 without access to the game jars. If the build or the game complains, the most
likely culprits are, in this order:
1. `Compat.java` - screen/overlay calls moved between 26.1, 26.2 and 26.3.
2. `render/BaseFinderRenderer.java` - the Fabric event (`LevelExtractionEvents.END_EXTRACTION`) and the
   vanilla `Gizmos`/`GizmoStyle` calls. Rendering is the area Mojang rewrites most often.
3. The mixins (`CameraMixin`, `KeyboardInputMixin`, ...). They are marked non-fatal: if a target changed,
   the game still starts, that feature just does nothing, and the reason is in `logs/latest.log`.
4. `scan/ChunkScanner.java` - `getMinSectionY`, `getChunkNow`, `hasOnlyAir`, `getStates().count(...)`.

## Getting the jar
### Easiest: let GitHub build it (nothing to install)
1. Make a free GitHub account and a new empty repository.
2. Upload the *contents* of this folder (including the `.github` folder) to it.
3. Open the **Actions** tab, wait for the "build" run to finish (a few minutes).
4. Open the finished run, download **basefinder-jar** under Artifacts, unzip it, and put
   `basefinder-1.0.0.jar` in your `mods` folder (with Fabric Loader and Fabric API for 26.3).
If the run fails, open it and copy the red error text.

### Local build
Install JDK 25 and Gradle 9.6 or newer, then in this folder run `gradle build`.
The jar is `build/libs/basefinder-1.0.0.jar` (not the `-sources` one).

## Controls (rebindable under Options -> Controls -> Key Binds -> Miscellaneous)
| Key | Action |
|-----|--------|
| G   | Toggle Freecam (W/A/S/D move, Space up, Left Shift down, sprint = faster, mouse = look) |
| H   | Toggle Block ESP (chest orange, trapped chest red, ender chest purple, shulker magenta, spawner cyan) |
| J   | Toggle chunk-flag boxes (yellow = flagged, red = strongly flagged) |
| K   | Settings menu: toggles, freecam speed, ESP range, flag threshold, list flagged chunks, rescan |

## Tuning
`Config.java` (scan time budget, thresholds, ESP range/limit, freecam speed) and `Signature.java` (per-block
weights). Defaults are heuristics, not tuned against any real server.

## Limits
* The client only has the chunks the server sends (its view distance); nothing beyond that is visible.
* Your real body stays put while you freecam. It auto-exits freecam on damage.
* Using ESP/freecam on a server may break that server's rules. Check before you use it.
