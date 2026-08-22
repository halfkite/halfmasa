# halfmasa 功能与配置

> 文档版本：`1.1.2`

按 `X + H` 打开配置界面，也可以从 Mod Menu 进入。除特别说明外，功能默认关闭；热键为空表示默认不绑定按键。

## Xaero 与路径点工具

| 配置或动作 | 默认值 | 说明 |
|---|---|---|
| `enableWorldBinding` | `false` | 在默认 Windows 游戏目录的绝对路径 `C:\Users\<username>\AppData\Roaming\.minecraft\saves\<world>\config\halfmasa\xaero-world-binding.json` 保存 Xaero Minimap 与 World Map 的根目录 ID，使改名、移动和备份恢复后的存档继续使用同一路径点与地图数据；旧版 `C:\Users\<username>\AppData\Roaming\.minecraft\saves\<world>\.halfmasa-xaero-binding.json` 会自动迁移。 |
| `customSavesPaths` | 空列表 | 自定义存档路径列表；支持绝对路径和相对游戏目录的路径。在单人游戏的“选择世界”界面左上角点击当前路径即可即时切换，原版 `saves` 始终可选。 |
| `keepWorldSelectionOnEmpty` | `false` | 开启后，当前存档路径没有世界时点击“单人游戏”仍显示世界选择界面，不再自动跳转到创建世界界面。 |
| `copyWaypointBundle` | 空热键 | 将当前 Xaero 世界中的普通路径点和分类压缩为 `XWB1:` 文本并复制到剪贴板。 |
| `importWaypointBundle` | 空热键 | 从剪贴板导入完整的 `XWB1:` 分享包到当前 Xaero 世界。 |
| `shareCurrentWaypointSet` | 空热键 | 使用 Xaero 原生聊天格式分享当前路径点分类。 |
| `shareAllWaypoints` | 空热键 | 分批分享当前 Xaero 世界中的全部普通路径点。 |
| `dedupeCurrentWaypointSet` / `dedupeAllWaypoints` | 空热键 | 按坐标和名称去重，并保留每个分类中较早的路径点。 |
| `waypointChatInterval` | `10` tick | 控制连续路径点聊天消息的发送间隔。 |

临时路径点、服务器路径点和第三方动态路径点不会进入分享包。Xaero's World Map 未安装时只处理 Minimap 数据。

## 创造模式工具

| 配置或动作 | 默认值 | 说明 |
|---|---|---|
| `enableGiveFullInventory` | `false` | 启用创造模式容器填充。 |
| `giveFullInventory` | `G` | 使用主手物品填充潜影盒、箱子、副手容器或收纳袋；具体结果根据主手、副手和容器类型决定。 |
| `bundleFill` | `1` | 副手为收纳袋时尝试插入物品堆的次数。 |
| `fillSafety` | `true` | 阻止不安全的容器和潜影盒嵌套。 |
| `itemSearchHistory` | `false` | 记录创造搜索后取得的物品，并在创造搜索结果顶部显示独立历史栏。 |
| `itemSearchHistoryRows` | `3` | 创造搜索历史的最大显示行数，范围 `1-9`。 |
| `itemSearchHistoryDuringSearch` | `false` | 输入搜索文字时仍显示历史栏；关闭时只在清空搜索栏后显示。 |
| `condensedCreative` | `false` | 将附魔书、药水、药箭和多种方块变体合并为可展开的创造物品条目。 |

## JEI/REI 查询历史（扩展）

| 配置或动作 | 默认值 | 说明 |
|---|---|---|
| `itemManagerRecipeHistory` | `false` | 为 JEI 和 REI 分别记录配方查询、用途查询和成功取得的物品。 |
| `itemManagerRecipeHistoryRows` | `3` | 历史网格显示行数，范围 `1-9`。 |
| `itemManagerRecipeHistoryPosition` | `bottom_right` | 将历史栏放在右下、右上、左上或左下，并让原生条目列表和收藏区避让。 |
| `cycleItemManagerRecipeHistoryPosition` | 空热键 | 在四个角之间循环切换；物品栏打开时也可触发，并显示当前位置提示。 |

