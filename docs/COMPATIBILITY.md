# 兼容性与升级指南

本文描述每个正式目标的依赖边界、API 断点和升级验证。精确、机器可检查的版本字符串和文件名以 [gradle/targets.json](../gradle/targets.json) 为唯一数据源；本文表格用于人工审查，不能单独替代构建与运行证据。

## 支持定义

“正式支持”要求同一目标同时满足：

1. 目标工程用该 Minecraft/映射和最低依赖套件编译。
2. 最低与当前整套依赖分别通过 `clean test build`，不单独替换某一个上游 JAR。
3. 最低套件生成的同一发行 JAR在最低、关键断点和当前运行时加载。
4. 客户端与专用服务器均通过物理端类加载检查。
5. 玩家目录、主副手、客户端 listed 展示与服务端 `allowsListing()` 授权、费用、回滚、耐久、事件取消、跨维度和失败界面行为通过对应运行验收。
6. 最终 JAR、平台元数据、SHA-256 和本文件声明一致。

这些证据只能降低已知风险，不能证明任何软件“绝对无缺陷”。未运行的场景必须明确披露，不能以编译成功替代。

## 分支与产物

| 分支 | 加载器 | Minecraft | 产物数 | 说明 |
|---|---|---|---:|---|
| `main` | NeoForge | 1.21.1 | 1 | 永久 canonical 主线 |
| `neoforge/1.21.x` | NeoForge | 1.21.2–1.21.11 | 9 | .2/.3 共用一份 JAR |
| `fabric/1.21.x` | Fabric | 1.21.1–1.21.11 | 10 | .2/.3 共用一份 JAR |

总计 20 个可上传 JAR，全部版本号 `1.0.0`。除明确共享的 1.21.2/1.21.3 外，每份元数据只接受一个 Minecraft 小版本。

## Canonical NeoForge 1.21.1

| 组件 | 最低 | 当前 | 声明范围 |
|---|---:|---:|---|
| Minecraft | 1.21.1 | 1.21.1 | `[1.21.1]` |
| Java | 21 | 21 | 21 |
| NeoForge | 21.1.229 | 21.1.248 | `[21.1.229,21.2)` |
| Waystones | 21.1.36 | 21.1.40 | `[21.1.36,21.2)` |
| Balm | 21.0.62 | 21.0.64 | `[21.0.62,21.1)` |

当前套件：

```bash
./gradlew clean test build \
  -PdependencyStack=current \
  -Pneo_version=21.1.248 \
  -Pwaystones_version=21.1.40+1.21.1 \
  -Pbalm_version=21.0.64+1.21.1 \
  --no-build-cache
```

最低套件：

```bash
./gradlew clean test build \
  -PdependencyStack=minimum \
  -Pneo_version=21.1.229 \
  -Pwaystones_version=21.1.36+1.21.1 \
  -Pbalm_version=21.0.62+1.21.1 \
  --no-build-cache
```

## NeoForge 1.21.x 锁定端点

Waystones/Balm 表中的版本需加对应 `+1.21.x` 构件后缀；共享行使用 `+1.21.3`。完整字符串见 JSON。

| 目标 | NeoForge 最低 / 当前 | Waystones 最低 / 当前 | Balm 最低 / 当前 | 备注 |
|---|---|---|---|---|
| 1.21.2 + 1.21.3 | .2 运行：21.2.0-beta / 21.2.1-beta；.3 编译/运行：21.3.56 / 21.3.97 | 21.3.2 / 21.3.3 | 21.3.1 / 21.3.5 | 同一 1.21.3 基线 JAR，两版分别运行 |
| 1.21.4 | 21.4.121 / 21.4.157 | 21.4.1 / 21.4.19 | 21.4.1 / 21.4.42 | 稳定 Loader |
| 1.21.5 | 21.5.74 / 21.5.98 | 21.5.1 / 21.5.11 | 21.5.3 / 21.5.26 | 稳定 Loader |
| 1.21.6 | 21.6.0-beta / 21.6.20-beta | 21.6.1 / 21.6.1 | 21.6.1 / 21.6.3 | 官方 beta Loader |
| 1.21.7 | 21.7.1-beta / 21.7.25-beta | 21.7.1 / 21.7.2 | 21.7.2 / 21.7.3 | 官方 beta Loader |
| 1.21.8 | 21.8.9 / 21.8.54 | 21.8.1 / 21.8.5 | 21.8.3 / 21.8.10 | 精确限制 1.21.8 |
| 1.21.9 | 21.9.1-beta / 21.9.16-beta | 21.9.1 / 21.9.2 | 21.9.1 / 21.9.4 | 官方 beta Loader |
| 1.21.10 | 21.10.63 / 21.10.64 | 21.10.1 / 21.10.5 | 21.10.3 / 21.10.11 | context/hand 断点 |
| 1.21.11 | 21.11.42 / 21.11.45 | 21.11.2 / 21.11.9 | 21.11.2 / 21.11.9.1 | Identifier/Balm 大断点 |

