# HalfMasa 1.7.3 跨版本验证 — 2026-11-08

## 修复

多人服务器描述排版原先通过 `@Redirect` 独占 `Font.split(...)` 调用，同一点注入可能产生冲突。现在通过 MixinExtras `@WrapOperation` 调用 `original.call(...)`，保留原有宽度计算与描述截断。

构建版本统一为 `1.7.3`，不带开发时间戳。没有发布到远端。

## 实际验证

十个目标逐版本运行 `:<版本>:build --offline -Ppublication_version=1.7.3`，并使用 `run_better_multiplayer_smoke.py` 对各自归档的安装包运行隔离客户端专项。十个构建和十个专项均通过。每个安装包内的 fabric.mod.json 版本均为 1.7.3，测试报告的 SHA-256 与对应归档清单一致。

第一轮专项包含多人列表创建及渲染、文件夹/搜索、隐私检测、真实本地 Ping 和 Mixin audit。该轮直接创建多人界面，未加载 ViaFabricPlus；加入服务器调用由探针记录并取消。后续另完成下述实际共存对照测试。

## ViaFabricPlus 共存对照测试

实际启动独立 Minecraft 客户端并加载正式 ViaFabricPlus JAR；4.4.15 使用本地整合包文件，5.1.3 使用官方 Modrinth 下载文件。两个 JAR 的 SHA-512 均与官方元数据一致。没有改写 ViaFabricPlus，也没有关闭其冲突注入。

| Minecraft / ViaFabricPlus | 旧 HalfMasa 1.7.2 | 正式 HalfMasa 1.7.3 |
| --- | --- | --- |
| 1.21.11 / 4.4.15 | [复现 Redirect 冲突](D:/ai/half_masa/build/better-multiplayer-tests/20261108-164645-798377/report.json) | [共存 PASS](D:/ai/half_masa/build/better-multiplayer-tests/20261108-165027-247158/report.json) |
| 26.3 / 5.1.3 | [复现 Redirect 冲突](D:/ai/half_masa/build/better-multiplayer-tests/20261108-165526-878565/report.json) | [共存 PASS](D:/ai/half_masa/build/better-multiplayer-tests/20261108-165702-953095/report.json) |

1.21.11 实际向主菜单发送鼠标点击，打开多人列表；26.3 直接执行主菜单原生多人按钮的 `onClick` 动作，绕过测试环境中的主菜单输入拦截。两个旧包都在此流程出现相同的 `@Redirect conflict` 和 `disableServerPinging` 零目标注入错误；两个新包均通过多人列表专项。

两个新包还设置真实 ViaFabricPlus 每服务器 Beta 1.7–1.7.3 协议，断言它的 `viaFabricPlus$disableServerPinging` 标志为真、条目显示所选协议名，并用只读 Font 探针记录地址替换后的 `Font.split` 返回了非空文本（各 121 次）。这验证了旧协议列表的显示调用链，不代表已连接真实 Beta 服务器或完成跨版本进服测试。

可复用入口：`scripts/python/run_viafabricplus_smoke.py --version <1.21.11 或 26.3> --artifact <HalfMasa.jar> --extra-mod <ViaFabricPlus.jar> --loader-version 0.19.5`。通用列表回归入口支持 `run_better_multiplayer_smoke.py --extra-mod`，测试隔离目录和报告记录双方 JAR 的 SHA-256。

### 其余版本共存矩阵

以下版本也加载了匹配的官方 ViaFabricPlus 并通过多人列表专项：

| Minecraft | ViaFabricPlus | HalfMasa 1.7.3 测试报告 |
| --- | --- | --- |
| 1.21.1 | 3.4.9 | [PASS](D:/ai/half_masa/build/better-multiplayer-tests/20261108-173110-844058/report.json) |
| 1.21.3 | 3.5.3 | [PASS](D:/ai/half_masa/build/better-multiplayer-tests/20261108-173215-169701/report.json) |
| 1.21.4 | 4.0.5-BACKPORT | [PASS](D:/ai/half_masa/build/better-multiplayer-tests/20261108-173252-373777/report.json) |
| 1.21.5 | 4.1.5 | [PASS](D:/ai/half_masa/build/better-multiplayer-tests/20261108-173332-439876/report.json) |
| 1.21.8 | 4.2.5 | [PASS](D:/ai/half_masa/build/better-multiplayer-tests/20261108-173416-901847/report.json) |
| 1.21.10 | 4.3.6 | [PASS](D:/ai/half_masa/build/better-multiplayer-tests/20261108-173455-289843/report.json) |
| 26.1.2 | 4.5.6 + MixinExtras 0.5.3 | [PASS](D:/ai/half_masa/build/better-multiplayer-tests/20261108-173846-494567/report.json) |
| 26.2 | 5.0.3 + MixinExtras 0.5.5 | [PASS](D:/ai/half_masa/build/better-multiplayer-tests/20261108-174215-152366/report.json) |

26.1.2 和 26.2 的 ViaFabricPlus 对 MixinExtras 版本有额外要求；测试使用兼容版本依赖后运行通过。26.2 使用 MixinExtras 0.5.5 是因为 0.5.3 仍会触发 ViaFabricPlus 自身的 ASM 注解转换异常。