历史栏会在物品管理器首次打开时初始化，不需要先搜索配方。默认 Windows 游戏目录下，查询历史保存在绝对路径 `C:\Users\<username>\AppData\Roaming\.minecraft\config\halfmasa\search-history\`，JEI 与 REI 使用独立文件。

## 客户端与界面功能

| 配置 | 默认值 | 说明 |
|---|---|---|
| `screenshotToClipboard` | `false` | 按 F2 保存截图时把完整图片同时复制到系统剪贴板。 |
| `elytraTimeTooltip` | `false` | 在鞘翅提示中显示剩余飞行时间。 |
| `reportElytraTime` | 空热键 | 在聊天栏报告当前装备鞘翅的预计剩余时间。 |
| `nightVisionFade` | `true` | 启用夜视平滑淡出；关闭后恢复原版结束前 10 秒闪烁。 |
| `nightVisionFadeSeconds` | `5` | 指定平滑淡出秒数；范围 `0-60`，`0` 表示不提前淡出。 |
| `boatView360` / `boatItemView` | `false` | 解除乘船视角旋转限制，并在划船时保留第一人称手持物品显示。 |
| `inventoryMove` | `false` | 原版背包和容器界面打开时继续移动、跳跃和潜行。 |
| `fastWorldLoadingScreen` / `fastResourcePackLoadingScreen` | `false` | 减少世界与资源包加载界面的额外等待。 |
| `betterSavedHotbars` | `false` | 增强创造保存工具栏：支持拖入或替换单个物品、中键删除，并记住滚动位置；旧版游戏根目录 `hotbar.nbt` 首次自动复制到 `config/halfmasa/better-saved-hotbars/hotbar.nbt`。 |
| `cooldownAutoAttack` | `false` | 按住攻击键时在原版攻击冷却完成后自动攻击准星目标。 |
| `draggableLists` | `false` | 支持拖动资源包和服务器列表条目，并可隐藏原生移动箭头。 |
| `fastScrolling` | `false` | 仅加速当前界面（包括 MaLiLib 配置界面）的滚轮事件，不影响游戏内快捷栏；展开后可分别配置两套模式。 |
| `fastScrollingPrimaryEnabled` / `fastScrollingPrimaryHotkey` / `fastScrollingPrimaryMultiplier` | `true` / `Left Ctrl` / `2` | 模式一可独立开关、改键并设置 `1–32` 倍率。 |
| `fastScrollingSecondaryEnabled` / `fastScrollingSecondaryHotkey` / `fastScrollingSecondaryMultiplier` | `true` / `Left Ctrl + Left Shift` / `6` | 模式二可独立开关、改键并设置 `1–32` 倍率；两套同时匹配时优先使用模式二。 |
| `bridgingAssist` | `false` / 空热键 | 准星未命中方块时启用基岩版式环绕放置；展开后可设置距离、潜行、轴向、延迟、视线、吸附、台阶辅助、火把过滤、准星和轮廓。 |
| `skipResourcePackCompatibilityCheck` | `false` | 将添加的资源包视为兼容并跳过版本不匹配确认。 |
| `disablePausedItemTrajectoryPrediction` | `false` | Carpet 或原版 `/tick freeze` 暂停时停止客户端继续预测掉落物轨迹。 |
| `keepModMenuScroll` | `false` | 分别记忆 Mod Menu 和每个 MaLiLib 配置分类的滚动位置。 |

## 输入、地图与实用功能

| 配置 | 默认值 | 说明 |
|---|---|---|
| `keybindPieMenu` | `false` | 为冲突或相关按键提供可定制的圆盘选择界面，支持颜色、动画、缩放和取消区设置。 |
| `clickAndSend` | `false` | 将可点击文本中的非斜杠命令内容作为普通聊天发送。 |
| `cjkLatinSpacing` | `false` / 空热键 | 在中文与相邻英文单词或数字之间加入显示空格；展开后可分别控制翻译文本、告示牌以及书与笔/成书，不修改告示牌或书本保存的原始内容。 |
| `cjkLatinSpacingTranslations` / `cjkLatinSpacingSigns` / `cjkLatinSpacingBooks` | `true` | 分别控制翻译文本、告示牌文字与书本页面的显示空格。 |
| `mapInSlot` | `false` | 在快捷栏、背包和容器槽位中预览已填写地图，同时保留数量和装饰层。 |
| `serverIconCache` | `true` | 缓存服务器图标，可按名称、地址或两者匹配，并设置缓存数量上限；清空缓存前需要二次确认。 |
| `toastKiller` | `false` | 清除当前提示并在启用期间拒绝新提示。 |
| `serverPingerFix` | `false` | 扩展服务器刷新线程池并清理过期排队任务。 |
| `contingameIme` | `false` | Windows JNI 游戏内输入法，支持组合文本、候选框、临时模式和持续模式。 |

默认 Windows 游戏目录下，快捷键圆盘数据位于绝对路径 `C:\Users\<username>\AppData\Roaming\.minecraft\config\halfmasa\keybind-pie\bindings.json`；服务器图标缓存位于 `C:\Users\<username>\AppData\Roaming\.minecraft\config\halfmasa\server-icons\`。
多人服务器相关的持久化数据统一保存在默认 Windows 游戏目录的绝对路径 `C:\Users\<username>\AppData\Roaming\.minecraft\config\halfmasa\`，不会写入单人存档目录。

## 已停用的高级功能

流体渲染屏蔽和实体渲染聚合位于“已停用功能”分类。它们可能显著改变画面或兼容性，因此不会进入推荐页，应在理解影响后单独启用和配置。

## 配置文件

默认 Windows 游戏目录下，主配置文件为绝对路径 `C:\Users\<username>\AppData\Roaming\.minecraft\config\halfmasa\halfmasa.json`。旧版根目录配置会迁移到 `C:\Users\<username>\AppData\Roaming\.minecraft\config\halfmasa\legacy\`。功能数据使用临时文件和原子替换写入，避免正常保存过程中留下不完整 JSON；如果启动器使用自定义游戏目录，请将 `.minecraft` 替换为该绝对路径。
