# Modrinth 发布材料

本文件提供项目页面文案与上传元数据模板。每个文件的精确游戏版本和文件名从 [gradle/targets.json](../gradle/targets.json) 读取，不手工扩大兼容范围。

## 项目设置

| 字段 | 值 |
|---|---|
| Title | Waystones Player |
| Slug | `waystonesplayer` |
| Client side | Required |
| Server side | Required |
| Project license | MIT |
| Required dependencies | Waystones、Balm；Fabric 文件另需 Fabric API |
| Unsupported loader | Forge |
| Version number | 所有文件均为 `1.0.1` |

每个上传文件：

- Release type：Release。
- Game versions：仅选择矩阵中该 JAR的 `minecraft` 列表。
- Loaders：只选择该 JAR的 NeoForge 或 Fabric。
- Dependencies：Waystones 与 Balm 均标记 Required；Fabric 文件另外将 Fabric API 标记 Required，NeoForge 文件不添加 Fabric API。
- 1.21.2/1.21.3 共用 JAR同时选择两个游戏版本；其他文件只选择一个。
- NeoForge 1.21.2、1.21.6、1.21.7、1.21.9 的 changelog 增加“requires an official beta NeoForge build”提示。
- 上传前核对 SHA-256 与最终交付清单；不要上传 `-sources`、`-dev`、缓存或当前套件重编译产物。

## English

### Summary

Adds a live online-player directory to the Waystones Warp Stone menu, with server-authoritative costs, durability, events, and responsive layout.

### Description

# Waystones Player

Sometimes the person you are looking for matters more than the place.

Waystones Player is an unofficial add-on that brings other listed players into the [Waystones](https://modrinth.com/mod/waystones) Warp Stone menu. Choose a player and travel through Waystones' normal event, sound, effect, and adjacent-destination pipeline—without commands, operator permissions, or a second confirmation screen.

## Features

- A live player directory based on Minecraft's existing listed-player data.
- A responsive 164px full list, 128–163px compact list, or 36px clickable avatar rail.
- Server-side validation of the exact Warp Stone and hand, online target UUID, experience requirement, and confirmed movement.
- Waystones-compatible cancellable events, sound, effects, and native adjacent-or-target-block destination placement.
- Exactly one point of native Warp Stone durability after confirmed success.
- Free, follow-Waystones, or always-evaluate-Waystones experience modes.
- No telemetry, advertising, analytics, or external uploads.

The client sends only the selected target UUID. The server independently resolves the corresponding currently online player; client listing state is not server-side teleport authorization. This add-on does not promise to prevent custom packets from guessing a third-party-hidden player's UUID. Player destinations are transient and are not stored as Waystones.

Player destinations carry no safety guarantee. They may be inside blocks, in mid-air, in dangerous fluids, or near cliffs; Waystones falls back to the target block center when no adjacent opening exists.

## Compatibility

Waystones Player must be installed on both client and server together with matching [Waystones](https://modrinth.com/mod/waystones) and [Balm](https://modrinth.com/mod/balm) files. Fabric files additionally require the matching [Fabric API](https://modrinth.com/mod/fabric-api); NeoForge files do not.

- NeoForge: Minecraft 1.21.1–1.21.11.
- Fabric: Minecraft 1.21.1–1.21.11.
- Minecraft 1.21.2 and 1.21.3 share one separately tested file per loader.
- Java 21.
- Forge is not supported.

Use the file whose loader and Minecraft version exactly match the instance. Some supported NeoForge lines use official beta NeoForge builds; those files disclose this in their changelog.

## Configuration

`playerTeleportExperienceMode` defaults to `NEVER`:

- `NEVER`: player destinations do not consume experience.
- `FOLLOW_WAYSTONES`: use Waystones experience rules only while its global cost switch is enabled.
- `ALWAYS`: evaluate the same Waystones experience rules regardless of that switch.

Only experience-point and experience-level requirements are selected. Item costs, cooldowns, and unrelated requirements are not inherited.

NeoForge keeps SERVER configuration and per-world override semantics. Fabric uses the same `config/waystonesplayer-server.toml` name, key, and default as a global instance configuration, without per-world overrides.

## License and attribution

Original code is open source under the [MIT License](https://github.com/PalosJ/waystonesplayer/blob/main/LICENSE). Use, modification, redistribution, and commercial use are permitted while retaining the copyright and license notice. Third-party icon material is excluded from the MIT grant and remains under the terms documented below.

The existing project icon is unchanged. Warp Stone artwork is adapted from **Roguelike/RPG Items** by [Joe Williamson (JoeCreates)](https://opengameart.org/content/roguelikerpg-items) under [CC BY-SA 3.0](https://creativecommons.org/licenses/by-sa/3.0/). Additional player imagery and complete terms are documented in [THIRD_PARTY_NOTICES.md](https://github.com/PalosJ/waystonesplayer/blob/main/THIRD_PARTY_NOTICES.md).

Waystones Player is not endorsed by Twelve Iterations.

NOT AN OFFICIAL MINECRAFT PRODUCT. NOT APPROVED BY OR ASSOCIATED WITH MOJANG OR MICROSOFT.

---

## 简体中文

### 摘要

在 Waystones 的传送石菜单中加入实时在线玩家目录，并提供服务端权威费用、耐久、事件兼容和响应式布局。

### 描述

# Waystones Player

有时候，你想找的不是某一座石碑，而是正在世界另一头冒险的朋友。

Waystones Player 会把当前连接中可列出的其他玩家加入 [Waystones（传送石碑）](https://modrinth.com/mod/waystones) 的 Warp Stone 菜单。选择玩家后，传送会进入 Waystones 的事件、声音、效果与相邻落点流程，不需要命令、OP 权限或第二次确认。

玩家目录会实时更新，并按界面空间显示完整名单、收窄名单或可点击头像栏。客户端复用 listed 玩家流并只发送目标 UUID；服务端独立解析当前在线玩家，再检查确切物品、使用手、经验和实际移动。客户端隐藏状态不构成服务端传送权限，本模组不承诺阻止自定义数据包猜测第三方隐藏玩家 UUID。确认成功后才损耗 1 点原生耐久；失败恢复经验、保留耐久并保持界面打开。

玩家目的地不提供安全落点保证：目标可能位于方块、空中、危险流体或悬崖；Waystones 找不到相邻空位时会回退到目标方块中心。

本模组需要在客户端和服务端同时安装，并配套安装对应版本的 Waystones 与 Balm；Fabric 文件还需要对应版本的 Fabric API，NeoForge 文件不需要 Fabric API：

- NeoForge：Minecraft 1.21.1–1.21.11。
- Fabric：Minecraft 1.21.1–1.21.11。
- 1.21.2 与 1.21.3 在每个加载器共用一份分别测试的文件。
- Java 21。
- 不支持 Forge。

原创代码依据 MIT License 开源；在保留版权与许可声明的条件下允许使用、修改、再分发和商业使用。现有图标中的第三方素材不属于 MIT 授权范围，完整署名和条款见项目的 `THIRD_PARTY_NOTICES.md`。

本项目是未经 Twelve Iterations 背书的非官方 Waystones 附属，也不是 Minecraft 官方产品。
