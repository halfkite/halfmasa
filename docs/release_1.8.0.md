# HalfMasa 1.8.0

## 简体中文

### 高亮与掉落物标签

- 方块高亮与实体高亮分组。每条方块规则可设置独立距离，留空时沿用全局距离。移动中保留已扫描区域的高亮，减少闪断。
- 掉落物合并数量、实体合并数量和掉落物消失时间分别支持常显示、指向显示和不显示，可用快捷键切换。掉落物数量与消失时间共用标签，名称可独立设置显示方式。

### 存档与服务器

- 存档和服务器列表均支持左侧星标收藏，默认关闭。
- 「更好的存档管理」默认关闭。开启后，可把自定义存档路径作为一级文件夹，各路径可继续建立二级文件夹。文件夹可独立展开，可隐藏路径，也可启用同一路径下的拖动排序和分组。跨路径拖动时会提示。
- 创建存档时可选择保存路径，默认使用当前选中的路径。「创建存档不自动进入游戏」可独立开启。

### 来自 Helpful-Tweake 的功能

- **更好的条目选择器**默认开启，可按名称或注册表 ID 搜索，悬停显示原版提示框。实体默认显示模型，也可切换为物品图标。
- **流体透明度**支持分别设置水、岩浆和流体内部效果。**自定义禁用渲染**为方块和实体分别提供独立的黑名单、白名单和名单模式，并迁移旧设置。
- **方块放置预览**支持流体、炼药锅和受支持版本中的 Tweakeroo 凭空放置。
- **容器比较器输出**可预估已打开容器的信号强度。**创造与生存手长一致**可用快捷键切换。**全模式 HUD**支持创造和旁观状态栏，旁观快捷栏可独立开启。
- **自动清理海带**支持只清理根部、自定义距离、每刻数量和可选的 Litematica 选区限制。

### 语言、Jade 与版本

- HalfMasa 和创造栏折叠内置繁体中文。模组简繁回退默认开启，在模组缺少当前中文语言文件时使用另一种中文。
- 所有目标均提供默认关闭的 **Jade 大木桶兼容**开关。服务端只提供单半库存时可合并预览，相同回包会保留 Jade 原有数量。
- 支持的 Fabric 构建目标为 Minecraft 1.21.1、1.21.3、1.21.4、1.21.5、1.21.8、1.21.10、1.21.11、26.1.2、26.2 和 26.3。常规构建产出 HalfMasa 主模组。

## English

### Highlights and item labels

- Block and entity highlights have separate settings groups. Each block rule can override the global highlight distance. Previously scanned blocks stay highlighted while you move to reduce flicker.
- Dropped-item counts, entity counts, and item despawn timers each support Always, On Aim, and Hidden modes with toggle hotkeys. Dropped-item counts and despawn timers share one label. Item names have their own display mode.

### Worlds and servers

- Star worlds and servers from the left side of their respective lists. Both favorites features default to off.
- Better World Management defaults to off. When enabled, each custom save path can appear as a top-level folder with optional subfolders. Folders expand independently, path labels can be hidden, and optional dragging reorders or groups saves within the same path. Dropping a save onto another path shows a message.
- Choose the destination when creating a world. The currently selected path is the default. An optional setting lets you create a world and return to the list before entering it.

### Features from Helpful-Tweake

- The **Better Registry Picker** is on by default. Search by name or registry ID and hover for vanilla tooltips. Entity models are shown by default, with an item icon option.
- **Fluid Transparency** provides separate settings for water, lava, and the view while inside fluids. **Custom Render Filtering** gives blocks and entities their own blocklists, allowlists, and list modes, and migrates existing settings.
- **Block Placement Preview** supports fluids, cauldrons, and Tweakeroo air placement on compatible versions.
- **Container Comparator Output** estimates signal strength for the open container. **Creative Reach Matches Survival** has a toggle hotkey. **All-Mode HUD** can show status bars in Creative and Spectator, with an independent Spectator hotbar option.
- **Auto Kelp Clearing** supports clearing roots only, a custom range, a per-tick limit, and an optional Litematica selection restriction.

### Languages, Jade, and supported versions

- HalfMasa and Condensed Creative include Traditional Chinese. The Chinese locale fallback is on by default and uses the other Chinese locale when a mod lacks a file for the selected one.
- **Jade Large Barrel Compatibility** is available on every target and defaults to off. It can combine previews when the server reports one half at a time. Identical replies preserve Jade's original counts.
- Fabric build targets are Minecraft 1.21.1, 1.21.3, 1.21.4, 1.21.5, 1.21.8, 1.21.10, 1.21.11, 26.1.2, 26.2, and 26.3. The standard build produces the main HalfMasa mod.
