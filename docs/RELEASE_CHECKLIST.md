# 发布与交付检查清单

本清单适用于 `main`、两条 `1.21.x` 统一分支和两条 `26.x` 统一分支。完成标记必须有命令输出、运行日志、JAR内容或远端状态作为证据。

## 1. 范围与版本

- [ ] 当前工作目录是唯一正式仓库 `发行版`。
- [ ] 当前分支与目标矩阵中的 branch 一致。
- [ ] `origin` 指向预期仓库；已获取远端且本地未落后或分叉。
- [ ] `gradle/targets.json` 通过 JSON 解析和 `verifyTargetMatrix`。
- [ ] `python3 scripts/verify-target-matrix.py` 通过，settings 与 Build/Runtime workflow 的 target 列表和中央矩阵一致。
- [ ] `python3 -m unittest discover -s scripts/tests -v` 通过，包含 target.properties family/source root 与 artifact provenance 负向测试。
- [ ] 所有 `mod_version`、加载器元数据、文件名、changelog 和平台版本均为 `1.0.1`。
- [ ] 原 `common/src/main/resources/waystonesptpt.png` 字节未改变。
- [ ] 1.21.x 使用 Java 21，26.x 使用 Java 25；26.x 元数据把 Waystones、Balm、Shogi 都声明为强制依赖。

## 2. 静态与源码边界

- [ ] `git diff --check` 无错误。
- [ ] `core` 不含 Minecraft、Waystones、Balm、Mixin、Fabric 或 NeoForge API。
- [ ] `common` 不含 Fabric/NeoForge API。
- [ ] 客户端类只从客户端入口或 mixin `client` 列表触达。
- [ ] payload 方向、编解码、主线程执行和 `waystonesptpt.network.json` 一致。
- [ ] 服务端重新验证菜单、确切物品/手、在线目标 UUID、费用和实际移动；客户端 listed 展示仅是目录输入。
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
- [ ] JAR 清单的 `WaystonesPTPT-Target`、`WaystonesPTPT-Build-Stack` 与 `WaystonesPTPT-Source-Commit` 分别匹配目标、`minimum` 和精确源码提交；运行器拒绝 current、提交不明或身份不明的二进制。
- [ ] 26.1.1 从固定官方提交和 SHA 锁定补丁重建；Build/Runtime 上游 JAR SHA 一致，清单记录上游提交、补丁 SHA、闭包和 JAR SHA。

## 4. 同一发行 JAR运行

把最低构建的同一 SHA-256 JAR分别放入目标最低、关键补丁和当前套件：

```bash
# 在对应统一分支执行；没有 key 套件的目标会自动跳过 key。
python3 scripts/runtime-matrix.py --target <target-id> \
  --profiles minimum key current --sides server client --skip-build \
  --binary <minimum-artifact.jar> --expected-sha256 <sha256> \
  --expected-commit <40-hex-source-commit> --fail-fast
```

- [ ] 专用服务器到达 `Done`，无客户端类、客户端 Mixin或 GUI 类加载。
- [ ] 客户端完成资源重载、GUI atlas 创建、入口和 Mixin 注册启动信号；这项脚本检查不等同于进入世界或实际玩法验收。
- [ ] 1.21.2/1.21.3 共用 JAR在两个 Minecraft 版本分别执行。
- [ ] 每个运行使用隔离的新目录、配置和世界。
- [ ] 退出方式和退出码已记录；Ctrl-C 启动冒烟不能写成优雅关服。
- [ ] Runtime workflow 的每个 job 记录 Build run ID；artifact 与当前分支精确 HEAD、文件名、target、版本、minimum 栈、manifest 提交和 SHA-256 全部一致，期间没有重建 JAR。
- [ ] Build provenance job 使用 `python3 scripts/release-manifest.py --branch <branch> --artifact-root build/runtime-artifacts --output build/release-manifest.json`，并上传同一 run 的 release manifest artifact。

## 5. 双客户端功能验收

