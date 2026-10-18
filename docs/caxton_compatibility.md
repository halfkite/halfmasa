# Caxton 书籍编辑兼容排查

排查日期：2026-10-17。对应 [Issue #5](https://github.com/halfkite/halfmasa/issues/5)。

【BUG表象】：同时安装 HalfMasa 与 Caxton 时，书与笔编辑界面可能在类加载阶段崩溃；其他模组提前加载该界面时，崩溃会发生在启动期间。

【真实根因】：1.21.1～1.21.5 的 HalfMasa 依赖原版书页布局及光标索引映射，而 Caxton 用 `@Overwrite` 替换书页布局。相同优先级的注入在准备阶段被 Mixin 拒绝。实际 Caxton 字节码仍有取当前文字的调用，因此不能将错误简单归为“找不到调用点”，也不能仅靠 `require = 0` 修复。

【修复方案】：使用旧布局的 1.21.1～1.21.5 已通过 Mixin 插件在加载前识别 `caxton`，成组跳过旧书页间距与光标映射注入，让 Caxton 接管编辑布局。未安装 Caxton 时保留原行为。此兼容模式不为编辑页添加 HalfMasa 显示空格；配置值保持不变。1.21.3 也应用同一加载防护，但其 Caxton 字体注入独立错误仍需上游修复。

【修改代码】：`XaeroMixinPlugin.shouldApplyMixin` 中的 `MC < 1.21.8` 分支；真实编辑回归脚本为 `scripts/python/run_caxton_book_smoke.py`。

【回归校验】：1.21.1 修复版与 Caxton 0.6.4 的真实世界编辑测试通过 146 项；无 Caxton 时通过 145 项，包括间距开关组合、文字点击、选区删除、导航、翻页、原始内容保存及 Mixin 加载审计。

以下为修复前排查：其他目标使用此前的 1.7.0 安装包、Fabric Loader 0.19.5、对应游戏 Java、Fabric API 与 MaLiLib，在隔离客户端到达标题后强制加载书籍编辑及新编辑控件类。Caxton 按官方 Modrinth 列表选择该目标最新可用 Fabric 发布，并核对官方 SHA-512：

| Minecraft | Caxton | 实际结果 |
|---|---|---|
| 1.21.3 | 0.6.0+1.21.1-FABRIC | 在 Caxton 自身 `TextRendererMixin.onDrawLayerString` 先失败；移除 HalfMasa 与 MaLiLib 的对照组仍复现。书页存在相同覆盖组合，但运行未到达该路径 |
| 1.21.4 | 0.6.0-alpha.6+1.21.4-FABRIC | 实际复现相同书籍布局注入冲突 |
| 1.21.5 | 0.6.4+1.21.5-FABRIC | 实际复现相同书籍布局注入冲突 |
| 1.21.8 | 0.7.0+1.21.8-FABRIC | 相关界面类加载通过 |
| 1.21.10 | 0.8.0-alpha.3+1.21.10-FABRIC | 相关界面类加载通过 |
| 1.21.11 | 0.9.0-beta.3+1.21.11-FABRIC | 相关界面类加载通过 |
| 26.1.2 | 1.0.0-beta.2+26.1.2-FABRIC | 相关界面类加载通过 |
| 26.2 | 1.1.0-alpha.3+26.2-FABRIC | 相关界面类加载通过 |
| 26.3 | 1.2.0-alpha.2+26.3-FABRIC | 相关界面类加载通过 |

1.21.8 起 HalfMasa 使用另一套编辑控件注入，不再包含这组旧书页注入。类加载通过只说明本次未复现同类转换错误，不等于全部编辑、字体包或其他模组组合已经验收。

本地证据：`build/caxton-compat/version-audit/summary.json` 记录下载哈希和字节码检查；`runtime-results.json` 记录每次启动、实际安装包及日志；`1.21.3-control.json` 记录不安装 HalfMasa 的对照结果。官方版本信息见 [Caxton 发布列表](https://modrinth.com/mod/caxton/versions)。

## 修复后的跨版本验证

全部十个目标已重新构建、单独时间戳归档并通过真实客户端回归：每版 36 项配置与原生快捷键检查，以及 80 项 MiniHUD 独立预览检查。MiniHUD 优化默认关闭、未绑定快捷键、旧配置值保留和标题同行布局均已同步。

1.21.1、1.21.4、1.21.5 最终修复包分别通过无 Caxton 的 145 项及安装表中对应 Caxton 的 146 项书籍编辑检查；1.21.3 通过无 Caxton 的 145 项检查。1.21.3 仍保留上游限制，不标记为 Caxton 组合通过。每个最终安装包的哈希和实际测试映射见 `build/compat-port-results.json`。完整仓库单元测试 110 项通过。
