# Waystones Player-to-player TP 1.0.1

- Renames the project identity from `waystonesplayer` to `waystonesptpt`, including packages, resources, network IDs, artifacts, and loader metadata.
- Migrates the legacy `waystonesplayer-server.toml` configuration to `waystonesptpt-server.toml` without deleting or overwriting user files.
- Relicenses Waystones Player-to-player TP's original code under the MIT License.
- Keeps third-party icon material under its existing CC BY-SA 3.0 and Minecraft Usage Guidelines terms.
- Updates all 28 NeoForge and Fabric release artifacts and metadata to version `1.0.1`.
- Restores player targeting for every currently online server player, including normally registered fake players, without treating server-list privacy as teleport authorization.
- Removes the add-on's redundant suffocation gate and defers player placement to Waystones' native adjacent-or-target-block destination resolution.
- Preserves the Minecraft target matrix, configuration keys, and network protocol while updating the explicitly reviewed current dependency stacks.

Minecraft 1.21.2 and 1.21.3 continue to share one separately runtime-tested file per loader. Every Minecraft 26.x target remains independently compiled and published.

Player destinations are not safety-checked by this add-on and may be inside blocks, in mid-air, in fluids, or near cliffs.
