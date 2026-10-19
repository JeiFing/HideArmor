# HideArmor

支持 Paper、Purpur 与 Folia 的护甲外观隐藏插件，构建目标为 Lophine 26.3、Java 25 与支持 26.3 的 PacketEvents 构建。

## 功能

- `/hide all` 或 `/hide 0000`：隐藏头盔、胸甲、护腿和靴子槽中的全部物品外观。
- `/hide none` 或 `/hide 1111`：显示全部装备外观。
- `/hide 0001` 等四位二进制参数依次控制头盔、胸甲、护腿和靴子，`1` 为显示，`0` 为隐藏。
- 分槽显示状态按 UUID 保存，重新登录、重生、传送、跨维度和服务器重启后继续生效。
- 盔甲槽中的生物头颅、方块和其他任意物品均可隐藏；只有鞘翅始终可见，主副手与其他实体装备槽不受影响。
- 插件只改写发往客户端的装备外观包，不修改服务器中的真实物品、属性、伤害计算或耐久消耗。
- 每次修改显示设置都会读取玩家当时真正穿着的装备，不缓存旧装备。
- 兼容 Tweakeroo 0.29.2 的自动切换鞘翅与低耐久物品功能；Shift+左键、数字键交换与拖拽换装后会在下一刻刷新本人外观。
- `/hide` 的成功提示会按固定槽位顺序列出隐藏与显示的装备，隐藏胸甲时明确标注“不含鞘翅”。

## 客户端表现与边界

Java 1.21.5 及以上客户端会尽量收到非空、可正常交换、但使用原版空人形装备图层的临时视图。物品类型、数量、耐久、附魔、属性、背包图标和悬停信息保持不变，因此本人第三人称、Tweakeroo Freecam 和其他观察者看不到所选槽位中的装备，同时本人的护甲值和自动换装判断仍然正常。没有可装备组件的任意物品仍会在实体外观包中隐藏，鞘翅不会被改写。

Tweakeroo 的自动鞘翅实现本身要求鞘翅先位于玩家背包、快捷栏或副手中的一个槽位，三次交换后会把换下的胸甲放回鞘翅原先的槽位；快捷栏第一格只作为临时缓冲，原物会在第三次交换后归位，因此不要求额外预留空格。插件不能从容器、末影箱或背包外自动取出鞘翅。

隐藏期间，发给 Java 本人的背包护甲副本会携带可逆的私有客户端标记。创造模式把完整物品发回服务器前，插件会还原真实装备材质并移除标记，避免临时外观组件进入服务器背包、掉落物或其他容器。执行 `/hide none` 或 `/hide 1111` 时会重新发送完整真实背包和当前真实装备。

基岩玩家通过 Floodgate 缓存与 UUID 形状回退进行识别，本人继续使用 Geyser 可分离容器与实体装备的隐藏方式；其他 Java、基岩观察者仍只收到空外观。旧于 1.21.5 的 Java 客户端不支持当前装备组件，会回退到旧的空外观兼容方式。

其他玩家只收到实体外观包，不会看到隐藏护甲。InvSee++ 使用独立的真实容器数据，因此管理员仍能看到并按原权限编辑目标的真实护甲。

Geyser 会立即把装备外观包转换为基岩版护甲更新；BedrockFreecam 1.0.2 的假身体只在开启 `/fc` 时复制一次装备，不修改该扩展时应先等待隐藏生效再开启 `/fc`，装备变化后关闭并重新开启 `/fc` 才会刷新假身体。

不支持使用插件管理器热重载；正常重启服务器会让客户端清除全部伪装状态并由插件重新下发。

## 构建

```bash
gradle clean test build
```

源码目录若存在 `libs/packetevents-spigot-2.14.0-dev-26.3.jar`，或其上级目录存在 `build-deps/packetevents-spigot-2.14.0-dev-26.3.jar`，构建会优先使用本地依赖，否则 Gradle 从 CodeMC 获取 PacketEvents 编译依赖。PacketEvents 是 `compileOnly`，不会打入插件 JAR。

产物位于 `build/libs/HideArmor-1.0.1-MC-26.3.jar`。

## 运行依赖

- Java 25
- Lophine 26.3
- PacketEvents：需要包含 Minecraft 26.3 支持的构建（测试基线为上游 PR #1582）

Floodgate 为可选软依赖，只用于可靠区分基岩玩家；未安装时会使用 Floodgate UUID 形状回退，不会阻止插件启动。


## 26.3 compatibility changes

- Plugin/API target updated to 26.3.
- Removed the old PacketEvents 2.13.0 EQUIPPABLE slot-id compensation because the 26.3-capable PacketEvents code uses explicit component slot ids correctly.
- Unit tests now exercise the 26.3 protocol version.
