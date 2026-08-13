# Waystones Player

把传送石变成一条直达队友的路。

Waystones Player 是 [Waystones（传送石碑）](https://modrinth.com/mod/waystones) 的非官方附属模组。完成 Warp Stone 蓄力后，原界面左侧会实时显示当前连接中可列出的其他玩家；选择玩家即可经 Waystones 的传送管线前往对方附近。

[简体中文](#简体中文) · [English](#english)

## 简体中文

### 功能

- 玩家目录始终位于左侧：宽屏显示 164px 完整名单，中等空间收窄至 128px，继续不足时改为 36px 可点击头像栏；只有必要时才把 Waystones 原界面整体右移。
- 名单实时复用 Minecraft 已同步的 listed 玩家数据，不增加第二套名单协议；加入、离开或改名时保留可见锚点与键盘焦点。
- 客户端只发送目标 UUID。客户端目录复用 Minecraft 的 listed 玩家流；服务端则独立用 `allowsListing()` 作为硬授权，并重新验证菜单、打开菜单的确切 Warp Stone 与使用手、费用和最终移动。
- 传送进入 Waystones 的可取消事件、声音、效果和相邻非窒息落点流程，兼容其他观察或取消 Waystones 传送的模组。
- 每次确认成功后才对原 Warp Stone 结算 1 点原生耐久；失败不扣耐久，经验会恢复且界面保持打开。
- 三种经验模式：免费、跟随 Waystones 总开关，或始终计算 Waystones 当前的经验点数/等级公式。

“相邻非窒息”只表示身体与头部空间不会窒息，不保证脚下有方块，也不排除危险流体或悬崖。目标玩家本身也可能在传送期间移动。

### 安装与支持范围

Waystones Player、Waystones 和 Balm 必须同时安装在客户端与服务端，并使用与 Minecraft 小版本及加载器匹配的三个 JAR；Fabric 目标还需要对应版本的 Fabric API，NeoForge 目标不需要 Fabric API。Java 版本为 21，模组版本始终为 `1.0.0`。

| 分支 | 加载器 | Minecraft | 可上传 JAR |
|---|---|---|---:|
| `main` | NeoForge | 1.21.1 | 1 |
| `neoforge/1.21.x` | NeoForge | 1.21.2–1.21.11 | 9 |
| `fabric/1.21.x` | Fabric | 1.21.1–1.21.11 | 10 |

1.21.2 与 1.21.3 在每个加载器共用一份经两版分别验收的 JAR；除此之外，不要跨 Minecraft 小版本混用文件。NeoForge 1.21.2、1.21.6、1.21.7 与 1.21.9 的官方可用运行栈包含 beta NeoForge，发布资料会如实标注。Forge 不在支持范围内。

机器可读的全部最低/当前依赖与精确文件名位于 [gradle/targets.json](gradle/targets.json)，人类可读说明见 [COMPATIBILITY.md](docs/COMPATIBILITY.md)。

### 使用与服务端规则

1. 手持 Waystones 的 Warp Stone 并完成蓄力。
2. 在左侧玩家目录选择一名当前可列出的在线玩家。
3. 服务端验证本次菜单仍绑定同一个主手或副手物品，并重新查询目标。
4. Waystones 执行事件、效果和落点流程；只有服务器确认发送者实际移动后，才结算费用、耐久并关闭界面。

客户端只展示原版 listed 玩家流中的目标；服务端另行要求目标的 `allowsListing()` 为真。因此第三方逐客户端 `UPDATE_LISTED` 隐藏可能让一个已显示条目在服务端被拒绝，而本模组不承诺阻止针对这类第三方隐藏状态的猜 UUID 请求。玩家目的地不会写入 Waystones 数据库，不会继承物品费用、冷却或非经验 requirement。

### 经验配置

配置键为 `playerTeleportExperienceMode`，默认值为 `NEVER`。

| 模式 | 行为 |
|---|---|
| `NEVER` | 默认。玩家目的地不消耗经验。 |
| `FOLLOW_WAYSTONES` | 仅当 Waystones 开启费用时，使用其经验点数/等级、距离、跨维度和上下限规则。 |
| `ALWAYS` | 忽略 Waystones 的费用总开关，但仍使用其当前经验规则。 |

只选择经验点数和经验等级 requirement；物品、冷却及其他费用不会被应用。需要兼容层但无法安全确认时，传送会被拒绝，不会静默变成免费。

- NeoForge 使用 SERVER 配置：全局默认位于 `config/waystonesplayer-server.toml`，世界可在 `world/serverconfig` 或单人世界的 `saves/<世界名>/serverconfig` 覆盖，并保留重载语义。
- Fabric 使用同名 `config/waystonesplayer-server.toml`、同一键和默认值，但它是实例全局配置，不声称支持 NeoForge 的按世界覆盖。

### 隐私、许可与声明

本模组没有遥测、广告、分析服务或外部数据上传。玩家选择只向当前连接的 Minecraft 服务端发送目标 UUID。

原创代码为 All Rights Reserved；[LICENSE](LICENSE) 另行允许最终用户从授权渠道下载、安装并运行官方未修改 JAR。修改、镜像、再分发、打包进整合包或商业使用仍需书面许可。项目不打包 Waystones/Balm 的代码或 JAR。

现有项目图标保持不变。其 Warp Stone 与玩家头像素材的来源、修改和适用条款见 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)。Waystones Player 不是 Twelve Iterations 官方项目，也不是 Minecraft 官方产品，未经 Mojang 或 Microsoft 批准或关联。

### 开发

`main` 的模块是 `core + common + neoforge`：

- `core`：纯 Java 业务规则、布局计算、差分和单元测试，不得链接 Minecraft、Waystones、Balm、Mixin 或加载器 API。
- `common`：按目标 Minecraft 重新编译的业务、协议、兼容层、客户端控件、资源和 Mixin，不得导入 Fabric/NeoForge API。
- 加载器模块：入口、配置、元数据、网络桥接和最终 JAR；统一分支用显式目标工程隔离各 Minecraft/API 断点。

```bash
JAVA_HOME=/path/to/jdk-21 ./gradlew clean test build --no-build-cache
```

主线产物为 `neoforge/build/libs/waystonesplayer-neoforge-1.21.1-1.0.0.jar`。构建会检查模块边界、20 产物矩阵、Mixin/网络契约、许可证、原图标以及未捆绑上游依赖。设计与维护细节见 [ARCHITECTURE.md](docs/ARCHITECTURE.md)、[COMPATIBILITY.md](docs/COMPATIBILITY.md) 和 [RELEASE_CHECKLIST.md](docs/RELEASE_CHECKLIST.md)。

---

## English

Turn a Warp Stone into a direct route to your teammates.

Waystones Player is an unofficial add-on for [Waystones](https://modrinth.com/mod/waystones). After a Warp Stone finishes charging, a live directory of other listed players appears to the left of the original destination screen.

### Features

- A responsive player directory: 164px full list, 128–163px narrowed list, or a 36px clickable avatar rail. The Waystones UI moves right only when required.
- Live updates from Minecraft's existing listed-player data, preserving the visible anchor and keyboard focus without adding a duplicate directory protocol.
- Server-authoritative validation of the menu, exact Warp Stone and hand, `allowsListing()` authorization, cost, and confirmed movement.
- Waystones' cancellable events, sound, effects, and adjacent non-suffocating destination selection.
- One point of native Warp Stone durability only after confirmed success; failures restore experience, preserve durability, and keep the menu open.
- `NEVER`, `FOLLOW_WAYSTONES`, and `ALWAYS` experience modes.

Adjacent non-suffocating placement is not an absolute safety guarantee: it does not require solid ground or exclude dangerous fluids and cliffs.

### Installation and supported targets

Install matching Waystones Player, Waystones, and Balm JARs on both client and server; Fabric targets also require the matching Fabric API, while NeoForge targets do not. Java 21 is required, and every project artifact remains version `1.0.0`.

| Branch | Loader | Minecraft | Uploadable JARs |
|---|---|---|---:|
| `main` | NeoForge | 1.21.1 | 1 |
| `neoforge/1.21.x` | NeoForge | 1.21.2–1.21.11 | 9 |
| `fabric/1.21.x` | Fabric | 1.21.1–1.21.11 | 10 |

Minecraft 1.21.2 and 1.21.3 share one separately runtime-tested JAR per loader. Every other file is restricted to one Minecraft minor release. Some supported NeoForge lines use official beta loader builds; this is disclosed in their file metadata. Forge is not supported.

The exact minimum/current dependency suites and canonical filenames are in [gradle/targets.json](gradle/targets.json).

### Rules and configuration

The client displays players from Minecraft's listed-player stream. The client sends a UUID, and the server independently resolves it and authorizes it only when `allowsListing()` is true. These are separate boundaries: a third-party per-client `UPDATE_LISTED` hide can leave an entry displayed but rejected by the server, and this add-on does not promise to prevent guessed-UUID requests against such third-party hiding. A player destination is transient and is never stored as a Waystone.

`playerTeleportExperienceMode` defaults to `NEVER`. Only experience-point and experience-level requirements are selected; item costs, cooldowns, and unrelated requirements are ignored. NeoForge keeps SERVER config and per-world override semantics. Fabric uses the same `config/waystonesplayer-server.toml`, key, and default as a global instance configuration, without claiming per-world overrides.

### Privacy, license, and development

The mod has no telemetry, advertising, analytics, or external uploads. It sends only the chosen target UUID to the Minecraft server already in use.

Original code is All Rights Reserved. [LICENSE](LICENSE) grants end users limited permission to download, install, and run an official unmodified JAR; modification, redistribution, modpack bundling, and commercial use still require written permission. The existing icon is unchanged; asset attribution and separate terms are in [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).

`main` uses `core + common + neoforge`. The pure-Java core is loader- and Minecraft-free; common is recompiled for each target and cannot import loader APIs; target modules contain only loader/version bridges and packaging. Run `./gradlew clean test build --no-build-cache` with JDK 21.

See [ARCHITECTURE.md](docs/ARCHITECTURE.md), [COMPATIBILITY.md](docs/COMPATIBILITY.md), and [RELEASE_CHECKLIST.md](docs/RELEASE_CHECKLIST.md) for architecture, dependency, and release evidence.

NOT AN OFFICIAL MINECRAFT PRODUCT. NOT APPROVED BY OR ASSOCIATED WITH MOJANG OR MICROSOFT.
