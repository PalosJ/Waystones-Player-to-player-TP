# Waystones Player

Waystones Player adds online-player destinations to the Warp Stone selection screen. Teleport requests are validated and executed by the server, so the mod must be installed on both the client and server.

## Requirements

- Minecraft 1.21.1
- NeoForge 21.1.229 or newer
- Waystones 21.1.36 or newer
- Balm 21.0.62 or newer
- Java 21

## Behavior

- Player destinations are available only while using a Warp Stone.
- The selected player must still be online when the server handles the request.
- Player teleportation does not require operator permissions or target-player approval.
- Player teleportation intentionally behaves like a direct teleport and does not consume Waystones durability or cooldowns.
- When hunger cost is enabled, saturation is consumed before food level. Creative and spectator players are exempt.

## Upstream Compatibility

- Waystones and Balm dependency ranges have no upper version bound for Minecraft 1.21.1 releases.
- Waystones event integration is loaded as an optional compatibility layer. If a future release removes the currently used API, the addon logs a warning and disables Waystone hunger integration instead of failing base-mod startup.
- Waystones menu internals are accessed through a guarded compatibility boundary. Unsupported future menu layouts disable player destinations rather than preventing the game from loading.
- Client player-button injection is already reflectively isolated, so a renamed or removed Waystones screen skips the addon UI without affecting dedicated-server startup.

## Server Configuration

The server config contains:

- `enableHungerCost`: enables hunger costs for Warp Stone and player teleportation. Default: `true`.
- `foodCostPer500Blocks`: food points charged per 500 horizontal blocks. Range: `1..20`; the final cost is clamped to `1..18`.

## Build

macOS (selects the installed JDK 21 explicitly):

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew clean build --no-daemon --console=plain --warning-mode all
```

Linux (with JDK 21 active):

```bash
./gradlew clean build --no-daemon --console=plain --warning-mode all
```

Windows (with JDK 21 active):

```powershell
.\gradlew.bat clean build --no-daemon --console=plain --warning-mode all
```

The release jar is written to `build/libs/waystonesplayer-<version>.jar`.