1.21.2、1.21.6、1.21.7、1.21.9 文件必须在平台元数据和 changelog 中明确写出 NeoForge beta，不能标成稳定加载器。

部分 Waystones 正式版的 Gradle 元数据仍引用同系列 Balm `SNAPSHOT`。矩阵中的“最低 Balm”因此不是数值最小的任意正式版，而是首个不低于该构建要求的稳定发布。目标与二进制运行工程排除 Waystones 的传递 Balm，并严格加载矩阵指定的稳定版本，防止缓存中的可变 SNAPSHOT 偷换验收组合。Waystones NeoForge 元数据中无法解析或非必需的 JourneyMap 桥接依赖也从本项目的编译/冒烟配置排除；这不改变 Waystones 自身对已安装地图模组的运行时集成。

1.21.11 还有构件发布边界：Waystones 21.11.2 与 21.11.4 分别只暴露 `waystones-neoforge-1.21.11`、`waystones-fabric-1.21.11` 自定义 Gradle capability，21.11.9 又恢复各自默认 capability。矩阵逐加载器、逐栈声明该差异，不能把最低构件的解析规则机械套到当前构件。

## Fabric 1.21.x 锁定端点

Waystones、Balm 与 Fabric API 使用对应 Minecraft 后缀；共享目标的发行编译基线为 1.21.3，1.21.2 运行时换用对应 Fabric API 1.21.2 构件。

| 目标 | Loader 最低 / 当前 | Fabric API 最低 / 当前 | Waystones 最低 / 当前 | Balm 最低 / 当前 |
|---|---|---|---|---|
| 1.21.1 | 0.17.3 / 0.19.3 | 0.116.7 / 0.116.15 | 21.1.36 / 21.1.40 | 21.0.62 / 21.0.64 |
| 1.21.2 + 1.21.3 | 0.16.8 / 0.19.3 | .2：0.106.1 / 0.106.1；.3：0.106.1 / 0.114.1 | 21.3.2 / 21.3.3 | 21.3.1 / 21.3.5 |
| 1.21.4 | 0.16.9 / 0.19.3 | 0.110.5 / 0.119.4 | 21.4.1 / 21.4.19 | 21.4.1 / 21.4.41 |
| 1.21.5 | 0.16.10 / 0.19.3 | 0.119.5 / 0.128.2 | 21.5.1 / 21.5.11 | 21.5.3 / 21.5.25 |
| 1.21.6 | 0.16.14 / 0.19.3 | 0.127.0 / 0.128.2 | 21.6.1 / 21.6.1 | 21.6.1 / 21.6.1 |
| 1.21.7 | 0.16.14 / 0.19.3 | 0.128.1 / 0.129.0 | 21.7.1 / 21.7.2 | 21.7.2 / 21.7.3 |
| 1.21.8 | 0.16.14 / 0.19.3 | 0.129.0 / 0.136.1 | 21.8.1 / 21.8.5 | 21.8.3 / 21.8.10 |
| 1.21.9 | 0.17.2 / 0.19.3 | 0.133.14 / 0.134.1 | 21.9.1 / 21.9.2 | 21.9.1 / 21.9.4 |
| 1.21.10 | 0.17.2 / 0.19.3 | 0.134.1 / 0.138.4 | 21.10.1 / 21.10.5 | 21.10.6 / 21.10.11 |
| 1.21.11 | 0.18.1 / 0.19.3 | 0.139.4 / 0.141.6 | 21.11.2 / 21.11.9 | 21.11.2 / 21.11.9.1 |

Fabric Balm 的模组 ID 是真实断点：

