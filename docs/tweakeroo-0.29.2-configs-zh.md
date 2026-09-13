# Tweakeroo 0.29.2 配置清单（中文）

这份清单按当前 Tweakeroo 原生配置分类列出全部配置项，并标记 halfmasa 当前的折叠归组结果。

## 来源与规则

- 适用环境：Minecraft 26.2、Fabric、Tweakeroo 0.29.2。
- Tweakeroo 中文名称来源：`D:\我的世界\.minecraft\versions\26.2-Fabric_0.19.3\mods\tweakeroo-fabric-26.2-0.29.2.jar` 内的 `assets/tweakeroo/lang/zh_cn.json`。
- 原生分类顺序：功能开关、通用、列表、快捷键、修复、禁用。
- “已归组”表示当前 halfmasa 的 `TweakerooConfigExpansionProvider` 显式归入某个折叠组；“未分类”表示当前没有命中任何折叠组。
- `Fixes` 和 `Disable` 保持独立，不参与 halfmasa 折叠归组；因此会在未分类部分按原生分类列出。
- 同一配置名若在 Tweakeroo 语言文件中同时属于多个原生分类，会按各自分类分别保留，避免丢失分类信息。
- `tweakSneak_1.15.2` 是配置实际名称；Tweakeroo 语言文件中的点号被写成下划线，清单已还原。

## 统计

- 全部配置记录：**365** 项
- 已归入 halfmasa 折叠组：**297** 项
- 未分类或保持独立：**68** 项
- 当前折叠组：**29** 组

## 当前 halfmasa 折叠组

下面按 halfmasa 当前折叠组顺序列出已归组配置。每项仍保留其 Tweakeroo 原生分类，方便回到原界面核对。


### 精准放置协议 - 开关 `accuratePlacementProtocol`

| 原生分类 | 配置名 | 中文名称 |
|---|---|---|
| 通用 | `accuratePlacementProtocolMode` | 精准放置协议 - 模式 |

### 精准方块放置 `tweakAccurateBlockPlacement` 开关

| 原生分类 | 配置名 | 中文名称 |
|---|---|---|
| 快捷键 | `accurateBlockPlacementInto` | 精准方块放置 - 内部朝向 |
| 快捷键 | `accurateBlockPlacementReverse` | 精准方块放置 - 反向朝向 |
| 快捷键 | `toggleAccuratePlacementProtocol` | 开关 - 精准放置协议 |
| 通用 | `clientPlacementRotation` | 客户端放置旋转 |
| 通用 | `clientPlacementValidation` | 客户端方块放置验证 |

### 放置相关 `placement`

| 原生分类 | 配置名 | 中文名称 |
|---|---|---|
| 通用 | `angelBlockPlacementDistance` | 浮空放置方块距离 |
| 通用 | `placementGridSize` | 放置网格尺寸 |
| 通用 | `placementLimit` | 放置数量限制 |
| 通用 | `placementRestrictionMode` | 放置限制模式 |
| 通用 | `placementRestrictionTiedToFast` | 放置限制与快速模式联动 |
| 快捷键 | `placementRestrictionModeColumn` | 放置限制模式 - 列(法线方向) |
| 快捷键 | `placementRestrictionModeDiagonal` | 放置限制模式 - 对角线(叉号方向) |
| 快捷键 | `placementRestrictionModeFace` | 放置限制模式 - 点击面(固定方向表面) |
| 快捷键 | `placementRestrictionModeLayer` | 放置限制模式 - 层状(固定y值) |
| 快捷键 | `placementRestrictionModeLine` | 放置限制模式 - 十字 |
| 快捷键 | `placementRestrictionModePlane` | 放置限制模式 - 平面 |
| 快捷键 | `placementYMirror` | Y轴镜像放置(台阶、楼梯等) |
| 通用 | `scaffoldPlaceDistance` | 脚手架放置距离 |
| 通用 | `scaffoldPlaceVanilla` | 脚手架放置沿玩家方向延伸 |
| 功能开关 | `tweakAngelBlock` | 浮空放置方块 |
| 功能开关 | `tweakBreakReplace` | 破坏后原位放置 |
| 功能开关 | `tweakPlacementGrid` | 放置限制 - 网格化 |
| 功能开关 | `tweakPlacementLimit` | 放置限制 - 单次数量 |
| 功能开关 | `tweakPlacementRestriction` | 放置限制 - 范围 |
| 功能开关 | `tweakPlacementRestrictionFirst` | 放置限制 - 首次 |
| 功能开关 | `tweakPlacementRestrictionHand` | 放置限制 - 手持物品 |
| 功能开关 | `tweakScaffoldPlace` | 像脚手架一样放方块 |
| 功能开关 | `tweakYMirror` | Y轴镜像放置(台阶、楼梯等) |

### 区域选择 `area_selection`

