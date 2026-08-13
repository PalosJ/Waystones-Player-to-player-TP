# 发布与交付检查清单

本清单适用于 `main`、`neoforge/1.21.x` 和 `fabric/1.21.x`。完成标记必须有命令输出、运行日志、JAR内容或远端状态作为证据。

## 1. 范围与版本

- [ ] 当前工作目录是唯一正式仓库 `发行版`。
- [ ] 当前分支与目标矩阵中的 branch 一致。
- [ ] `origin` 指向预期仓库；已获取远端且本地未落后或分叉。
- [ ] `gradle/targets.json` 通过 JSON 解析和 `verifyTargetMatrix`。
- [ ] 所有 `mod_version`、加载器元数据、文件名、changelog 和平台版本均为 `1.0.0`。
- [ ] 原 `common/src/main/resources/waystonesplayer.png` 字节未改变。

## 2. 静态与源码边界

- [ ] `git diff --check` 无错误。
- [ ] `core` 不含 Minecraft、Waystones、Balm、Mixin、Fabric 或 NeoForge API。
- [ ] `common` 不含 Fabric/NeoForge API。
- [ ] 客户端类只从客户端入口或 mixin `client` 列表触达。
- [ ] payload 方向、编解码、主线程执行和 `waystonesplayer.network.json` 一致。
- [ ] 服务端重新验证菜单、确切物品/手、`allowsListing()` 目标授权、费用和实际移动；客户端 listed 展示仅是目录输入。
- [ ] Waystones/Balm 内部访问集中在兼容/适配边界。
- [ ] NeoForge SERVER 与 Fabric 全局配置语义没有被混写。

## 3. 每目标源码构建

对矩阵中当前分支的每个 target：

- [ ] 最低整套依赖执行 `clean test build --no-build-cache`。
- [ ] 当前整套依赖执行 `clean test build --no-build-cache`。
- [ ] 两次构建使用该 target 的独立 Gradle project 与运行目录。
- [ ] 当前分支 wrapper 与矩阵一致：NeoForge 为 Gradle 9.2.1，Fabric 为 Gradle 9.5.1；distribution SHA-256 已锁定。
- [ ] warning 已审查，未忽略 Mixin target/refmap、弃用 API 或元数据问题。
- [ ] 上传候选保留最低套件生成的 JAR；当前套件 JAR不覆盖它。
- [ ] JAR 清单的 `WaystonesPlayer-Target` 与 `WaystonesPlayer-Build-Stack` 分别匹配目标和 `minimum`；运行器拒绝 current 或身份不明的二进制。

## 4. 同一发行 JAR运行

把最低构建的同一 SHA-256 JAR分别放入目标最低、关键补丁和当前套件：

```bash
# 在对应统一分支执行；没有 key 套件的目标会自动跳过 key。
python3 scripts/runtime-matrix.py --target <target-id> \
  --profiles minimum key current --sides server client --fail-fast
```

- [ ] 专用服务器到达 `Done`，无客户端类、客户端 Mixin或 GUI 类加载。
- [ ] 客户端完成资源重载、GUI atlas 创建、入口和 Mixin 注册启动信号；这项脚本检查不等同于进入世界或实际玩法验收。
- [ ] 1.21.2/1.21.3 共用 JAR在两个 Minecraft 版本分别执行。
- [ ] 每个运行使用隔离的新目录、配置和世界。
- [ ] 退出方式和退出码已记录；Ctrl-C 启动冒烟不能写成优雅关服。

## 5. 双客户端功能验收

