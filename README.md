> [!WARNING]  
> 该README正在重构，目前的内容可能不完整或有误。请耐心等待更新或者贡献这个项目。

Litematica Printer
==================

该模组为 投影 的 Minecraft Fabric 1.18.2 至 1.21.10 版本添加了自动建造功能。允许玩家通过自动放置周围正确方块来快速还原投影。

这个版本基于[宅咸鱼二改版](https://github.com/zhaixianyu/litematica-printer)修改，添加了一些实用的功能。

如果你觉得好用，可以给该项目点个 Star ⭐️ 来以支持我们。

该分支始终保持开源免费，不会存在任何收费内容。当然条件允许的话可以给作者[买瓶脉动](https://ifdian.net/a/BlinkWhite)哦！

下载
----------

官方提供的下载渠道有两种: 
- [**Github Releases**](https://github.com/BiliXWhite/litematica-printer/releases)
- [**蓝奏云分流(密码cgxw)**](https://xeno.lanzoue.com/b00l1v20vi)

### 游戏版本

目前该模组支持以下游戏版本：
- 1.18.2
- 1.19.4
- 1.20.1
- 1.20.2
- 1.20.4
- 1.20.6
- 1.21(.1)
- 1.21.3
- 1.21.4
- 1.21.5
- 1.21.6/7/8
- 1.21.9/10

暂不接受1.18.2以下版本的更新，之间的小版本是否可用请自行尝试，一般版本进度会跟进上游分支


## 前置模组

该模组必须先安装 **Fabric API** , **MaLiLib** 和 **Litematica** 作为前置。可选前置有 **Twrakeroo** , **Chest Tracker**(≤1.21.4) 和 **Quick Shulker**。

## 特性

- **🚀优化**
  - [x] 更流畅的打印体验
  - [x] 使用数据包打印功能（速度更快，无幽灵方块）
  - [x] 可视化放置进度条
  - [x] 服务器卡顿检测，防止因卡顿导致的大量方块放置错误
  - [x] 不会因缺少水源而在迭代水时卡死不打印的 bug

- **⏩改进**
  - [x] 填充功能（填充Litematica的选区范围）
  - [x] 支持双兼容快捷潜影盒功能（服务器AxShulkers和模组Quick Shulker）
  - [x] 替换珊瑚（使用活珊瑚打印投影内的死珊瑚）
  - [x] 更好的破坏错误方块和破冰放水
  - [x] 支持多达 48 种范围迭代逻辑
  - [x] 支持破坏错误额外方块和错误状态方块

- **🛠️修复**
  - [x] 修复很多方块的放置算法，包括：
    - 合成器、拉杆、红石粉（非连接模式）
    - 枯叶、各种花簇的方向数量
    - 发光浆果、带花的花盆
    - 楼梯、藤蔓、缠怨藤、垂泪藤
    - 砂轮、门、活版门、漏斗、箱子

使用方法
----------

1. 在 Litematica 中加载一个投影。
2. 身移到可以接触到投影方块的地方。
3. 按下`大写锁定（Caps Lock）`键开启打印机。
4. 享受自动的打印:)

## 未支持方块列表
以下方块由于特殊原因暂未实现，打印机将自动跳过，亦或者是呈现错误的打印状态。如果发现其他方块放置错误，请尝试降低建造速度。若问题依旧存在，请提交 [Issue](https://github.com/BiliXWhite/litematica-printer/issues)。
- 头颅，告示牌，旗帜(以及具有16个朝向的任何方块)
- 装有液体的炼药锅
- 实体方块（包括但不限于物品展示框、盔甲架、画等等）
- 非原版游戏内容

编译
----------
1. 使用任意方式将源码下载至你的机器上。
2. 运行`gradlew build`进行编译。
3. 构建出来的多版本jar文件位于 `./fabricWrapper/build/libs/`内，单独版本位于`./fabricWrapper/build/tmp/submods/META-INF/jars`内。

如果你想使用IDEA进行编译，请使用以下步骤：
1. 在IDEA中打开项目。
2. 在Gradle面板中，找到`Tasks -> build`，双击`build`任务进行编译。
3. 编译完成后，构建出来的多版本jar文件位于 `./fabricWrapper/build/libs/`内，单独版本位于`./fabricWrapper/build/tmp/submods/META-INF/jars`内。

> [!TIP]
> 您可能需要多次运行`gradlew build`，以确保所有依赖项都已正确下载和编译。
> 
> 在中国大陆环境可能会导致支持库下载失败。请尝试使用**代理**进行下载。
> 如果您是 Windows 11 及以上系统，则无需开启代理的全局网卡模式，该项目配置自动识别代理。

常见问题
----------

## 推荐加入QQ群聊
- 毕竟不是人人都有能力在 GitHub 上提交 Issue ，您可以加入我们的QQ群聊，以便更好的反馈问题，获取更新和获得帮助。
[点击此处加入QQ群聊](http://qm.qq.com/cgi-bin/qm/qr?_wv=1027&k=ttinzrJB3jYRLSTJM8R2YfwYdCm4Zo90&authKey=vfwF)

### 为什么开启打印后，打印机不工作？
- 由于投影打印机是基于发送静默看向的方式进行打印的，不会考虑点击面合法化，所以会被服务器反作弊检测。
- `打印机工作间隔`设置过小，导致类似于 Luminol 等有放置速率限制的服务器不会及时响应，请尝试开启`使用数据包打印`功能打印或者调高`打印机工作间隔`。 
- 某些玄学问题，在开启正版验证的服务器里打印数据交互不正常。可尝试重新登陆游戏账号。（推荐使用[AuthMe](https://modrinth.com/mod/auth-me)模组） 

如果以上方法都无法解决问题，请尝试提交 [Issue](https://github.com/BiliXWhite/litematica-printer/issues/new?template=bug%E6%8A%A5%E5%91%8A.yml) ，开发者会协助您解决问题。

### 为什么打印机放置的方块是错的？

1. 服务器装有反作弊插件，可能会导致打印机无法模拟看向放置。
2. 打印机工作间隔设置过小，服务器无法及时响应，导致方块出现错误。属于正常现象，请尝试增大`打印机工作间隔`的值。
3. 识别算法没有考虑到关于的方块，导致打印机不会正确处理。请提交 [Issue](https://github.com/BiliXWhite/litematica-printer/issues/new?template=%E6%89%93%E5%8D%B0%E6%96%B9%E5%9D%97%E8%AF%B7%E6%B1%82.yml) ，表明什么方块出现错误。

### 快捷潜影盒功能无法使用？

1. 服务器未装有可以在背包右键打开潜影盒的插件(推荐使用AxShulkers)，无法使用快捷潜影盒功能。
2. 投影打印机设置与实际能用的模式不符，请调整为正确支持的模式。
3. 预选栏位填满了潜影盒。须在Litematica设置中设置好`pickBlockableSlots`（快捷选择栏位）值。如图所示：
![预设位置](预设位置.png)

快捷潜影盒仍处于测试阶段，可能会有一些问题，如果遇到问题请提交[Issue](https://github.com/BiliXWhite/litematica-printer/issues)。


感谢
----------

- [aleksilassila/litematica-printer](https://github.com/aleksilassila/litematica-printer): 如果没有[aleksilassila](https://github.com/aleksilassila)的成果，那么整个改版分支将不存在！
- [zhaixianyu/litematica-printer](https://github.com/zhaixianyu/litematica-printer): 在原著的基础上解决了很多问题，同时也引进了很多新的内容
- [MoRanpcy/quickshulker](https://github.com/MoRanpcy/quickshulker): 新版的快捷潜影盒支持（由于没有开发包支持所以只能硬塞jar包）\
- [bunnyi116/fabric-bedrock-miner](https://github.com/bunnyi116/fabric-bedrock-miner): 新的破基岩模式前置，虽说速度没有宅咸鱼的快，但是能在高版本使用且不会卡顿。
- 以及所有支持开发的人，包括你！