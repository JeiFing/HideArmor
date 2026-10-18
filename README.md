# HideArmor

Minecraft 服务端护甲外观隐藏插件。支持按槽位隐藏头盔、胸甲、护腿和靴子的**外观**，而不移除真实装备，不改变护甲属性、附魔、耐久和伤害计算。**鞘翅始终保持可见。**

> 当前最新维护版本：**HideArmor 1.0.1（Minecraft / Lophine 26.3）**。仓库的完整源码与历史 Release 附件尚在上传中，**现在不要将本仓库视为已经完成的下载发布页**。

## 功能

- 每位玩家独立设置头盔、胸甲、护腿、靴子四个槽位的显示/隐藏状态。
- 盔甲槽中的头颅、方块及其他物品外观也可隐藏；鞘翅除外。
- 只修改客户端收到的装备外观数据包，服务器保留真实装备。
- 设置按玩家 UUID 保存，重进服务器、重生和跨维度后仍可生效。
- 可配合 Geyser / Floodgate 服务 Java 及基岩玩家；Floodgate 为可选依赖。
- 无须在玩家客户端安装 Mod；与 Tweakeroo 自动切换胸甲/鞘翅的路径进行了专项兼容处理。

## 安装教程

**最新版 1.0.1 的构建目标为 Minecraft 26.3、Java 25、Lophine 26.3。** 源码声明支持 Paper、Purpur、Folia，但其他核心组合仍需实测，不能仅凭声明保证完全兼容。

1. 从 [PacketEvents 官方发布页](https://github.com/retrooper/packetevents/releases) 下载支持目标 Minecraft 版本的服务端插件。对于 26.3，应使用具备该版本协议支持的 PacketEvents 版本。
2. 下载与服务端 Minecraft 版本匹配的 HideArmor JAR。
3. **停止服务器**，将 PacketEvents 与 HideArmor 两个 JAR 放入 `plugins/` 文件夹；同一插件不要放多个不同版本。
4. 正常启动服务器，检查日志确认 PacketEvents 和 HideArmor 成功启用，且无缺失依赖与类加载错误。
5. 进入游戏，使用 `/hide all` 及 `/hide none` 测试，最好让另一位玩家观察第三人称显示。

不支持通过插件管理器热重载；更换版本应正常重启。Floodgate 是可选依赖，用于更可靠地识别基岩玩家。

## 命令与参数

普通玩家默认具有 `hidearmor.use` 权限。所有命令只修改**自己**的显示状态。

| 命令 | 效果 |
| --- | --- |
| `/hide all` 或 `/hide 0000` | 隐藏四件护甲外观，鞘翠除外 |
| `/hide none` 或 `/hide 1111` | 显示所有护甲外观 |
| `/hide 0001` | 仅显示靴子 |
| `/hide 1000` | 仅显示头盔 |
| `/hide 0101` | 显示胸甲和靴子 |
| `/hide 1010` | 显示头盔和护腿 |

四位参数从左至右依次为 **头盔 → 胸甲 → 护腿 → 靴子**，`1` 代表显示，`0` 代表隐藏。命令支持 Tab 补全。

## 保存与权限

- 权限节点：`hidearmor.use`（默认 `true`）。
- 玩家配置保存在 `plugins/HideArmor/hidden-players.yml`，与版本替换前应备份；该文件包含玩家 UUID，请勿公开。
- 管理员从独立的真实背包/容器检查插件查看装备时，仍可看到玩家实际穿着的物品。

## 注意事项

HideArmor 不是取消护甲或修改装备数值的插件。鞘翅不会隐藏。旧协议客户端、特殊服务端分支、协议转换链、其他装备数据包插件可能影响外观显示。各历史 JAR 的兼容性应按其 `plugin.yml` 和实际服务器版本分别判断，不把最新版的能力外推到所有旧版本。

## 项目源码与历史

最新版维护源码采用 Gradle + Java。旧版 1.0.0 曾分别构建适配 Minecraft 1.19–26.2；历史 JAR 归档会独立提供，且只在新版主分支保留最新源码。由于旧编译历史的精确日期没有可核实的 Git 记录，GitHub 归档发布时间不代表最初编译日期。

作者：**JeiFing**。代码公开可读；当前尚未为第三方授予明确的开源许可。PacketEvents、Paper/Lophine 等依赖保留各自许可与版权。

---

### English description

**HideArmor** is a Minecraft server-side plugin that hides selected armor slots visually while preserving actual equipment stats, enchantments and durability. It supports per-slot `/hide` commands, integrates with PacketEvents, and provides compatibility paths for Paper/Purpur/Folia and Geyser/Floodgate. Elytra remains visible. The latest maintained source targets Minecraft 26.3 and Java 25.
