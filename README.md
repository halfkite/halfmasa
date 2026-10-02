# halfmasa

[![License](https://img.shields.io/github/license/halfkite/halfmasa)](https://choosealicense.com/licenses/mit/)
[![Modrinth](https://img.shields.io/modrinth/dt/9ZHJ1Ue9?color=00AF5C&label=Modrinth%20downloads&logo=modrinth)](https://modrinth.com/project/9ZHJ1Ue9)
[![CurseForge](https://img.shields.io/curseforge/dt/1661919?logo=curseforge&label=CurseForge%20downloads&color=f16436)](https://www.curseforge.com/minecraft/mc-mods/halfmasa)
[![MC Versions](https://cf.way2muchnoise.eu/versions/For%20MC_1661919_all.svg)](https://www.curseforge.com/minecraft/mc-mods/halfmasa)
[![GitHub](https://img.shields.io/github/downloads/halfkite/halfmasa/total?color=161616&label=GitHub%20downloads&logo=github)](https://github.com/halfkite/halfmasa/releases)

halfmasa是Minecraft Fabric 客户端辅助模组，集中提供路径点管理、投影补货、虚空交易、创造模式工具和界面效率功能。多数可选功能默认关闭，具体默认值见配置说明。

## 依赖

| 名称 | 类型 | 说明 |
|---|---|---|
| [Fabric Loader](https://fabricmc.net/use/installer/) | 必需 | Minecraft 1.21.x 使用 `0.17.3+`；Minecraft 26.x 使用 `0.18.4+`。 |
| [MaLiLib](https://modrinth.com/mod/malilib) | 必需 | 安装与 Minecraft 版本匹配的 MaLiLib。 |
| [Xaero's Minimap](https://modrinth.com/mod/xaeros-minimap) / [World Map](https://modrinth.com/mod/xaeros-world-map)| 可选联动 | 提供路径点绑定、地图功能 |
| [Mod Menu 模组菜单](https://github.com/TerraformersMC/ModMenu) |可选联动 |[Mod Menu 模组菜单](https://github.com/TerraformersMC/ModMenu) 本次游戏进程的位置 |
| [Litematica](https://link.mcmod.cn/target/aHR0cHM6Ly9naXRodWIuY29tL3Nha3VyYS1yeW9rby9saXRlbWF0aWNh)| 功能可选 | 投影轻松放置补货需要相应版本的 Litematica 。 |
| [Carpet-FGA-Addition](https://github.com/halfkite/Carpet-FGA-Addition) | 服务端可选 | 投影与打印机补货需要服务端提供兼容的 FGA 库存接口和相应权限。 |
| [QuickShulker](https://github.com/MoRanpcy/quickshulker) | 功能可选 | 虚空交易材料准备可从随身 QuickShulker 潜影盒取出绿宝石；此功能需要匹配的服务端支持。 |
| [Conflux Map](https://github.com/Conflux-Union/conflux-map) | 可选联动 | 提供路径点列表、临时路径点和传送相关扩展功能。 |

halfmasa 本体是客户端模组，普通客户端功能不要求服务器安装 halfmasa。依赖服务端库存 API 或虚空交易扩展的功能，需要服务器安装对应组件；详情见[兼容与设置说明](docs/features.md)。

## 版本支持

| Minecraft 版本 | halfmasa 版本 | 最低 Fabric Loader |
|---|---|---|
| `1.21.1`、`1.21.3`、`1.21.4`、`1.21.5`、`1.21.8`、`1.21.10`、`1.21.11` | `1.6.0` | `0.17.3` |
| `26.1.2`、`26.2`、`26.3` | `1.6.0` | `0.18.4` |

## 下载

- [GitHub Releases](https://github.com/halfkite/halfmasa/releases)
- [Modrinth](https://modrinth.com/project/9ZHJ1Ue9)
- [CurseForge](https://www.curseforge.com/minecraft/mc-mods/halfmasa)

选择对应 Minecraft 版本的 JAR，放入该游戏实例的 `mods` 文件夹。使用自定义游戏目录时，请将文件放入启动器配置所指向的绝对路径。

## 功能简介

### 路径点与地图

- 绑定单人存档与 Xaero 路径点目录；存档改名、移动或恢复备份后仍可沿用原路径点。
- 切换多个单人存档目录，导入或导出多维度路径点，并支持去重、撤销和重做。
- 扩展 Conflux Map：同时展示本地和共享路径点、创建重进世界后失效的临时路径点、传送后关闭地图，并为未知高度的目标设置传送高度。

### 投影补货与虚空交易

- 为 Litematica 轻松放置和兼容打印机补充材料；可配置是否从假人库存取货、取货数量和静默取物。
- 自动打开村民交易界面，并由当前玩家按交易栏序号或产出物品白名单购买；可在交易完成后关闭界面或丢出所得物品。
- 虚空交易可识别同一船只或矿车上的假人，并在关闭交易界面后按配置恢复；材料准备支持拆分绿宝石块和从 QuickShulker 取货。
- 自动补货依赖服务端库存接口；虚空交易材料准备依赖对应服务端扩展。具体兼容条件见[投影补货说明](docs/litematica-auto-refill.md)及[功能配置文档](docs/features.md)。

### 创造模式与界面效率

- 填充潜影盒、箱子、副手容器和收纳袋；整理创造物品搜索历史、可展开的创造物品条目及已保存工具栏。
- 提供 JEI/REI 配方与用途查询历史、背包内移动、快速滚动、可拖动列表和快捷键圆盘。
- 包含船只视角与手持物显示、截图复制到剪贴板、鞘翅时间提示、中英文显示空格、输入法和其他客户端界面辅助功能。

## 文档

- [中文功能与配置说明](docs/features.md)
- [English features and configuration](docs/features_en.md)
- [Litematica 与打印机补货兼容说明](docs/litematica-auto-refill.md)
- [版本兼容记录](docs/version_compatibility.md)
- [English Modrinth description](docs/modrinth_en.md)
- [构建与发布流程](docs/releasing.md)

默认按 `X + H` 打开 halfmasa 配置界面，也可从 Mod Menu 进入。完整功能和快捷键设置见功能文档。


## 许可证

项目主体使用 [MIT License](LICENSE)。第三方实现的归属及其许可证见 [THIRD_PARTY_NOTICES.md](src/main/resources/META-INF/halfmasa/THIRD_PARTY_NOTICES.md)。
