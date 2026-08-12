# ADR 0001：显式多目标构建与适配族

- 状态：Accepted
- 日期：2026-08-12

## Context

Minecraft 1.21.1–1.21.11、Balm 与 Waystones 在输入事件、菜单、传送上下文、标识符和初始化 API 上存在真实编译断点；一个宽依赖范围或单一 Minecraft source set 无法证明兼容。与此同时，19 套复制业务会让安全修复迅速漂移。

## Decision

使用 pure Java `core`、按目标重新编译的 Minecraft-bound common、显式 Gradle target projects 与少量 API 适配族。目标矩阵和构建约定集中维护，不使用 Stonecutter 或源码条件预处理器。

## Consequences

- 每个正式目标有独立依赖、元数据、运行目录和 JAR。
- 业务规则只保留一份，Minecraft 类型不能作为已编译 common JAR跨版本复用。
- 新增目标必须先证明其可归入现有适配族，否则添加最小的新适配族。
