# 兼容性与升级指南

本文记录各分支的支持范围、Waystones/Balm 依赖边界和升级步骤。它不是对所有未来补丁的自动承诺；README、发布页和实际验证记录优先。

## Canonical 主线

`main` 固定代表 Minecraft 1.21.1 / NeoForge，不因试验分支改变默认发布方向：

| 项目 | 声明最低 | 当前开发/CI | 声明范围 |
|---|---:|---:|---|
| Minecraft | 1.21.1 | 1.21.1 | `[1.21.1]` |
| Java | 21 | 21 | 21 |
| NeoForge | 21.1.229 | 21.1.248 | `[21.1.229,21.2)` |
| Waystones | 21.1.36 | 21.1.40 | `[21.1.36,21.2)` |
| Balm | 21.0.62 | 21.0.64 | `[21.0.62,21.1)` |

CI 使用“最低整套”和“当前整套”矩阵，而不是只替换 Waystones：最低为 NeoForge 21.1.229 + Waystones 21.1.36 + Balm 21.0.62，当前为 NeoForge 21.1.248 + Waystones 21.1.40 + Balm 21.0.64。范围上界阻止加载器把跨 Minecraft 系列的内部结构当成已声明兼容。

## 移植验证分支

| 分支 | Minecraft / Java | 加载器 | Waystones / Balm | 用途 |
|---|---|---|---|---|
| `fabric/1.21.1` | 1.21.1 / 21 | Fabric Loader 0.17.3+、Fabric API 0.116.x | Waystones 21.1.36–21.1.x / Balm 21.0.62–21.0.x | 同版本多加载器实证；同时回归 NeoForge 模块 |
| `neoforge/1.21.11` | 1.21.11 / 21 | NeoForge 21.11.x | Waystones 21.11.x / Balm 21.11.x | Minecraft 与上游新 API 移植实证 |

这两个分支是从 canonical 行为移植出的扩展，不会反向改变 `main` 的 Minecraft/加载器含义。各分支的 README、Gradle 属性和 CI 必须记录其实际最低/当前验证组合；不能把“能解析依赖”写成运行支持。

## 1.21.1 上游依赖点

### 公开或相对稳定接口

- Balm 模块初始化、事件、网络注册和屏幕工具。
- Waystones `WaystonesAPI`、`WaystoneTeleportContext`、类型与 requirement API。
- Minecraft 菜单注册表、玩家/物品/传送和原生耐久 API。

### 内部或需重点复核的结构

| 依赖点 | 用途 | 失败策略 |
|---|---|---|
| 菜单 ID `waystones:warp_stone_selection` | 限定合法请求上下文 | 请求失效，不扣费 |
| 菜单方法 `getWarpItem()` | 找到打开界面的原始传送石对象 | 玩家目的地禁用，只告警一次 |
| `WaystoneImpl` 瞬态实例构造 | 表示目标玩家位置与维度 | 需要经验时拒绝传送 |
| `WaystonesConfig.getActive().teleports` | 读取总开关与 `warpRequirements` | 需要经验时拒绝传送 |
| `RequirementModifierParser` / `WarpRequirementsContextImpl` | 应用当前经验公式 | 需要经验时拒绝传送，只告警一次 |
| `AbstractWaystoneList` | 推导 1.21.1 GUI 的位置和尺寸 | 不注入玩家面板 |
| `AbstractContainerScreen.leftPos` 客户端访问器 | 同步移动 1.21.1 的文本、命中区域和控件 | 按最低/当前依赖编译并实际启动客户端；字段不匹配不得发布 |
| 选择界面类名包含 `WaystoneSelectionScreen` | 延迟加载客户端注入器 | 不注入玩家面板 |

非经验 requirement（物品、冷却、自定义非经验费用等）会被主动忽略。`ALWAYS` 只能强制启用 Waystones 命名空间中的经验函数，不能擅自启用未知第三方函数。创造模式的 affordability、consume、rollback 三条路径由统一费用包装器豁免。

## NeoForge 1.21.1 升级步骤

升级任何一个依赖时按成套依赖复核：

1. 查看 NeoForge、Waystones 与 Balm 的更新日志、源码分支和依赖约束。
2. 区分当前开发版本与声明最低版本；除非完成最低版本回归，不修改元数据下限。
3. 核对菜单 ID、`getWarpItem()`、`WaystoneImpl` 构造、requirement 解析类型和选择界面列表控件。
4. 运行当前整套：
   ```bash
   ./gradlew clean test build \
     -Pneo_version=21.1.248 \
     -Pwaystones_version=21.1.40+1.21.1 \
     -Pbalm_version=21.0.64+1.21.1 \
     --no-build-cache
   ```
