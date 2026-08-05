# Waystones Player

[简体中文](#简体中文) | [English](#english)

## 简体中文

Waystones Player 会在传送石（Warp Stone）的选择界面中加入在线玩家目的地。所有传送请求均由服务端验证并执行，因此客户端和服务端都必须安装本模组。

### 环境要求

- Minecraft 1.21.1
- NeoForge 21.1.229 或更高版本
- Waystones 21.1.36 或更高版本
- Balm 21.0.62 或更高版本
- Java 21

### 功能行为

- 玩家目的地仅在使用传送石（Warp Stone）时可用。
- 服务端处理请求时，所选玩家必须仍然在线。
- 传送至玩家不需要管理员权限，也无需目标玩家确认。
- 玩家传送被有意设计为直接传送，不会产生 Waystones 的耐久消耗或冷却。
- 启用饥饿消耗后，会先扣除饱和度，再扣除食物值；创造模式和旁观模式玩家不受影响。

### 上游兼容性

- 对于 Minecraft 1.21.1 版本，Waystones 和 Balm 的依赖范围均不设置版本上限。
- Waystones 事件集成以可选兼容层的形式加载。如果未来版本移除了当前使用的 API，本附属模组会记录警告并禁用与 Waystones 事件相关的饥饿消耗集成，而不会导致本模组无法启动。
- 对 Waystones 菜单内部实现的访问集中在带防护的兼容边界中。未来不受支持的菜单布局只会禁用玩家目的地，不会阻止游戏加载。
- 客户端玩家按钮注入已通过反射隔离；如果 Waystones 界面被重命名或移除，本附属模组会跳过相关界面功能，且不会影响专用服务端启动。

### 服务端配置

服务端配置包含：

- `enableHungerCost`：启用传送石和玩家传送的饥饿值消耗。默认值：`true`。
- `foodCostPer500Blocks`：每 500 格水平距离扣除的食物值。可配置范围：`1..20`；最终消耗会限制在 `1..18` 范围内。

### 构建

macOS（显式选择已安装的 JDK 21）：

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew clean build --no-daemon --console=plain --warning-mode all
```

Linux（已启用 JDK 21）：

```bash
./gradlew clean build --no-daemon --console=plain --warning-mode all
```

Windows（已启用 JDK 21）：

```powershell
.\gradlew.bat clean build --no-daemon --console=plain --warning-mode all
```

发行 JAR 位于 `build/libs/waystonesplayer-<version>.jar`。

---

## English

Waystones Player adds online-player destinations to the Warp Stone selection screen. Teleport requests are validated and executed by the server, so the mod must be installed on both the client and server.

### Requirements

- Minecraft 1.21.1
- NeoForge 21.1.229 or newer
- Waystones 21.1.36 or newer
- Balm 21.0.62 or newer
- Java 21

### Behavior

- Player destinations are available only while using a Warp Stone.
- The selected player must still be online when the server handles the request.
- Player teleportation does not require operator permissions or target-player approval.
- Player teleportation intentionally behaves like a direct teleport and does not consume Waystones durability or cooldowns.
- When hunger cost is enabled, saturation is consumed before food level. Creative and spectator players are exempt.

### Upstream Compatibility

- Waystones and Balm dependency ranges have no upper version bound for Minecraft 1.21.1 releases.
- Waystones event integration is loaded as an optional compatibility layer. If a future release removes the currently used API, the addon logs a warning and disables Waystone hunger integration instead of failing base-mod startup.
- Waystones menu internals are accessed through a guarded compatibility boundary. Unsupported future menu layouts disable player destinations rather than preventing the game from loading.
- Client player-button injection is already reflectively isolated, so a renamed or removed Waystones screen skips the addon UI without affecting dedicated-server startup.

### Server Configuration

The server config contains:

- `enableHungerCost`: enables hunger costs for Warp Stone and player teleportation. Default: `true`.
- `foodCostPer500Blocks`: food points charged per 500 horizontal blocks. Range: `1..20`; the final cost is clamped to `1..18`.

### Build

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