- [ ] 客户端 listed 玩家出现；`allowsListing()=false` 的目标被服务端拒绝。记录客户端可见但服务端拒绝的双层边界；不把第三方 `UPDATE_LISTED` 隐藏或猜 UUID 防护写成已保证能力。
- [ ] 玩家加入、退出、重连或改名时实时更新，滚动锚点和焦点稳定。
- [ ] 空列表、长名称、大量玩家、皮肤失败首字符回退、滚动、tooltip 和叙述正常。
- [ ] 854px 宽屏不移动 Waystones；480px 自动缩放仍为完整名单。
- [ ] 约 426/416px 进入收窄名单；320px 进入头像栏；反复缩放无累计偏移。
- [ ] 1.21.11 搜索、分页、排序、删除和动态目的地重建后偏移正确。
- [ ] 主手与副手都能传送；换走物品、关闭菜单、重复包和自身目标被拒绝。
- [ ] `NEVER`、`FOLLOW_WAYSTONES`、`ALWAYS` 的零费用、足够/不足经验和创造模式正确。
- [ ] Waystones 事件可观察、取消和重定向；取消/失败恢复精确经验且不扣耐久。
- [ ] 同维度、跨维度、相邻落点、目标移动和 post-move 异常符合确认成功语义。
- [ ] 每次确认成功只结算 1 点原生耐久；创造豁免，附魔/损坏使用原生行为。

## 6. 配置验收

- [ ] NeoForge 生成 `waystonesplayer-server.toml`，全局默认、世界覆盖和重载实测。
- [ ] Fabric 生成同名文件、同键/注释/默认值，重启读取实测。
- [ ] Fabric 文档和平台页面没有声称按世界覆盖或热重载。
- [ ] 非法枚举值/损坏文件的失败或回退行为已记录。

## 7. JAR内容与可重现性

- [ ] 文件名与矩阵 `artifactFile` 完全一致。
- [ ] JAR可读取，模组元数据无模板占位符且 Minecraft/Loader/依赖范围精确。
- [ ] 包含 LICENSE、THIRD_PARTY_NOTICES、语言、网络契约、Mixin配置和既有图标。
- [ ] 图标可读且元数据引用正确；不重新绘制或替换。
- [ ] 服务端/客户端 Mixin分别位于 `mixins` / `client`，服务端不会加载客户端 accessor。
- [ ] 不含 `net/blay09`、Fabric API、NeoForge、依赖 JAR、私有路径、凭据或无关大文件。
- [ ] 归档关闭时间戳并固定顺序；两次相同输入构建的 SHA-256 一致。
- [ ] `unzip -t` 或等价 ZIP 完整性检查通过。

## 8. 平台文件

- [ ] Modrinth：客户端 Required、服务端 Required、正确游戏版本/Loader、Waystones/Balm Required；Fabric 文件另有 Fabric API Required，NeoForge 文件不添加它。
- [ ] CurseForge：英文描述、正确 Game Version/Mod Loader、Waystones/Balm Required；Fabric 文件另有 Fabric API Required，NeoForge 文件不添加它，Release 类型。
- [ ] Custom License 链接/文本与根 LICENSE 一致，不标成开源许可证。
- [ ] beta NeoForge 文件在 changelog 明确披露。
- [ ] 1.21.2/1.21.3 共用文件同时勾选两版；其他文件只勾选一版。
- [ ] 页面包含非官方声明、既有图标署名和“相邻非窒息不是绝对安全”说明。
- [ ] 上传清单同时保存文件名、大小、SHA-256、分支提交、目标和依赖。

## 9. GitHub 与分支收尾

- [ ] 审查完整 diff、暂存清单、敏感信息和大文件；不提交 JAR、缓存、日志或本地运行文件。
- [ ] 先提交 canonical `main`，再按已确认基线移植两条统一分支。
- [ ] core、完整 common、目标矩阵、运行脚本、文档、许可证与既有图标和统一分支记录的 canonical 提交逐文件一致。
- [ ] 普通推送，不强推，不创建 PR、标签、GitHub Release，也不代为上传平台。
- [ ] 本地 HEAD、`origin/<branch>` 和 GitHub 远端提交一致。
- [ ] 三条分支 GitHub Actions 全绿，main/统一分支共享漂移门禁通过。
- [ ] 仅在新统一分支远端可验证且 CI 全绿后，删除本地/远端 `fabric/1.21.1` 与 `neoforge/1.21.11`。
- [ ] 最终切回 clean `main`，刷新仓库外的交付 JAR、SHA-256、changelog 和检查结果。