- 1.21.1：`balm`。
- 1.21.2/1.21.3 共用线：`balm-fabric`，且该旧 JAR不提供 `balm`。
- 1.21.4：依赖 `balm-fabric`；最低/关键 Balm 的运行时主 ID 仍是 `balm-fabric`，当前 Balm 的主 ID 是 `balm` 并以 `provides` 保留 `balm-fabric` 别名。机器矩阵分别记录元数据依赖 ID 和实际运行时 ID。
- 1.21.5–1.21.11：依赖 `balm`；上游同时提供 `balm-fabric` 别名。

部分旧 Balm Fabric POM 对 KumaAPI 声明了跨整个 21.x 的宽版本范围，但 Balm JAR 本身已内嵌与该 Minecraft 线匹配的 KumaAPI。构建和运行器明确排除这份外部重复依赖，避免 Gradle 把最新 Minecraft 线的 KumaAPI 当成旧版根模组加载；Balm 内嵌依赖仍保持不变。

1.21.4、1.21.10 与 1.21.11 还固定了一组关键补丁运行套件，用于覆盖菜单载体 getter、requirement `consume` 签名和 Identifier/platform 大断点；NeoForge 与 Fabric 都用各自加载器套件执行。精确版本由 `gradle/targets.json` 的 `key` 字段锁定，其他目标不得虚构与最低或当前重复的“关键套件”。

## 为什么 1.21.2 与 1.21.3 共用

Waystones 和 Balm 的官方 1.21.3 源码线明确写有：

- 编译 Minecraft：1.21.3；
- 最低 Minecraft：1.21.2；
- 支持 Minecraft：1.21.2、1.21.3。

其真实 NeoForge/Fabric JAR元数据也把最低版本设为 1.21.2。因此共用是上游的有意兼容策略，不是文件名巧合。

Waystones 21.3.1 虽然声明同时支持 1.21.2/1.21.3，但在 1.21.3 创建新世界时其 `JigsawPlacementPlacerMixin` 仍指向旧的 Registry 方法并会崩溃；上游 21.3.2 的发布说明与源码均专门修复该问题。因此本项目明确排除 21.3.1，把 21.3.2 作为共享目标最低版本，并仍对两个 Minecraft 版本分别启动验证，避免只凭元数据认定兼容。

这只证明上游依赖设计为共用，不自动证明本附属共用。本附属仍需把同一最低构建 JAR分别放入 1.21.2 与 1.21.3 的最低/当前套件，验证 Mixin、网络、屏幕和传送行为后才能发布。

共用产物的 Fabric Loader 下限为 `0.16.8`：1.21.2 的同线 API 可在 0.16.7 启动，但 1.21.3 对应 Fabric API 的 BlockView/Rendering Data 子模块明确要求 0.16.8。共用 JAR 的元数据必须使用同时满足两版的下限。

## 已确认 API 断点

| 范围 | 关键断点 | 维护要求 |
|---|---|---|
| 1.21.1 | `ResourceLocation`、旧屏幕列表、同步 transient 传送 | 保持 canonical 行为；不能切到会验证真实方块的异步入口 |
| 1.21.2/1.21.3 | Balm 只有 Runnable 初始化；菜单没有稳定 Warp Stone getter | 使用 Runnable family 与菜单载体 Mixin |
| 1.21.4–1.21.8 | 旧 Balm module；legacy input；菜单 getter 在 21.4 线中途才出现 | 不依赖 getter；继续使用载体 |
| 1.21.9 | GUI 输入事件签名变化 | 单独 event-input screen family |
| 1.21.10 | Waystones 增加 hand/context-aware requirement | 单独 context-hand family，保持本附属固定耐久 |
| 1.21.11 | Minecraft `Identifier`、皮肤 API、Balm `platform.*`、分页屏幕几何 | 新 platform/identifier family；动态控件从原始坐标重新偏移 |

Waystones 21.10.1 的费用管理器调用 `WarpRequirement.consume(Player)`，从 21.10.2 起改为调用 `consume(context, Player)`。最低版本编译出的适配类显式提供两个 JVM 签名，并在新版本写入 Warp Stone 手位、关闭上游 item modifier；缺少这层二进制桥接会在当前 Waystones 上静默走默认空实现而免除经验费用。

