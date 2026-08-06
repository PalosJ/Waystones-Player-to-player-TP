# Waystones Player

An unofficial add-on for [Waystones](https://modrinth.com/mod/waystones) that adds online players as destinations in the Warp Stone menu.

[简体中文](#简体中文) | [English](#english)

## 简体中文

Waystones Player 是 [Waystones（传送石碑）](https://modrinth.com/mod/waystones) 的非官方附属模组。它会在传送石（Warp Stone）的选择界面中加入在线玩家列表，让玩家无需管理员权限或目标玩家确认，直接传送到另一名在线玩家的位置。

### 安装要求

本模组必须同时安装在客户端和服务端。

- Minecraft 1.21.1
- NeoForge 21.1.229 或更高版本
- Waystones 21.1.36 或更高版本（开发版本使用并验证至 21.1.39）
- [Balm](https://modrinth.com/mod/balm) 21.0.62 或更高版本
- Java 21

将 Waystones Player、Waystones 和 Balm 的 JAR 放入客户端与服务端的 `mods` 目录后启动游戏。

### 使用方法

1. 手持 Waystones 的传送石并完成蓄力。
2. 在传送目的地界面左侧选择一名在线玩家；窄屏界面可通过玩家按钮展开列表。
3. 服务端重新验证传送石、目标与费用后执行传送。

玩家传送不会要求 OP 权限或目标玩家确认，也不会套用 Waystones 的传送冷却、物品费用或其他目的地限制。每次成功传送会对打开界面时使用的传送石造成 1 点原生耐久伤害；创造模式、耐久附魔与物品损坏行为遵循 Minecraft 和 Waystones 的原生规则。失败请求不会扣除经验或耐久。

### 服务端经验配置

首次启动后，默认配置文件位于服务端或客户端实例的 `config/waystonesplayer-server.toml`。如需按世界覆盖，可使用专用服务器的 `world/serverconfig/waystonesplayer-server.toml`，或单人世界的 `saves/<世界名>/serverconfig/waystonesplayer-server.toml`。建议关闭服务器后修改并重新启动。

`playerTeleportExperienceMode` 提供三个互斥模式：

| 模式 | 行为 |
|---|---|
| `NEVER` | 默认值。玩家传送不消耗经验。 |
| `FOLLOW_WAYSTONES` | Waystones 启用传送费用时，按其当前经验点数/等级、距离、跨维度和上下限规则收费；关闭费用时不收费。 |
| `ALWAYS` | 无论 Waystones 的费用总开关是否开启，始终计算其当前经验规则。 |

本附属只提取 Waystones 的经验点数和经验等级要求。若服务器自定义规则没有产生经验费用，计算结果仍可能为零。

### 兼容性与隐私

- 对 Waystones 菜单和经验规则内部结构的访问集中在兼容层中；不兼容时会拒绝需要经验计算的玩家传送，而不会静默绕过费用。
- 本模组不包含遥测、广告、分析服务或外部数据上传。玩家选择时仅将目标 UUID 发送给当前连接的 Minecraft 服务端。
- 本模组不会打包或重新分发 Waystones、Balm 的代码、JAR、图标或其他资产。

发布后的问题请通过对应 Modrinth/CurseForge 项目页面提供的反馈渠道提交，并附上 Minecraft、NeoForge、Waystones、Balm 与本模组版本以及相关日志。

### 构建

使用 Java 21：

```bash
./gradlew clean build --no-daemon --console=plain --warning-mode all
```

macOS 可显式选择 JDK 21：

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew clean build --no-daemon --console=plain --warning-mode all
```

发行 JAR 位于 `build/libs/waystonesplayer-<version>.jar`。

### 许可与声明

Waystones Player 采用 All Rights Reserved，详见 `LICENSE`。Waystones、Balm 及其名称和资产归各自权利人所有。本项目不是 Twelve Iterations 的官方项目，也不代表其认可或背书。

---

## English

Waystones Player is an unofficial add-on for [Waystones](https://modrinth.com/mod/waystones). It adds online players to the Warp Stone destination screen, allowing direct player-to-player teleportation without operator permissions or target confirmation.

### Requirements

The mod must be installed on both the client and server.

- Minecraft 1.21.1
- NeoForge 21.1.229 or newer
- Waystones 21.1.36 or newer (development is built and verified through 21.1.39)
- [Balm](https://modrinth.com/mod/balm) 21.0.62 or newer
- Java 21

Place the Waystones Player, Waystones, and Balm JARs in the `mods` directory on both sides, then start the game.

### Usage

1. Hold and finish charging a Waystones Warp Stone.
2. Select an online player from the panel beside the destination list; on narrow screens, open it with the player button.
3. The server revalidates the Warp Stone, target, and costs before teleporting.

Player destinations require neither operator permissions nor target approval. They do not inherit Waystones cooldowns, item costs, or other destination restrictions. Every successful player teleport applies one point of native durability damage to the exact Warp Stone that opened the menu. Creative-mode exemptions, durability enchantments, and item breakage follow native Minecraft and Waystones behavior. Failed requests consume neither experience nor durability.

### Server Experience Configuration

After the first launch, the default file is `config/waystonesplayer-server.toml` in the server or client instance. Per-world overrides can be placed at `world/serverconfig/waystonesplayer-server.toml` on a dedicated server or `saves/<world>/serverconfig/waystonesplayer-server.toml` in single-player. Stop and restart the server when changing it.

`playerTeleportExperienceMode` has three mutually exclusive values:

| Mode | Behavior |
|---|---|
| `NEVER` | Default. Player teleportation never consumes experience. |
| `FOLLOW_WAYSTONES` | Applies Waystones' current experience-points/levels, distance, dimensional, minimum, and maximum rules only when Waystones costs are enabled. |
| `ALWAYS` | Evaluates the same configured experience rules even when Waystones' global cost switch is disabled. |

Only Waystones experience-point and experience-level requirements are selected. A server configuration that produces no experience requirement can therefore still resolve to zero cost.

### Compatibility and Privacy

- Access to Waystones menu and experience-rule internals is isolated behind a compatibility boundary. If experience rules cannot be evaluated safely, teleports that require them are rejected instead of becoming silently free.
- The mod contains no telemetry, advertising, analytics, or external data uploads. Selecting a player sends only the target UUID to the Minecraft server you are already connected to.
- The mod does not bundle or redistribute Waystones or Balm code, JARs, icons, or other assets.

After publication, report issues through the feedback channel linked on the Modrinth or CurseForge project page. Include the Minecraft, NeoForge, Waystones, Balm, and Waystones Player versions and any relevant logs.

### Build

With Java 21 active:

```bash
./gradlew clean build --no-daemon --console=plain --warning-mode all
```

On macOS, JDK 21 can be selected explicitly:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew clean build --no-daemon --console=plain --warning-mode all
```

The release JAR is written to `build/libs/waystonesplayer-<version>.jar`.

### License and Disclaimer

Waystones Player is All Rights Reserved; see `LICENSE`. Waystones, Balm, and their names and assets belong to their respective owners. This project is not official, endorsed, or sponsored by Twelve Iterations.