| 原生分类 | 配置名 | 中文名称 |
|---|---|---|
| 通用 | `areaSelectionUseAll` | 选区包含空气设置 |
| 快捷键 | `areaSelectionOffset` | 选区偏移调整 |
| 快捷键 | `areaSelectionAddToList` | 添加选区到列表 |
| 快捷键 | `areaSelectionRemoveFromList` | 从列表移除选区 |
| 功能开关 | `tweakAreaSelector` | 区域选择器 |

### 方块破坏限制 `breaking_restriction` 开关

| 原生分类 | 配置名 | 中文名称 |
|---|---|---|
| 通用 | `blockTypeBreakRestrictionWarn` | 方块破坏限制 - 警告提示位置 |
| 列表 | `blockTypeBreakRestrictionListType` | 方块破坏限制 - 列表类型 |
| 列表 | `blockTypeBreakRestrictionBlackList` | 方块破坏限制 - 黑名单 |
| 列表 | `blockTypeBreakRestrictionWhiteList` | 方块破坏限制 - 白名单 |
| 通用 | `breakingGridSize` | 破坏限制 - 网格尺寸 |
| 通用 | `breakingRestrictionMode` | 破坏限制 - 模式 |
| 快捷键 | `breakingRestrictionModeColumn` | 破坏限制模式 - 列 |
| 快捷键 | `breakingRestrictionModeDiagonal` | 破坏限制模式 - 对角线(叉号) |
| 快捷键 | `breakingRestrictionModeFace` | 破坏限制模式 - 点击面 |
| 快捷键 | `breakingRestrictionModeLayer` | 破坏限制模式 - 层(固定y值) |
| 快捷键 | `breakingRestrictionModeLine` | 破坏限制模式 - 十字 |
| 快捷键 | `breakingRestrictionModePlane` | 破坏限制模式 - 平面 |
| 功能开关 | `tweakBlockTypeBreakRestriction` | 破坏限制 - 方块类型 |
| 功能开关 | `tweakBreakingGrid` | 破坏限制 - 网格化 |
| 功能开关 | `tweakBreakingRestriction` | 破坏限制 - 范围 |

### 快速放置与连点 `fast_placement`

| 原生分类 | 配置名 | 中文名称 |
|---|---|---|
| 通用 | `fastBlockPlacementCount` | 快速方块放置数量 |
| 通用 | `fastLeftClickAllowTools` | 快速左键允许工具 |
| 通用 | `fastPlacementRememberOrientation` | 快速放置记忆朝向 |
| 列表 | `fastPlacementItemListType` | 快速放置 - 物品列表类型 |
| 列表 | `fastPlacementItemBlackList` | 快速放置 - 物品黑名单 |
| 列表 | `fastPlacementItemWhiteList` | 快速放置 - 物品白名单 |
| 功能开关 | `tweakFastBlockPlacement` | 快速方块放置 |

### 快速左键 `tweakFastLeftClick` 开关

| 原生分类 | 配置名 | 中文名称 |
|---|---|---|
| 通用 | `fastLeftClickCount` | 快速左键点击次数 |
| 通用 | `fastLeftClickAllowTools` | 快速左键允许工具 |

### 快速右键 `tweakFastRightClick` 功能开关

| 原生分类 | 配置名 | 中文名称 |
|---|---|---|
| 通用 | `fastRightClickCount` | 快速右键点击次数 |
| 列表 | `fastRightClickBlockListType` | 快速右键 - 方块列表类型 |
| 列表 | `fastRightClickBlockBlackList` | 快速右键 - 方块黑名单 |
| 列表 | `fastRightClickBlockWhiteList` | 快速右键 - 方块白名单 |
| 列表 | `fastRightClickListType` | 快速右键 - 物品列表类型 |
| 列表 | `fastRightClickBlackList` | 快速右键 - 物品黑名单 |
| 列表 | `fastRightClickWhiteList` | 快速右键 - 物品白名单 |

### 灵活方块放置 `tweakFlexibleBlockPlacement`功能开关

| 原生分类 | 配置名 | 中文名称 |
|---|---|---|
| 通用 | `flexibleBlockPlacementOverlayColor` | 灵活放置高亮颜色 |
| 通用 | `rememberFlexibleFromClick` | 记忆灵活放置初始点击 |
| 快捷键 | `flexibleBlockPlacementAdjacent` | 灵活方块放置 - 相邻位置 |
| 快捷键 | `flexibleBlockPlacementOffset` | 灵活方块放置 - 偏移位置 |
| 快捷键 | `flexibleBlockPlacementRotation` | 灵活方块放置 - 旋转朝向 |

### 飞行速度调节 `tweakFlySpeed` 功能开关

