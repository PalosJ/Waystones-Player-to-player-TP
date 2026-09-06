# CurseForge publishing material (draft)

Use the player description and attribution in [MODRINTH.md](MODRINTH.md), adapting only platform links. This file does not record a completed upload.

| Field | Value |
|---|---|
| Project title | Waystones Player-to-player TP |
| Summary | Adds a player directory to the Waystones Warp Stone menu, enabling player-to-player teleportation. |
| Main category | Addons |
| Client / server | Required on both |
| License | MIT License; third-party icon terms remain separately documented |
| Version | 1.0.1; protocol 2 requires both sides to update |
| Relations | Required: Waystones, Balm; Fabric also Fabric API; 26.x also Shogi |

Only upload a target after the evidence requirements in [VALIDATION.md](VALIDATION.md) pass. Do not describe the complete maintenance matrix as gameplay-verified support. The 26.1.1 targets remain outside the upload-ready list until normal public dependency installation is possible.

For each accepted target:

- Display name: `Waystones Player-to-player TP 1.0.1 – <Loader> <Minecraft target>`.
- Release type: Release.
- Game versions: exactly the target's `minecraft` array; accept 1.21.2 and 1.21.3 separately before uploading their shared file.
- Loader: exactly NeoForge or Fabric; disclose official beta NeoForge requirements where applicable.
- Required relations: select every dependency listed above for this loader/version.
- Changelog: [CHANGELOG-1.0.1.md](CHANGELOG-1.0.1.md), plus exact dependency information from [targets.json](../gradle/targets.json).
- File: the tested minimum binary with matching target, source commit, and SHA-256. Never substitute a current-stack rebuild, sources, development files, or bundled upstream dependencies.

Keep the unchanged icon attribution, unofficial-project notice, and no-telemetry statement from the shared description. Personal receiving preferences are world-specific on both loaders; Fabric cost configuration remains instance-wide and read on restart.
