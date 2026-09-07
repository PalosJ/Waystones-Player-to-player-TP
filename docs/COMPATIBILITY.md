# 兼容性与升级指南

本文描述每个正式目标的依赖边界、API 断点和升级验证。精确、机器可检查的版本字符串和文件名以 [gradle/targets.json](../gradle/targets.json) 为唯一数据源；本文表格用于人工审查，不能单独替代构建与运行证据。

## 当前维护范围

自 2026-09-07 起仅继续维护 NeoForge／Fabric 的 1.21.1、1.21.11、26.1.2、26.2，共 8 个目标／8 个游戏组合。1.21.2–1.21.10、26.1 和 26.1.1 停止后续更新，退出构建、CI 和本地产物同步；历史提交与验收记录保留。NeoForge 1.21.1 继续作为 `main` 基线。

1.21.x 使用 Java 21，26.x 使用 Java 25；版本保持 1.0.1、协议保持 2。每个目标各自重新编译 common。

## 精确依赖端点

以下为最低 / 当前完整字符串，与矩阵同步；此次未提升最低要求。Fabric API 和 Shogi 按目标存在时列出。

| 目标 | Loader | Waystones | Balm | Fabric API / Shogi |
|---|---|---|---|---|
| `neoforge-1.21.1` | 21.1.229 / 21.1.249 | 21.1.36+1.21.1 / 21.1.42+1.21.1 | 21.0.62+1.21.1 / 21.0.65+1.21.1 | — |
| `neoforge-1.21.11` | 21.11.42 / 21.11.45 | 21.11.2 / 21.11.9 | 21.11.2 / 21.11.9.1 | — |
| `fabric-1.21.1` | 0.17.3 / 0.19.5 | 21.1.36+1.21.1 / 21.1.42+1.21.1 | 21.0.62+1.21.1 / 21.0.65+1.21.1 | fabricApi: 0.116.7+1.21.1 / 0.116.17+1.21.1 |
| `fabric-1.21.11` | 0.18.1 / 0.19.5 | 21.11.2 / 21.11.9 | 21.11.2 / 21.11.9.1 | fabricApi: 0.139.4+1.21.11 / 0.141.6+1.21.11 |
| `neoforge-26.1.2` | 26.1.2.0-beta / 26.1.2.97 | 26.1.2.1 / 26.1.2.15 | 26.1.1.2 / 26.1.2.13 | shogi: 26.1.0.1 / 26.1.2.8 |
| `neoforge-26.2` | 26.2.0.0-beta / 26.2.0.66 | 26.2.0.1 / 26.2.0.11 | 26.2.0.1 / 26.2.0.7 | shogi: 26.2.0.1 / 26.2.0.5 |
| `fabric-26.1.2` | 0.19.1 / 0.19.3 | 26.1.2.1 / 26.1.2.15 | 26.1.1.2 / 26.1.2.13 | fabricApi: 0.145.4+26.1.2 / 0.155.2+26.1.2；shogi: 26.1.0.1 / 26.1.2.8 |
| `fabric-26.2` | 0.19.3 / 0.19.3 | 26.2.0.1 / 26.2.0.11 | 26.2.0.1 / 26.2.0.7 | fabricApi: 0.152.1+26.2 / 0.158.0+26.2；shogi: 26.2.0.1 / 26.2.0.5 |

## 真实 API 与依赖边界

- 1.21.1 保持旧 module、ResourceLocation、旧屏幕输入与同步 transient 传送入口；NeoForge main 为共享体验基线。
- 1.21.11 使用 Identifier、事件输入、皮肤 API 和 Balm platform module。Waystones 21.11.2 / 21.11.4 的自定义 Gradle capability 与 current 默认 capability 分别锁定；不能混用解析规则。两加载器保留 key 套件覆盖这一边界。Fabric key 的 API 0.141.6 提供 Balm 所需模型加载入口。
- 26.1.2 / 26.2 使用 load-context、Shogi 和 graphics extractor；minimum 分页界面与 current 滚动列表均须验证。新列表在 270px 容器内缩 25px，原生管理／维度按钮在列表左侧 33px。布局读取实际原生控件边界并保留容器右边界；不把第三方控件作为移动对象。1.21.1 / 1.21.11 继续使用原来的布局入口。
- 26.x 覆盖每个原生 WarpStoneItem 的材质变体、完整默认规则、warpSettings、damage_item 延迟结算与耐久开关；不能把一次结算当成固定损耗 1 点。未知效果与非法金额在消费前拒绝，异步回调回到服务器线程，规则重载后重新计算。
- 原生异步传送入口在不同依赖端点间有差异，兼容边界优先调用可用公共入口并保留同步包装回退；不得绕开锁定上下文直接裸传送。早期 Shogi 的副手上下文修复只应用于标记的玩家请求。
- 当前 Shogi 加载器 JAR 内嵌公共 API 时，从锁定 JAR 确定性提取同一字节用于编译；禁止单独解析浮动 Shogi API 快照。最低栈按矩阵声明使用对应正式 API。
- Waystones 的传递 Balm、外部重复 KumaAPI 和非必需 JourneyMap 编译桥接从构建/冒烟配置排除，严格加载整套固定前置；这不改变原生已安装模组的运行集成。Fabric 当前维护目标依赖 Balm ID 为 `balm`。
- NeoForge 26.x minimum 使用官方 beta 加载器，平台说明应如实披露。不能把数值相近但 Minecraft 元数据不同的前置互换。

