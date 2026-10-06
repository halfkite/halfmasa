# halfmasa 功能与配置

两项投影补货功能位于“其他模组扩展”栏：轻松放置与打印机各自拥有允许从假人库存取货、取货数量（默认32，0为半组）和静默子选项，并支持折叠。打印机按Hana协调器或同系InventoryUtils实际方法接入；指定数量需更新FGA服务端。[兼容范围与验收](litematica-auto-refill.md)

> 文档版本：`1.6.0`

Minecraft 26.3 新增[投影保存、删除与粘贴黑白名单](litematica-block-filters.md)，三组设置位于“其他模组扩展”栏，按方块 ID 独立筛选。

按 `X + H` 打开配置界面，也可以从 Mod Menu 进入。除特别说明外，功能默认关闭；热键为空表示默认不绑定按键。

## 其他模组扩展：Xaero 路径点与 Conflux Map

| 配置或动作 | 默认值 | 说明 |
|---|---|---|
| `enableWorldBinding` | `false` | 配置名为“Xaero 路径点与单人存档绑定”。在默认 Windows 游戏目录的绝对路径 `C:\Users\<username>\AppData\Roaming\.minecraft\saves\<world>\config\halfmasa\xaero-world-binding.json` 保存 Xaero Minimap 与 World Map 的根目录 ID，使改名、移动和备份恢复后的存档继续使用同一路径点与地图数据；旧版 `C:\Users\<username>\AppData\Roaming\.minecraft\saves\<world>\.halfmasa-xaero-binding.json` 会自动迁移。 |
| `importWaypointBundle` | 操作按钮 | 自动识别剪贴板中的 `XWB1:`/`XWB2:` 文本或复制的第一个文本文件；旧 `XWB1` 导入当前维度，`XWB2` 保持原维度与分类。 |
| `exportAllDimensions` | 操作按钮 | 将当前 Xaero 世界根容器中的全部维度、全部分类导出为 `XWB2:` 文本或 UTF-8 `.txt` 文件；包括 VC 等模组创建的额外维度，不限于主世界、下界和末地。每个维度同时保存正式维度 ID、Xaero 等效维度 ID、完整容器节点、完整世界路径、本地世界键、Xaero 维度名和自定义名称。 |
| `exportCurrentDimension` | 操作按钮 | 将当前维度的全部分类导出为文本或文件。 |
| `exportCurrentWaypointSet` | 操作按钮 | 将当前维度的当前分类导出为文本或文件。 |
| `dedupeWaypoints` | 操作按钮 | “合并当前”处理当前分类，“合并全部”分别处理本维度全部分类；按坐标和名称去重并保留较早的路径点。 |
| `waypointHistory` | 操作按钮 | 撤回或反撤回当前游戏会话内最近的导入与去重操作，最多保留 5 步；切换 Xaero 世界后清空。 |
| `confluxMapExtensions` | `false` | 需要匹配游戏版本及扩展 API 的 Conflux Map；打开路径点列表时可默认展示所有维度或公共路径点，传送后可关闭地图；未知高度的目标使用 `confluxMapUnknownHeight`（默认 `128`）传送；可在列表同时显示本地和共享路径点（左右或上下排列），并可用快捷键连续创建多个临时本地路径点（重进该世界后清除）。 |

文件导出默认命名为 `halfmasa-xaero-yyyyMMdd-HHmm.txt`，保存成功后同一内容也会复制到剪贴板。临时路径点、服务器路径点和第三方动态路径点不会进入分享包。导入和去重操作会保存完整的 Xaero 路径点快照用于撤回，导出不会进入操作历史。导入 `XWB2:` 时按正式维度 ID、Xaero 等效维度 ID、完整容器节点和本地世界键逐级匹配，避免多个 VC 维度因共享 `waypoints` 节点而合并。

## 单人存档路径

| 配置 | 默认值 | 说明 |
|---|---|---|
| `customSavesPaths` | 空列表 | 自定义存档路径列表；支持绝对路径和相对游戏目录的路径。在单人游戏的“选择世界”界面左上角点击当前路径即可即时切换，原版 `saves` 始终可选。 |
| `keepWorldSelectionOnEmpty` | `false` | 开启后，当前存档路径没有世界时点击“单人游戏”仍显示世界选择界面，不再自动跳转到创建世界界面。 |

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
| `trialCreativeTab` | `false` | 在创造模式物品栏末尾新增「试炼栏」，收录全部试炼刷怪笼配置与试炼宝库（详见下节）。 |

### 试炼栏

开启 `trialCreativeTab` 后，创造模式物品栏的页签列表末尾会出现一个「试炼栏」，只在本模组的客户端可见。

- **试炼刷怪笼**：收录全部 14 种原版试炼密室配置，每种包含三个条目——普通、不祥，以及进入冷却（cooldown）状态的版本，共 42 个条目。
- 条目为携带方块实体数据的原版试炼刷怪笼，放置后即为对应的预设状态；冷却版本的 `cooldown_ends_at` 设为未来时间，放置后立即处于冷却中。
- **试炼宝库**：普通与不祥各一个条目，共 2 个条目。

