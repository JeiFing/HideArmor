# HideArmor · 隐藏盔甲

**简体中文** | [English](README_EN.md)

无需卸下装备，即可隐藏头盔、胸甲、护腿和靴子的外观。**护甲值、附魔、耐久和保护效果不变，鞘翅始终显示。**

## 安装方法

1. 在 [Releases 下载页](https://github.com/JeiFing/HideArmor/releases) 下载与你的 **Minecraft 服务端版本**对应的 HideArmor JAR。
2. 安装必需前置 [PacketEvents](https://github.com/retrooper/packetevents/releases)，选择支持对应 Minecraft 版本的构建。
3. 将两个 JAR 放入服务器的 `plugins` 文件夹，**重启服务器**即可。

玩家无需安装客户端 Mod。插件安装在 Paper/Purpur/Folia 服务端；基岩版玩家可通过 Geyser 连接后使用。为隐藏自己的盔甲且保留物品栏图标，请同时安装 Release 中的基岩配套映射和资源包。

## 使用方法

| 命令 | 效果 |
| --- | --- |
| `/hide all` | 隐藏全部盔甲外观 |
| `/hide none` | 显示全部盔甲外观 |
| `/hide 0101` | 隐藏头盔和护腿，显示胸甲和靴子 |

也可以输入任意四位 `0/1` 组合，例如 `/hide 1010`。

**四位数字依次对应：头盔 → 胸甲 → 护腿 → 靴子。** `1` 表示显示，`0` 表示隐藏；`0000` 等同于 `all`，`1111` 等同于 `none`。鞘翅不受隐藏设置影响。

权限节点：`hidearmor.use`（默认允许所有玩家使用）。显示设置会自动保存。

遇到问题？欢迎[提交 Issue](https://github.com/JeiFing/HideArmor/issues)。

## GSit 与基岩版

兼容 GSit 3.7.0 的坐下、躺下、抬腿、趴下、旋转和爬行动作。GSit 可选，未安装时保留普通隐藏盔甲功能。

基岩配套安装：把 `bedrock/hidearmor-v2.json` 放入 Geyser 的 `custom_mappings`，把配套 `.mcpack` 放入 Geyser 的 `packs`，然后重启代理。使用 Geyser 官方自定义物品映射，无需修改 Geyser 或客户端。映射只用于 HideArmor 的隐藏外观；真实装备和属性保持不变。自定义模型盔甲保留原有外观逻辑，鞘翅不隐藏。

## 构建

需要 Java 25、Gradle 9 和与目标 Minecraft 26.3 兼容的 PacketEvents、GSit 构建。将 GSit JAR 命名为 `libs/GSit-26.3.jar`；PacketEvents 可放入 `libs/PacketEvents-26.3.jar`，也可使用构建文件中的 Maven 前置。执行 `gradle test jar`。不在源码包内重分发第三方插件 JAR。

基岩资源的可编辑文件在 `bedrock/resource_pack`，映射在 `bedrock/hidearmor-v2.json`。
