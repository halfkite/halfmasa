# HalfMasa 1.8.1

## 简体中文

- 修复资源包列表无法拖动排序的问题（[#8](https://github.com/halfkite/halfmasa/issues/8)）。筛选列表后仍可拖动，固定资源包保持原位，点击「完成」后保存排序。修复覆盖全部十个 Fabric 构建目标。
- 「更好的 Masa 收藏」现在默认关闭。已有配置中的开关设置继续生效。
- 收藏和全局搜索显示配置来源，搜索结果可按模组整组折叠。「点击来源跳转」可独立关闭，跳到原配置项时会以淡蓝色边框高亮 5 秒。
- 收藏星标可调整大小，已收藏和未收藏的星标可分别设色。收藏和搜索结果中的 Tweakeroo 配置项平铺显示。
- 「更好的条目选择器」默认折叠「实体模型预览」子选项。展开或折叠只改变选项的可见性。
- 新增「旁观模式保留准星」选项，开启后在第一人称旁观时始终显示准星。此选项默认关闭。
- 修复 Minecraft 1.21–1.21.1 中窄窗口下 JEI 搜索框消失的问题。配方历史面板挤占全部物品网格空间时，HalfMasa 会隐藏该面板。

## English

- Fixed drag-and-drop reordering in the resource pack list ([#8](https://github.com/halfkite/halfmasa/issues/8)). Reordering also works with a filter applied. Fixed packs stay in place, and clicking Done saves the order. The fix is available on all ten Fabric build targets.
- Better Masa Favorites is now off by default. Existing settings are preserved.
- Favorites and global search now show each option’s source mod. Search results can be collapsed by mod. Source links can be turned off, and jumping to the original option outlines its row in light blue for five seconds.
- The favorite star is resizable and supports separate colors for its selected and unselected states. Tweakeroo options are shown as individual rows in Favorites and search results.
- The Better Registry Picker now groups Entity Model Preview under a section collapsed by default. Expanding or collapsing the section only changes the option’s visibility.
- Added Keep Crosshair in Spectator Mode. When enabled, the crosshair remains visible in first-person spectator mode. The option is off by default.
- Fixed JEI's search box disappearing in narrow windows on Minecraft 1.21–1.21.1. HalfMasa hides its recipe history panel when the panel would leave no room for JEI's item grid.
