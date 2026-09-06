# Waystones Player-to-player TP 1.0.1

Version remains **1.0.1**. Client and server must both install this build: network protocol is now **2**, including older installations already labeled 1.0.1.

- Adds the menu-only “Allow others to teleport to me” setting, enabled by default and saved by UUID in the server/world. Disabled targets stay listed; outgoing travel still works.
- Avoids the legacy Balm/NeoForge abstract server tick listener failure; lifecycle checks run once on the server thread.
- Honors the native 1.21.x durability switch as well as the 26.x damage rules.
- Guards each player's teleport with one pending transaction, a 200-server-tick preparation limit, and final session, menu, item, target, preference, position, and payment checks. Late callbacks cannot revive cancelled requests.
- Updates moving targets before execution and recalculates costs. Failed uncommitted requests do not overwrite experience earned while waiting.
- Recognizes native Warp Stone item types, including the variants available in 26.x. Preserves applicable charge, durability, creative, and enchantment behavior.
- Fixes 26.x native button backgrounds and states, pagination control rebuilding, scrolling layouts, and blocked-neighbor destination fallback.
- Supports complete current Waystones/Shogi rules, including warpSettings and fleeting memorial predicates. Checks actual numeric costs, handles rule reloads, and settles configured item damage once after success. Fixes early Shogi offhand item inheritance for player teleports.
- Refreshes delayed player skins without querying profiles during rendering.
- Restricts NeoForge legacy configuration migration to this add-on; normal world overrides and reloads remain owned by the loader.
- Retains online server-side target resolution for normally registered fake players; client listing is not teleport authorization.
- Keeps the renamed WaystonesPTPT mod ID, configuration migration, MIT original-code license, and unchanged third-party icon from the earlier 1.0.1 work.
- Updates current Waystones/Balm suites for 26.1.2 and 26.2 without raising existing minimum requirements.

28 maintenance artifacts cover 30 Minecraft/loader combinations. This changelog is not a claim of complete gameplay acceptance: see [VALIDATION.md](VALIDATION.md). The shared 1.21.2/1.21.3 JAR requires separate acceptance on both versions. 26.1.1 remains outside the upload-ready list until matching dependencies are publicly installable.

Player placement prefers an adjacent position and falls back to the target block center; targets may be inside blocks, in mid-air, in fluids, or near cliffs.