| 原生分类 | 配置名 | 中文名称 |
|---|---|---|
| 功能开关 | `tweakCustomFlyDeceleration` | 自定义飞行减速 |
| 通用 | `flyDecelerationFactor` | 飞行减速系数 |
| 通用 | `flySpeedPreset1` | 飞行速度 - 预设1 |
| 通用 | `flySpeedPreset2` | 飞行速度 - 预设2 |
| 通用 | `flySpeedPreset3` | 飞行速度 - 预设3 |
| 通用 | `flySpeedPreset4` | 飞行速度 - 预设4 |
| 通用 | `flySpeedIncrement1` | 飞行速度 - 增量1 |
| 通用 | `flySpeedIncrement2` | 飞行速度 - 增量2 |
| 快捷键 | `flyPreset1` | 飞行预设1 |
| 快捷键 | `flyPreset2` | 飞行预设2 |
| 快捷键 | `flyPreset3` | 飞行预设3 |
| 快捷键 | `flyPreset4` | 飞行预设4 |
| 快捷键 | `flyIncrement1` | 飞行速度 - 增量1 |
| 快捷键 | `flyIncrement2` | 飞行速度 - 增量2 |

### 灵魂出窍 `tweakFreeCamera`功能开关

| 原生分类 | 配置名 | 中文名称 |
|---|---|---|
| 通用 | `freeCameraPlayerInputs` | 灵魂出窍 - 玩家左右键 |
| 快捷键 | `freeCameraPlayerInputs` | 灵魂出窍 - 玩家左右键 |
| 通用 | `freeCameraPlayerMovement` | 灵魂出窍 - 本体移动 |
| 快捷键 | `freeCameraPlayerMovement` | 灵魂出窍 - 本体移动 |
| 通用 | `freeCameraShowHands` | 灵魂出窍 - 显示双手 |
| 通用 | `freeCameraShowHotBar` | 灵魂出窍 - 显示快捷栏 |
| 通用 | `freeCameraShowStatusBars` | 灵魂出窍 - 显示状态栏 |
| 快捷键 | `freeCameraPresetAdd` | 灵魂出窍 - 预设添加 |
| 快捷键 | `freeCameraPresetCycle` | 灵魂出窍 - 预设循环 |
| 快捷键 | `freeCameraPresetDelete` | 灵魂出窍 - 预设删除 |
| 快捷键 | `freeCameraPresetDeleteAll` | 灵魂出窍 - 删除全部预设 |

### 鞘翅与火箭 `elytra_camera`

| 原生分类 | 配置名 | 中文名称 |
|---|---|---|
| 通用 | `elytraCameraIndicator` | 鞘翅视角指示器 |
| 快捷键 | `elytraCamera` | 鞘翅飞行视角 |
| 快捷键 | `swapElytraChestplate` | 切换鞘翅与胸甲 |
| 通用 | `rocketSwapAllowExplosions` | 自动切换烟花火箭允许使用爆炸烟花 |
| 功能开关 | `tweakAutoSwitchElytra` | 自动切换鞘翅 |
| 功能开关 | `tweakAutoSwitchRockets` | 自动切换烟花火箭 |
| 功能开关 | `tweakElytraCamera` | 鞘翅视角分离 |

### 视野与环境 `gamma_darkness`

| 原生分类 | 配置名 | 中文名称 |
|---|---|---|
| 通用 | `gammaOverrideValue` | 伽马覆盖值 |
| 通用 | `darknessScaleOverrideValue` | 黑暗效果减弱比例 |
| 功能开关 | `tweakDarknessVisibility` | 黑暗效果能见度调整 |
| 功能开关 | `tweakGammaOverride` | 伽马值修改 |
| 功能开关 | `tweakLavaVisibility` | 熔岩视野优化 |
| 功能开关 | `tweakMatchingSkyFog` | 雾气匹配天空 |
| 功能开关 | `tweakWaterVisibility` | 水中能见度提高 |

### 快捷栏 `hotbar`

| 原生分类 | 配置名 | 中文名称 |
|---|---|---|
| 通用 | `hotbarSlotCycleMax` | 快捷栏循环上限 |
| 通用 | `hotbarSlotRandomizerMax` | 快捷栏随机上限 |
| 通用 | `hotbarSwapOverlayAlignment` | 快捷栏切换界面对齐 |
| 通用 | `hotbarSwapOverlayOffsetX` | 快捷栏切换界面X偏移 |
| 通用 | `hotbarSwapOverlayOffsetY` | 快捷栏切换界面Y偏移 |
| 快捷键 | `hotbarScroll` | 快捷栏滚动 |
| 快捷键 | `hotbarSwapBase` | 快捷栏交换 - 基础按键 |
| 快捷键 | `hotbarSwap1` | 快捷栏交换 - 第一行 |
| 快捷键 | `hotbarSwap2` | 快捷栏交换 - 第二行 |
| 快捷键 | `hotbarSwap3` | 快捷栏交换 - 第三行 |
| 功能开关 | `tweakHotbarScroll` | 快捷栏滚动 |
| 功能开关 | `tweakHotbarSlotCycle` | 快捷栏循环切换 |
| 功能开关 | `tweakHotbarSlotRandomizer` | 快捷栏随机切换 |
| 功能开关 | `tweakHotbarSwap` | 快捷栏交换 |

