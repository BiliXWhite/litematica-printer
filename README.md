> [!WARNING]  
> 该README正在重构，目前的内容可能不完整或有误。请耐心等待更新或者Pull Request以贡献这个项目。

Litematica Printer
==================

该模组为 Litematica 的 Minecraft Fabric 1.18.2 至 1.21.8 版本添加了自动建造功能。允许玩家通过自动放置周围正确方块来快速还原投影。

这个版本基于[宅咸鱼二改版](https://github.com/zhaixianyu/litematica-printer)修改，添加了一些实用的功能。


下载
----------

官方提供的下载渠道有两种: 
- [**Releases**](https://github.com/BiliXWhite/litematica-printer/releases)
- [**蓝奏云分流(密码cgxw)**](https://xeno.lanzoue.com/b00l1v20vi)

### 选择游戏版本

目前该模组支持以下游戏版本：
- 1.18.2
- 1.19.4
- 1.20.1
- 1.20.2
- 1.20.4
- 1.20.6
- 1.21(.1)
- 1.21.4
- 1.21.5
- 1.21.6/7/8
暂不接受1.18.2以下版本的更新，之间的小版本是否可用请自行尝试，一般版本进度会跟进上游分支


## 前置模组

该模组必须先安装 **Fabric API** , **MaLiLib** 和 **Litematica** 作为前置。可选前置有**Twrakeroo**,**Chest Tracker**(≤1.21.4)

## 特性

- **打印优化**
  - [x] 更流畅的打印体验
  - [x] 使用数据包打印功能（速度更快，出错率低，无幽灵方块）
  - [x] 可视化放置进度条
  - [x] 服务器卡顿检测，防止因卡顿导致的大量方块放置错误
  - [x] 不会因缺少水源而在迭代水时卡死不打印的 bug

- **功能改进**
  - [x] 填充功能（支持填充选区范围）
  - [x] 支持服务器快捷潜影盒功能（需服务器安装 AxShulkers 等插件）
  - [x] 替换珊瑚（使用活珊瑚打印投影内的死珊瑚）
  - [x] 删除已无法使用的破基岩功能

- **修复与支持**
  - [x] 修复以下方块的放置问题：
    - 合成器、拉杆、红石粉（非连接模式）
    - 枯叶、各种花簇的方向数量
    - 发光浆果、带花的花盆
    - 楼梯、藤蔓、缠怨藤、垂泪藤
    - 砂轮、门、活版门、漏斗、箱子
  - [x] 支持多达 48 种范围迭代逻辑

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
> 在中国大陆环境可能会导致支持库下载失败。请尝试使用**代理**进行下载。
> 如果您是 Windows 11 系统及以上，则无需开启代理的全局网卡模式，该项目配置自动识别代理。

常见问题
----------

## 推荐加入QQ群聊
- 毕竟不是人人都会在GitHub上反馈问题，您可以加入我们的QQ群聊，以便更好的反馈问题，获取更新和获得帮助。
[点击此处加入QQ群聊](https://qun.qq.com/universal-share/share?ac=1&authKey=7SiflHo922nbl3yp2YqgZ372t783ma)

## 为什么开启打印后，打印机不工作？
- 服务器装有反作弊插件，可能会导致打印机无法工作。
- `打印机工作间隔`设置过小，导致服务器无法及时响应，请尝试开启`使用数据包打印`功能打印或者调高`打印机工作间隔`。 
- 某些玄学问题，在开启正版验证的服务器里打印数据交互不正常。可尝试重新登陆游戏账号。（推荐使用[AuthMe](https://modrinth.com/mod/auth-me)模组） 

如果以上方法都无法解决问题，请尝试提交[Issue](https://github.com/BiliXWhite/litematica-printer/issues)，开发者会协助您解决问题。

## 为什么打印机放置的方块是错的？

1. 服务器装有反作弊插件，可能会导致打印机无法模拟看向放置。
2. 打印机工作间隔设置过小，服务器无法及时响应，导致方块出现错误。属于正常现象，请尝试增大`打印机工作间隔`的值。
3. 识别算法没有考虑到关于的方块，导致打印机无法正确识别方块类型。请提交[Issue](https://github.com/BiliXWhite/litematica-printer/issues)，表明什么方块出现错误。

## 快捷潜影盒功能无法使用？

1. 服务器未装有右键快捷打开潜影盒的插件(推荐使用AxShulkers)，无法使用快捷潜影盒功能。
2. 预设物品栏位置不足，无法使用快捷潜影盒功能。须在Litematica设置中设置好`pickBlockableSlots`值。如图所示：
![预设位置](预设位置.png)

快捷潜影盒仍处于测试阶段，可能会有一些问题，如果遇到问题请提交[Issue](https://github.com/BiliXWhite/litematica-printer/issues)。
