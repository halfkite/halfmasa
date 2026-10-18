# Conflux Map 0.1.10+ 兼容适配

当前实现由 Minecraft 26.3 基线同步到全部支持版本：1.21.1、1.21.3、1.21.4、
1.21.5、1.21.8、1.21.10、1.21.11、26.1.2、26.2、26.3。

## 根因与选择规则

旧 `ConfluxMapTeleportScreenMixin` 匹配 `OptionalInt` 参数，并在地图界面中寻找
`OptionalInt.isPresent()`。Conflux Map 已将传送参数改为地表查询，0.1.10 起使用
captured + predicted 两个查询，菜单许可也移到了 `teleportPositionKnown`。
原来的 `require=0` 使匹配失败不报错，设置因而静默失效。

新适配读取未加载类的字节码检测接口，不提前加载 Mixin 目标：

| 功能 | 新公开 API 存在 | 0.1.10 / 0.1.11 |
| --- | --- | --- |
| 兜底高度 | `teleportLandingResolve` | 双查询原生界面的独立适配 |
| 列表打开默认筛选 | `waypointListOpening` | 原有初始筛选适配 |
| 地图动作后关闭 | `locationActionPerformed` 和 `closeFullscreenMap` | 原有地图动作完成后的关闭语义 |
| 合并列表、紧凑网格 | 继续使用经过检查的原生界面适配 | 同左 |
| 临时路径点 | 保留现有公开 Waypoint API 实现 | 同左 |

每项新旧路径互斥。接口通过可选反射访问，不引入必装依赖，也不将上游 API 打包进
HalfMasa。监听器读取当前配置；重复 Tick 不重复注册，关闭扩展或 API 实例销毁时注销。
监听失败会记录并注销相应监听器，避免异常中断上游其他插件的事件通知。

## 高度规则

保留配置键 `confluxMapUnknownHeight` 和已保存的数值，仅修正文案和应用条件：

- 已加载的同维度区块：由原生逻辑采样实际地面。
- 已缓存的真实地形：保留原生落点。
- 空柱：无法获得实际地面时使用配置的玩家落脚 Y。
- 离线浏览中的未知落点：使用配置 Y。
- 实时世界中的未知区块：保留原生分阶段传送与加载后的修正。

加载状态通过 `getChunkSource().getChunk(..., FULL, false)` 查询，不主动加载区块。
不能使用客户端 `hasChunk()`：其原版实现始终返回真，会把未加载目标误判成已加载地面。

旧版需要兜底时调用原生 `teleportExact`，坐标保持方块中心，取消原来的地图动作，
避免另发一次指令或留下延迟修正任务。权限、命令模板和服务端限制仍由原生流程负责。
动作事件只表示动作已执行，不表示服务器已经确认传送成功。

## 新 API 基准与验证边界

适配对照 [上游 API 文档](https://github.com/Conflux-Union/conflux-map/blob/bfcac5326994a1088402a7205d665f87c9949a83/docs/api.md)，
锁定提交 `bfcac5326994a1088402a7205d665f87c9949a83`。编写时最新正式版本为
0.1.11；新 API 为主分支接口，不能将契约测试描述为未发布整包的实机验证。

`scripts/python/test_conflux_compat.py` 使用锁定的上游 API 源码检查事件、热修改配置、
注销及重建监听器，并使用官方 Conflux Map JAR 内的模型检查旧版落点策略。
通过 `CONFLUX_TEST_JAR` 分别指定官方 0.1.10 / 0.1.11 JAR。

`scripts/python/run_conflux_grid_smoke.py` 启动隔离客户端和本地服务器，检查合并列表、
紧凑布局、原生操作、资源包主题，以及真实地图动作中的兜底高度、关闭地图和任务取消。
客户端使用 `--jvm-arg=-Xint` 可避开本机此前出现的 Java 原生编译进程退出；该参数只用于测试。

跨版本回归脚本 `scripts/python/run_compat_fixes_smoke.py` 同时验证真实地图动作、
合并列表及自动进食中断。26.x 搭配 Conflux Map 0.1.11 时，测试使用
`--loader-version 0.19.5`，满足该联动模组要求的 Fabric Loader 0.19.0 以上版本。

新版本仍须按实际接口验证，不能保证任意未来版本都兼容；接口改变时应补充测试再更新适配。