- [ ] 客户端 listed 玩家出现；服务端可解析普通在线玩家、`allowsListing()=false` 玩家和正常注册的 Carpet 假人。客户端隐藏状态不作为服务端传送权限；不把第三方隐藏玩家 UUID防猜测写成已保证能力。
- [ ] 玩家加入、退出、重连或改名时在 5 tick 内更新；连接变化立即刷新，稳定目录不重复排序，滚动锚点和 UUID 行复用稳定；焦点目标退出时回退到邻近行。Waystones 搜索不筛选玩家目录。
- [ ] 玩家搜索支持大小写与部分名字，显示匹配人数及无结果提示；输入回到列表顶部，清空恢复全目录，隐藏行不丢失接收状态与头像缓存。命名模式缩放保留查询，头像模式及关闭菜单清空；E/数字/Enter 不触发容器操作，Tab 导航、Esc 关闭正常。
- [ ] 接收开关为原生 20x20 按钮，绿色勾/红色叉/灰色待确认，悬停和键盘叙述完整；工具行间隙 2px、相对玩家按钮中心对齐，头像栏只显示开关。标题的悬停范围不覆盖控件。
- [ ] 空列表、长名称、`String.hashCode` 碰撞名称、大量玩家、皮肤失败/20-tick 重试/首字符回退、滚动、tooltip 和叙述正常；渲染期连接/Profile 查询为零。
- [ ] 854px 宽屏不移动 Waystones；480px 自动缩放仍为完整名单。
- [ ] 约 426/416px 进入收窄名单；320px 进入头像栏；反复缩放无累计偏移。
- [ ] 1.21.11 搜索、分页、排序、删除和动态目的地重建后偏移正确。
- [ ] 主手与副手都能传送；换走物品、关闭菜单、重复包和自身目标被拒绝。
- [ ] `NEVER`、`FOLLOW_WAYSTONES`、`ALWAYS` 的零费用、足够/不足经验和创造模式正确。
- [ ] Waystones 事件可观察、取消和重定向；取消/失败恢复精确经验且不扣耐久。
- [ ] 非空费用规则解析为空、负数、NaN、无穷、缩放/组合溢出、未知第三方 requirement 和事件替换费用均在扣费/移动前拒绝；合法零费用保留，创造模式不能绕过锁。
- [ ] 同维度、跨维度、相邻落点、目标移动和 post-move 异常符合确认成功语义；已移动但最终位置不匹配验证目标时不退费、结算一次耐久并显示兼容性警告。
- [ ] 四周封堵时由 Waystones 原生回退到目标方块中心；目标位于方块、空中、流体或悬崖时本附属不拒绝，也不把这些位置写成安全保证。
- [ ] 每次确认成功只结算一次该版本适用的原生耐久；26.x 按损耗规则总额及耐久开关处理；创造豁免，附魔/损坏使用原生行为。
- [ ] 成功移动后物品仍在原手、被同组件替换、原引用移到其他槽位及被第三方彻底移除四种耐久路径均符合语义；不得损坏无关物品或回滚已完成移动。

- [ ] 本人接收默认允许，关闭后目录行禁用且有 tooltip／键盘叙述，本人仍可向外传送。
- [ ] 接收设置经重连、死亡、换维度、正常重启后保留，不同世界独立；正常假人默认允许。
- [ ] 设置只允许本人提交，确认等待、旧会话包、超长列表和准备期间关闭接收均验证。
- [ ] 全部原生 Warp Stone、完整 minimum／关键断点／current 默认规则、warpSettings、合法算术、耐久开关、附魔和创造模式均覆盖。
- [ ] 延迟 Future、后台完成、重复请求、200 tick 超时、关菜单、断线重连及目标走动不产生迟到扣费或移动。
- [ ] 26.x 分页与滚动列表的搜索、排序、删除、筛选和动态重建持续对齐，不移动第三方控件。
- [ ] 320、416／426、480、854 等逻辑宽度和不同 GUI 缩放通过；连续打开／关闭／缩放 100 次无偏移或残留。
- [ ] 普通石碑、卷轴、Warp Plate 及其他模组传送不受个人设置或新增事务规则影响。
- [ ] 50／200／1000 玩家目录记录同机基线与新版本帧时间、刷新耗时、分配及关闭后的对象释放；纯算法计时不能替代真实帧时间。

## 6. 配置验收

- [ ] NeoForge 生成 `waystonesptpt-server.toml`，全局默认、世界覆盖和重载实测。
- [ ] Fabric 生成同名文件、同键/注释/默认值，重启读取实测。
- [ ] Fabric 文档和平台页面没有声称按世界覆盖或热重载。
- [ ] 非法枚举值/损坏文件的失败或回退行为已记录。

