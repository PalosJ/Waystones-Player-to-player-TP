# ADR 0005：原创代码采用 MIT License

- 状态：Accepted
- 日期：2026-08-31

## Context

项目此前按 ADR 0003 保留原创代码的全部权利，仅向最终用户授予有限的官方二进制使用许可。权利人现决定从 1.0.1 起允许社区使用、修改、再分发和商业使用原创代码。

项目图标还包含 CC BY-SA 3.0 素材和受 Minecraft Usage Guidelines 约束的玩家图像，这些第三方内容不能由本项目重新授权为 MIT。

## Decision

自 1.0.1 起，Waystones Player-to-player TP 的原创代码使用标准 MIT License，并保留 `Copyright (c) 2026 PalosJ`。使用、修改、再分发、再许可和商业使用必须保留 MIT版权与许可声明。

根 `LICENSE` 只授权项目原创代码。项目图标及其他在 `THIRD_PARTY_NOTICES.md` 中明确识别的第三方内容继续遵循各自条款，不包含在 MIT授权范围内。

## Consequences

- 加载器元数据、Modrinth 与 CurseForge材料均将项目原创代码标记为 MIT。
- 发行 JAR继续同时包含 MIT `LICENSE`、NeoForge模板许可证和第三方声明。
- 1.0.0 及此前授权历史由 ADR 0003 保留，本决策不撤销已经获得的旧授权。
