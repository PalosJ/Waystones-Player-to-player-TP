# Waystones Player

把传送石变成一条直达队友的路。

Waystones Player 是 [Waystones（传送石碑）](https://modrinth.com/mod/waystones) 的非官方附属模组。完成传送石蓄力后，界面会显示当前在线的其他玩家；点一下头像，就能直接前往对方所在位置，不需要 OP 权限，也不需要对方确认。

[简体中文](#简体中文) · [English](#english)

## 简体中文

### 你会得到什么

- 传送石界面中的在线玩家目的地，宽屏直接显示，窄屏通过头像按钮展开。
- 服务端权威校验：客户端只提交目标 UUID，物品、目标、费用与传送结果都由服务端重新确认。
- 明确的生存成本：每次成功传送消耗传送石 1 点耐久；失败不会扣经验、耐久，也不会强制关闭界面。
- 三种经验模式，可选择免费、跟随 Waystones，或始终使用 Waystones 当前的经验公式。

### 安装与兼容

本模组必须同时安装在客户端和服务端。

| 组件 | 当前要求 |
|---|---|
| Minecraft | 1.21.1 |
| 加载器 | NeoForge 21.1.229–21.1.x；开发与 CI 使用 21.1.248 |
| Waystones | 21.1.36–21.1.x；开发与 CI 验证至 21.1.40 |
| Balm | 21.0.62–21.0.x；开发与 CI 验证至 21.0.64 |
| Java | 21 |

把 Waystones Player、Waystones 和 Balm 的 JAR 一起放入两端的 `mods` 目录即可。`main` 与默认发布继续正式支持 NeoForge 1.21.1。仓库另有 `fabric/1.21.1` 和 `neoforge/1.21.11` 移植验证分支；它们用于实证多加载器、多 Minecraft 版本架构，不会取代原设计主线。Forge 版本不在计划内。

### 玩法与费用

1. 手持 Waystones 的传送石并完成蓄力。
2. 在目的地界面选择一名在线玩家。
3. 服务端验证同一个传送石仍在主手或副手、目标仍在线且费用足够，然后执行传送。

玩家目的地不会自动继承 Waystones 的冷却、物品费用或其他目的地限制。成功后只结算一次经验费用和一次原生耐久伤害；创造模式不检查或扣除经验且免耐久损耗，耐久附魔和最终损坏行为沿用 Minecraft/Waystones 规则。

### 经验配置

配置键为 `playerTeleportExperienceMode`，默认值是 `NEVER`。文件名保持为 `waystonesplayer-server.toml`：全局默认位于实例的 `config` 目录，专用服务器可在 `world/serverconfig`、单人世界可在 `saves/<世界名>/serverconfig` 使用世界级覆盖。建议关闭服务器后修改并重新启动。

| 模式 | 行为 |
|---|---|
| `NEVER` | 默认。玩家间传送不消耗经验。 |
| `FOLLOW_WAYSTONES` | 仅当 Waystones 开启费用时，使用其经验点数/等级、距离、跨维度和上下限规则。 |
| `ALWAYS` | 忽略 Waystones 的费用总开关，但仍使用其当前经验规则。 |

本附属只提取经验点数和经验等级要求，不套用物品、冷却或其他费用。服务器规则没有产生经验要求时，结果仍可能为零；需要计算经验但兼容层无法安全解析时，传送会被拒绝，不会悄悄变成免费。

### 隐私与声明

Waystones Player 没有遥测、广告、分析服务或外部数据上传。选择玩家时，只会向当前连接的 Minecraft 服务端发送目标 UUID。

本项目不打包或重新分发 Waystones/Balm 的代码或 JAR。项目图标使用了 Joe Williamson（JoeCreates）以 CC BY-SA 3.0 发布的 Warp Stone 美术素材，以及 Minecraft 默认玩家皮肤中的头像元素；来源、修改说明和适用条款见 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)。Waystones、Balm 及其名称与资产归各自权利人所有；本项目并非 Twelve Iterations 官方项目，也不代表其认可或背书。

本项目并非 Minecraft 官方产品，未经 Mojang 或 Microsoft 批准，也不与其存在关联。

### 开发

`main` 采用 `common + neoforge` 结构。`common` 是共享源码与测试，不是可安装模组；发布 JAR 由加载器模块生成：

```bash
JAVA_HOME=/path/to/jdk-21 ./gradlew clean test build --no-build-cache
```

`build` 会自动检查 common 没有 Fabric/NeoForge API、最终 JAR 含模组图标/元数据/许可证，并确认没有打包 Waystones/Balm 类。主线产物位于 `neoforge/build/libs/waystonesplayer-neoforge-1.21.1-<版本>.jar`。

| 分支 | 定位 | 加载器模块 |
|---|---|---|
| `main` | 原设计与默认发布基线 | NeoForge 1.21.1 |
| `fabric/1.21.1` | 同版本多加载器移植验证 | Fabric 1.21.1，并保留 NeoForge 1.21.1 对照构建 |
| `neoforge/1.21.11` | 新 Minecraft 版本移植验证 | NeoForge 1.21.11 |

架构、分支职责与升级边界分别见 [ARCHITECTURE.md](docs/ARCHITECTURE.md) 和 [COMPATIBILITY.md](docs/COMPATIBILITY.md)。

原创源码与内容采用 All Rights Reserved；项目图标中另行标注的素材按各自条款使用，详见 [LICENSE](LICENSE) 与 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)。发布后的问题请通过对应 Modrinth/CurseForge 项目页面提供的反馈渠道提交，并附上相关版本与日志。