### 容器与地图预览 `inventory_preview`

| 原生分类 | 配置名 | 中文名称 |
|---|---|---|
| 通用 | `customInventoryGuiScale` | 自定义背包界面缩放 |
| 通用 | `inventoryPreviewVillagerBGColor` | 容器预览 - 村民交易背景色 |
| 通用 | `bundleDisplayBgColor` | 收纳袋预览 - 背景颜色 |
| 通用 | `bundleDisplayRequireShift` | 收纳袋预览 - 需按住Shift |
| 通用 | `bundleDisplayRowWidth` | 收纳袋预览 - 行宽 |
| 通用 | `mapPreviewRequireShift` | 地图预览需按住Shift |
| 通用 | `mapPreviewSize` | 地图预览尺寸 |
| 通用 | `shulkerDisplayBgColor` | 潜影盒 - 背景颜色 |
| 通用 | `shulkerDisplayEnderChest` | 潜影盒 - 末影箱显示 |
| 通用 | `shulkerDisplayRequireShift` | 潜影盒 - 预览需按住Shift |
| 快捷键 | `inventoryPreview` | 容器预览 - 快捷键 |
| 快捷键 | `inventoryPreviewToggleScreen` | 容器预览 - 可交互界面 |
| 快捷键 | `playerInventoryPeek` | 玩家背包预览 |
| 功能开关 | `tweakBundleDisplay` | 收纳袋内容显示 |
| 功能开关 | `tweakInventoryPreview` | 容器预览 - 开关 |
| 功能开关 | `tweakMapPreview` | 地图预览 |
| 功能开关 | `tweakPlayerInventoryPeek` | 玩家背包预览 |
| 功能开关 | `tweakShulkerBoxDisplay` | 潜影盒内容显示 |

### 周期动作 `periodic_actions`

| 原生分类 | 配置名 | 中文名称 |
|---|---|---|
| 通用 | `periodicAttackInterval` | 周期性攻击 - 间隔 |
| 通用 | `periodicAttackResetIntervalOnActivate` | 周期性攻击 - 激活时重置间隔 |
| 通用 | `periodicUseInterval` | 周期性使用 - 间隔 |
| 通用 | `periodicUseResetIntervalOnActivate` | 周期性使用 - 激活时重置间隔 |
| 通用 | `periodicHoldAttackDuration` | 周期性长按攻击 - 时长 |
| 通用 | `periodicHoldAttackInterval` | 周期性长按攻击 - 间隔 |
| 通用 | `periodicHoldAttackResetIntervalOnActivate` | 周期性长按攻击 - 激活时重置间隔 |
| 通用 | `periodicHoldUseDuration` | 周期性长按使用 - 时长 |
| 通用 | `periodicHoldUseInterval` | 周期性长按使用 - 间隔 |
| 通用 | `periodicHoldUseResetIntervalOnActivate` | 周期性长按使用 - 激活时重置间隔 |
| 功能开关 | `tweakHoldAttack` | 持续攻击 |
| 功能开关 | `tweakHoldUse` | 持续使用 |
| 功能开关 | `tweakPeriodicAttack` | 周期性攻击 |
| 功能开关 | `tweakPeriodicHoldAttack` | 周期性长按攻击 |
| 功能开关 | `tweakPeriodicHoldUse` | 周期性长按使用 |
| 功能开关 | `tweakPeriodicUse` | 周期性使用 |

### 药水效果预警 `potion_warning`

| 原生分类 | 配置名 | 中文名称 |
|---|---|---|
| 通用 | `potionWarningBeneficialOnly` | 仅警告有益药水 |
| 通用 | `potionWarningThreshold` | 药水警告阈值 |
| 列表 | `potionWarningListType` | 药水警告 - 列表类型 |
| 列表 | `potionWarningBlackList` | 药水警告 - 黑名单 |
| 列表 | `potionWarningWhiteList` | 药水警告 - 白名单 |
| 功能开关 | `tweakPotionWarning` | 药水效果预警 |

### 渲染与显示 `rendering`

| 原生分类 | 配置名 | 中文名称 |
|---|---|---|
| 通用 | `renderLimitItem` | 物品渲染数量上限 |
| 通用 | `renderLimitXPOrb` | 经验球渲染数量上限 |
| 快捷键 | `skipAllRendering` | 跳过全部渲染 |
| 快捷键 | `skipWorldRendering` | 跳过世界渲染 |
| 功能开关 | `tweakExplosionReducedParticles` | 爆炸粒子简化 |
| 功能开关 | `tweakF3Cursor` | F3光标显示 |
| 功能开关 | `tweakPlayerListAlwaysVisible` | 常显玩家列表 |
| 功能开关 | `tweakRenderEdgeChunks` | 边缘区块渲染 |
| 功能开关 | `tweakRenderInvisibleEntities` | 渲染隐形实体 |
| 功能开关 | `tweakRenderLimitEntities` | 实体渲染数量限制 |
| 快捷键 | `writeMapsAsImages` | 导出地图为图片 |