条目覆盖的原版配置为 `trial_chamber/breeze`、`melee/{husk,spider,zombie}`、`ranged/{poison_skeleton,skeleton,stray}`、`slow_ranged/{poison_skeleton,skeleton,stray}`、`small_melee/{baby_zombie,cave_spider,silverfish,slime}`。

> 注：1.21.1 尚未把试炼刷怪笼配置放入注册表，该版本改用等价的内联配置对象实现相同效果。

## JEI/REI 查询历史（其他模组扩展）

| 配置或动作 | 默认值 | 说明 |
|---|---|---|
| `itemManagerRecipeHistory` | `false` | 为 JEI 和 REI 分别记录配方查询、用途查询和成功取得的物品。 |
| `itemManagerRecipeHistoryRows` | `3` | 历史网格显示行数，范围 `1-9`。 |
| `itemManagerRecipeHistoryPosition` | `bottom_right` | 将历史栏放在右下、右上、左上或左下，并让原生条目列表和收藏区避让。 |
| `cycleItemManagerRecipeHistoryPosition` | 空热键 | 在四个角之间循环切换；物品栏打开时也可触发，并显示当前位置提示。 |

历史栏会在物品管理器首次打开时初始化，不需要先搜索配方。默认 Windows 游戏目录下，查询历史保存在绝对路径 `C:\Users\<username>\AppData\Roaming\.minecraft\config\halfmasa\search-history\`，JEI 与 REI 使用独立文件。

## 客户端与界面功能

投影补货子选项 `litematicaRefillSilent`（“投影补货静默取物”）默认关闭，可绑定快捷键；需要新版 FGA `silent_take` 服务端接口，直接扣减已验证的离线来源库存，不召唤或下线假人。已经在线的来源保持在线；服务端不支持时提示并暂停补货。

所有支持版本提供 `litematicaAutoRefill`（投影轻松放置自动补货，默认关闭，可绑定快捷键）。开启后，仅当轻松放置所需物品在背包、快捷栏、副手及随身潜影盒内全部耗尽时，优先直接从无前后缀中文名假人按配置数量取货（默认32，0为半组），未找到有效且足量的来源后再按物品 ID 查询分类库存（直接取货需要新版 FGA direct_take 接口）；取货前再次检查本地材料，等待原版背包同步后重试原位置。客户端需要 Litematica 和 Fabric API，服务端需要支持库存 API v1 的 FGA；无频道、无权限、缺货或背包满时会提示并停止当前补货。取物结果不确定时暂停自动补货至重连，避免重复扣货。[设置与验收步骤](litematica-auto-refill.md)

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
| `keepConfigSearchPosition`（26.3） | `true` | “保持配置界面位置”的子选项；主开关开启时，按 MaLiLib 配置分类分别恢复搜索文字、按键搜索、搜索栏开合及列表滚动位置。仅在本次游戏内记忆。 |
| `keepConfigSelectedTab`（26.3） | `true` | 主开关开启时，重新打开 halfmasa 配置恢复上次选中的栏位；只支持全部、建议开启、创造模式工具、移植功能、其他模组扩展、禁用。 |

26.3 中展开“保持配置界面位置”即可设置上述子项。开启主开关后，在一个栏位输入搜索并滚动，切到其他栏位再切回来或关闭后重开，即可检查各栏位的内容和位置是否恢复。关闭搜索/位置子选项会清除这部分记忆；关闭栏位记忆后，普通配置入口恢复默认“全部”。重启游戏清除所有界面记忆。

## 虚空交易（扩展）

虚空交易主开关默认关闭。开启后，打开船只或矿车上的村民交易界面时，可按配置名称或自动识别同一载具上的假人并令其下线；关闭交易界面后默认执行 `rejoin`，也可改为等待 `spawn` 上线后再执行 `mount`。自动识别可能把载具上的真人玩家也当作假人，仅在确认载具上没有真人时使用。

村民消失或配置的假人全部下线后，自动交易由当前玩家在打开的村民交易界面中购买；启动后需保持界面打开。可填写从 1 开始的交易栏序号（例如 `1,2,3`），每一项买到材料不足；也可启用物品白名单，按交易产出物品 ID 筛选。交易后可选自动关闭面板或丢出本次获得的物品。自动打开村民界面支持独立的取消快捷键，默认 `Esc`。

材料准备选项可在交易前把绿宝石块拆成绿宝石，或通过 QuickShulker API 从随身潜影盒取出绿宝石和绿宝石块。材料准备需要客户端与服务端的 Fabric API，以及兼容的虚空交易服务端扩展；使用 QuickShulker 取物时服务端还需 QuickShulker。

## 输入、地图与实用功能

Minecraft 26.3 的“更好的按键设置”按键浏览界面会在本次游戏中默认记住列表位置和每个模组的折叠状态。折叠或展开时保留当前浏览位置；按键冲突在键帽右侧用分开的浅红、深红色条标识。

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