5. 运行最低整套：
   ```bash
   ./gradlew clean test build \
     -Pneo_version=21.1.229 \
     -Pwaystones_version=21.1.36+1.21.1 \
     -Pbalm_version=21.0.62+1.21.1 \
     --no-build-cache
   ```
6. 在专用服务器测试主手/副手、同维度/跨维度、创造/生存、经验不足、目标离线、菜单失效、物品移走、重复包和传送异常。
7. 在客户端测试宽屏零偏移、自动缩放下的完整/收窄名单、320 像素头像栏、反复调整窗口、空列表、长名称、滚动列表、键盘焦点和失败后界面保持打开。
8. 核对 `waystonesplayer-server.toml` 的全局默认、世界覆盖和重载，并检查最终 JAR 内容。
9. 更新 README 的“验证至”版本、CI 矩阵和产物名称；版本范围仍只覆盖经过证明的 Minecraft 系列。

如果新 Waystones 改变经验规则结构，不得捕获异常后返回零费用。安全选择是保持 `NEVER` 可用，并拒绝 `FOLLOW_WAYSTONES` / `ALWAYS` 中无法确认费用的请求。

## Fabric 1.21.1 维护步骤

Fabric 分支必须满足以下对等条件：

1. `common` 继续没有 Fabric/NeoForge 导入，协议版本和服务端验证顺序不变。
2. Fabric 通用/客户端入口分别通过 Balm Fabric load context 启动共享模块；客户端类不从服务端入口加载。
3. `fabric.mod.json` 声明 Minecraft、Fabric Loader、Fabric API、Waystones 和 Balm 的同系列范围，并引用根路径 `waystonesplayer.png`。
4. Fabric 配置保留 `playerTeleportExperienceMode`、`NEVER` 默认值和运行时 Supplier；配置位于全局 `config`，不声称支持 NeoForge 的世界级 `serverconfig`。
5. 同一分支的 `build` 同时产出 Fabric 与 NeoForge JAR，两个 JAR 都通过图标、元数据、许可证和未捆绑上游依赖检查。
6. 最低/当前 Fabric Loader、Fabric API、Waystones 与 Balm 成套验证，并分别启动客户端和专用服务器。
7. 只有上述实证完成后，分支 README 或平台页面才能声明对应 Fabric 版本可用。

## NeoForge 1.21.11 迁移检查

1.21.11 不是只改版本号。已知必须显式处理的上游变化包括：

- Minecraft 标识符由 1.21.1 映射中的 `ResourceLocation` 迁移为 `Identifier`，GameProfile 访问器也需按新映射复核。
- Balm API 包结构和加载上下文发生变化；入口、事件、网络、配置与客户端初始化要逐项对照 1.21.11 官方源码。
- Waystones `WaystoneImpl` 构造改为不直接接收名称；创建后需设置名称和 transient 状态。
- 选择界面不再提供 1.21.1 的 `AbstractWaystoneList`，而采用全宽、分页式 `WaystoneSelectionScreenBase`。客户端注入必须从新版屏幕几何/控件重新定位，通过 `imageWidth` 移动标签中心，并在渲染前把搜索、翻页、排序、删除等动态重建控件恢复到“原始坐标 + 当前偏移”；找不到结构时安全禁用。
- requirement 解析和传送上下文虽然概念仍在，具体包名、泛型和运行行为必须由编译与最小/当前上游源码双重确认。

移植后必须重跑服务端信任边界、创造模式费用、失败回滚、窄屏布局和客户端/专用服务器冒烟，不能把仅通过 `compileJava` 当成兼容完成。

## 分支与回合策略

- `main` 始终为 NeoForge 1.21.1；修复共享行为后再按适用性移植到两个分支。
- `fabric/1.21.1` 验证同一 common 对两个加载器的复用；不复制业务类。
- `neoforge/1.21.11` 吸收 Minecraft/Waystones/Balm API 变化；不在 `main` 混入条件编译或第二套源码树。
- 任何分支升级前先获取相应远端并确认未落后/分叉；普通推送，禁止强推和改写历史。
- 所有分支与构建产物默认固定为 `1.0.0`。只有用户明确确认更新版本后，才按其指定同步 Gradle、元数据、README、CI 和产物名；Bug 修复、重要功能、破坏性变化、移植或依赖更新都不得自行触发版本递增。