### 选择性方块 `selective_blocks`

| 原生分类 | 配置名 | 中文名称 |
|---|---|---|
| 通用 | `selectiveBlocksTrackPistons` | 追踪活塞运动 |
| 通用 | `selectiveBlocksHideParticles` | 隐藏粒子设置 |
| 通用 | `selectiveBlocksHideEntities` | 隐藏实体设置 |
| 通用 | `selectiveBlocksNoHit` | 禁用隐藏方块选中 |
| 列表 | `selectiveBlocksListType` | 选择性方块 - 列表类型 |
| 列表 | `selectiveBlocksWhitelist` | 选择性方块 - 白名单 |
| 列表 | `selectiveBlocksBlacklist` | 选择性方块 - 黑名单 |
| 功能开关 | `tweakSelectiveBlocksRendering` | 选择性方块渲染 |
| 功能开关 | `tweakSelectiveBlocksRenderOutline` | 选择性方块轮廓渲染 |

### 对齐视线 `snap_aim`

| 原生分类 | 配置名 | 中文名称 |
|---|---|---|
| 通用 | `snapAimIndicator` | 对齐视线 - 指示器 |
| 通用 | `snapAimIndicatorColor` | 对齐视线 - 指示器颜色 |
| 通用 | `snapAimMode` | 对齐视线 - 模式 |
| 通用 | `snapAimOnlyCloseToAngle` | 对齐视线 - 仅邻近角度 |
| 通用 | `snapAimPitchOvershoot` | 对齐视线 - 俯仰过冲 |
| 通用 | `snapAimPitchStep` | 对齐视线 - 俯仰步长 |
| 通用 | `snapAimThresholdPitch` | 对齐视线 - 俯仰(上下)阈值 |
| 通用 | `snapAimThresholdYaw` | 对齐视线 - 偏航(左右)阈值 |
| 通用 | `snapAimYawStep` | 对齐视线 - 偏航步长 |
| 功能开关 | `tweakAimLock` | 视线锁定 |
| 功能开关 | `tweakSnapAim` | 对齐视线 |
| 功能开关 | `tweakSnapAimLock` | 对齐视线并锁定 |

### 视角缩放 `zoom`

| 原生分类 | 配置名 | 中文名称 |
|---|---|---|
| 通用 | `zoomAdjustMouseSensitivity` | 缩放调整鼠标灵敏度 |
| 通用 | `zoomFov` | 缩放视野 |
| 通用 | `zoomFovDifference` | 缩放视野差异 |
| 通用 | `zoomFovDifferenceCtrl` | 缩放视野差异(Ctrl) |
| 通用 | `zoomResetFovOnActivate` | 每次缩放时重置视野值(FOV) |
| 快捷键 | `zoomActivate` | 缩放激活 |
| 功能开关 | `tweakSpyglassUsesTweakZoom` | 望远镜改用tweakeroo的视角缩放 |
| 功能开关 | `tweakZoom` | 视角缩放 |

### 工具与武器切换 `tool_swap`

| 原生分类 | 配置名 | 中文名称 |
|---|---|---|
| 通用 | `toolSwitchableSlots` | 工具切换 - 可用槽位 |
| 通用 | `toolSwitchIgnoredSlots` | 工具切换 - 忽略槽位 |
| 快捷键 | `toolPick` | 工具切换 |
| 通用 | `itemSwapDurabilityThreshold` | 物品交换耐久阈值 |
| 快捷键 | `swapSpyglassAndActivate` | 自动切换并使用望远镜 |
| 通用 | `toolSwapAllowUnenchantedToBreak` | 工具切换 - 允许未附魔工具损坏 |
| 通用 | `toolSwapBetterEnchants` | 工具切换 - 自适应附魔 |
| 通用 | `toolSwapPreferFortuneOverride` | 工具切换 - 偏好时运覆盖 |
| 通用 | `toolSwapPreferSilkTouch` | 工具切换 - 偏好精准采集 |
| 通用 | `toolSwapBambooUsesSwordFirst` | 工具切换 - 竹子优先使用剑 |
| 通用 | `toolSwapLeavesUsesHoeFirst` | 工具切换 - 树叶优先使用锄头 |
| 通用 | `toolSwapNeedsShearsFirst` | 工具切换 - 优先使用剪刀 |
| 通用 | `toolSwapNeedsPickaxeFirst` | 工具切换 - 优先使用镐子 |
| 通用 | `toolSwapSilkTouchFirst` | 工具切换 - 优先精准采集 |
| 通用 | `toolSwapSilkTouchOres` | 工具切换 - 矿石精准采集 |
| 通用 | `toolSwapSilkTouchOverride` | 工具切换 - 精准采集覆盖 |
| 通用 | `toolSwapPickaxeOverride` | 工具切换 - 镐子覆盖 |
| 通用 | `weaponSwapBetterEnchants` | 武器切换 - 自适应附魔 |
| 功能开关 | `tweakPickBeforePlace` | 放置前自动选材 |
| 功能开关 | `tweakSwapAlmostBrokenTools` | 自动切换濒损工具 |
| 功能开关 | `tweakToolSwitch` | 自动切换工具 |
| 功能开关 | `tweakWeaponSwitch` | 自动切换武器 |