Waystones/Balm 内部类或 Mixin 目标只能存在于兼容边界。若签名改变，安全失败是禁用玩家目的地并告警，而不是放宽菜单校验、跳过费用或直接调用裸 `teleportTo`。

## 配置兼容

三个模式与默认值在所有目标相同：

| 键 | 类型 | 默认 |
|---|---|---|
| `playerTeleportExperienceMode` | `NEVER / FOLLOW_WAYSTONES / ALWAYS` | `NEVER` |

NeoForge：

- 使用 `ModConfig.Type.SERVER`。
- 保留 `config` 全局默认、世界 `serverconfig` 覆盖和加载器重载。
- 运行时读取 active config，不缓存为启动常量。

Fabric：

- 使用无额外依赖的小型 TOML存储生成/读取 `config/waystonesplayer-server.toml`。
- 保持同键、注释、默认值和重启读取。
- 它是实例全局配置；不得在 README 或平台页面声称按世界覆盖或热重载。

## 最低构建与同一二进制运行

每个正式目标的上传 JAR始终由最低依赖套件生成。验证分两层：

- 源码兼容：最低和当前依赖各自重新执行 `clean test build`，发现编译/API 漂移。
- 二进制兼容：只保留最低套件的发行 JAR，把同一文件放入最低、关键断点、当前运行目录；不得用当前依赖重编译出的另一个 JAR替代。

1.21.2/1.21.3 共用目标还必须对两个 Minecraft 版本分别执行二进制运行。每个运行目录隔离，禁止复用其他分支的世界、配置、模组或缓存来生成验收日志。

在对应统一分支可用以下自动化运行单个目标；脚本会核对最低构建清单、实际 Mod List 精确版本、专服 `Done` 与客户端资源/GUI 图集信号，并终止完整 Gradle/游戏进程组：

```bash
python3 scripts/runtime-matrix.py \
  --target <target-id> \
  --profiles minimum key current \
  --sides server client \
  --fail-fast
```

没有 `key` 套件的目标会自动跳过该 profile。Linux CI 对客户端追加 `--xvfb`。这是启动、二进制链接、Mixin 和资源冒烟，不替代双客户端内的交互、费用与事件人工验收。

## 逐目标升级流程

1. 从官方 Minecraft、Loader、Waystones、Balm 与 Fabric API 源码/元数据确认目标仍存在且依赖成套兼容。
2. 更新 `gradle/targets.json` 的 snapshotDate、最低/当前套件和 adapter family；运行 `verifyTargetMatrix`。
3. 比较当前 target 的已解析上游源码：菜单、`finishUsingItem`、传送 context/结果、requirement、屏幕、初始化与网络线程。
4. 能复用既有 family 时不复制；出现真实编译断点时新增最小适配，并保持 core/协议不变。
5. 执行最低和当前源码构建；检查 warning、Mixin refmap/目标和最终 JAR内容。
6. 使用最低构建 JAR执行最低/关键/当前客户端与专服启动。
7. 至少双客户端验证：客户端 listed 展示与服务端 `allowsListing()` 授权（包括可见但被拒绝的状态差异）、加入/退出/改名、主副手、经验三模式、事件取消、实际移动、回滚、耐久、跨维度和 GUI 三种宽度；不把第三方逐客户端 `UPDATE_LISTED` 隐藏的猜 UUID 防护列为本模组承诺。
8. 检查 NeoForge 世界配置与 Fabric 全局配置的真实差异。
9. 更新 README、平台文案、changelog、文件清单和 SHA-256。
10. 获取远端、核对分支未落后/分叉，普通推送；禁止强推。

更新依赖、修复 Bug、重要功能或移植都不能自行提升模组版本。版本号只有用户明确确认后才同步修改 Gradle、元数据、文档、CI 和产物名。

## 分支同步

共享行为先落 `main`，然后移植到两条统一分支。统一分支记录 canonical main 基线，CI 逐文件比对 pure Java `core`、完整 `common`、目标矩阵与运行脚本、README/CONTEXT/全部文档以及许可证和既有图标；main push 也检查两条远端统一分支是否落后。只有加载器入口、目标工程、版本适配族、构建器和加载器元数据允许按分支不同。

旧 `fabric/1.21.1` 与 `neoforge/1.21.11` 只有在新统一分支已经普通推送、远端提交可验证且 GitHub Actions 全绿后才能删除本地与远端引用。删除旧分支不删除对应正式目标。
