# halfmasa

halfmasa is a client-side Minecraft Fabric utility mod for Xaero and MaLiLib users, providing save and waypoint binding, creative tools, input and UI improvements, and JEI/REI recipe lookup history.

halfmasa（半马萨）是一个面向 Minecraft Fabric 客户端的 Xaero 与 MaLiLib 实用工具模组。它把存档与路径点绑定、路径点分享、创造模式工具、输入和界面增强，以及 JEI/REI 查询历史集中到统一的 MaLiLib 配置界面中。除特别说明外，功能默认关闭。

## 支持版本

当前版本为 `1.1.1-beta.1`，构建配置支持 Minecraft `1.21.1`、`1.21.4`、`1.21.8`、`1.21.10`、`1.21.11`、`26.1.2` 和 `26.2`。

## 文档

- [中文功能与配置说明](https://github.com/halfkite/halfmasa/blob/main/docs/features.md)
- [English features and configuration](https://github.com/halfkite/halfmasa/blob/main/docs/features_en.md)
- [Modrinth English description](https://github.com/halfkite/halfmasa/blob/main/docs/modrinth_en.md)
- [构建与发布流程](https://github.com/halfkite/halfmasa/blob/main/docs/releasing.md)

## 功能概览

- 绑定单人存档与 Xaero 路径点、地图目录，存档改名、移动或恢复备份后仍可复用原有数据。
- 导入、导出和分享压缩的 `XWB1:` 路径点文本包，并按 Xaero 原生格式分批发送路径点。
- 创造模式容器填充、创造搜索历史和可展开的合并创造物品条目。
- JEI/REI 配方与用途查询历史，支持首次打开初始化、四角定位、原生列表避让和位置切换快捷键。
- 截图复制到剪贴板、鞘翅飞行时间、夜视平滑淡出、资源包检查跳过、服务器图标缓存和地图物品预览。
- 船只 360°视角、划船手持物显示、背包移动、冷却自动攻击、列表拖动、快速界面滚动、环绕放置搭桥辅助和快捷键圆盘。
- 游戏内输入法、点击发送、快速加载、提示消息屏蔽和服务器列表刷新修复。
- 可选的中英文自动空格，支持快捷键，并可分别控制翻译文本、告示牌以及书与笔/成书显示。
- 配置说明按当前屏幕宽度自动换行，配置页与 Mod Menu 分别记忆滚动位置。

## 重要说明

Fabric Loader `0.16.14+` 和 MaLiLib 是必需依赖。Xaero's Minimap、Xaero's World Map、Mod Menu、Carpet、REI 和 JEI 为可选联动模组，不会打进 halfmasa JAR；未安装对应模组时，相关兼容功能不会启用。

在默认 Windows 游戏目录中，全局配置位于绝对路径 `C:\Users\<username>\AppData\Roaming\.minecraft\config\halfmasa\`。搜索与查询历史、快捷键圆盘数据和服务器图标缓存使用该目录下的独立文件或子目录；跟随单人存档的持久化数据位于 `C:\Users\<username>\AppData\Roaming\.minecraft\saves\<world>\config\halfmasa\`。如果启动器使用自定义游戏目录，请将 `.minecraft` 替换为该绝对路径。模组是纯客户端工具，不要求服务器安装 halfmasa。

## 主要入口

按 `X + H` 打开 halfmasa 配置界面，也可以从 Mod Menu 进入。配置页按“推荐”“路径点工具”“创造模式工具”“移植功能”“扩展”和“已停用功能”分类。动作项可直接点击“触发”，也可以绑定 MaLiLib 快捷键。

## 构建

```powershell
.\gradlew.bat buildAllVersions
```

全部版本的可安装 JAR 会归档到 `build/libs/<timestamp>/`。单独构建 1.21.1：

```powershell
.\gradlew.bat :1.21.1:build
```

单版本产物位于对应的 `versions/<version>/build/libs/`。

## 许可证与致谢

项目主体按 [MIT License](https://github.com/halfkite/halfmasa/blob/main/LICENSE) 发布。部分功能参考或移植自 TechUtils、JEI Recipe History、REI、InvMove、BoatView360、Boat Item View、ElytraTime 等项目；对应许可证和归属说明见源码及 JAR 内的 `META-INF/halfmasa/THIRD_PARTY_NOTICES.md`。
