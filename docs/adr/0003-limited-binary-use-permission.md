# ADR 0003：ARR 下的有限最终用户授权

- 状态：Accepted
- 日期：2026-08-12

## Context

原许可证只写 All Rights Reserved，会使正常下载、安装和运行官方 JAR也缺少明确许可。开放修改或再分发又不符合权利人的发布意图。

## Decision

保留 All Rights Reserved，明确允许最终用户从权利人授权渠道下载、安装和运行官方未修改二进制。修改、镜像、再分发、打包、转售、商业利用和衍生作品仍需事先书面许可；第三方素材继续服从各自条款。

## Consequences

- Modrinth 使用自定义许可证 URL，CurseForge 选择 Custom License。
- README、平台材料和 JAR必须包含相同边界，不能把项目描述为开源。
- 该授权不能对已获得的正式版本任意追溯收回，因此变更需要新的明确决定。
