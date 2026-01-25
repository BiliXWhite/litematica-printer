# Changelog
所有显著的变更都会记录在这个文件中，遵循 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.0.0/) 规范。
本项目的版本号遵循 [语义化版本](https://semver.org/lang/zh-CN/)（主版本.次版本.补丁版本，如 1.2.3）。

### v1.2.3-dev

#### 2026-1-25
- 构建脚本调整, 为 Minecraft 26.1 提前准备
- 将破坏成功冷却调整为1TICK冷却, 避免小白认为挖掘不流畅
- 每刻破坏数量默认值调整为15个
- 挖掘模式下, 调整创造模式下挖掘不检查方块硬度