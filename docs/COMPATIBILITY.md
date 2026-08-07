# 兼容性与升级指南

本文记录当前支持范围、对 Waystones 内部结构的依赖以及升级检查步骤。它不是未来版本承诺；平台页面和 README 中声明的版本仍以实际验证结果为准。

## 当前支持矩阵

| 项目 | 最低支持 | 当前开发/CI | 状态 |
|---|---:|---:|---|
| Minecraft | 1.21.1 | 1.21.1 | 支持 |
| Java | 21 | 21 | 支持 |
| NeoForge | 21.1.229 | 21.1.229 | 支持 |
| Waystones | 21.1.36 | 21.1.40 | 双版本构建验证 |
| Balm | 21.0.62 | 21.0.62 | 支持 |
| Fabric | — | — | 计划中，尚无产物 |
| Forge | — | — | 不在计划内 |

NeoForge 元数据继续声明 Waystones `[21.1.36,)` 与 Balm `[21.0.62,)`。开放上界代表允许用户尝试新补丁版本，不代表未经验证的新内部结构一定兼容；遇到解析失败时遵循下述关闭策略。

## 上游依赖点

### 公开或相对稳定接口

- Balm 模块初始化、事件、网络注册与屏幕工具。
- Waystones `WaystonesAPI`、`WaystoneTeleportContext`、类型与 requirement API。
- Minecraft 菜单注册表、玩家/物品/传送与原生耐久 API。

### 内部或需重点复核的结构

| 依赖点 | 用途 | 失败策略 |
|---|---|---|
| 菜单 ID `waystones:warp_stone_selection` | 限定合法请求上下文 | 请求失效，不扣费 |
| 菜单方法 `getWarpItem()` | 找到打开界面的原始传送石对象 | 玩家目的地禁用，只告警一次 |
| `WaystoneImpl` 瞬态实例构造 | 表示目标玩家的位置与维度 | 需要经验时拒绝传送 |
| `WaystonesConfig.getActive().teleports` | 读取总开关与 `warpRequirements` | 需要经验时拒绝传送 |
| `RequirementModifierParser` 与 `WarpRequirementsContextImpl` | 应用当前经验公式 | 需要经验时拒绝传送，只告警一次 |
| `AbstractWaystoneList` | 推导 Waystones GUI 的位置和尺寸 | 不注入玩家面板 |
| 选择界面类名包含 `WaystoneSelectionScreen` | 延迟加载客户端注入器 | 不注入玩家面板 |

非经验 requirement（物品、冷却、自定义非经验费用等）会被主动忽略。`ALWAYS` 只能强制启用 Waystones 命名空间中的经验函数，不能擅自启用未知第三方函数。

## Waystones 更新检查清单

升级 `waystones_version` 前后依次确认：

1. 查看 Waystones 与 Balm 的更新日志、源码模块和依赖版本变化。
2. 核对上述菜单 ID、`getWarpItem()`、瞬态 `WaystoneImpl` 构造与 requirement 解析类型。
3. 核对 Waystones 选择界面仍包含 `AbstractWaystoneList`，并重新测量面板定位与 12 像素间距。
4. 运行默认版本的 `clean test build`，再用 `-Pwaystones_version=21.1.36+1.21.1` 验证最低版本。
5. 在专用服务器测试主手/副手、同维度/跨维度、经验不足、目标离线、菜单失效、物品移走、重复包和传送异常。
6. 在客户端测试宽屏、窄屏、空列表和滚动列表；确认失败时界面保持打开。
7. 检查最终 JAR 不包含 Waystones/Balm 资产或依赖 JAR，并更新 README 的“验证至”版本。

如果新 Waystones 版本改变经验规则结构，不得通过捕获异常后返回零费用来“兼容”。安全选择是保持 `NEVER` 可用，并拒绝 `FOLLOW_WAYSTONES`/`ALWAYS` 中无法确认费用的请求。

## 未来 Fabric 接入

只有实际开始 Fabric 适配时才创建 `fabric` 模块。建议顺序：

1. 先确认 common 源码仍无 NeoForge/Fabric 导入，并保持网络载荷类型与协议版本 `1` 不变。
2. 新建 Fabric 入口与客户端入口，分别通过 Balm Fabric load context 启动现有通用模块。
3. 使用 Fabric 的服务端配置实现同一个 `Supplier<PlayerTeleportExperienceMode>` 契约；配置键与默认语义不变。
4. 合并 common Java/资源到 Fabric 最终 JAR，但不复制传送、兼容或 GUI 代码。
5. 添加 Fabric 依赖、元数据、运行任务和与 NeoForge 对等的 CI/冒烟场景。
6. 只有客户端、专用服务器与核心事务通过后，才在 README 和平台页面声明 Fabric 支持。

## Minecraft 版本分支策略

- `main` 代表当前主支持的 Minecraft 版本，并在同一分支内维护 common、Fabric（提供后）和 NeoForge。
- 当前不创建 `mc/1.21.1`。只有准备让 `main` 升级到新的 Minecraft 主支持版本前，才从稳定主线建立 `mc/1.21.1` 维护分支。
- 不在一个分支内堆叠多个 Minecraft 源码集；每个 Minecraft 版本分支独立维护其加载器矩阵。
- 主模组补丁适配不自动提升本模组版本。只有用户明确指定、修复明显 Bug、加入重要功能或产生破坏性变化时按 SemVer 更新。
