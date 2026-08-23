# 构建与发布流程

halfmasa 使用 GitHub Actions 构建和发布 Minecraft `1.21.1`、`1.21.4`、`1.21.8`、`1.21.10`、`1.21.11`、`26.1.2` 和 `26.2`。

## 日常构建

`.github/workflows/build.yml` 会在推送到 `main`、Pull Request 和手动触发时运行。Minecraft 1.21.x 使用 Java 21，Minecraft 26.x 使用 Java 25。每个版本执行独立的 clean build，并验证 JAR 内的模组 ID 和版本号。

每个 CI 构建归档包含可安装 JAR、SHA-256 和 `build-manifest`，保留 14 天。构建成功仅代表编译和打包通过，不等于完成游戏内功能测试。

## 正式发布

1. 完成目标版本的静默构建、启动日志检查和必要的游戏内验证。
2. 更新根目录 `gradle.properties` 中的 `mod_version`，并同步 README 与功能文档版本。
3. 提交并推送全部发布源码。
4. 在 GitHub Actions 手动运行 `Release halfmasa`，输入与 `mod_version` 完全相同的版本号。
5. 选择 `all` 发布全部支持版本，或选择一个 Minecraft 版本进行单版本发布。

默认同时发布 GitHub、Modrinth 和 CurseForge。可以分别关闭 `publish_modrinth` 和 `publish_curseforge`；两者都关闭时仅发布 GitHub Release，之后仍可单独补发其他平台。

发布工作流会先完成全部目标构建，之后创建同名 Git 标签和 GitHub Release，再将 JAR 发布到 GitHub、Modrinth 和 CurseForge。正式版公开文件名为 `halfmasa-fabric-<版本>-mc<Minecraft版本>.jar`，不含构建时间戳。GitHub Release 标题只显示模组版本号，并且只上传可安装 JAR。

Modrinth 和 CurseForge 的显示标题只使用模组版本号；每个 Minecraft 版本使用独立技术版本号 `<版本>-mc<Minecraft版本>`，以便分别绑定兼容版本。MaLiLib 声明为必需依赖。

选择 `beta` 时，公开 JAR 保留原始构建时间戳，GitHub Release 标记为预发布，Modrinth 和 CurseForge 使用 beta 类型。

## 仓库配置

GitHub 仓库需要配置以下 Actions Secrets：

- `MODRINTH_API_TOKEN`
- `CURSEFORGE_TOKEN`

项目 ID 可通过仓库变量覆盖：

- `MODRINTH_PROJECT_ID`，默认 `9ZHJ1Ue9`
- `CURSEFORGE_PROJECT_ID`，默认 `1661919`

令牌只保存在 GitHub Actions Secrets 中，不写入源码、构建日志、JAR 或公开 Release。

## 发布后检查

- GitHub 标签、Release 标题和模组内部版本一致。
- GitHub Release 只包含七个目标 JAR，不包含 sources、dev、JSON 或 TXT。
- Modrinth 与 CurseForge 的 Minecraft 版本、Fabric 加载器、客户端环境和 MaLiLib 依赖正确。
- 三个平台对应 JAR 的 SHA-256 一致。