### 物品管理 `item_management`

| 原生分类 | 配置名 | 中文名称 |
|---|---|---|
| 通用 | `handRestockPre` | 自动补货 - 预先补货 |
| 通用 | `handRestockPreThreshold` | 自动补货 - 预先补货阈值 |
| 列表 | `handRestockListType` | 自动补货 - 列表类型 |
| 列表 | `handRestockBlackList` | 自动补货 - 黑名单 |
| 列表 | `handRestockWhiteList` | 自动补货 - 白名单 |
| 列表 | `repairModeSlots` | 修复模式槽位 |
| 列表 | `unstackingItems` | 溢出背包保护物品 |
| 功能开关 | `tweakEmptyShulkerBoxesStack` | 空潜影盒堆叠 |
| 功能开关 | `tweakHandRestock` | 自动补货 |
| 功能开关 | `tweakItemUnstackingProtection` | 物品溢出背包保护 |
| 功能开关 | `tweakRepairMode` | 自动修复模式 |

### 聊天与文本 `chat`

| 原生分类 | 配置名 | 中文名称 |
|---|---|---|
| 通用 | `chatBackgroundColor` | 聊天背景色 |
| 通用 | `chatTimeFormat` | 聊天时间格式 |
| 快捷键 | `copySignText` | 复制告示牌文本 |
| 功能开关 | `tweakChatBackgroundColor` | 聊天背景色修改 |
| 功能开关 | `tweakChatPersistentText` | 聊天文本持久化 |
| 功能开关 | `tweakChatTimestamp` | 聊天时间戳 |
| 功能开关 | `tweakCommandBlockExtraFields` | 命令方块扩展字段 |
| 功能开关 | `tweakPrintDeathCoordinates` | 死亡坐标打印 |
| 功能开关 | `tweakSignCopy` | 告示牌文本复制 |
| 功能开关 | `tweakTabCompleteCoordinate` | 坐标补全优化 |

### 服务器数据同步 `server_sync`

| 原生分类 | 配置名 | 中文名称 |
|---|---|---|
| 通用 | `entityDataSync` | 实体数据同步 |
| 通用 | `entityDataSyncBackup` | 实体数据同步 - 备用方案 |
| 通用 | `entityDataSyncCacheRefresh` | 实体数据同步 - 缓存刷新 |
| 通用 | `entityDataSyncCacheTimeout` | 实体数据同步 - 缓存超时 |
| 通用 | `serverDataSyncCacheRefresh` | 服务器数据同步 - 缓存刷新间隔 |
| 通用 | `serverDataSyncCacheTimeout` | 服务器数据同步 - 缓存超时 |
| 通用 | `serverNbtRequestRate` | 服务器NBT请求频率 |
| 通用 | `slotSyncWorkaround` | 槽位同步修复 |
| 通用 | `slotSyncWorkaroundAlways` | 始终启用槽位同步修复 |
| 功能开关 | `tweakServerDataSync` | 服务器数据同步 |
| 功能开关 | `tweakServerDataSyncBackup` | 服务器数据同步 - 备用方案 |

### 实体与触及距离 `entity_interaction`

| 原生分类 | 配置名 | 中文名称 |
|---|---|---|
| 通用 | `blockReachDistance` | 手长 - 方块触及距离 |
| 通用 | `entityReachDistance` | 手长 - 实体触及距离 |
| 通用 | `entityTypeAttackRestrictionWarn` | 实体攻击限制 - 警告提示位置 |
| 列表 | `entityTypeAttackRestrictionListType` | 实体攻击限制 - 列表类型 |
| 列表 | `entityTypeAttackRestrictionBlackList` | 实体攻击限制 - 黑名单 |
| 列表 | `entityTypeAttackRestrictionWhiteList` | 实体攻击限制 - 白名单 |
| 列表 | `entityWeaponMapping` | 实体武器映射 |
| 通用 | `hangableEntityBypassInverse` | 悬挂实体绕过反转 |
| 功能开关 | `tweakBlockReachOverride` | 手长 - 方块触及距离修改 |
| 功能开关 | `tweakEntityReachOverride` | 手长 - 实体触及距离修改 |
| 功能开关 | `tweakEntityTypeAttackRestriction` | 实体类型攻击限制 |
| 功能开关 | `tweakHangableEntityBypass` | 悬挂实体绕过 |

### 结构与指令 `structure`