旧 26.1.1 的固定上游源码工具只保留用于历史来源记录，不再由维护矩阵或 CI 调用；其存在不代表继续支持该版本。旧版本兼容调查可从历史提交检索。

## 支持与升级要求

目标通过最低/当前源码构建、同一 minimum JAR 的最低/key/当前客户端与专服启动，仍不等于玩法或 GUI 验收。每项证据必须绑定源码提交、SHA 和整套前置；交互状态见 [VALIDATION.md](VALIDATION.md)。上游内部类、反射和 Mixin 只留在兼容边界，不能通过放宽菜单或费用校验掩盖 API 不兼容。

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

- 使用无额外依赖的小型 TOML存储生成/读取 `config/waystonesptpt-server.toml`。
- 保持同键、注释、默认值和重启读取。
- 它是实例全局配置；不得在 README 或平台页面声称按世界覆盖或热重载。

## 最低构建与同一二进制运行

每个正式目标的上传 JAR始终由最低依赖套件生成。验证分两层：

- 源码兼容：最低和当前依赖各自重新执行 `clean test build`，发现编译/API 漂移。
- 二进制兼容：只保留最低套件的发行 JAR，把同一文件放入最低、关键断点、当前运行目录；不得用当前依赖重编译出的另一个 JAR替代。

每个运行目录隔离，禁止复用其他分支的世界、配置、模组或缓存来生成验收日志。

在对应统一分支可用以下自动化运行单个目标；脚本会核对最低构建清单、实际 Mod List 精确版本、专服 `Done` 与客户端资源/GUI 图集信号，并终止完整 Gradle/游戏进程组：

```bash
python3 scripts/runtime-matrix.py \
  --target <target-id> \
  --profiles minimum key current \
  --sides server client \
  --skip-build \
  --binary <minimum-artifact.jar> \
  --expected-sha256 <sha256> \
  --expected-commit <40-hex-source-commit> \
  --fail-fast
```

没有 `key` 套件的目标会自动跳过该 profile。显式二进制、SHA-256 和提交参数必须一起出现；artifact 模式必须使用 `--skip-build`。Linux CI 对客户端追加 `--xvfb`。GitHub Build 会从本次 minimum artifacts 生成并上传 release manifest；Runtime 仅接受同分支、精确 HEAD 的成功 Build artifact，并要求 JAR与该 manifest 的文件名、目标、版本、minimum 栈、提交和 SHA-256 完全一致。手动触发必须给出 Build run ID，定时触发无法解析当前提交 artifact 时直接失败。这是启动、二进制链接、Mixin 和资源冒烟，不替代双客户端内的交互、费用与事件人工验收。

## 逐目标升级流程

1. 从官方 Minecraft、Loader、Waystones、Balm 与 Fabric API 源码/元数据确认目标仍存在且依赖成套兼容。
2. 更新 `gradle/targets.json` 的 snapshotDate、最低/当前套件和 adapter family；运行 `verifyTargetMatrix`。
3. 比较当前 target 的已解析上游源码：菜单、`finishUsingItem`、传送 context/结果、requirement、屏幕、初始化与网络线程。
4. 能复用既有 family 时不复制；出现真实编译断点时新增最小适配，并保持 core/协议不变。
5. 执行最低和当前源码构建；检查 warning、Mixin refmap/目标和最终 JAR内容。
6. 使用最低构建 JAR执行最低/关键/当前客户端与专服启动。
7. 至少双客户端验证：客户端 listed 展示、服务端在线 UUID解析（包括正常注册的假玩家）、加入/退出/改名、主副手、经验三模式、事件取消、Waystones 原生相邻/目标方块中心落点、回滚、耐久、跨维度和 GUI 三种宽度；不把第三方隐藏玩家 UUID防猜测列为本模组承诺。
8. 检查 NeoForge 世界配置与 Fabric 全局配置的真实差异。
9. 更新 README、平台文案、changelog、文件清单和 SHA-256。
10. 获取远端、核对分支未落后/分叉，普通推送；禁止强推。

更新依赖、修复 Bug、重要功能或移植都不能自行提升模组版本。版本号只有用户明确确认后才同步修改 Gradle、元数据、文档、CI 和产物名。

## 分支同步

共享行为先落 `main`，然后移植到两条 1.21.x 和两条 26.x 统一分支。统一分支记录 canonical main 基线，CI 逐文件比对 pure Java `core`、完整 `common`、目标矩阵与运行脚本、README/CONTEXT/全部文档以及许可证和既有图标；main push 也检查四条远端统一分支是否落后。只有加载器入口、目标工程、版本适配族、构建器和加载器元数据允许按分支不同。

五条维护分支名称保持不变；停止维护的目标从 settings、CI 和目标矩阵移除，不建立新分支。
