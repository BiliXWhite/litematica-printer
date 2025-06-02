> [!WARNING]  
> 该README正在重构，目前的内容可能不完整或有误。请耐心等待更新或者Pull Request以贡献这个项目。

Litematica Printer
==================
为 Litematica 模组的 Minecraft Fabric 1.18.2 至 1.21.4 版本添加了自动建造功能。打印机允许玩家通过自动放置周围正确方块来快速还原投影。

这个仓库分支于[宅咸鱼二改版](https://github.com/zhaixianyu/litematica-printer)，添加了一些实用的功能。

下载发布版本请前往 [**Releases**](https://github.com/BiliXWhite/litematica-printer/releases)。安装前请先下载 **Litematica** 和 **MaLiLib** 两个模组，并确保已安装 **Fabric API**。最后将 jar 文件放入 `mods` 文件夹。

使用方法
----------
按 `大写锁定键（CAPS_LOCK）` 切换打印机开关。 

按住 `V` 键（默认）以按住的方式启用打印机。

按 `Z` + `Y` 键打开设置界面。

### 未支持方块列表
以下方块由于特殊原因暂未实现，打印机将自动跳过，亦或者是呈现错误的打印状态。如果发现其他方块放置错误，请尝试降低建造速度。若问题依旧存在，请提交 [issue](https://github.com/BiliXWhite/litematica-printer/issues)。
- 地面放置的头颅（虽然说可以实现但是我懒）
- 告示牌（这个也是）
- 实体方块（包括但不限于物品展示框、盔甲架、画等等）

编译
----------
1. 下载源码至你的机器上。
2. 在本仓库里打开`gradlew.bat`批处理文件以完成项目初始化。
3. 运行 `gradlew build` 编译
4. 构建出来的jar文件位于 `./build/libs/`

> [!TIPS]
> 在中国大陆环境可能会导致支持库下载失败。请尝试使用**代理**进行下载。
> 您不需要开启代理的网卡模式，项目拥有系统代理自动识别功能（仅限Windows环境下）。

## 常见问题

#### 为什么开启打印后，打印机不工作？

1. 服务器装有反作弊插件，可能会导致打印机无法工作。
2. `打印机工作间隔`设置过小，导致服务器无法及时响应，请尝试开启`使用数据包打印`功能打印。
3. 某些玄学问题，在开启正版验证的服务器里打印数据交互不正常。可尝试重新登陆游戏账号。（推荐使用[AuthMe](https://modrinth.com/mod/auth-me)模组）
如果以上方法都无法解决问题，请尝试提交[Issue](https://github.com/BiliXWhite/litematica-printer/issues)，并附上详细的错误描述和日志。

#### 为什么打印机放置的方块是错的？

1. 服务器装有反作弊插件，可能会导致打印机无法模拟看向放置。
2. 打印机工作间隔设置过小，服务器无法及时响应，导致方块出现错误。属于正常现象，请尝试增大`打印机工作间隔`的值。
3. 智能识别功能没有写到关于的方块，导致打印机无法正确识别方块类型。请提交[Issue](https://github.com/BiliXWhite/litematica-printer/issues)，表明什么方块出现错误。

#### 快捷潜影盒功能无法使用？

1. 服务器未装有AxShulkers插件，无法使用快捷潜影盒功能。
2. 未设置好预设位置，无法使用快捷潜影盒功能。须在Litematica设置中设置好`pickBlockableSlots`值。如图所示：
![预设位置](预设位置.png)

快捷潜影盒仍处于测试阶段，可能会有一些问题，如果遇到问题请提交[Issue](https://github.com/BiliXWhite/litematica-printer/issues)。