| 原生分类 | 配置名 | 中文名称 |
|---|---|---|
| 通用 | `fillCloneLimit` | fill clone 指令上限 |
| 通用 | `structureBlockMaxSize` | 结构方块最大尺寸 |
| 功能开关 | `tweakFillCloneLimit` | fill clone 指令限制 |
| 功能开关 | `tweakStructureBlockLimit` | 结构方块限制 |

### 创造与世界预设 `creative_world`

| 原生分类 | 配置名 | 中文名称 |
|---|---|---|
| 列表 | `creativeExtraItems` | 创造模式额外物品 |
| 列表 | `flatWorldPresets` | 超平坦世界预设 |
| 功能开关 | `tweakCreativeExtraItems` | 创造模式扩展物品 |
| 功能开关 | `tweakCustomFlatPresets` | 自定义超平坦预设 |

### 幽匿感测体 `sculk`

| 原生分类 | 配置名 | 中文名称 |
|---|---|---|
| 通用 | `sculkSensorPulseLength` | 幽匿感测体脉冲时长 |
| 功能开关 | `tweakSculkPulseLength` | 幽匿脉冲时长 |

### 后置点击器 `after_clicker`

| 原生分类 | 配置名 | 中文名称 |
|---|---|---|
| 通用 | `afterClickerClickCount` | 后置点击器点击次数 |
| 功能开关 | `tweakAfterClicker` | 后置点击器 |

### 移动状态 `movement_states`

| 原生分类 | 配置名 | 中文名称 |
|---|---|---|
| 通用 | `permanentSneakAllowInGUIs` | 永久潜行在打开GUI界面时依然有效 |
| 功能开关 | `tweakFakeSneaking` | 伪潜行 |
| 功能开关 | `tweakFakeSneakPlacement` | 伪潜行放置 |
| 功能开关 | `tweakMovementKeysLast` | 末次移动按键优先 |
| 功能开关 | `tweakPermanentSneak` | 永久潜行 |
| 功能开关 | `tweakPermanentSprint` | 永久疾跑 |
| 功能开关 | `tweakSneak_1.15.2` | 1.15.2潜行机制 |
| 功能开关 | `tweakSpectatorTeleport` | 旁观者传送 |

### 宠物动作 `pets`

| 原生分类 | 配置名 | 中文名称 |
|---|---|---|
| 快捷键 | `sitDownNearbyPets` | 附近宠物坐下 |
| 快捷键 | `standUpNearbyPets` | 附近宠物站立 |

## 未分类配置

以下配置没有命中当前 halfmasa 折叠组。它们仍按 Tweakeroo 原生分类完整保留；其中“修复”和“禁用”是有意保持独立的分类。

### 功能开关 `feature_toggle`

| 原生分类 | 配置名 | 中文名称 |
|---|---|---|
| 功能开关 | `tweakCustomInventoryScreenScale` | 自定义背包界面缩放 |

### 通用 `generic`

| 原生分类 | 配置名 | 中文名称 |
|---|---|---|
| 通用 | `debugLogging` | 调试日志记录 |
| 通用 | `itemUsePacketCheckBypass` | 物品使用数据包检查绕过 |
| 通用 | `translationLanguage` | 翻译语言 |
| 通用 | `translationMode` | 翻译模式 |
| 通用 | `translationTryBaseLanguage` | 翻译语言跟随MaLiLib |
| 通用 | `utilityHandSlot` | 惯用手 |

### 列表 `lists`

| 原生分类 | 配置名 | 中文名称 |
|---|---|---|
| 列表 | `pickaxeOverride` | 镐子覆盖 |
| 列表 | `silkTouchOverride` | 工具切换 - 优先精准采集的方块列表 |

### 快捷键 `hotkey`

| 原生分类 | 配置名 | 中文名称 |
|---|---|---|
| 快捷键 | `openCameraPresetEditorGui` | 打开灵魂出窍预设编辑器GUI |
| 快捷键 | `openConfigGui` | 打开配置界面 |
| 快捷键 | `toggleGrabCursor` | 开关 - 光标锁定 |

### 修复 `fixes`

| 原生分类 | 配置名 | 中文名称 |
|---|---|---|
| 修复 | `elytraFix` | 鞘翅着陆修复 |
| 修复 | `elytraSprintCancel` | 鞘翅疾跑取消修复 |
| 修复 | `macHorizontalScroll` | Mac水平滚动修复 |
| 修复 | `ravagerClientBlockBreakFix` | 劫掠兽客户端破块修复 |
| 修复 | `stackableShulkersInHopperFix` | 潜影盒会在漏斗里堆叠修复 |

### 禁用 `disable`

