# CurseForge publishing material

Use the exact artifact filename, game versions, loader, and dependency suite from [gradle/targets.json](../gradle/targets.json). Do not broaden a file's compatibility in the CurseForge form.

## Project fields

| Field | Value |
|---|---|
| Project title | Waystones Player |
| Summary | Adds a live online-player directory to the Waystones Warp Stone menu. |
| Main category | Addons |
| Client / server | Required on both |
| License | Custom License; use the repository `LICENSE` text/URL |
| Required relations | Waystones, Balm |
| Unsupported loader | Forge |

## Project description

# Waystones Player

Waystones Player is an unofficial Waystones add-on that lets a Warp Stone target other listed online players. The directory updates live and stays on the left of the original screen, using a full list, compact list, or clickable avatar rail depending on available GUI space.

The client sends only a target UUID. The server revalidates the exact Warp Stone and hand, the target's listed status, experience requirements, and actual movement. A successful player destination uses Waystones' cancellable events, sound, effects, and adjacent non-suffocating placement, then applies exactly one point of native Warp Stone durability. Failed or cancelled attempts restore experience, preserve durability, and keep the menu open.

Adjacent non-suffocating placement is not a guarantee of solid ground or protection from dangerous fluids and cliffs.

## Requirements

- Install on both client and server.
- Install matching Waystones and Balm versions on both sides.
- Java 21.
- NeoForge: Minecraft 1.21.1 through 1.21.11.
- Fabric: Minecraft 1.21.1 through 1.21.11.
- Minecraft 1.21.2 and 1.21.3 share one separately tested file per loader.
- Forge is not supported.

Use only the file that exactly matches the instance's loader and Minecraft version. NeoForge files for Minecraft 1.21.2, 1.21.6, 1.21.7, and 1.21.9 depend on official beta NeoForge builds and say so in their file changelog.

## Configuration

`playerTeleportExperienceMode` defaults to `NEVER`. It can keep player travel free, follow Waystones' global experience-cost switch, or always evaluate Waystones' experience point/level formula. Item costs, cooldowns, and unrelated requirements are not inherited.

NeoForge supports its SERVER config and per-world overrides. Fabric uses the same `config/waystonesplayer-server.toml` key and default as a global instance config; it does not claim per-world override support.

## Privacy and license

There is no telemetry, advertising, analytics, or external upload. The only add-on payload contains the UUID selected from the current Minecraft server's listed-player data.

Original code is All Rights Reserved. The custom [license](https://github.com/PalosJ/waystonesplayer/blob/main/LICENSE) grants limited end-user permission to download, install, and run an official unmodified JAR. Modification, redistribution, mirroring, modpack bundling, and commercial use require written permission.

The existing icon is unchanged. Warp Stone artwork is adapted from **Roguelike/RPG Items** by Joe Williamson (JoeCreates), CC BY-SA 3.0. Complete attribution and the separate terms for player imagery are in [THIRD_PARTY_NOTICES.md](https://github.com/PalosJ/waystonesplayer/blob/main/THIRD_PARTY_NOTICES.md).

Waystones Player is not endorsed by Twelve Iterations.

NOT AN OFFICIAL MINECRAFT PRODUCT. NOT APPROVED BY OR ASSOCIATED WITH MOJANG OR MICROSOFT.

## File upload template

For every JAR:

- Display name: `Waystones Player 1.0.0 – <Loader> <Minecraft target>`.
- Release type: Release.
- Game version(s): exactly the target's `minecraft` array.
- Mod loader: exactly one of NeoForge or Fabric.
- Required dependencies: Waystones and Balm.
- Changelog: use [CHANGELOG-1.0.0.md](CHANGELOG-1.0.0.md), plus the target dependency block generated from the matrix.
- Verify the local SHA-256 before selecting the file.
- Never upload sources, development JARs, caches, dependency JARs, or a JAR rebuilt against the current stack in place of the minimum-built release binary.
