[中文](https://github.com/halfkite/halfmasa/blob/main/docs/features_cn.md) | [English](https://github.com/halfkite/halfmasa/blob/main/docs/features_en.md)

# halfmasa 功能与配置

1.7.0 将范围喂食、自动进食、更好的 Boss 栏、不可见方块标记、动画控制、自定义禁用实体渲染、Conflux Map 合并列表、创造材料分类、保持界面位置及 MiniHUD 预览扩展同步到全部项目目标版本。原版输入法显示选项仅适用于提供原版预编辑界面的 26.1+；更旧版本继续使用 Windows JNI 输入法。

联动 Conflux Map 0.1.11-beta.1 时，26.x 的 Fabric Loader 需满足其 0.19.0+ 要求；组合验证使用 0.19.5。HalfMasa 单独运行的最低依赖以安装包元数据为准。

> 文档版本 `1.7.0` ｜ 按 `X+H` 打开配置（也可从 Mod Menu 进入）｜ 除特别说明外默认关闭，热键空表示不绑定

配置界面顶部提示“点击 [+] 可以展开子配置项”。打开、关闭或保存无关配置不会重建区块；隐藏流体的实际显示结果改变时才进行必要的区块刷新。中英文间距切换只更新语言，无需完整重载资源包。

## 不可见方块可视化

位于客户端配置，主开关默认关闭，可绑定快捷键。三个类型开关「显示屏障方块」「显示光源方块」「显示结构空位」默认开启；另有默认关闭的「仅按住显示」及未绑定的显示键。

开启后，无需手持特殊物品或处于创造模式即可看到这些方块的原版标记。复用原版随机采样、粒子贴图、粒子设置和遮挡规则；光源使用对应等级的原版图标，不额外绘制等级数字。标记不会立即覆盖附近全部方块，已有标记在关闭功能或移除方块后按原版 80 tick 生命周期自然消失，正常速度下约四秒。

类型开关控制本功能新增的标记，不屏蔽原版创造模式手持物品时产生的标记；同一次采样已生成原版标记时不额外重复生成。此功能仅使用客户端已接收的方块数据，不需要服务端扩展。

## 自动进食

独立客户端功能，默认关闭，可绑定主开关快捷键。展开后设置开始饥饿值（默认 `14/20`）、停止目标（默认 `20/20`）、来源、食物策略、过滤名单、槽位恢复和战斗暂停。来源一行包含模式按钮、独立快捷键和重置，点击或按键循环：快捷栏 → 背包 → 快捷潜影盒。

三种来源共用名单。默认白名单为金胡萝卜、熟猪排、面包；默认黑名单为金苹果、附魔金苹果。切换过滤模式保留两个名单。智能策略优先不超出目标，其次比较营养值和饱和度；也可按从左到右或高营养优先。需要有效原版食物及正营养值；1.21–1.21.1 使用旧食物属性，其余版本同时检查 CONSUMABLE 组件。

快捷潜影盒模式依次查找快捷栏、背包、随身潜影盒、携带末影箱对应的内部库存。主要兼容 MoRanpcy QuickShulker 26.3 开箱协议，客户端和服务端需要对应 QuickShulker 与 Fabric API。缺少支持时仍使用背包；无需虚空交易服务端扩展。只搜索直接存放的食物，不递归容器。开箱可能短暂显示容器界面；满背包不发起开箱。

换槽和取物使用正常服务端容器请求，不做本地物品预测；收到同步后才进食或恢复。玩家输入始终优先，创造及旁观模式不运行。槽位冲突时等待并提示，不覆盖物品。开箱或取物超时后本次连接停止远程取物，重新进入世界可重试。已确认无食物的容器不反复打开，改变过滤规则、来源或容器物品后重新搜索。

支持食物使用后留下的碗、瓶子等余留物；堆叠食物需要额外空间容纳余留物时，空间不足不会自动食用。退出世界或死亡会取消流程并清理状态。

开启「进食后恢复槽位」时，快捷潜影盒取出的剩余食物会放回原容器的原槽位，然后恢复临时交换的物品和原快捷栏位置。只处理本次自动取出的物品；容器或槽位被手动改变时停止归还并保留物品，不覆盖或丢弃。

「静默进食」默认关闭。单人游戏可直接启用；多人服务器需要匹配游戏版本且提供此支持协议的 FGA、Carpet、Fabric API，并由管理员执行 `/carpet silentAutoEat true`，客户端无需安装 FGA。服务端核验实际物品后调用原版食物消耗流程，保留饥饿值、饱和度、食物效果和余留物，省去使用动画、声音和食物粒子。缺少支持或规则关闭时提示并回退普通进食；请求结果不明确时暂停，避免重复消耗。

## 更好的 Boss 栏

位于客户端配置，主开关默认关闭，可绑定独立快捷键。展开后包含「同类 Boss 栏合并」（默认开启）、「Boss 栏显示血量」（默认关闭）和「简洁的 Boss 栏」（默认关闭）。三个子功能均可独立绑定快捷键，默认不绑定；按键只切换对应子功能，主开关关闭时不生效。每行按开关、快捷键、重置排列，重置同时恢复开关默认值并清空绑定。合并选项可继续展开以下设置。旧版合并开关、快捷键和自定义分组引用自动迁移到主开关。

| 配置 | 默认值 | 说明 |
|---|---|---|
| 合并判断方式 | 名称 + 样式 | 可见文字及文字样式、颜色和分段样式相同才合并；忽略名称中隐藏的悬停、点击、插入信息；仅名称模式使用第一条的血条样式 |
| 血量不统一时合并 | 关闭 | 默认按服务器目标进度严格匹配，不按动画中的进度分组 |
| 合并血量计算 | 平均进度 | 支持平均、最低或最高；保留原版平滑动画 |
| 显示 Boss 数量 | 开启 | 合并名称追加 `×N`，单条不追加 |
| 最多显示栏数 | `0` | 启用功能时施加额外上限；`0` 不增加限制，仍受原版高度限制 |

例如 `80%、80%、40%` 默认显示两条：`×2`（80%）和单条（40%）；允许不同血量合并后平均约为 66.7%。每帧重新分组，原始事件不变，不影响 Boss 音乐、天空或迷雾。客户端无法从 Boss 栏直接获知实体类型，服务器自定义同名栏也可能匹配。异常进度保留独立显示，不参与合并。

血量显示优先使用可靠的实体关联或服务端数据，例如 `240/300`；合并组中全部成员都有可靠数据时显示合计，如两只凋灵 `480/600`。客户端可通过名称悬停 UUID 和一致的进度关联已加载凋灵；单人服务端或此版 FGA 可通过原版 Boss 栏 UUID 提供凋灵、末影龙血量。不能可靠关联的自定义栏显示百分比，不猜测最大血量。

简洁模式使用细血条，将名称、数量和血量放在同一行，长名称截短时保留血量显示。关闭主开关恢复原版显示。

## 掉落物消失时间

位于客户端配置，默认关闭，可绑定独立快捷键，不依赖掉落物聚合或实体高亮。点击主开关旁的折叠按钮可展开以下设置。

| 配置 | 默认值 | 说明 |
|---|---|---|
| `itemDespawnTimer` | `false` | 独立倒计时开关 |
| `itemDespawnSource` | 自动 | 自动 / Servux / 客户端估算 |
| `itemDespawnFormat` | 分:秒 | 分:秒 / 秒 / 游戏刻 |
| `itemDespawnThroughWalls` | `true` | 标签穿墙显示 |
| `itemDespawnMaxLabels` | `64` | 最近的 8–256 个标签 |
| `itemDespawnSyncInterval` | `20` tick | 服务端 Age 请求最小间隔，范围 10–200；每 tick 最多提交 4 个请求 |
| `itemDespawnDistance` | `64` 格 | 显示范围 8–128 格，仅限客户端已加载实体 |

自动模式在单人游戏的服务端线程读取 Age；多人优先复用 MiniHUD 的 Servux 请求队列和服务端数据，没有 MiniHUD 时使用独立 Servux 客户端。1.21.10 及更早版本使用 v1 原生 NBT 回复，1.21.11 和 26.x 使用 v2 数据；v2 同时兼容未压缩命名 NBT 和长度前缀 GZIP 封装。旧 MiniHUD 只接入服务端回复，避免读取客户端生成的估算 Age。网络路径需要 Fabric 网络模块；MiniHUD 同步关闭或服务端不允许查询时，自动模式显示估算，Servux 模式显示 `--:--`。

`≈` 表示首次观测或陈旧样本的估算，`∞` 仅表示有效样本确认不会自然消失。聚合标签显示成员中最短的预测剩余时间；成员数据不完整时不能显示精确结果。秒数按 20 TPS 换算，默认假设原版 6000 tick 寿命；服务器自定义规则、低 TPS 和真实物品合并可能改变剩余时间。切换世界、卸载实体或关闭功能会清理缓存。

支持项目全部目标版本：1.21–1.21.1、1.21.2–1.21.3、1.21.4、1.21.5、1.21.8、1.21.10、1.21.11、26.1.2、26.2、26.3。已有配置中的明确开关值会保留；新配置和重置配置默认关闭。

已实现 Servux 普通实体回复；10/11 分包回复当前按不支持处理并记录日志，不会当作 Age 解码。大组件物品是否可用取决于上游能否发送普通回复；无样本时遵循所选模式的降级规则。

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
| 投影保存黑白名单<br>`litematicaSaveFilter` | 布尔值 | `false` | 投影保存时按方块ID筛选，展开后可设白/黑名单 |
| 投影保存方块白名单<br>`litematicaSaveWhitelist` | 字符串列表 | `[]` | 投影保存白名单，按方块ID筛选 |
| 投影保存方块黑名单<br>`litematicaSaveBlacklist` | 字符串列表 | `[]` | 投影保存黑名单，按方块ID筛选 |
| 投影删除黑白名单<br>`litematicaDeleteFilter` | 布尔值 | `false` | 投影删除时按方块ID筛选 |
| 投影删除方块白名单<br>`litematicaDeleteWhitelist` | 字符串列表 | `[]` | 投影删除白名单 |
| 投影删除方块黑名单<br>`litematicaDeleteBlacklist` | 字符串列表 | `[]` | 投影删除黑名单 |
| 投影粘贴黑白名单<br>`litematicaPasteFilter` | 布尔值 | `false` | 投影粘贴时按方块ID筛选，详见本节配置说明。 |
| 投影粘贴方块白名单<br>`litematicaPasteWhitelist` | 字符串列表 | `[]` | 投影粘贴白名单 |
| 投影粘贴方块黑名单<br>`litematicaPasteBlacklist` | 字符串列表 | `[]` | 投影粘贴黑名单 |

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
| 空柱与离线浏览传送兜底高度<br>`confluxMapUnknownHeight` | 整数 | `128` | Conflux Map 0.1.10 及以上：空柱或离线浏览中无落点时使用；已加载地面优先，实时未知区块保留原生寻地。范围 -2048–4096；保留旧配置值 |

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
| 创造物品栏合并条目<br>`condensedCreative` | 布尔值 | `false` | 附魔书/药水/药箭/方块变体合并为可展开条目；物品栏悬停提示不显示来源 |
| 合并分类方式<br>`condensedCreativeGrouping` | 选项列表 | `default` | 现有分类 / 材料优先；材料优先将同材质的不同方块形态归组，仅合并当前页签或搜索结果中的条目 |
| 保持条目展开<br>`condensedCreativeKeepExpanded` | 布尔值 | `true` | 本次游戏运行期间按页签和分类方式记忆展开状态，重新打开物品栏保留；不写入磁盘，重启游戏清空 |
| 条目背景<br>`condensedCreativeBackground` | 布尔值 | `false` | 合并条目的半透明背景；保留已有配置选择 |
| 轮换条目预览<br>`condensedCreativeRotatingPreview` | 布尔值 | `false` | 折叠条目每秒轮换图标；默认关闭，保留已有配置选择 |
| 试炼栏<br>`trialCreativeTab` | 布尔值 | `false` | 创造栏末尾新增「试炼栏」收录42个试炼刷怪笼（14配置×3状态：普通/不祥/冷却，携带方块实体数据）+2个试炼宝库 |

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
| 保持界面位置<br>`keepScreenPosition` | 带热键布尔值 | `false` | 记忆 Mod Menu 位置；子项控制配置搜索/滚动、选中栏位和创造界面。所有目标版本自动迁移旧配置 `keepModMenuScroll`，保留原值及快捷键 |
| 保持搜索内容和位置<br>`keepConfigSearchPosition` | 布尔值 | `true` | 按分类恢复搜索文字/按键搜索/搜索栏开合/列表滚动，仅本次游戏内记忆 |
| 保持选中的栏位<br>`keepConfigSelectedTab` | 布尔值 | `true` | 重开配置恢复上次选中栏位 |
| 保持创造界面位置<br>`keepCreativeScreenPosition` | 带热键布尔值 | `false` | 主功能开启时恢复创造分类、分页、搜索及每类独立滚动位置；只在本次游戏运行期间记忆 |
| 经典暂停菜单<br>`classicPauseMenu` | 布尔值 | `false` | 经典暂停菜单：主区恢复原版按钮排列，附加按钮移右侧，小窗口分列排列 |

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

### 更好的按键设置（默认开启）

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



### 其他输入与实用功能

| 名称 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| 点击并发送<br>`clickAndSend` | 布尔值 | `false` | 可点击文本中的非斜杠命令内容作为普通聊天发送 |
| 中英文自动空格<br>`cjkLatinSpacing` | 带热键布尔值 | 无快捷键, `false` | 中文与相邻英文/数字间加显示空格；可分别控制翻译文本/告示牌/书与笔，不修改原始保存内容 |
| 翻译文本自动空格<br>`cjkLatinSpacingTranslations` | 布尔值 | `true` | 控制翻译文本的显示空格 |
| 告示牌显示自动空格<br>`cjkLatinSpacingSigns` | 布尔值 | `true` | 控制告示牌文字的显示空格 |
| 书与笔显示自动空格<br>`cjkLatinSpacingBooks` | 布尔值 | `true` | 控制书本页面的显示空格；1.21.1～1.21.5 安装 Caxton 时，编辑页由 Caxton 排版，不应用本功能的编辑页空格与光标映射 |
| 物品栏预览地图<br>`mapInSlot` | 布尔值 | `false` | 快捷栏/背包/容器槽位预览已填写地图，保留数量和装饰层 |
| 服务器地图缓存<br>`serverIconCache` | 布尔值 | `true` | 缓存服务器图标，按名称/地址/两者匹配，可设上限；清空前二次确认 |
| 禁用提示弹窗<br>`toastKiller` | 布尔值 | `false` | 清除当前提示并在启用期间拒绝新提示 |
| 服务器ping刷新修复<br>`serverPingerFix` | 布尔值 | `false` | 扩展服务器刷新线程池并清理过期排队任务 |
| 游戏内输入法<br>`contingameIme` | 布尔值 | `false` | 旧版使用 Windows JNI；26.1+ 使用原版输入法并可定制显示；关闭总开关后保持原版输入及界面，旧版同时释放 JNI 钩子 |

## 单人存档路径

| 名称 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| 自定义存档路径<br>`customSavesPaths` | 字符串列表 | `[]` | 自定义存档路径列表，支持绝对路径和相对游戏目录路径；"选择世界"界面左上角点当前路径即时切换，原版 `saves` 始终可选 |
| 无存档时保留世界选择界面<br>`keepWorldSelectionOnEmpty` | 布尔值 | `false` | 当前路径无世界时点"单人游戏"仍显示世界选择界面，不自动跳创建世界 |

## 实体与方块高亮

| 名称 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| 实体与方块高亮<br>`entityGlowHighlight` | 带热键布尔值 | `false` | 开启实体、掉落物发光轮廓及列表中的穿墙方块轮廓；仅客户端生效 |
| 实体高亮模式<br>`entityGlowMode` | 选项列表 | 全部 | 全部实体和掉落物，或只高亮详细设置中启用的规则；方块始终按规则高亮 |
| 切换实体高亮模式<br>`entityGlowModeHotkey` | 热键 | 空 | 在全部与单独模式间切换，不会开启主开关 |
| 全部高亮颜色<br>`entityGlowAllColor` | 颜色 | 白色 | 全部模式的轮廓颜色 |
| 高亮详细设置<br>`entityGlowRuleEditor` | 操作 | — | 按实体、掉落物或方块 ID 设置颜色、启停和规则快捷键；保存后生效 |

## 实体合并渲染

实体与掉落物是两个独立的可展开配置组，各自刷新、过滤并设置标签位置。合并数表示“至少达到多少个实体”；旧配置自动换算以保留原来的触发数量。

| 名称 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| 禁用流体渲染<br>`disableFluidRendering` | 布尔值 | `false` | 禁用流体渲染，可能显著改变画面 |
| 禁用非主流体渲染<br>`disableNonSourceFluidRendering` | 布尔值 | `false` | 仅禁用非源头流体渲染 |
| 同类实体渲染聚合<br>`entityRenderAggregation` | 布尔值 | `false` | 同类实体渲染聚合，可能显著改变画面或兼容性 |
| 按年龄和体型分组<br>`entityAggregationSeparateSizes` | 布尔值 | `true` | 幼年、成年及不同体型分别计数并显示标签 |
| 禁用渲染仅统计数量<br>`entityAggregationCountOnly` | 布尔值 | `false` | 生物聚合时仅显示数量，不影响掉落物 |
| 实体聚合半径<br>`entityAggregationRadius` | 实数 | `1` | 相邻生物按三维距离连锁合并的范围 |
| 实体合并数<br>`entityAggregationThreshold` | 整数 | `11` | 每组生物至少达到此数量才合并 |
| 实体聚合统计刷新间隔<br>`entityAggregationScanInterval` | 整数 | `10` | 聚合统计的刷新间隔（tick，10 tick 约半秒） |
| 实体聚合标签位置<br>`entityAggregationLabelPosition` | 选项列表 | 头顶 | 头顶或侧边 |
| 实体聚合名单模式<br>`entityAggregationListMode` | 选项列表 | — | 白名单/黑名单/关闭 |
| 实体聚合白名单<br>`entityAggregationWhitelist` | 字符串列表 | `[]` | 聚合白名单 |
| 实体聚合黑名单<br>`entityAggregationBlacklist` | 字符串列表 | `[]` | 聚合黑名单 |

| 掉落物独立配置 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| 掉落物渲染合并<br>`itemRenderAggregation` | 布尔值 | `true` | 每堆只渲染一个模型，附近同物品及组件的掉落实体合并并显示物品总件数 |
| 掉落物合并数<br>`itemAggregationThreshold` | 整数 | `2` | 至少达到此数量的掉落实体堆才合并 |
| 掉落物合并范围<br>`itemAggregationRadius` | 实数 | `1` | 相邻掉落实体按三维距离连锁合并的范围 |
| 掉落物名单模式<br>`itemAggregationListMode` | 选项列表 | 无名单 | 白名单仅处理列出物品，黑名单排除列出物品 |
| 掉落物白名单<br>`itemAggregationWhitelist` | 字符串列表 | `[]` | 物品 ID，例如 `minecraft:stone`；空白名单不处理任何物品 |
| 掉落物黑名单<br>`itemAggregationBlacklist` | 字符串列表 | `[]` | 排除的物品 ID |
| 掉落物标签位置<br>`itemAggregationLabelPosition` | 选项列表 | 头顶 | 头顶或侧边 |
| 掉落物刷新时间<br>`itemAggregationScanInterval` | 整数 | `10` | 独立刷新间隔（tick） |

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

## 显示按住键、Boss栏大小与动画控制

更好的Boss栏支持50%–200%整体缩放，普通和简洁布局共用。Boss栏及不可见方块各有一个默认关闭的“仅按住显示”子开关和独立显示键；被动显示键不参与冲突选择，松开后立即隐藏。主开关关闭时恢复原版行为。

相关布尔子配置均可绑定独立快捷键，包括不可见方块的三种类型、Boss合并高级开关和自动进食开关子项。新增“去除攻击与使用冷却动画”总开关（默认关闭），攻击挥动、交互挥动、物品冷却遮罩及物品切换下沉动画子项均默认关闭。仅调整本地视觉，保留原版攻击、挖掘、使用、冷却和第三人称动作。

## Conflux Map 路径点合并列表

启用 Conflux Map 扩展和“同时显示本地与共享路径点”后，两类路径点共用一个滚动区。默认服务端路径点在前、个人路径点在后；顶部按钮或“路径点来源顺序”子配置可切换顺序，组内沿用原有距离排序。

Conflux Map 附属功能开关默认关闭，保留用户已保存的选择。“路径点简洁紧凑模式”及独立的显示维度、显示距离选项均默认关闭，可绑定快捷键。紧凑网格默认仅显示名称和原有图标，悬停展开详细信息和可移入的原生操作面板，提供该来源可用的选择、隐藏/显示、分享、编辑、删除、定位、传送操作。关闭紧凑模式时保留原生列表风格、选择框与完整操作。条目从左到右、再换行，个人条目接在服务端条目后补位；筛选、删除和同步更新后重排并限制滚动范围。

编辑、传送和权限检查复用 Conflux Map 原生入口；操作按来源及路径点 ID 重新确认目标，已删除的条目不会把操作转向邻近条目。下拉菜单始终在列表上层并优先接收输入；关闭合并列表后恢复原生界面。桥接使用 Conflux Map 0.1.11-beta.1 界面协议，需安装对应游戏版本的 Conflux Map；已变更协议的版本会恢复原生列表。

## 范围喂食

范围喂食默认关闭，开启后可通过“执行一次范围喂食”扫描一批动物，使用当前主手食物逐个交互，完成后停止。“长按启用范围喂食”可在主开关关闭时临时持续执行，松开清空长按队列，且不会修改主开关的保存值。所有相关快捷键默认未绑定，通过 MaLiLib 按键设置按钮修改。

“禁止喂食幼体”默认开启，支持独立快捷键；依据 `Animal.isBaby()` 判断，在无名单、黑名单及白名单模式下均生效，不影响玩家手动右键。每次实际交互前重新检查保护开关、名单、食物、实体存活、视线和距离，切换设置会立即影响下一次交互。

范围默认 3 格，按眼睛到动物碰撞箱的距离计算，同时受实际实体交互距离限制。名单匹配已注册实体类型 ID，支持省略 `minecraft:`；空白名单不喂食，非法 ID 不会误匹配。喂食间隔可设为 1–200 Tick，默认 4 Tick（正常 20 TPS 时约每秒 5 次），数值越小越快。单次、长按、重扫和同一幼体的重复喂食共用该间隔，运行中修改立即生效。切换幼体保护会刷新正在执行的目标队列；单次批次不会重复喂已处理的目标。打开界面、失焦、死亡、退出世界或切换维度取消任务，恢复后须重新按键。正在手动使用物品、攻击或由其他功能持续使用物品时暂停范围交互。

## 自定义禁用实体渲染

禁用分类新增默认关闭的“自定义禁用实体渲染”，开关与快捷键同排，支持折叠。模式按黑名单、白名单、无名单循环，可点击或独立快捷键切换。名单语义为：无名单禁用全部非保护实体；白名单仅禁用名单内实体，空白名单不禁用任何实体；黑名单禁用名单外实体、保留名单内实体，默认空黑名单会禁用全部非保护实体。悬停“名单模式”名称或按钮可查看完整说明。名单按已注册实体类型 ID 匹配，支持省略 minecraft:；不匹配命名牌，非法或不存在的类型不会误匹配默认实体。名单变化或配置重载时更新缓存。

“长按显示实体”默认关闭，必须先开启此开关，临时显示键才生效。临时显示键默认未绑定且不参与冲突选择。按住仅解除本功能的隐藏，松开恢复；打开界面、失去窗口焦点或退出世界时停止临时显示。本地玩家和摄像机实体始终受保护。复用现有世界渲染过滤入口，聚合、高亮和其他模组各自规则仍生效，碰撞、交互与服务端模拟保持原状。


## MiniHUD 容器预览优化

MiniHUD 容器预览优化位于“其他模组扩展”。默认关闭，提供默认未绑定的切换快捷键；已有保存的开关值会保留，重置后恢复关闭及未绑定。复用 MiniHUD 原有预览入口，缓存容器、潜影盒、收纳袋与末影箱的数据准备。长按世界容器预览参考 Jade 的更新与绘制分离方式：每个客户端 Tick 检查内容，同一 Tick 内复用准备结果；新数据、数据对象替换、方块状态或目标变化可提前刷新。相同数据的不同槽位数请求共用内容检查，预览屏幕与独立布局持续复用。每帧仍进行原有目标检测、数据请求和动态绘制，保留完整槽位、数量、颜色及原有操作。普通网格使用独立的显示物品快照，按 Tick 与数据更新校验，数量变化可立即刷新，减少每帧复制；特殊布局和悬停提示继续使用原有保护。修改合成器禁用槽可立即更新。退出世界、资源重载或关闭功能时清理缓存，关闭后恢复原始路径，未安装 MiniHUD 时不应用相关 Mixin。地图和附魔等动态效果继续逐帧绘制，不缓存跨帧绘制状态。此实现不需要安装 Jade。


## MiniHUD 独立预览

MiniHUD 独立预览默认关闭，位于“其他模组扩展”，切换快捷键默认未绑定。开启后，即使 MiniHUD 主渲染开关关闭，仍可按 MiniHUD 原有预览键查看世界容器；继续遵守其容器预览开关、背景颜色和目标检测。主开关开启时由原生路径处理，避免重复绘制，关闭时仅补上世界容器预览，信息行保持关闭。物品、收纳袋、末影箱和地图悬停预览沿用原有设置。此开关与容器预览优化分别控制，未安装 MiniHUD 时不生效。
