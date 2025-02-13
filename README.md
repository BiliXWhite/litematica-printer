Litematica 打印机
==================
此分支为 Litematica 的 Fabric 1.18.2 至 1.21.4 版本添加了自动建造功能。打印机允许玩家通过自动放置周围正确方块来快速建造大型结构。

再分支于 宅咸鱼 的版本，添加了使用数据包打印的一系列功能，并且将快捷潜影盒改为适配给装有AxShulkers插件的服务器。

下载构建出的版本请访问 [Releases](https://github.com/BiliXWhite/litematica-printer/releases)。安装前请先下载 Litematica 和 MaLiLib，并确保已安装 [Fabric API](https://www.curseforge.com/minecraft/mc-mods/fabric-api/）。最后将打印机的 .jar 文件放入 mods 文件夹。

![演示](printer_demo.gif)

使用方法
----------
默认按 `大写锁定键（CAPS_LOCK）` 切换打印机功能。配置参数（如建造速度和范围）需按 `M + C` 打开 Litematica 设置，在"通用"标签页底部找到打印机配置。快捷键可在"热键"标签页中重新绑定。按住 `V` 键（默认）可临时启用打印机（无论当前是否开启）。

### 未支持方块列表
以下方块由于特殊原因暂未实现，打印机将自动跳过。如果发现其他方块放置错误，请尝试降低建造速度。若问题依旧存在，请提交 [issue](https://github.com/BiliXWhite/litematica-printer/issues)。
- 砂轮
- 地面放置的头颅
- 告示牌
- 发光地衣和藤蔓
- 实体（包括物品展示框和盔甲架）

编译指南
----------
* 克隆本仓库
* 在仓库目录打开cmd
* 运行 `gradlew build` 编译
* 生成文件位于 `build/libs/`
