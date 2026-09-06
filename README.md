# Waystones Player-to-player TP

Waystones 的非官方附属模组：在传送石（Warp Stone）菜单左侧显示在线玩家，点击即可传送到对方附近。无需 OP 权限、命令或额外道具。

[简体中文](#简体中文) · [English](#english)

## 简体中文

### 安装

客户端和服务端都要安装与游戏版本、加载器匹配的本模组、[Waystones](https://modrinth.com/mod/waystones) 和 [Balm](https://modrinth.com/mod/balm)。Fabric 另需 [Fabric API](https://modrinth.com/mod/fabric-api)，26.x 另需对应 Shogi。1.21.x 使用 Java 21，26.x 使用 Java 25。不支持 Forge。

当前维护版本为 **1.0.1**。本次新增接收设置，客户端与服务端须一起更新；较早的 1.0.1 文件也不能混用。升级旧模组 ID 时先删除 `waystonesplayer` JAR，避免新旧模组同时加载。

### 使用

1. 手持该版本 Waystones 的任意原生传送石并完成蓄力，主手和副手均可。
2. 在原菜单左侧选择玩家。目录会随玩家加入、离开而更新，并根据窗口空间显示姓名列表或头像栏。
3. 在目录顶部搜索玩家名字，支持不区分大小写的部分匹配。输入会即时筛选，显示匹配人数；清空后恢复全部玩家。窗口缩到头像栏时会隐藏搜索框并清空筛选，关闭菜单也会清空。
4. 搜索框右侧的小按钮控制“允许他人传送到我”：绿色勾为允许、红色叉为关闭，悬停查看说明；灰色表示等待服务器确认。默认允许；关闭后别人仍能看到你，但不能点击传送。你仍可以前往其他允许接收的玩家。头像栏保留此开关。

接收设置按玩家 UUID 保存在当前服务器／世界中，重连、死亡、换维度和正常重启后保留。正常注册的假人默认允许接收。菜单保留 Waystones 的搜索、排序、分页、滚动和筛选功能。

传送采用 Waystones 的声音、效果及适用版本的事件。目标走动时会在执行前更新位置；优先落在目标相邻位置，四周封闭时回退目标方块中心。目标可能位于空中、流体、悬崖或方块内，请自行判断是否前往。

### 经验与传送石

配置文件为 `waystonesptpt-server.toml`，键为 `playerTeleportExperienceMode`：

| 模式 | 行为 |
|---|---|
| `NEVER` | 默认；玩家传送不消耗经验。 |
| `FOLLOW_WAYSTONES` | Waystones 开启经验费用时，使用其当前经验点数／等级、距离、跨维度及上下限规则。 |
| `ALWAYS` | 忽略 Waystones 的经验总开关，始终使用其经验规则。 |

传送石保留对应版本的原生蓄力与耐久行为；26.x 使用适用的损耗规则和耐久开关，成功后只结算一次。普通石碑、卷轴和 Warp Plate 不受玩家目录或个人接收设置影响。

NeoForge 使用 SERVER 配置，保留全局默认、世界 `serverconfig` 覆盖和重载。Fabric 使用同名文件、同一键和默认值，但它是实例全局配置，修改后重启读取。接收设置在两加载器上均按世界保存，不受这一配置差异影响。旧配置仅在新文件不存在时复制，原文件保留。损坏的附属费用配置回退到 `NEVER` 并记录日志；有效收费配置中的规则解析错误会拒绝本次传送。

### 发布与验收状态

[正式 Modrinth 页面](https://modrinth.com/mod/waystonesplayerptpt) 当前公开文件为 1.0.0 的 NeoForge／Fabric 1.21.1。以下是 **1.0.1 维护矩阵**，不是全部玩法验收通过的声明：

| 维护分支 | Minecraft | JAR 数 | 1.0.1 玩法状态 |
|---|---|---:|---|
| `main` | NeoForge 1.21.1 | 1 | 新改动待完整验收；原版本有用户基本实测 |
| `fabric/1.21.x` | Fabric 1.21.1–1.21.11 | 10 | 待完整验收；1.21.1 原版本有用户基本实测 |
| `neoforge/1.21.x` | NeoForge 1.21.2–1.21.11 | 9 | 待完整验收 |
| `fabric/26.x` | Fabric 26.1、26.1.1、26.1.2、26.2 | 4 | 待完整验收 |
| `neoforge/26.x` | NeoForge 26.1、26.1.1、26.1.2、26.2 | 4 | 待完整验收 |

28 份 JAR 对应 30 个游戏版本／加载器组合；1.21.2／1.21.3 的共用文件仍需分别验收。26.1.1 依赖固定源码构建的上游 JAR，在公开安装条件闭合前不进入上传清单。精确依赖见 [目标矩阵](gradle/targets.json)，证据与门禁见 [验收状态](docs/VALIDATION.md)。

### 许可与开发

原创代码使用 [MIT License](LICENSE)。原图标未修改，其第三方素材的署名和条款见 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)。本模组无遥测、广告或外部上传；目录与个人接收状态只在当前 Minecraft 连接中同步。

开发者请阅读 [架构](docs/ARCHITECTURE.md)、[兼容性](docs/COMPATIBILITY.md)、[网络契约](docs/NETWORK.md) 和 [发布清单](docs/RELEASE_CHECKLIST.md)。项目不打包 Waystones、Balm、Shogi 或其他前置。

## English

An unofficial [Waystones](https://modrinth.com/mod/waystones) add-on that adds an online-player directory to the Warp Stone menu, enabling player-to-player teleportation without commands, operator permissions, or extra items.

### Install and play

Install matching copies of this mod, Waystones, and Balm on both client and server. Fabric also requires Fabric API; 26.x also requires matching Shogi. Use Java 21 for 1.21.x and Java 25 for 26.x. Forge is not supported.

Charge any native Warp Stone available in your version, in either hand, then select a player in the directory on the left. The directory adapts to the window width while keeping Waystones' own controls available.

Search player names above the directory using case-insensitive partial matches. Results and their count update as you type; clearing the field restores the full list. The search is cleared when the menu closes or the window shrinks to the avatar-only layout, where the search field is hidden.

The small button beside the search field controls **Allow others to teleport to me**: a green check means enabled, a red cross means disabled, and gray means waiting for server confirmation. Hover for details. The setting defaults to enabled, and its button remains available in the avatar-only layout. Turning it off keeps your row visible but unavailable to others; you can still travel to players who allow incoming teleports. This UUID-based setting is saved in the server/world and survives reconnects, death, dimension changes, and normal restarts. Normally registered fake players default to enabled.

Destinations follow the target's position before execution. An adjacent position is preferred; if none is available, the target block center is used. Targets may be inside blocks, in mid-air, in fluids, or near cliffs.

### Configuration

`playerTeleportExperienceMode` in `waystonesptpt-server.toml` defaults to `NEVER` (no experience cost). `FOLLOW_WAYSTONES` uses Waystones' experience rules when its cost switch is enabled; `ALWAYS` uses those rules regardless of the switch. Warp Stones retain the applicable native charge and durability behavior, including 26.x damage rules and the durability switch; successful travel settles damage once.

NeoForge keeps SERVER configuration, world overrides, and reloads. Fabric uses an instance-wide configuration read on restart. Receiving preferences are world-specific on both loaders. Corrupt add-on cost configuration falls back to `NEVER` with a log message; invalid active cost rules reject the teleport.

### Release status

The [public Modrinth page](https://modrinth.com/mod/waystonesplayerptpt) currently offers the older 1.0.0 files for NeoForge and Fabric 1.21.1. The 1.0.1 maintenance matrix has 28 artifacts for 30 combinations across 1.21.1–1.21.11 and 26.1/26.1.1/26.1.2/26.2. Build or startup success does not mean gameplay acceptance. See [validation status](docs/VALIDATION.md) before treating a target as ready to upload. The shared 1.21.2/1.21.3 file needs separate acceptance on both versions; 26.1.1 remains excluded from upload readiness while public dependency installation is unresolved.

This update remains **1.0.1**, but both sides must update together, including installations already labeled 1.0.1, because receiving preferences require network protocol 2.

Original code is available under the [MIT License](LICENSE); third-party icon terms remain in [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md). There is no telemetry, advertising, or external upload.

This is not a Twelve Iterations project. NOT AN OFFICIAL MINECRAFT PRODUCT. NOT APPROVED BY OR ASSOCIATED WITH MOJANG OR MICROSOFT.
