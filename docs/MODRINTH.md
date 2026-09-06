# Modrinth 发布材料（待上传）

正式页面保留 [waystonesplayerptpt](https://modrinth.com/mod/waystonesplayerptpt)，不改 slug。本文件是可审阅材料，不代表已修改平台或已经上传 1.0.1。

## 现有页面修正清单

- 名称：Waystones Player-to-player TP；源码链接：https://github.com/PalosJ/Waystones-Player-to-player-TP 。
- 客户端与服务端均 Required，原创代码 MIT，图标署名与第三方条款保留。
- 当前公开的 1.0.0 Fabric 1.21.1 文件补齐 Required dependencies：Waystones、Balm、Fabric API；NeoForge 文件需要 Waystones、Balm。
- 正文同时说明 Fabric 和 NeoForge；仅把实际已上传且完成验收的文件列为发布支持。
- 1.0.1 双方必须一起更新，即使原文件也叫 1.0.1；协议已升级为 2。

## English summary

Adds an online-player directory to the Waystones Warp Stone menu, enabling player-to-player teleportation without commands or extra items.

## English description draft

# Waystones Player-to-player TP

An unofficial [Waystones](https://modrinth.com/mod/waystones) add-on for travelling directly to other players. Charge a Warp Stone, choose a player in the menu on the left, and teleport to their location. No commands, operator permissions, or extra items are needed.

The player directory updates as players join and leave, adapts to the window width, and keeps Waystones' own search, sorting, paging, scrolling, and filtering controls available. Native Warp Stones available in each supported version retain their applicable charge and durability behavior.

Use the short search field above the player directory to find part or all of a player's name. The small button beside it shows a green check when incoming player teleportation is allowed, or a red cross when disabled; hover for details. In the narrow avatar layout, the search is cleared and hidden while the toggle stays available.

The menu includes **Allow others to teleport to me**, enabled by default. Turn it off to keep your row visible but unavailable as a destination; you can still travel to other players. The setting is saved in the server/world and survives reconnects, death, dimension changes, and normal restarts.

Travel prefers an adjacent position and falls back to the target block center when surrounded. Targets may be inside blocks, in mid-air, in fluids, or near cliffs.

Install the matching mod on **both client and server**, together with [Waystones](https://modrinth.com/mod/waystones) and [Balm](https://modrinth.com/mod/balm). Fabric additionally requires [Fabric API](https://modrinth.com/mod/fabric-api); 26.x additionally requires matching Shogi. Use Java 21 for 1.21.x and Java 25 for 26.x. Choose a file listed for your exact Minecraft version and loader. Forge is not supported.

Experience defaults to free. Set `playerTeleportExperienceMode` in `waystonesptpt-server.toml` to `NEVER`, `FOLLOW_WAYSTONES`, or `ALWAYS` to keep travel free, follow Waystones' experience switch, or always apply its experience rules. Native durability is settled once after success. NeoForge keeps world configuration overrides; Fabric uses an instance-wide configuration read on restart. Receiving preferences are saved per world on both loaders.

**1.0.1 update:** install this build on both sides, including installations already labeled 1.0.1. Receiving preferences use network protocol 2.

There is no telemetry, advertising, or external upload. Original code uses the [MIT License](https://github.com/PalosJ/Waystones-Player-to-player-TP/blob/main/LICENSE). The unchanged icon contains third-party material under separate terms: Warp Stone artwork adapted from Joe Williamson (JoeCreates), Roguelike/RPG Items, CC BY-SA 3.0; complete attribution is in [THIRD_PARTY_NOTICES.md](https://github.com/PalosJ/Waystones-Player-to-player-TP/blob/main/THIRD_PARTY_NOTICES.md).

Not endorsed by Twelve Iterations. NOT AN OFFICIAL MINECRAFT PRODUCT. NOT APPROVED BY OR ASSOCIATED WITH MOJANG OR MICROSOFT.

## 中文摘要与说明草稿

在 Waystones 的传送石菜单中加入在线玩家目录，实现玩家间传送，无需命令、OP 权限或额外道具。

完成传送石蓄力后，点击左侧玩家即可前往对方附近。目录实时更新，并根据窗口空间显示姓名列表或头像栏，保留原生搜索、排序、分页、滚动和筛选。“允许他人传送到我”默认开启；关闭后别人仍能看到你，但不能点击传送，你仍可前往其他允许接收的玩家。设置随当前服务器／世界保存，重连、死亡、换维度和正常重启后保留。

目录上方的短搜索框支持按部分或完整玩家名查找。右侧小按钮用绿勾表示允许接收、红叉表示关闭，悬停查看说明。窗口缩到头像栏时清空并隐藏搜索，保留小开关。

传送相邻位置优先，封闭时回退目标方块中心；目标可能位于方块、空中、流体或悬崖。传送石继承该版本适用的原生蓄力与耐久，成功后一次结算；经验默认免费，也可跟随或始终使用 Waystones 经验规则。

客户端和服务端须同时安装本模组、Waystones、Balm；Fabric 另需 Fabric API，26.x 另需 Shogi。1.21.x 使用 Java 21，26.x 使用 Java 25，不支持 Forge。以实际文件的游戏版本和加载器标注为准。此次 1.0.1 使用协议 2，即使原文件同为 1.0.1 也须双方同时更新。

## 每文件上传门禁

从 [targets.json](../gradle/targets.json) 读取游戏版本、加载器和完整依赖，使用 [CHANGELOG-1.0.1.md](CHANGELOG-1.0.1.md) 并附该目标依赖块。

- 只选择完成 [VALIDATION.md](VALIDATION.md) 所列门禁的 minimum JAR；Release 类型。未验收目标继续维护，不上传。
- Required dependencies：Waystones、Balm；Fabric 另有 Fabric API；26.x 另有 Shogi。
- 1.21.2／1.21.3 共用文件同时勾选两版，且两版必须分别有证据；其他文件只选对应版本。
- 使用官方 beta NeoForge 的组合如实在 changelog 披露。
- 26.1.1 在公开前置安装条件闭合前排除。
- 记录目标、分支、源码提交、SHA-256、大小、前置组合、游戏版本、加载器和逐项验收证据；current 重编译、dev、sources、前置和缓存均不上传。

实际平台修改和上传另行执行。本轮上传就绪清单只收录证据完整的目标，不能从“28 个构建目标”直接生成 28 个上传任务。