| Minecraft | 安装包 | 多人专项报告 |
| --- | --- | --- |
| 1.21.1 | [halfmasa-fabric-1.21-1.21.1-1.7.3.jar](D:/ai/half_masa/mod-builds/20261108-155835/halfmasa-fabric-1.21-1.21.1-1.7.3.jar) | [PASS](D:/ai/half_masa/build/better-multiplayer-tests/20261108-155835-935208/report.json) |
| 1.21.3 | [halfmasa-fabric-1.21.2-1.21.3-1.7.3.jar](D:/ai/half_masa/mod-builds/20261108-160010/halfmasa-fabric-1.21.2-1.21.3-1.7.3.jar) | [PASS](D:/ai/half_masa/build/better-multiplayer-tests/20261108-160010-979455/report.json) |
| 1.21.4 | [halfmasa-fabric-1.21.4-1.7.3.jar](D:/ai/half_masa/mod-builds/20261108-160146/halfmasa-fabric-1.21.4-1.7.3.jar) | [PASS](D:/ai/half_masa/build/better-multiplayer-tests/20261108-160146-344975/report.json) |
| 1.21.5 | [halfmasa-fabric-1.21.5-1.7.3.jar](D:/ai/half_masa/mod-builds/20261108-160413/halfmasa-fabric-1.21.5-1.7.3.jar) | [PASS](D:/ai/half_masa/build/better-multiplayer-tests/20261108-160413-841071/report.json) |
| 1.21.8 | [halfmasa-fabric-1.21.6-1.21.8-1.7.3.jar](D:/ai/half_masa/mod-builds/20261108-160701/halfmasa-fabric-1.21.6-1.21.8-1.7.3.jar) | [PASS](D:/ai/half_masa/build/better-multiplayer-tests/20261108-160701-671829/report.json) |
| 1.21.10 | [halfmasa-fabric-1.21.9-1.21.10-1.7.3.jar](D:/ai/half_masa/mod-builds/20261108-160948/halfmasa-fabric-1.21.9-1.21.10-1.7.3.jar) | [PASS](D:/ai/half_masa/build/better-multiplayer-tests/20261108-160948-373496/report.json) |
| 1.21.11 | [halfmasa-fabric-1.21.11-1.7.3.jar](D:/ai/half_masa/mod-builds/20261108-161213/halfmasa-fabric-1.21.11-1.7.3.jar) | [PASS](D:/ai/half_masa/build/better-multiplayer-tests/20261108-161213-506339/report.json) |
| 26.1.2 | [halfmasa-fabric-26.1-26.1.2-1.7.3.jar](D:/ai/half_masa/mod-builds/20261108-161347/halfmasa-fabric-26.1-26.1.2-1.7.3.jar) | [PASS](D:/ai/half_masa/build/better-multiplayer-tests/20261108-161348-334761/report.json) |
| 26.2 | [halfmasa-fabric-26.2-1.7.3.jar](D:/ai/half_masa/mod-builds/20261108-161454/halfmasa-fabric-26.2-1.7.3.jar) | [PASS](D:/ai/half_masa/build/better-multiplayer-tests/20261108-161454-456686/report.json) |
| 26.3 | [halfmasa-fabric-26.3-1.7.3.jar](D:/ai/half_masa/mod-builds/20261108-161600/halfmasa-fabric-26.3-1.7.3.jar) | [PASS](D:/ai/half_masa/build/better-multiplayer-tests/20261108-161601-215865/report.json) |

## SHA-256

| Minecraft | SHA-256 |
| --- | --- |
| 1.21.1 | `c4a6385784dc5616dd4e5e6a0e13a1375cc4e1dc2a5925fb875dc22ade874dda` |
| 1.21.3 | `13d55798addf6eb18154a346d0459b08f1a4311f5a027f45508351059f5be6c9` |
| 1.21.4 | `a37b5e6193316bb3cd1d11e6a9a1974febc14f3cbd6900cdd276c32ca671ee61` |
| 1.21.5 | `d23f390bfc09572bc14e3ea50ab7cfa4a2481498b11c7f29c9bf132c92259e80` |
| 1.21.8 | `483ca78424f067a04db20f2652e18c0b4bfa44848a9fdc5c7941014b4460bced` |
| 1.21.10 | `c634d2299ca9edd975464be907d5ce0f81f588acc62080476943f4328cd73b08` |
| 1.21.11 | `40149b40e3d3ffe5723d31bdde92daf415c6b01d150edc844665b0782d32d21d` |
| 26.1.2 | `af501c3af11f08eff360582f7c10196f94a45b424f3fa49435df1f20035b19a9` |
| 26.2 | `963e7bf313dc7965db45ccf6fc1ac610f033f9b9f7ee79b9ce9485a6794b23cb` |
| 26.3 | `a3c5ad0402424f01391c81a02f042b9cd8d1df13d564a194381391717c661a88` |

汇总数据：[report.json](D:/ai/half_masa/build/sync-1.7.3-20261108-155651/report.json)。
