# CurseForge publishing material

Use the exact artifact filename, game versions, loader, and dependency suite from [gradle/targets.json](../gradle/targets.json). Do not broaden a file's compatibility in the CurseForge form.

## Project fields

| Field | Value |
|---|---|
| Project title | Waystones Player-to-player TP |
| Summary | Adds a live online-player directory to the Waystones Warp Stone menu. |
| Main category | Addons |
| Client / server | Required on both |
| License | MIT License |
| Required relations | Waystones, Balm；Fabric files also require Fabric API |
| Unsupported loader | Forge |

## Project description

# Waystones Player-to-player TP

Waystones Player-to-player TP is an unofficial Waystones add-on that lets a Warp Stone target other listed online players. The directory updates live and stays on the left of the original screen, using a full list, compact list, or clickable avatar rail depending on available GUI space.

The client sends only a target UUID. The server revalidates the exact Warp Stone and hand, the corresponding currently online player, experience requirements, and actual movement. Client listing state is not server-side teleport authorization, and this add-on does not promise to prevent custom packets from guessing a third-party-hidden player's UUID. A successful player destination uses Waystones' cancellable events, sound, effects, and native adjacent-or-target-block placement, then applies exactly one point of native Warp Stone durability. Failed or cancelled attempts restore experience, preserve durability, and keep the menu open.

Player destinations carry no safety guarantee and may be inside blocks, in mid-air, in dangerous fluids, or near cliffs.

## Requirements

- Install on both client and server.
- Install matching Waystones and Balm versions on both sides. Fabric files additionally require the matching Fabric API; NeoForge files do not.
- Java 21.
- NeoForge: Minecraft 1.21.1 through 1.21.11.
- Fabric: Minecraft 1.21.1 through 1.21.11.
- Minecraft 1.21.2 and 1.21.3 share one separately tested file per loader.
- Forge is not supported.

Use only the file that exactly matches the instance's loader and Minecraft version. NeoForge files for Minecraft 1.21.2, 1.21.6, 1.21.7, and 1.21.9 depend on official beta NeoForge builds and say so in their file changelog.

## Configuration

`playerTeleportExperienceMode` defaults to `NEVER`. It can keep player travel free, follow Waystones' global experience-cost switch, or always evaluate Waystones' experience point/level formula. Item costs, cooldowns, and unrelated requirements are not inherited.

NeoForge supports its SERVER config and per-world overrides. Fabric uses the same `config/waystonesptpt-server.toml` key and default as a global instance config; it does not claim per-world override support.

## Privacy and license

There is no telemetry, advertising, analytics, or external upload. The only add-on payload contains the UUID selected from the current Minecraft server's listed-player data.

Original code is open source under the [MIT License](https://github.com/PalosJ/waystonesptpt/blob/main/LICENSE). Use, modification, redistribution, and commercial use are permitted while retaining the copyright and license notice. Third-party icon material is excluded from the MIT grant and remains under the separately documented terms.

The existing icon is unchanged. Warp Stone artwork is adapted from **Roguelike/RPG Items** by Joe Williamson (JoeCreates), CC BY-SA 3.0. Complete attribution and the separate terms for player imagery are in [THIRD_PARTY_NOTICES.md](https://github.com/PalosJ/waystonesptpt/blob/main/THIRD_PARTY_NOTICES.md).

Waystones Player-to-player TP is not endorsed by Twelve Iterations.

NOT AN OFFICIAL MINECRAFT PRODUCT. NOT APPROVED BY OR ASSOCIATED WITH MOJANG OR MICROSOFT.

## File upload template

For every JAR:

- Display name: `Waystones Player-to-player TP 1.0.1 – <Loader> <Minecraft target>`.
- Release type: Release.
- Game version(s): exactly the target's `minecraft` array.
- Mod loader: exactly one of NeoForge or Fabric.
- Required dependencies: Waystones and Balm; Fabric files also require Fabric API.
- Changelog: use [CHANGELOG-1.0.1.md](CHANGELOG-1.0.1.md), plus the target dependency block generated from the matrix.
- Verify the local SHA-256 before selecting the file.
- Never upload sources, development JARs, caches, dependency JARs, or a JAR rebuilt against the current stack in place of the minimum-built release binary.
