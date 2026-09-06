# 网络协议 2

模组版本保持 `1.0.1`，网络版本从 1 升为 2。新旧 1.0.1 文件不能混用；客户端和服务器必须同时更新。机器契约为 [waystonesptpt.network.json](../common/src/main/resources/waystonesptpt.network.json)。

| 载荷 | 方向 | 数据与用途 |
|---|---|---|
| `request_player_teleport` | C2S | 仅目标 UUID；不接受坐标、费用或成功声明 |
| `receiving_directory` | C2S | 当前菜单会话 UUID、replace 标志、最多 512 个已有目录 UUID；大目录分批 |
| `update_receiving` | C2S | 当前菜单会话 UUID、变更序号、本人允许接收状态；无目标 UUID 字段 |
| `receiving_state` | S2C | 菜单会话 UUID、确认序号、本人状态及最多 512 个订阅目标状态 |

处理均由 Balm/加载器桥接安排至对应逻辑端主线程。服务端验证发送者是当前在线实例、仍打开原生 Warp Stone 菜单并绑定原物品／手；偏好修改只作用于发送者 UUID。过期会话、旧菜单、非法长度和断开连接不能改变其他玩家设置。

目录继续来自 Minecraft 已同步的 listed 玩家信息。接收状态只附加到客户端已提交的目录 UUID；服务器不会主动下发额外玩家名单。客户端提交目录不构成传送授权，也不提供隐藏 UUID 防猜测承诺。服务端传送最终边界独立检查目标在线、不是本人、允许接收及事务有效性。

服务端以菜单会话维护订阅，仅向订阅相应 UUID 的当前菜单广播其状态。客户端忽略旧会话及目录外 UUID，按钮提交后等待对应确认；普通广播不能误清除尚未确认的本地提交。关闭／替换菜单或断开连接清除客户端会话；服务端每 5 tick 清理过期订阅。

旧 Balm 不提供统一版本握手时，服务端还要求先建立协议 2 的有效菜单订阅，才接受原有 UUID 传送请求。因此协议 1 客户端不能利用相同旧载荷绕过本次更新。

接收偏好不是费用配置：只将 opt-out UUID 写入 overworld 的 `waystonesptpt_receiving` SavedData，默认允许、变化时标脏。所有维度共用同一世界数据，两个不同存档相互独立。
