# HideArmor · 隐藏盔甲

无需卸下装备，即可隐藏头盔、胸甲、护腿和靴子的外观。**护甲值、附魔、耐久和保护效果不变，鞘翅始终显示。**

## 安装方法

1. 在 [Releases 下载页](https://github.com/JeiFing/HideArmor/releases) 下载与你的 **Minecraft 服务端版本**对应的 HideArmor JAR。
2. 安装必需前置 [PacketEvents](https://github.com/retrooper/packetevents/releases)，选择支持对应 Minecraft 版本的构建。
3. 将两个 JAR 放入服务器的 `plugins` 文件夹，**重启服务器**即可。

玩家无需安装客户端 Mod。Java 版可直接使用；基岩版可搭配 Geyser/Floodgate。

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

---

<sub>HideArmor is a Minecraft server plugin that hides armor appearances without affecting actual equipment attributes. Made by JeiFing.</sub>
