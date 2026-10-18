# Litematica Printer — Hana

[English](README.md) | **简体中文**

[![GitHub Actions](https://github.com/Yur1Ca/litematica-printer/actions/workflows/build.yml/badge.svg)](https://github.com/Yur1Ca/litematica-printer/actions/workflows/build.yml)
[![Modrinth](https://img.shields.io/modrinth/dt/nriQwbvD?logo=modrinth&label=Modrinth)](https://modrinth.com/mod/litematica-printer-hana)
[![GitHub release](https://img.shields.io/github/v/release/Yur1Ca/litematica-printer?include_prereleases&label=GitHub)](https://github.com/Yur1Ca/litematica-printer/releases)
[![License](https://img.shields.io/github/license/Yur1Ca/litematica-printer)](LICENSE.md)

Litematica Printer — Hana 是一个客户端 Fabric 模组，为 Litematica 添加自动还原投影的能力。它会在玩家周围自动放置正确的方块，并提供填充、排流体、挖掘、破基岩、物品管理等辅助功能。

这是在多个 Litematica Printer 分支基础上继续维护的独立版本，包含重写的破基岩流程、扫描器与迭代器优化，以及更多建造功能。

## 下载与发布通道

- [Modrinth](https://modrinth.com/mod/litematica-printer-hana) 提供多版本 Wrapper JAR。需要启动器或 Mod Menu 识别更新时，推荐从这里安装。
- [GitHub Releases](https://github.com/Yur1Ca/litematica-printer/releases) 同时提供各 Minecraft 版本的独立 JAR。
- Actions 自动构建的 `devXXX` 为 **Alpha** 版本。
- 手动创建的 Release 为 **Beta** 版本。
- 目前没有正式版通道。

## 支持的 Minecraft 版本

- 1.18.2
- 1.19.4
- 1.20.1、1.20.2、1.20.4、1.20.6
- 1.21 至 1.21.11
- 26.1.x、26.2、26.3

暂不支持 1.18.2 以下版本。处于同一兼容范围内的小版本可能可以直接使用，但不一定提供单独构建的 JAR。

## 前置模组

必需：

- [Fabric Loader](https://fabricmc.net/)
- [Fabric API](https://modrinth.com/mod/fabric-api)
- [MaLiLib](https://modrinth.com/mod/malilib)
- [Litematica](https://modrinth.com/mod/litematica)

可选联动：

- [Tweakeroo](https://modrinth.com/mod/tweakeroo)
- [Quick Shulker](https://github.com/MoRanpcy/quickshulker)
- [Take It Out](https://modrinth.com/mod/takeitout)，仅用于受支持的 1.21.x 和 26.x 版本
- [Chest Tracker](https://github.com/ponuing/ChestTracker)，仅用于有对应版本构建的 1.21.4+ 和 26.x 版本

所有前置模组都需要选择与当前 Minecraft 版本匹配的版本。

## 功能

### 打印与性能

- 以玩家为中心、按距离排序执行放置。
- 面向大选区的扫描缓存与时间预算迭代。
- 数据包放置模式，降低幽灵方块并提高效率。
- 打印进度 HUD 与缺少材料 HUD。
- 服务器卡顿和 RTT 防护，减少混合材料打印错位。
- 对方向和状态敏感方块提供更安全的放置处理。

### 建造工具

- 在活动投影或选区内填充。
- 自动移除水源和岩浆源。
- 在选区内自动挖掘。
- 分别控制多余方块和错误状态方块的处理。
- 带白名单的独立破基岩模式。
- 含水方块放置和破冰放水。
- 使用活珊瑚替换投影中的死珊瑚。

### 物品联动

- 容器同步：在热键设置中绑定“同步容器”，开启工作总开关，主手选空槽，指向模板箱子、木桶或潜影盒启动。按槽位同步 Litematica 区域选区内同类型、同容量的其他容器，双箱按一个整体处理。需要走到目标容器的正常交互范围内。
- 同步使用背包材料，多余物品回收到背包；缺料或空间不足时暂停，处理后按热键重试。运行时按热键或关闭工作总开关会停止新任务，并等待当前搬运安全收尾。主手、护甲、副手及受保护的快捷栏不参与搬运，不依赖远程取物联动。
- 搬运中被服务器强制关箱时，会等待完整库存确认后暂停。鼠标物品由服务器处理，背包放不下时原版可能产生掉落物。

- Quick Shulker 自动取料与有序存回。
- 在受支持版本中使用 Take It Out 远程取物。
- 在受支持版本中，仅从明确加入当前 Litematica 投影选区缓存的 Chest Tracker 容器远程取物。
- 切换材料时锁定放置，尽量避免使用上一个快捷栏物品错放。

打印机还包含大量原版方块的专用放置逻辑，包括楼梯、门、活版门、漏斗、箱子、拉杆、红石粉、藤蔓、垂落植物、砂轮、合成器和多种方向方块。

## 基本使用

1. 使用 Litematica 在世界中加载原理图。
2. 移动到投影方块的交互范围内。
3. 按 `Caps Lock` 开启打印机。
4. 根据服务器情况在配置界面调整工作间隔和打印模式。

大部分配置项都带有说明文本。目前还没有完整的官方视频教程。

## 暂不完整支持的内容

以下内容可能会被跳过，或者无法可靠还原状态：

- 头颅、告示牌、旗帜以及其他具有复杂状态或数据的方块。
- 装有液体的炼药锅。
- 物品展示框、盔甲架、画等实体。
- 未明确适配的非原版内容。

如果原版方块在较保守的工作间隔下仍然放置错误，请提交[方块支持报告](https://github.com/Yur1Ca/litematica-printer/issues/new/choose)。

## 常见问题

### 开启打印机后没有工作

- 部分反作弊会拦截打印机的静默转向或放置交互。
- 工作间隔过短可能超过服务器放置速率限制。请提高间隔，或者在服务器支持时尝试数据包打印。
- 登录状态或会话异常偶尔会影响交互数据包，可以尝试重新登录账号或服务器。

### 放置出的方块状态错误

- 服务器反作弊或高延迟可能导致模拟朝向未被接受。
- 工作间隔过短时，服务器可能来不及确认材料切换和放置。
- 方块可能尚未实现对应放置规则。提交 Issue 时请提供方块 ID、目标状态、Minecraft 版本、打印机版本和稳定复现步骤。

### 快捷潜影盒无法使用

- 服务器需要支持所选模式对应的背包内潜影盒打开方式。
- 打印机设置中的模式必须与实际安装的模组或服务器功能一致。
- Litematica 的 `pickBlockableSlots` 必须包含可用快捷栏槽位，且不应全部放置潜影盒。

![推荐的快捷选择栏位](预设位置.png)

## 反馈与交流

- 安装帮助、配置问题、兼容性讨论和一般交流请使用 [GitHub Discussions](https://github.com/Yur1Ca/litematica-printer/discussions)。
- 可稳定复现的 Bug、方块支持问题和明确的功能建议请使用 [GitHub Issues](https://github.com/Yur1Ca/litematica-printer/issues/new/choose)。
- 提交前请搜索已有 Issues 和 Discussions。
- 每个 Issue 只提交一个可以独立复现的问题，并按情况附上 `latest.log`、崩溃报告、截图或短视频。

### QQ 群

不方便使用 GitHub 的中文用户，可以[点击这里加入 QQ 群](https://qm.qq.com/q/L8wglHf3GI)进行交流、反馈和获取帮助。

## 编译

完整构建多版本 Wrapper 时推荐使用 JDK 25。

```bash
./gradlew :fabricWrapper:build
```

构建产物：

- 多版本 Wrapper：`fabricWrapper/build/libs/`
- 各版本独立 JAR：`fabricWrapper/build/libs/jars/`

首次构建会下载 Minecraft 和模组依赖，需要能够正常访问对应 Maven 仓库。在中国大陆网络环境下，如果下载失败，请检查网络或代理配置。

## 感谢

- [bunny_i](https://github.com/bunnyi116)：为项目提供了广泛的支持和贡献。
- [aleksilassila/litematica-printer](https://github.com/aleksilassila/litematica-printer)：原始项目。
- [zhaixianyu/litematica-printer](https://github.com/zhaixianyu/litematica-printer)：早期修复与功能改进。
- [MoRanpcy/quickshulker](https://github.com/MoRanpcy/quickshulker)：Quick Shulker 联动支持。
- [bunnyi116/fabric-bedrock-miner](https://github.com/bunnyi116/fabric-bedrock-miner)：破基岩功能基础。
- 所有参与测试、反馈问题、贡献代码和支持项目的人。

## 许可证

本项目使用 [GNU Affero General Public License v3.0](LICENSE.md) 许可证。
