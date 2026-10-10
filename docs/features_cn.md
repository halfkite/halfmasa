[中文](https://github.com/halfkite/halfmasa/blob/main/docs/features_cn.md) | [English](https://github.com/halfkite/halfmasa/blob/main/docs/features_en.md)

# halfmasa 功能与配置

> 文档版本 `1.6.1` ｜ 按 `X+H` 打开配置（也可从 Mod Menu 进入）｜ 除特别说明外默认关闭，热键空表示不绑定

## 投影相关功能

| 名称 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| 投影轻松放置自动补货<br>`litematicaAutoRefill` | 带热键布尔值 | 无快捷键, `false` | 轻松放置物品耗尽时从假人取货（默认32，0为半组），再按物品ID查分类库存；取货前复查本地材料。需 Litematica + Fabric API + FGA(库存API v1)。无频道/权限/缺货/背包满时提示并停止；取物不确定时暂停至重连。[兼容与配置说明](https://github.com/halfkite/halfmasa/blob/main/docs/features_cn.md#投影相关功能) |
| 投影允许从假人库存取货<br>`litematicaRefillAllowFake` | 布尔值 | `false` | 允许轻松放置补货从假人库存取货 |
| 投影取货数量<br>`litematicaRefillAmount` | 整数 | `32` | 每次取货数量，0=半组。范围 0–64 |
| 投影补货静默取物<br>`litematicaRefillSilent` | 带热键布尔值 | 无快捷键, `false` | 静默取物，直接扣减离线来源库存，不召唤/下线假人。需 FGA `silent_take` 接口 |
| 打印机自动补货<br>`printerAutoRefill` | 布尔值 | `false` | 打印机补货，按 Hana 协调器或 InventoryUtils 接入；含取货开关、数量、静默子选项，支持折叠。指定数量需更新 FGA 服务端 |
| 打印机允许从假人库存取货<br>`printerRefillAllowFake` | 布尔值 | `false` | 允许打印机补货从假人库存取货 |
| 打印机取货数量<br>`printerRefillAmount` | 整数 | `32` | 打印机每次取货数量，0=半组 |
| 打印机静默取物<br>`printerRefillSilent` | 布尔值 | `false` | 打印机静默取物，直接扣减离线来源库存 |
| 投影保存黑白名单<br>`litematicaSaveFilter` | 布尔值 | `false` | 投影保存时按方块ID筛选（26.3），展开后可设白/黑名单 |
| 投影保存方块白名单<br>`litematicaSaveWhitelist` | 字符串列表 | `[]` | 投影保存白名单，按方块ID筛选（26.3） |
| 投影保存方块黑名单<br>`litematicaSaveBlacklist` | 字符串列表 | `[]` | 投影保存黑名单，按方块ID筛选（26.3） |
| 投影删除黑白名单<br>`litematicaDeleteFilter` | 布尔值 | `false` | 投影删除时按方块ID筛选（26.3） |
| 投影删除方块白名单<br>`litematicaDeleteWhitelist` | 字符串列表 | `[]` | 投影删除白名单（26.3） |
| 投影删除方块黑名单<br>`litematicaDeleteBlacklist` | 字符串列表 | `[]` | 投影删除黑名单（26.3） |
| 投影粘贴黑白名单<br>`litematicaPasteFilter` | 布尔值 | `false` | 投影粘贴时按方块ID筛选（26.3），详见本节配置说明。 |
| 投影粘贴方块白名单<br>`litematicaPasteWhitelist` | 字符串列表 | `[]` | 投影粘贴白名单（26.3） |
| 投影粘贴方块黑名单<br>`litematicaPasteBlacklist` | 字符串列表 | `[]` | 投影粘贴黑名单（26.3） |

## Xaero 路径点与 Conflux Map

| 名称 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| Xaero 路径点与单人存档绑定<br>`enableWorldBinding` | 布尔值 | `false` | 在 `saves\<world>\config\halfmasa\xaero-world-binding.json` 保存 Xaero 根目录ID，改名/移动/备份后继续使用同一路径点；旧版 `.halfmasa-xaero-binding.json` 自动迁移 |
| 导入路径点<br>`importWaypointBundle` | 操作按钮 | — | 识别剪贴板 `XWB1:`/`XWB2:` 或文本文件；XWB1导入当前维度，XWB2保持原维度与分类。导入时按维度ID/容器节点/世界键逐级匹配，避免VC维度合并 |
| 分享全部维度的全部分类<br>`exportAllDimensions` | 操作按钮 | — | 导出全部维度全部分类为 `XWB2:` 文本或 `.txt`（含VC等额外维度）；默认命名 `halfmasa-xaero-yyyyMMdd-HHmm.txt`，同时复制到剪贴板 |
| 分享本维度全部分类<br>`exportCurrentDimension` | 操作按钮 | — | 导出当前维度全部分类 |
| 分享当前分类<br>`exportCurrentWaypointSet` | 操作按钮 | — | 导出当前维度当前分类 |
| 路径点去重<br>`dedupeWaypoints` | 操作按钮 | — | 按坐标和名称去重，保留较早路径点；可合并当前分类或本维度全部分类 |
| 路径点操作历史<br>`waypointHistory` | 操作按钮 | — | 撤回/反撤回最近导入与去重，最多5步；切换Xaero世界后清空。导出不进入历史 |
| Conflux Map 扩展功能开关<br>`confluxMapExtensions` | 布尔值 | `false` | Conflux Map扩展：列表默认展示所有维度/公共路径点、传送后关地图、同时显示本地+共享路径点、快捷键连续创建临时本地路径点（重进清除）。需匹配版本及扩展API |
| 未知高度传送高度<br>`confluxMapUnknownHeight` | 整数 | `128` | 未知高度目标的传送高度。范围 -64–320 |

> 临时路径点、服务器路径点和第三方动态路径点不会进入分享包。

## JEI/REI 查询历史

| 名称 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| JEI/REI 配方查询历史记录<br>`itemManagerRecipeHistory` | 布尔值 | `false` | 分别记录 JEI/REI 的配方查询、用途查询和成功取得物品。首次打开物品管理器时初始化。历史存于 `config\halfmasa\search-history\`，JEI与REI独立文件 |
| 配方查询历史显示行数<br>`itemManagerRecipeHistoryRows` | 整数 | `3` | 历史网格行数。范围 1–9 |
| 配方查询历史显示位置<br>`itemManagerRecipeHistoryPosition` | 选项列表 | `bottom_right` | 历史栏位置：右下/右上/左上/左下，原生条目和收藏区避让 |
| 切换配方查询历史位置<br>`cycleItemManagerRecipeHistoryPosition` | 热键 | 无快捷键 | 四角循环切换，物品栏打开时也可触发并显示提示 |

## 创造模式工具

| 名称 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| 启用 G 键打盒<br>`enableGiveFullInventory` | 布尔值 | `false` | 启用创造模式容器填充 |
| 执行一次打盒<br>`giveFullInventory` | 热键 | `G` | 用主手物品填充潜影盒/箱子/副手容器/收纳袋，结果依主手副手和容器类型而定 |
| 收纳袋填充次数<br>`bundleFill` | 整数 | `1` | 副手为收纳袋时尝试插入物品堆的次数。范围 1–64 |
| 容器嵌套安全限制<br>`fillSafety` | 布尔值 | `true` | 阻止不安全的容器和潜影盒嵌套 |
| 创造搜索历史记录<br>`itemSearchHistory` | 布尔值 | `false` | 记录创造搜索取得的物品，结果顶部显示历史栏 |
| 历史记录行数<br>`itemSearchHistoryRows` | 整数 | `3` | 历史最大行数。范围 1–9 |
| 搜索时显示历史记录<br>`itemSearchHistoryDuringSearch` | 布尔值 | `false` | 输入搜索文字时仍显示历史栏；关闭则仅清空搜索栏后显示 |
| 创造物品栏合并条目<br>`condensedCreative` | 布尔值 | `false` | 附魔书/药水/药箭/方块变体合并为可展开条目 |
| 试炼栏<br>`trialCreativeTab` | 布尔值 | `false` | 创造栏末尾新增「试炼栏」，仅本模组客户端可见。26.3装Fabric创造栏API时正常注册显示，未装时用右侧按钮打开。收录42个试炼刷怪笼（14配置×3状态：普通/不祥/冷却，携带方块实体数据）+2个试炼宝库。物品按方块状态显示原版材质+内部生物；悬停显示笼子类型/生物/状态/冷却；Ctrl+中键复制保留状态属性。图标用原版笼子材质不叠加色条，生物模型直立面向玩家（史莱姆原尺寸、其他放大20%），前景居中防裁剪。覆盖配置：`trial_chamber/breeze`、`melee/{husk,spider,zombie}`、`ranged/{poison_skeleton,skeleton,stray}`、`slow_ranged/{poison_skeleton,skeleton,stray}`、`small_melee/{baby_zombie,cave_spider,silverfish,slime}`。1.21.1改用等价内联配置对象 |

## 客户端与界面功能

| 名称 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| F2 截图复制到剪贴板<br>`screenshotToClipboard` | 布尔值 | `false` | F2截图时同时复制到系统剪贴板 |
| 鞘翅剩余飞行时间提示<br>`elytraTimeTooltip` | 布尔值 | `false` | 鞘翅提示显示剩余飞行时间 |
| 聊天栏显示鞘翅时间<br>`reportElytraTime` | 热键 | 无快捷键 | 聊天栏报告鞘翅预计剩余时间 |
| 夜视平滑淡出<br>`nightVisionFade` | 布尔值 | `true` | 夜视平滑淡出；关闭恢复原版结束前10秒闪烁 |
| 夜视淡出时间（秒）<br>`nightVisionFadeSeconds` | 整数 | `5` | 淡出秒数，0=不提前淡出。范围 0–60 |
| 船上 360° 自由视角<br>`boatView360` | 布尔值 | `false` | 解除乘船视角旋转限制 |
| 划船时显示手持物品<br>`boatItemView` | 布尔值 | `false` | 划船时保留第一人称手持物品显示 |
| 边走边看背包<br>`inventoryMove` | 布尔值 | `false` | 背包/容器界面打开时可继续移动、跳跃、潜行 |
| 快速关闭世界加载界面<br>`fastWorldLoadingScreen` | 布尔值 | `false` | 减少世界加载界面额外等待 |
| 快速关闭资源包加载遮罩<br>`fastResourcePackLoadingScreen` | 布尔值 | `false` | 减少资源包加载界面额外等待 |
| 增强创造保存工具栏<br>`betterSavedHotbars` | 布尔值 | `false` | 增强创造保存工具栏：拖入/替换单物品、中键删除、记住滚动；旧 `hotbar.nbt` 自动复制到 `config\halfmasa\better-saved-hotbars\` |
| 长按左键按攻击冷却自动攻击<br>`cooldownAutoAttack` | 布尔值 | `false` | 按住攻击键时冷却完成后自动攻击准星目标 |
| 可拖拽列表<br>`draggableLists` | 布尔值 | `false` | 资源包和服务器列表条目可拖动，可隐藏原生移动箭头 |
| 快速界面滚动<br>`fastScrolling` | 布尔值 | `false` | 加速当前界面（含MaLiLib配置）滚轮，不影响快捷栏；展开后两套模式 |
| 启用快速滚动模式一<br>`fastScrollingPrimaryEnabled` | 布尔值 | `true` | 模式一开关 |
| 快速滚动模式一按键<br>`fastScrollingPrimaryHotkey` | 热键 | `Left Ctrl` | 模式一触发热键 |
| 快速滚动模式一倍率<br>`fastScrollingPrimaryMultiplier` | 整数 | `2` | 模式一滚动倍率。范围 1–32 |
| 启用快速滚动模式二<br>`fastScrollingSecondaryEnabled` | 布尔值 | `true` | 模式二开关；同时匹配优先模式二 |
| 快速滚动模式二按键<br>`fastScrollingSecondaryHotkey` | 热键 | `Left Ctrl+Left Shift` | 模式二触发热键 |
| 快速滚动模式二倍率<br>`fastScrollingSecondaryMultiplier` | 整数 | `6` | 模式二滚动倍率。范围 1–32 |
| 环绕放置搭桥辅助<br>`bridgingAssist` | 带热键布尔值 | 无快捷键, `false` | 准星未命中方块时基岩版式环绕放置；展开后可设距离/潜行/轴向/延迟/视线/吸附/台阶辅助/火把过滤/准星/轮廓 |
| 取消资源包兼容性检查<br>`skipResourcePackCompatibilityCheck` | 布尔值 | `false` | 添加的资源包视为兼容，跳过版本不匹配确认 |
| 禁用暂停时客户端掉落物轨迹预测<br>`disablePausedItemTrajectoryPrediction` | 布尔值 | `false` | Carpet或 `/tick freeze` 暂停时停止客户端预测掉落物轨迹 |
| 保持配置界面位置<br>`keepModMenuScroll` | 布尔值 | `false` | 分别记忆 Mod Menu 和每个 MaLiLib 配置分类的滚动位置 |
| 保持搜索内容和位置<br>`keepConfigSearchPosition` | 布尔值 | `true` | 按分类恢复搜索文字/按键搜索/搜索栏开合/列表滚动，仅本次游戏内记忆（26.3） |
| 保持选中的栏位<br>`keepConfigSelectedTab` | 布尔值 | `true` | 重开配置恢复上次选中栏位（26.3） |
| 经典暂停菜单<br>`classicPauseMenu` | 布尔值 | `false` | 经典暂停菜单：主区恢复原版按钮排列，附加按钮移右侧，小窗口分列排列（26.3） |

## 虚空交易

| 名称 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| 虚空交易<br>`voidTrading` | 布尔值 | `false` | 总开关。打开船只/矿车上村民交易界面时，按配置名称或自动识别同载具假人并令其下线；关闭界面后默认 `rejoin`，可改为等 `spawn` 后 `mount`。**注意**：自动识别可能把载具上真人也当假人，仅确认无真人时使用 |
| 虚空交易假人名称<br>`voidTradingFakePlayerNames` | 字符串列表 | `[]` | 指定要下线的假人名称 |
| 自动识别载具上的假人<br>`voidTradingAutoDetectFakePlayers` | 布尔值 | `false` | 自动识别同一载具上的假人 |
| 假人恢复方式<br>`voidTradingRecoveryMode` | 选项列表 | `rejoin` | 关闭交易界面后假人恢复方式：`rejoin` 或等 `spawn` 后 `mount` |
| 自动购买村民交易<br>`voidTradingAutoTrade` | 布尔值 | `false` | 村民消失或假人全部下线后，由当前玩家在打开的界面购买（需保持界面打开） |
| 自动打开村民界面<br>`voidTradingAutoOpen` | 布尔值 | `false` | 自动打开村民交易界面 |
| 取消自动打开快捷键<br>`voidTradingAutoOpenCancel` | 热键 | `Esc` | 取消自动打开村民界面的快捷键 |
| 交易栏项目序号<br>`voidTradingTradeIndices` | 字符串 | 空 | 填写交易栏序号（如 `1,2,3`），每项买到材料不足 |
| 只交易指定物品<br>`voidTradingTradeSpecifiedItems` | 布尔值 | `false` | 启用物品白名单，按产出物品ID筛选 |
| 交易指定物品白名单<br>`voidTradingTradeItems` | 字符串列表 | `[]` | 按交易产出物品ID筛选 |
| 交易完成后关闭面板<br>`voidTradingAutoClose` | 布尔值 | `false` | 交易后自动关闭面板 |
| 交易完成后丢出交易物品<br>`voidTradingDropTradeItems` | 布尔值 | `false` | 交易后丢出本次获得的物品 |
| 自动分解绿宝石块<br>`voidTradingAutoUncraftEmeraldBlocks` | 布尔值 | `false` | 交易前把绿宝石块拆成绿宝石 |
| 从快捷潜影盒取交易材料<br>`voidTradingQuickShulker` | 布尔值 | `false` | 通过 QuickShulker API 从随身潜影盒取绿宝石/绿宝石块。需客户端+服务端 Fabric API 及兼容的虚空交易服务端扩展；用 QuickShulker 时服务端还需 QuickShulker |

## 输入与按键设置

### 更好的按键设置（26.3，默认开启）

| 名称 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| 更好的按键设置<br>`keymapSettingsGroup` | 带热键布尔值 | 开启, 无快捷键 | 主开关 + 打开界面快捷键，原"打开按键映射浏览"快捷键迁移至此 |
| 打开按键绑定界面<br>`openKeymapBrowser` | 操作按钮/热键 | — | 打开按键绑定浏览界面 |
| 按键绑定方式<br>`keymapBindingMode` | 选项列表 | 虚拟按键 | 虚拟按键模式：点列表键位→点上方键盘选中，预览用箭头连接，左侧"确认"应用；机械按键模式：按下生成预览，松开不自动确认，Esc清空，Backspace可作普通绑定 |
| 虚拟键盘布局<br>`keymapKeyboardLayout` | 选项列表 | `104键` | 104键或122键布局；122键增F13–F24和六个终端组合键 |
| 按键设置背景透明度<br>`keymapBackgroundTransparency` | 整数 | `31` | 背景透明度 0–100%，默认31%与原背景一致 |

**浏览界面特性**：记住列表位置和模组折叠状态；各行统一边界对齐；已绑定绿色/未绑定半透明灰；标识点右侧排列：蓝点=原版、橙点=MaLiLib、紫点=组合键；冲突提示相邻列：浅红=单键冲突、深红=组合键冲突。

**列表操作**：右侧三列"重置"/"以按键顺序触发"/"触发按键选项"，后两列仅显示是/否；单独Shift/Ctrl/Alt和鼠标左中右键默认不触发选项，含修饰键组合仍可触发；Fn由硬件处理；关闭触发选项不禁用原热键；所有改动暂存，"保存退出"才写入，有改动时"不保存退出"需二次确认。

**鼠标行为**：未按键盘时鼠标左/右/中键不自动触发冲突选择；带键盘的鼠标组合和侧键仍按原规则。

### 冲突按键选择

| 名称 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| 冲突按键选择<br>`keybindPieMenu` | 布尔值 | `false` | 为冲突/相关按键提供轮盘或条目选择界面，支持颜色/透明度/动画/缩放/压暗背景 |
| 冲突按键选择布局<br>`keybindSelectionLayout` | 选项列表 | 轮盘 | 轮盘或条目模式；条目模式每项一行，滚轮滚动，松开触发键执行当前条目。组合键仅与完全相同的组合键冲突；已开X选择界面继续按C切换到X+C冲突项 |
| 选择重复冷却<br>`keybindPieRepeatCooldown` | 整数 | — | 保持所选动作时两次重复触发之间的 tick 数 |
| 选择冷却<br>`keybindPieSelectionCooldown` | 整数 | — | 选择动作的冷却时间 |
| 选择攻击修复<br>`keybindPieAttackWorkaround` | 布尔值 | — | 选择攻击动作时清除客户端攻击等待时间 |
| 选择忽略按键<br>`keybindPieIgnoredKeys` | 字符串 | — | 轮盘忽略的 GLFW 键码，逗号分隔 |
| 反转选择忽略规则<br>`keybindPieInvertIgnoredKeys` | 布尔值 | `false` | 把忽略列表改为允许列表 |
| 圆形精细度（轮盘模式）<br>`keybindPieCircleVertices` | 整数 | — | 绘制轮盘圆形的顶点数量 |
| 压暗选择界面背景<br>`keybindPieDarkenBackground` | 布尔值 | — | 压暗轮盘后方的游戏画面 |
| 选中选项扩张<br>`keybindPieExpansion` | 实数 | — | 选中扇区向外扩张的倍率 |
| 选择界面大小<br>`keybindPieScale` | 实数 | — | 轮盘整体缩放比例；条目模式同时改行高/文字/可见行数 |
| 中心取消区域（轮盘模式）<br>`keybindPieCancelZone` | 实数 | — | 放开按键后取消选择的中心区域；条目模式指向行外/空隙取消 |
| 选择基础颜色<br>`keybindPieMenuColor` | 颜色 | — | 扇区基础颜色 |
| 选择选中颜色<br>`keybindPieSelectedColor` | 颜色 | — | 选中扇区的颜色 |
| 选择高亮颜色<br>`keybindPieHighlightColor` | 颜色 | — | 高亮标签的颜色 |
| 交替选项增亮<br>`keybindPieAlternateLighten` | 实数 | — | 交替扇区增加的亮度 |
| 选择界面透明度<br>`keybindPieAlpha` | 整数 | — | 扇区透明度 0–255，外框和滚动条跟随 |
| 选择明暗交替<br>`keybindPieGradation` | 布尔值 | — | 交替改变相邻扇区亮度 |
| 选择打开动画<br>`keybindPieAnimate` | 布尔值 | — | 播放轮盘打开动画 |
| 打开冲突按键逐项配置<br>`openKeybindPieEditor` | 操作按钮 | — | 编辑每个原版按键 ID 的名称、分类显示和扇区颜色 |
| 重新加载冲突按键配置<br>`reloadKeybindPieData` | 操作按钮 | — | 重新加载 `config/halfmasa/keybind-pie/bindings.json` |

> 26.3 已移除标签内缩/外边距/文字阴影/模糊背景/颜色混合开关；浅色自动用深色文字；JSON 字段名保持兼容。

### 其他输入与实用功能

| 名称 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| 点击并发送<br>`clickAndSend` | 布尔值 | `false` | 可点击文本中的非斜杠命令内容作为普通聊天发送 |
| 中英文自动空格<br>`cjkLatinSpacing` | 带热键布尔值 | 无快捷键, `false` | 中文与相邻英文/数字间加显示空格；可分别控制翻译文本/告示牌/书与笔，不修改原始保存内容 |
| 翻译文本自动空格<br>`cjkLatinSpacingTranslations` | 布尔值 | `true` | 控制翻译文本的显示空格 |
| 告示牌显示自动空格<br>`cjkLatinSpacingSigns` | 布尔值 | `true` | 控制告示牌文字的显示空格 |
| 书与笔显示自动空格<br>`cjkLatinSpacingBooks` | 布尔值 | `true` | 控制书本页面的显示空格 |
| 物品栏预览地图<br>`mapInSlot` | 布尔值 | `false` | 快捷栏/背包/容器槽位预览已填写地图，保留数量和装饰层 |
| 服务器地图缓存<br>`serverIconCache` | 布尔值 | `true` | 缓存服务器图标，按名称/地址/两者匹配，可设上限；清空前二次确认 |
| 禁用提示弹窗<br>`toastKiller` | 布尔值 | `false` | 清除当前提示并在启用期间拒绝新提示 |
| 服务器ping刷新修复<br>`serverPingerFix` | 布尔值 | `false` | 扩展服务器刷新线程池并清理过期排队任务 |
| 游戏内输入法<br>`contingameIme` | 布尔值 | `false` | Windows JNI 游戏内输入法，支持组合文本/候选框/临时模式/持续模式 |

## 单人存档路径

| 名称 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| 自定义存档路径<br>`customSavesPaths` | 字符串列表 | `[]` | 自定义存档路径列表，支持绝对路径和相对游戏目录路径；"选择世界"界面左上角点当前路径即时切换，原版 `saves` 始终可选 |
| 无存档时保留世界选择界面<br>`keepWorldSelectionOnEmpty` | 布尔值 | `false` | 当前路径无世界时点"单人游戏"仍显示世界选择界面，不自动跳创建世界 |

## 实体合并渲染

| 名称 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| 禁用流体渲染<br>`disableFluidRendering` | 布尔值 | `false` | 禁用流体渲染，可能显著改变画面 |
| 禁用非主流体渲染<br>`disableNonSourceFluidRendering` | 布尔值 | `false` | 仅禁用非源头流体渲染 |
| 同类实体渲染聚合<br>`entityRenderAggregation` | 布尔值 | `false` | 同类实体渲染聚合，可能显著改变画面或兼容性 |
| 掉落物渲染合并<br>`itemRenderAggregation` | 布尔值 | `false` | 掉落物渲染合并 |
| 禁用渲染仅统计数量<br>`entityAggregationCountOnly` | 布尔值 | `false` | 聚合时仅统计数量不渲染模型 |
| 实体聚合半径<br>`entityAggregationRadius` | 实数 | — | 实体聚合的半径范围 |
| 实体聚合阈值<br>`entityAggregationThreshold` | 整数 | — | 触发聚合的实体数量阈值 |
| 实体聚合统计刷新间隔<br>`entityAggregationScanInterval` | 整数 | — | 聚合统计的刷新间隔（tick） |
| 实体聚合标签位置<br>`entityAggregationLabelPosition` | 选项列表 | — | 聚合标签显示位置 |
| 实体聚合名单模式<br>`entityAggregationListMode` | 选项列表 | — | 白名单/黑名单/关闭 |
| 实体聚合白名单<br>`entityAggregationWhitelist` | 字符串列表 | `[]` | 聚合白名单 |
| 实体聚合黑名单<br>`entityAggregationBlacklist` | 字符串列表 | `[]` | 聚合黑名单 |

> 以上功能位于"已停用功能"分类，不进入推荐页，应在理解影响后单独启用。

## 配置文件与数据路径

主配置 `config\halfmasa\halfmasa.json`

| 数据 | 路径（相对 `.minecraft`） |
|---|---|
| 主配置 | `config\halfmasa\halfmasa.json` |
| 快捷键圆盘 | `config\halfmasa\keybind-pie\bindings.json` |
| 服务器图标缓存 | `config\halfmasa\server-icons\` |
| JEI/REI 查询历史 | `config\halfmasa\search-history\` |
| Xaero 世界绑定 | `saves\<world>\config\halfmasa\xaero-world-binding.json` |
| 增强保存工具栏 | `config\halfmasa\better-saved-hotbars\hotbar.nbt` |

多人服务器相关持久化数据统一存于 `config\halfmasa\`，不写入单人存档目录。
