# 26.3 客户端功能跨版本适配

支持目标：1.21.1、1.21.3、1.21.4、1.21.5、1.21.8、1.21.10、1.21.11、26.1.2、26.2、26.3。使用现有 ReplayMod preprocess 版本树，共享行为与配置，按真实 Minecraft / MaLiLib API 边界选择实现。

## 范围

- 更好的按键设置：虚拟/机械改键、暂存与保存/放弃、紧凑顶栏、104/122 布局、背景透明度、固定标识点、忽略键编辑。
- 冲突按键选择：轮盘/条目、组合匹配、修饰键与鼠标默认忽略、进出世界会话清理。
- 配置界面搜索、分类与滚动位置记忆。
- 经典暂停页和主页：主操作恢复经典排列，附加按钮移到右侧，小窗口自动分列。保留原始动作、提示与禁用状态，由同一个 classicPauseMenu 开关控制。
- 试炼栏注册与分页、44 个有效预设条目、自然复制的状态保留、原版状态材质、前景直立生物预览。
- 实体与掉落物合并显示、大小/幼年分组、数量标签在屏幕空间自动避让。
- Litematica 保存、删除、直接/命令粘贴的方块过滤。

独立的 halfmasa-void-trading-server 服务端扩展仍仅构建 26.3；此次适配范围是客户端。

## 兼容边界

| 边界 | 实现 |
|---|---|
| 26.3 / 更早版本输入 | SDL 扫描码 / GLFW 键码；默认忽略值分别生成，保留自定义列表 |
| 1.21.1 试炼配置 | 读取该版本原版自然密室结构 NBT；按原版 loadAdditional 行为合并不祥配置继承值 |
| 1.21.3 起试炼配置 | 原版 trial_spawner_config 注册表中 normal / ominous 配置引用 |
| 1.21.1、1.21.3 笼子渲染 | 旧版 BakedModel 与 ItemRenderer；使用原版方块状态模型 |
| 1.21.4—1.21.8 笼子渲染 | ItemModel / SpecialModelRenderer 与旧版直接提交实体渲染 |
| 1.21.10 起笼子渲染 | 提交式实体渲染；保留各版 LayerRenderState、变换、相机和实体创建接口差异 |
| 旧版标签投影 | 原版 GameRenderer FOV/投影矩阵；1.21.1 为 double FOV |
| 26.x 标签投影 | 相机渲染状态自带矩阵，不注册旧版 FOV 访问器 |
| Litematica 调用 Minecraft 方法 | 显式对注入点生成映射，混合描述符中的 Minecraft 类型同步映射；第三方方法名保留 |

## 自动验证

构建单版示例：

```powershell
.\gradlew.bat :1.21.1:build --offline --console=plain --max-workers=1
```

需要 Java 25 启动 Gradle，同时提供 Java 21 工具链供 1.21.x 编译；可通过 org.gradle.java.installations.paths 指定本机路径。

原生跨版本验证：

```powershell
.\gradlew.bat -I scripts/gradle/port-test-classpath.init.gradle portTestClasspath --offline --console=plain
python scripts/python/test_ported_trial_items.py
```

须先完成各目标的构建/预处理。该验证使用实际预处理源码、对应 Minecraft 的注册表与 codec、原版结构和方块状态资源，覆盖每版 42 个刷怪笼、2 个宝库、20 种状态材质、内部生物、冷却和 GLFW/SDL 默认值及配置迁移。不会把编译成功当作游戏内验证。

其他回归脚本：test_trial_and_classic_pause.py、test_keymap_rebind_flow.py、test_entity_aggregation.py、test_ignored_keys_keyboard.py、test_keybind_wheel_store.py、test_conflict_selection_appearance.py、test_trial_creative_items.py、test_spawner_item_appearance.py。

## 游戏内人工验收

每个目标安装对应构建与匹配的 MaLiLib；旧版另需 MaLiLib 声明的 Fabric API 模块。试炼栏的创造栏 API 是可选模块，并不替代 MaLiLib 的必需依赖。

| 场景 | 操作与验收 |
|---|---|
| 经典主页与暂停页 | 切换 classicPauseMenu，分别检查两页；缩小窗口、切换 GUI 缩放；主按钮不重叠，附加按钮在右侧；点击动作与禁用提示保持原样 |
| 按键暂存 | 两种改键方式分别修改组合、重复取消、Esc 清空；保存后重开配置，结果一致；放弃有改动时询问，取消/Esc 返回；无改动直接退出 |
| 104/122 与标识点 | 两处虚拟键盘切换布局；检查绿/灰底色、蓝/橙/紫点、两种冲突红点与灰色忽略点的固定位置；关闭重开后配置一致 |
| 冲突选择生命周期 | 首次进世界、退出再进、切换世界后触发重复组合；单独左中右键、Shift/Ctrl/Alt 不弹出；完整组合按配置触发，轮盘与条目动作一致 |
| 试炼栏分页 | 开启/关闭后反复切换原版和其他创造页签；安装创造栏 API 时分页正常，缺少该模块时右侧入口可用 |
| 刷怪与宝库 | 放置普通、不祥和冷却预设；留足空间，以非和平难度的生存玩家靠近触发；普通/不祥生物及装备正确，冷却状态正确；不祥宝库接受不祥钥匙 |
| 材质与模型 | 检查普通/不祥、激活/冷却等状态的原版颜色；内部生物在前景正常直立，史莱姆保留尺寸，其他生物略放大；手持、掉落物视角正常 |
| 自然复制 | 创造模式 Ctrl+中键复制自然生成普通/不祥、激活/冷却笼子与宝库；搬动到其他栏位再放置，状态、内部生物和说明保持一致 |
| 合并统计 | 同时生成大小/幼年不同的同种生物和多种生物，以及近邻同种掉落物；默认分组统计；转动相机后标签自动避让；关闭功能、切换世界后恢复正常显示 |
| 投影过滤 | 安装对应 Litematica；保存/删除/直接粘贴/命令粘贴分别测试黑白名单、空列表、容器与计划刻；被排除方块及其数据不被错误清除或粘贴 |

完整世界内交互、不同资源包、多人服务器授权复制及其他模组联合行为仍需按上表人工验收。
