# FGA 26.3 halfmasa 支持

客户端安装本次 halfmasa 构建与 MaLiLib。多人服务器的静默进食需要本次 FGA 构建、Carpet 和 Fabric API；客户端还需要 Fabric API，无需安装 FGA。单人静默进食可在没有 Fabric API、FGA 的情况下使用。

- halfmasa（含子配置快捷键、按住显示、Boss 栏缩放与动画控制）：`mod-builds/20261014-201728/halfmasa-fabric-26.3-1.6.3+20261014.201603.jar`
- FGA：`mod-builds/20261014-155844/carpet-fga-addition-1.6.4+v2610141554-mc26.3.jar`
- 服务端本次开启：`/carpet silentAutoEat true`
- 服务端保存为默认值：`/carpet setDefault silentAutoEat true`
- 客户端展开自动进食，启用「静默进食」；此选项默认关闭

QuickShulker 取食与静默进食使用独立协议。快捷潜影盒来源主要兼容 MoRanpcy/quickshulker 的 26.3 版本，客户端、服务端需要相应 QuickShulker 与 Fabric API。启用槽位恢复后，自动取出的剩余食物归还原容器的原槽位。服务器缺少 QuickShulker 时使用普通背包；缺少静默协议或服务器规则关闭时使用普通进食。

Boss 血量协议根据真实原版 Boss 栏 UUID 查找向该玩家显示的凋灵或末影龙，返回当前 / 最大生命值。未知或无法可靠关联的 Boss 栏由客户端显示百分比。该查询不需要开启静默进食规则。

`fga-26.3-support.patch` 基于 Carpet FGA Addition 提交 `d03e36e`，只包含本次支持协议、Mixin、默认关闭的 Carpet 规则、中英文说明和版本记录。FGA 主源码版本为 1.21.1，因此补丁保留项目的 `//#if`、`//$$` 预处理写法；本次仅实现并验证 Minecraft 26.3。

复现用脚本位于 halfmasa 的 `scripts/python/`：`run_boss_bar_merge_smoke.py`、`run_auto_eat_smoke.py`、`run_halfmasa_support_smoke.py`。这些脚本创建隔离测试世界及客户端，不修改玩家存档。构建及测试报告见 `mod-builds/verification/` 中本次汇总。
