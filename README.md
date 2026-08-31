# Waystones Player

把传送石变成一条直达队友的路。

Waystones Player 是 [Waystones（传送石碑）](https://modrinth.com/mod/waystones) 的非官方附属模组。完成 Warp Stone 蓄力后，原界面左侧会实时显示当前连接中可列出的其他玩家；选择玩家即可经 Waystones 的传送管线前往对方附近。

[简体中文](#简体中文) · [English](#english)

## 简体中文

### 功能

- 玩家目录始终位于左侧：宽屏显示 164px 完整名单，中等空间收窄至 128px，继续不足时改为 36px 可点击头像栏；只有必要时才把 Waystones 原界面整体右移。
- 名单复用 Minecraft 已同步的 listed 玩家数据，不增加第二套名单协议；每 5 个客户端 tick 刷新一次，连接变化时立即刷新。稳定目录只做线性精确比较，加入、离开或改名时才排序，并按 UUID 复用行、保留可见锚点；焦点玩家退出时回退到邻近行。Waystones 自身搜索框只筛选 Waystones，不会隐藏玩家目录。
- 客户端只发送目标 UUID。客户端目录复用 Minecraft 的 listed 玩家流；服务端则独立用 `allowsListing()` 作为硬授权，并重新验证菜单、打开菜单的确切 Warp Stone 与使用手、费用和最终移动。
- 传送进入 Waystones 的可取消事件、声音和效果流程；事件重定向完成后还会再次要求目标四个水平相邻位置至少有一处身体与头部均不窒息。事件可以观察、取消或重定向，但不能替换已锁定费用。
- 每次确认成功后才重新定位原手中的当前 Warp Stone，并结算 1 点原生耐久；失败不扣耐久，经验会恢复且界面保持打开。若第三方模组在移动后彻底移除该物品，不会损坏无关物品或回滚已经完成的移动。
- 若玩家已经移动，但最终位置不再匹配事件后验证的目标或实际身体/头部变为窒息位置，传送仍按已发生移动结算且不会退还经验，同时显示兼容性警告。
- 三种经验模式：免费、跟随 Waystones 总开关，或始终计算 Waystones 当前的经验点数/等级公式。

“相邻非窒息”只表示身体与头部空间不会窒息，不保证脚下有方块，也不排除危险流体或悬崖。目标玩家本身也可能在传送期间移动。

### 安装与支持范围

Waystones Player、Waystones 和 Balm 必须同时安装在客户端与服务端，并使用与 Minecraft 小版本及加载器匹配的 JAR；26.x 目标还必须安装对应版本的 Shogi。Fabric 目标另需对应 Fabric API，NeoForge 目标不需要 Fabric API。1.21.x 使用 Java 21，26.x 使用 Java 25；模组版本始终为 `1.0.1`。

| 分支 | 加载器 | Minecraft | 可上传 JAR |
|---|---|---|---:|
| `main` | NeoForge | 1.21.1 | 1 |
| `neoforge/1.21.x` | NeoForge | 1.21.2–1.21.11 | 9 |
| `fabric/1.21.x` | Fabric | 1.21.1–1.21.11 | 10 |
| `neoforge/26.x` | NeoForge | 26.1、26.1.1、26.1.2、26.2 | 4 |
| `fabric/26.x` | Fabric | 26.1、26.1.1、26.1.2、26.2 | 4 |

1.21.2 与 1.21.3 在每个加载器共用一份经两版分别验收的 JAR；26.x 的四个游戏版本始终独立编译、独立发布，不跨补丁版本复用二进制。部分 NeoForge 运行栈使用官方 beta，发布资料会如实标注。Forge 不在支持范围内。

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

只选择 Waystones 自带且可验证的经验点数和经验等级 requirement；物品、冷却及其他费用不会被应用。合法零费用保留，但非空规则解析为空、未知第三方类型、负数、非有限数、溢出或事件替换费用都会拒绝整次传送，不会静默变成免费。

- NeoForge 使用 SERVER 配置：全局默认位于 `config/waystonesplayer-server.toml`，世界可在 `world/serverconfig` 或单人世界的 `saves/<世界名>/serverconfig` 覆盖，并保留重载语义。
- Fabric 使用同名 `config/waystonesplayer-server.toml`、同一键和默认值，但它是实例全局配置，不声称支持 NeoForge 的按世界覆盖。

### 隐私、许可与声明

本模组没有遥测、广告、分析服务或外部数据上传。玩家选择只向当前连接的 Minecraft 服务端发送目标 UUID。

原创代码依据 [MIT License](LICENSE) 开源，可在保留版权与许可声明的条件下使用、修改和再分发。项目图标中的第三方素材不属于 MIT 授权范围，继续遵循各自条款。项目不打包 Waystones、Balm 或 Shogi 的代码或 JAR。

现有项目图标保持不变。其 Warp Stone 与玩家头像素材的来源、修改和适用条款见 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)。Waystones Player 不是 Twelve Iterations 官方项目，也不是 Minecraft 官方产品，未经 Mojang 或 Microsoft 批准或关联。

### 开发

`main` 的模块是 `core + common + neoforge`：

- `core`：纯 Java 业务规则、布局计算、差分和单元测试，不得链接 Minecraft、Waystones、Balm、Mixin 或加载器 API。
- `common`：按目标 Minecraft 重新编译的业务、协议、兼容层、客户端控件、资源和 Mixin，不得导入 Fabric/NeoForge API。
- 加载器模块：入口、配置、元数据、网络桥接和最终 JAR；统一分支用显式目标工程隔离各 Minecraft/API 断点。

```bash
JAVA_HOME=/path/to/jdk-21 ./gradlew clean test build --no-build-cache
```

主线产物为 `neoforge/build/libs/waystonesplayer-neoforge-1.21.1-1.0.1.jar`。构建会检查模块边界、28 产物矩阵、Mixin/网络契约、许可证、原图标以及未捆绑上游依赖。26.x 分支使用 JDK 25；26.1.1 的缺失 Waystones 二进制由固定官方提交在忽略的 `build/` 下重建，既不提交也不嵌入。设计与维护细节见 [ARCHITECTURE.md](docs/ARCHITECTURE.md)、[COMPATIBILITY.md](docs/COMPATIBILITY.md) 和 [RELEASE_CHECKLIST.md](docs/RELEASE_CHECKLIST.md)。

---

## English

Turn a Warp Stone into a direct route to your teammates.

Waystones Player is an unofficial add-on for [Waystones](https://modrinth.com/mod/waystones). After a Warp Stone finishes charging, a live directory of other listed players appears to the left of the original destination screen.

### Features

- A responsive player directory: 164px full list, 128–163px narrowed list, or a 36px clickable avatar rail. The Waystones UI moves right only when required.
- Updates from Minecraft's existing listed-player data every five client ticks, with immediate refresh on connection changes. Stable directories use a linear exact comparison; sorting happens only after a join, leave, rename, or identity replacement. Rows are reused by UUID, visible anchors are preserved, and removed focus falls back to a nearby row. The Waystones search field filters only Waystones, never the player directory.
- Server-authoritative validation of the menu, exact Warp Stone and hand, `allowsListing()` authorization, cost, and confirmed movement.
- Waystones' cancellable events, sound, effects, and a final adjacent non-suffocating-space guard after event redirection. Events may observe, cancel, or redirect, but cannot replace the locked cost.
- One point of native Warp Stone durability only after confirmed success and re-resolving the original hand/reference; failures restore experience, preserve durability, and keep the menu open.
- Confirmed movement to a destination that no longer matches the post-event validation, or to a suffocating actual body/head position, keeps the consumed experience and durability settlement but reports a compatibility warning.
- `NEVER`, `FOLLOW_WAYSTONES`, and `ALWAYS` experience modes.

Adjacent non-suffocating placement is not an absolute safety guarantee: it does not require solid ground or exclude dangerous fluids and cliffs.

### Installation and supported targets

Install matching Waystones Player, Waystones, and Balm JARs on both client and server. The 26.x targets also require matching Shogi; Fabric targets require the matching Fabric API. The 1.21.x line uses Java 21 and the 26.x line uses Java 25. Every project artifact remains version `1.0.1`.

| Branch | Loader | Minecraft | Uploadable JARs |
|---|---|---|---:|
| `main` | NeoForge | 1.21.1 | 1 |
| `neoforge/1.21.x` | NeoForge | 1.21.2–1.21.11 | 9 |
| `fabric/1.21.x` | Fabric | 1.21.1–1.21.11 | 10 |
| `neoforge/26.x` | NeoForge | 26.1, 26.1.1, 26.1.2, 26.2 | 4 |
| `fabric/26.x` | Fabric | 26.1, 26.1.1, 26.1.2, 26.2 | 4 |

Minecraft 1.21.2 and 1.21.3 share one separately runtime-tested JAR per loader. Every 26.x patch target is compiled and published independently. Some supported NeoForge stacks use official beta builds; this is disclosed in their file metadata. Forge is not supported.

The exact minimum/current dependency suites and canonical filenames are in [gradle/targets.json](gradle/targets.json).

### Rules and configuration

The client displays players from Minecraft's listed-player stream. The client sends a UUID, and the server independently resolves it and authorizes it only when `allowsListing()` is true. These are separate boundaries: a third-party per-client `UPDATE_LISTED` hide can leave an entry displayed but rejected by the server, and this add-on does not promise to prevent guessed-UUID requests against such third-party hiding. A player destination is transient and is never stored as a Waystone.

`playerTeleportExperienceMode` defaults to `NEVER`. Only verifiable built-in experience-point and experience-level requirements are selected; item costs, cooldowns, and unrelated requirements are ignored. A non-empty rule that parses empty, an unknown third-party type, a negative/non-finite/overflowing value, or an event cost replacement rejects the teleport instead of becoming free. NeoForge keeps SERVER config and per-world override semantics. Fabric uses the same `config/waystonesplayer-server.toml`, key, and default as a global instance configuration, without claiming per-world overrides.

### Privacy, license, and development

The mod has no telemetry, advertising, analytics, or external uploads. It sends only the chosen target UUID to the Minecraft server already in use.

Original code is open source under the [MIT License](LICENSE), permitting use, modification, and redistribution while retaining the copyright and license notice. Third-party material in the existing icon is outside the MIT grant and remains subject to the separate terms in [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).

`main` uses `core + common + neoforge`. The pure-Java core is loader- and Minecraft-free; common is recompiled for each target and cannot import loader APIs; target modules contain only loader/version bridges and packaging. Use JDK 21 for 1.21.x and JDK 25 for 26.x.

See [ARCHITECTURE.md](docs/ARCHITECTURE.md), [COMPATIBILITY.md](docs/COMPATIBILITY.md), and [RELEASE_CHECKLIST.md](docs/RELEASE_CHECKLIST.md) for architecture, dependency, and release evidence.

NOT AN OFFICIAL MINECRAFT PRODUCT. NOT APPROVED BY OR ASSOCIATED WITH MOJANG OR MICROSOFT.