---

## English

Turn a Warp Stone into a direct route to your teammates.

Waystones Player is an unofficial add-on for [Waystones](https://modrinth.com/mod/waystones). Once a Warp Stone finishes charging, its destination screen gains a list of other online players. Select a player to teleport directly to them—no operator permission and no target confirmation required.

### Highlights

- Online-player destinations integrated into the Warp Stone screen, with a compact overlay on narrow displays.
- Server-authoritative validation: the client sends only a target UUID; the server rechecks the item, target, cost, and teleport result.
- A clear survival cost: each successful teleport damages the Warp Stone by one durability point. Failures consume nothing and keep the menu open.
- Three experience modes: free, follow Waystones, or always evaluate Waystones' current experience formula.

### Installation and Compatibility

Install the mod on both the client and server.

| Component | Current requirement |
|---|---|
| Minecraft | 1.21.1 |
| Loader | NeoForge 21.1.229 through 21.1.x; development and CI use 21.1.248 |
| Waystones | 21.1.36 through 21.1.x; development and CI verified through 21.1.40 |
| Balm | 21.0.62 through 21.0.x; development and CI verified through 21.0.64 |
| Java | 21 |

Place Waystones Player, Waystones, and Balm in both `mods` directories. `main` and the default release continue to support NeoForge 1.21.1. The repository also contains `fabric/1.21.1` and `neoforge/1.21.11` port-validation branches; they exercise the multi-loader and multi-Minecraft architecture without replacing the original mainline. Forge is not planned.

### Costs and Configuration

Every successful player teleport charges experience once and applies one point of native durability damage to the exact Warp Stone that opened the menu. Creative players neither check nor consume experience and are exempt from durability loss. Durability enchantments and breakage use native Minecraft/Waystones behavior. Waystones cooldowns, item costs, and unrelated destination requirements are not inherited.

The SERVER config remains `waystonesplayer-server.toml`, with the key `playerTeleportExperienceMode`:

| Mode | Behavior |
|---|---|
| `NEVER` | Default. Player teleportation never consumes experience. |
| `FOLLOW_WAYSTONES` | Uses Waystones' experience rules only while its global costs are enabled. |
| `ALWAYS` | Evaluates the same experience rules regardless of the global cost switch. |

Only experience-point and experience-level requirements are selected. A rule set with no experience requirement can still resolve to zero cost. If a required compatibility calculation cannot be completed safely, the teleport is rejected instead of silently becoming free.

### Privacy, Development, and License

The mod contains no telemetry, advertising, analytics, or external uploads. Selecting a player sends only the target UUID to the Minecraft server you are already connected to. It does not bundle or redistribute Waystones or Balm code or JARs. The project icon incorporates Warp Stone artwork released by Joe Williamson (JoeCreates) under CC BY-SA 3.0 and player-face elements from Minecraft's default skins. Sources, modifications, and applicable terms are documented in [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).

The canonical `main` branch uses `common + neoforge`; `common` is shared source and tests, not an installable mod. Build with JDK 21 using `./gradlew clean test build --no-build-cache`. The build verifies the common loader boundary and required distribution contents. The canonical artifact is `neoforge/build/libs/waystonesplayer-neoforge-1.21.1-<version>.jar`; the `fabric/1.21.1` and `neoforge/1.21.11` branches contain the corresponding port builds.

See [ARCHITECTURE.md](docs/ARCHITECTURE.md) for module and transaction design and [COMPATIBILITY.md](docs/COMPATIBILITY.md) for the support matrix and upgrade checklist.

Waystones Player is All Rights Reserved except for material identified in [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md); see [LICENSE](LICENSE). Waystones, Balm, and their names and assets belong to their respective owners. This project is not official, endorsed, or sponsored by Twelve Iterations.

NOT AN OFFICIAL MINECRAFT PRODUCT. NOT APPROVED BY OR ASSOCIATED WITH MOJANG OR MICROSOFT.
