# Waystones Player 1.0.0

- Adds a live directory of other Minecraft-listed players to the Waystones Warp Stone screen.
- Uses responsive full, compact, and avatar-only layouts while preserving the original Waystones controls.
- Routes player destinations through Waystones events, sound, effects, and adjacent non-suffocating placement.
- Revalidates the exact Warp Stone, hand, `allowsListing()` target authorization, cost, and confirmed movement on the server.
- Applies one point of native Warp Stone durability only after confirmed success.
- Supports `NEVER`, `FOLLOW_WAYSTONES`, and `ALWAYS` experience modes with exact failure rollback.
- Includes NeoForge targets for Minecraft 1.21.1–1.21.11 and Fabric targets for Minecraft 1.21.1–1.21.11.
- Keeps the project/mod version at `1.0.0` for every file.

Minecraft 1.21.2 and 1.21.3 share one separately runtime-tested file per loader. NeoForge targets for Minecraft 1.21.2, 1.21.6, 1.21.7, and 1.21.9 require official beta NeoForge builds. Use the dependency versions listed for the selected file.

Adjacent non-suffocating placement does not guarantee solid ground or exclude dangerous fluids and cliffs.
