# halfmasa

面向 Minecraft Fabric 客户端的 Xaero 与 MaLiLib 实用工具模组。halfmasa 将存档路径绑定、路径点管理、创造模式工具、输入与界面增强，以及 JEI/REI 配方查询历史整合到一个可配置的客户端模组中。

## 主要功能

- Xaero 单人存档与路径点、地图目录绑定。存档改名、移动或恢复备份后仍可复用原有 Xaero 数据。
- 路径点分享包：将路径点和分类压缩为 `XWB1:` 文本，从剪贴板导入或导出，并支持按 Xaero 原生格式分批发送到聊天栏。
- 创造模式容器填充：使用主手物品填充潜影盒、箱子、副手容器或收纳袋。
- JEI/REI 配方与用途查询历史，支持四角定位、原生列表避让、位置切换快捷键和首次打开初始化。
- 截图复制到系统剪贴板、鞘翅飞行时间提示、资源包兼容性检查跳过、暂停 tick 时的掉落物轨迹预测控制。
- 船只 360°视角、划船时保留第一人称手持物品、背包和容器界面移动、可配置的输入法与按键功能。
- 配置界面支持中文和英文，并沿用 MaLiLib 的快捷键、分类和热键设置体验。

## 依赖

必需：

- Minecraft Fabric
- Fabric Loader `>=0.16.14`
- MaLiLib

可选联动模组：

- Xaero's Minimap
- Xaero's World Map
- Mod Menu
- Tweakeroo
- Carpet
- Roughly Enough Items（REI）
- Just Enough Items（JEI）

Xaero's World Map、REI、JEI 和其他可选模组不会打进 halfmasa JAR。未安装对应联动模组时，相关功能会自动隐藏或停用。

## 使用

安装后按 `X + H` 打开 halfmasa 配置界面，也可以从 Mod Menu 进入。配置界面按路径点工具、创造模式工具、移植功能和客户端工具分类；大多数功能默认关闭，需要在对应配置项中启用。

JEI/REI 配方历史默认关闭。启用后可以设置显示行数和四角位置；位置切换快捷键默认未绑定，可在配置界面中自定义。说明文字会根据屏幕宽度自动换行。

## 支持版本

当前构建配置覆盖：

`1.21.1` · `1.21.4` · `1.21.8` · `1.21.10` · `1.21.11` · `26.1.2` · `26.2`

请使用与 Minecraft 版本匹配的构建产物。功能实现会优先在单个基线版本上验证，再进行其他版本适配。

## 构建

在 Windows PowerShell 中运行：

```powershell
.\.gradle-dist\gradle-9.5.1\bin\gradle.bat -g "$env:USERPROFILE\.gradle" buildAllVersions
```

单独构建 1.21.1：

```powershell
.\.tools\gradle-9.5.1\bin\gradle.bat :1.21.1:build
```

可安装 JAR 会输出到构建目录，并按构建流程归档到 `mod-builds/<timestamp>/`。归档目录是本地构建产物，不属于源码提交内容。

## 致谢与许可证

项目以 MIT License 发布，详见 [LICENSE](LICENSE)。

部分功能参考或移植自 TechUtils、JEI Recipe History、REI、InvMove、BoatView360、Boat Item View、ElytraTime 等项目。对应许可证和第三方声明随源码及构建产物保存在 `META-INF/halfmasa/THIRD_PARTY_NOTICES.md`。

---

# halfmasa

`halfmasa` is a client-side Minecraft Fabric utility mod for Xaero and MaLiLib users. It combines save-path binding, waypoint management, creative utilities, input and UI improvements, and JEI/REI recipe lookup history in one configurable mod.

## Highlights

- Bind Xaero waypoint and map data to singleplayer saves so renamed, moved, or restored worlds can keep using the same data.
- Export and import compressed `XWB1:` waypoint bundles through the system clipboard, plus batch sharing in Xaero's native chat format.
- Fill shulker boxes, chests, offhand containers, and bundles from creative mode.
- Separate JEI and REI recipe/usage history with corner placement, native-list avoidance, position cycling, and first-open initialization.
- Copy screenshots to the system clipboard, report elytra flight time, skip resource-pack compatibility checks, and control dropped-item trajectory prediction while ticks are paused.
- Boat camera improvements, inventory movement, configurable IME behavior, and additional client utilities.
- Chinese and English configuration screens using MaLiLib-style categories and hotkey settings.

## Dependencies

Required:

- Minecraft Fabric
- Fabric Loader `>=0.16.14`
- MaLiLib

Optional integrations include Xaero's Minimap, Xaero's World Map, Mod Menu, Tweakeroo, Carpet, REI, and JEI. Optional mods are not bundled into the halfmasa JAR.

## Usage

Press `X + H` to open the halfmasa configuration screen, or open it through Mod Menu. Most features are disabled by default and can be enabled in their respective categories.

JEI/REI recipe history is disabled by default. After enabling it, configure the row count and corner position. The position-cycle hotkey is unbound by default and can be customized. Long configuration descriptions wrap automatically to the current screen width.

## Supported Versions

The current build configuration covers:

`1.21.1` · `1.21.4` · `1.21.8` · `1.21.10` · `1.21.11` · `26.1.2` · `26.2`

Use the artifact matching your Minecraft version. Features are validated against one baseline version before being ported to other version targets.

## Build

On Windows PowerShell:

```powershell
.\.gradle-dist\gradle-9.5.1\bin\gradle.bat -g "$env:USERPROFILE\.gradle" buildAllVersions
```

To build only Minecraft 1.21.1:

```powershell
.\.tools\gradle-9.5.1\bin\gradle.bat :1.21.1:build
```

Installable JARs are produced by the build and archived locally under `mod-builds/<timestamp>/`. Those archives are intentionally excluded from source control.

## Credits and License

Licensed under the MIT License; see [LICENSE](LICENSE).

Some features are inspired by or ported from TechUtils, JEI Recipe History, REI, InvMove, BoatView360, Boat Item View, ElytraTime, and other projects. Their licenses and notices are included in `META-INF/halfmasa/THIRD_PARTY_NOTICES.md`.
