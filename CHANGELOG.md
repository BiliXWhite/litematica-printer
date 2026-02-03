# Changelog
所有显著的变更都会记录在这个文件中，遵循 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.0.0/) 规范。
本项目的版本号遵循 [语义化版本](https://semver.org/lang/zh-CN/)（主版本.次版本.补丁版本，如 1.2.3）。

### v1.2.3-dev

#### 2026-2-03
- 挖掘进度阈值默认值调整为100%

##### 2026-2-02
- 翻新大部分文本内容
- 优化配置项分类和顺序
- 移除单模下单独的“打印按钮”

#### 2026-2-01
- 修复前几个版本破坏类调整导致的一系列问题(完善破坏, 修复破冰, 修复挖掘黑白名单)
- 将每刻放置数调整为1(符合原先设定, 之前是工作间隔 >0 情况下, 都是以1运行)
- 将每刻破坏数调整为20

#### 2026-1-30
- 完善比较器方块状态不一致破坏逻辑 [issues#78](https://github.com/BiliXWhite/litematica-printer/issues/78#issuecomment-3815415357)
- 添加 InventoryUtils.showMessageWithCooldown 冷却避免刷屏
- 将破坏统一由 InteractionUtils 类进行管理

#### 2026-1-29
- 添加[issues#78](https://github.com/BiliXWhite/litematica-printer/issues/78)
- 跳过放置列表常驻
- 添加破坏检查方块硬度选项

#### 2026-1-25
- 构建脚本调整, 为 Minecraft 26.1 提前准备
- 将破坏成功冷却调整为1TICK冷却, 避免小白认为挖掘不流畅
- 每刻破坏数量默认值调整为15个
- 挖掘模式下, 调整创造模式下挖掘不检查方块硬度
- 修复打印(新增)与打印热键序列化键名重复
- 更改选项名称(配置键名没改)
- 修复生存模式下无法挖掘问题