- [ ] 新旧配置并存时新文件优先；旧文件迁移不删除原文件，世界覆盖和重载保留。
- [ ] 迁移本模组配置不触发其他模组 SERVER 配置卸载／重载。
- [ ] 附属费用配置损坏回退 NEVER 并明确日志，合法收费配置的规则错误拒绝本次传送。

## 7. JAR内容与可重现性

- [ ] 文件名与矩阵 `artifactFile` 完全一致。
- [ ] JAR可读取，模组元数据无模板占位符且 Minecraft/Loader/依赖范围精确。
- [ ] 包含 LICENSE、THIRD_PARTY_NOTICES、语言、网络契约、Mixin配置和既有图标。
- [ ] manifest 的 source commit 与 `git rev-parse HEAD`、Build artifact 元数据和运行证据一致。
- [ ] 图标可读且元数据引用正确；不重新绘制或替换。
- [ ] 服务端/客户端 Mixin分别位于 `mixins` / `client`，服务端不会加载客户端 accessor。
- [ ] 不含 `net/blay09`、Fabric API、NeoForge、依赖 JAR、私有路径、凭据或无关大文件。
- [ ] 归档关闭时间戳并固定顺序；两次相同输入构建的 SHA-256 一致。
- [ ] `unzip -t` 或等价 ZIP 完整性检查通过。

## 8. 平台文件

- [ ] 每目标构建、启动、玩法、GUI、性能、公开安装六类证据均齐全，绑定源码提交、minimum SHA 和前置组合；缺证据不标记发布就绪。
- [ ] Modrinth 保留 `waystonesplayerptpt` 地址；修正旧源码链接与旧 Fabric 文件的缺失前置。26.1.1 未解决公开安装前不上传。
- [ ] 版本 1.0.1 与网络协议 2 的双方同时升级要求已说明。

- [ ] Modrinth：客户端 Required、服务端 Required、正确游戏版本/Loader、Waystones/Balm Required；26.x 另有 Shogi Required；Fabric 文件另有 Fabric API Required，NeoForge 文件不添加它。
- [ ] CurseForge：英文描述、正确 Game Version/Mod Loader、Waystones/Balm Required；26.x 另有 Shogi Required；Fabric 文件另有 Fabric API Required，NeoForge 文件不添加它，Release 类型。
- [ ] Modrinth 与 CurseForge 均选择 MIT；平台链接/文本与根 LICENSE 一致，第三方图标条款继续单独披露。
- [ ] beta NeoForge 文件在 changelog 明确披露。
- [ ] 1.21.2/1.21.3 共用文件同时勾选两版；其他文件只勾选一版。
- [ ] 页面包含非官方声明、既有图标署名和“玩家目的地不提供安全落点保证”说明。
- [ ] 上传清单同时保存文件名、大小、SHA-256、分支提交、目标和依赖。

生成仓库外/忽略目录中的清单（必须先完成当前分支全部最低套件构建）：

```bash
python3 scripts/release-manifest.py --output build/release-manifest.json
```

输出路径必须位于被忽略的 `build/`；脚本会重新检查 manifest、内容门禁、source commit 和 SHA-256。清单中的每个 `sha256` 必须与实际上传文件和运行证据中的 `BINARY-SHA256` 一致；清单不提交到仓库。

## 9. GitHub 与分支收尾

- [ ] 审查完整 diff、暂存清单、敏感信息和大文件；不提交 JAR、缓存、日志或本地运行文件。
- [ ] 先提交 canonical `main`，再按已确认基线移植两条 1.21.x 和两条 26.x 统一分支。
- [ ] core、1.21.x 完整 common、26.x 共享业务与同系列适配语义、目标矩阵、运行脚本、文档、许可证与既有图标和统一分支记录的 canonical 提交逐文件一致。
- [ ] 普通推送，不强推，不创建 PR、标签、GitHub Release，也不代为上传平台。
- [ ] 本地 HEAD、`origin/<branch>` 和 GitHub 远端提交一致。
- [ ] 五条分支 GitHub Actions 全绿，main/统一分支共享漂移门禁通过；两条 26.x 的 common 与非加载器适配源一致。
- [ ] 仅在新统一分支远端可验证且 CI 全绿后，删除本地/远端 `fabric/1.21.1` 与 `neoforge/1.21.11`。
- [ ] 最终切回 clean `main`，刷新仓库外的交付 JAR、SHA-256、changelog 和检查结果。
