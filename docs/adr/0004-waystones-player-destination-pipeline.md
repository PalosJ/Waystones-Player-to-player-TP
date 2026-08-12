# ADR 0004：玩家目的地接入 Waystones 传送管线

- 状态：Accepted
- 日期：2026-08-12

## Context

直接调用 Minecraft `teleportTo` 会绕过 Waystones 传送事件、声音、效果和相邻落点，降低其他附属兼容性。完整继承 Waystones 费用又会改变本附属既有经验与固定耐久语义。

## Decision

以瞬态、未注册玩家 Waystone 和 unbound context 调用 Waystones 公开同步管线；开启声音/效果，关闭 Waystone 方块 modifier，不携带宠物/牵引实体。只把本附属筛选出的经验 requirement 放入 context；确认实际移动后再固定结算 1 点耐久。

目标落点严格采用上游相邻非窒息规则，不增加地面、流体或悬崖检查。事件取消或未移动时恢复精确经验快照。

## Consequences

- 其他模组可观察、取消或重定向玩家目的地事件。
- 文档不得称该落点为“绝对安全”。
- 1.21.1 使用同步入口，因为在线目标区块已加载，而异步入口会把瞬态目标当作缺失的真实 Waystone 方块拒绝；其他版本由适配族保持同一语义。