| 原生分类 | 配置名 | 中文名称 |
|---|---|---|
| 禁用 | `disableAllTerrainFog` | 禁用 - 所有地形雾 |
| 禁用 | `disableArmorStandRendering` | 禁用 - 盔甲架渲染 |
| 禁用 | `disableAtmosphericFog` | 禁用 - 大气雾效 |
| 禁用 | `disableAxeStripping` | 禁用 - 斧头去皮 |
| 禁用 | `disableBatSpawning` | 禁用 - 蝙蝠生成 |
| 禁用 | `disableBeaconBeamRendering` | 禁用 - 信标光束渲染 |
| 禁用 | `disableBlockBreakCooldown` | 禁用 - 方块破坏冷却 |
| 禁用 | `disableBlockBreakingParticles` | 禁用 - 方块破坏粒子 |
| 禁用 | `disableBossBar` | 禁用 - Boss血条 |
| 禁用 | `disableBossFog` | 禁用 - Boss雾效 |
| 禁用 | `disableChunkRendering` | 禁用 - 区块渲染 |
| 禁用 | `disableClientBlockEvents` | 禁用 - 客户端方块事件 |
| 禁用 | `disableClientEntityUpdates` | 禁用 - 客户端实体更新 |
| 禁用 | `disableClientLightUpdates` | 禁用 - 客户端光照更新 |
| 禁用 | `disableConstantChunkSaving` | 禁用 - 持续区块保存 |
| 禁用 | `disableCreativeMenuInfestedBlocks` | 禁用 - 创造模式菜单的虫蚀方块 |
| 禁用 | `disableDeadMobRendering` | 禁用 - 死亡生物渲染 |
| 禁用 | `disableDeadMobTargeting` | 禁用 - 死亡生物目标选择 |
| 禁用 | `disableDoubleTapSprint` | 禁用 - 双击前进疾跑 |
| 禁用 | `disableEntityRendering` | 禁用 - 实体渲染 |
| 禁用 | `disableEntityTicking` | 禁用 - 实体更新 |
| 禁用 | `disableFallingBlockEntityRendering` | 禁用 - 下落方块渲染 |
| 禁用 | `disableFireOverlay` | 禁用 - 着火显示 |
| 禁用 | `disableFirstPersonEffectParticles` | 禁用 - 第一人称药水粒子 |
| 禁用 | `disableFreezeOverlay` | 禁用 - 冰冻显示 |
| 禁用 | `disableInventoryEffectRendering` | 禁用 - 背包药水效果显示 |
| 禁用 | `disableItemSwitchRenderCooldown` | 禁用 - 物品切换动画 |
| 禁用 | `disableMobSpawnerMobRendering` | 禁用 - 刷怪笼生物渲染 |
| 禁用 | `disableNauseaEffect` | 禁用 - 恶心效果 |
| 禁用 | `disableNetherFog` | 禁用 - 下界雾效 |
| 禁用 | `disableNetherPortalSound` | 禁用 - 下界传送门音效 |
| 禁用 | `disableObserver` | 禁用 - 侦测器触发 |
| 禁用 | `disableOffhandRendering` | 禁用 - 副手物品渲染 |
| 禁用 | `disableParticles` | 禁用 - 所有粒子效果 |
| 禁用 | `disablePortalGuiClosing` | 禁用 - 传送门关闭界面 |
| 禁用 | `disableRainEffects` | 禁用 - 降雨效果 |
| 禁用 | `disableRenderDistanceFog` | 禁用 - 渲染距离雾效 |
| 禁用 | `disableRenderingScaffolding` | 禁用 - 脚手架渲染 |
| 禁用 | `disableScoreboardRendering` | 禁用 - 计分板显示 |
| 禁用 | `disableShovelPathing` | 禁用 - 铲子铺路 |
| 禁用 | `disableShulkerBoxTooltip` | 禁用 - 潜影盒原版提示 |
| 禁用 | `disableSignGui` | 禁用 - 告示牌界面 |
| 禁用 | `disableSkyDarkness` | 禁用 - 天空暗化 |
| 禁用 | `disableSlimeBlockSlowdown` | 禁用 - 粘液块减速 |
| 禁用 | `disableStatusEffectHud` | 禁用 - 状态效果HUD |
| 禁用 | `disableTickRatePlayerSlowdown` | 禁用 - 玩家随游戏刻变慢 |
| 禁用 | `disableTileEntityRendering` | 禁用 - 方块实体渲染 |
| 禁用 | `disableTileEntityTicking` | 禁用 - 方块实体更新 |
| 禁用 | `disableVillagerTradeLocking` | 禁用 - 村民交易锁定 |
| 禁用 | `disableWallUnsprint` | 禁用 - 碰墙停止疾跑 |
| 禁用 | `disableWorldViewBob` | 禁用 - 视角晃动 |

## 核对结果

- 清单总数核对：297 + 68 = 365。
- 每条原生分类配置记录只在“当前 halfmasa 折叠组”或“未分类配置”中出现一次。
- 文档记录的是 26.2 / Tweakeroo 0.29.2 的当前配置清单；Tweakeroo 后续版本增加或改名配置时，需要重新生成。

