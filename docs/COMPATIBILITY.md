# 历史版本与兼容包索引

## 如何选择

- 以**服务端实际 Minecraft 版本**选择名称匹配的 JAR；文件名中的 `MC-*` 是当时的构建目标，不是所有新旧版本均互通的保证。
- `v1.0.0` 保存原历史文件夹中所有对应版本的原始 JAR（字节不作修改）。不同文件名即使 SHA-256 相同，也按原文件名提供。
- `v1.0.1` 是面向 Minecraft / Lophine 26.3 的最新源码版本。原存档 `HideArmor-26.3.jar` 与 `HideArmor-1.0.1-MC-26.3.jar` 的内容哈希相同。
- PacketEvents 是所有版本的必需前置，需选**与你的服务端版本相匹配**的 PacketEvents。Minecraft 26.3 推荐 PacketEvents 2.14.0 或明确声明支持 26.3 的更高兼容版本。
- 旧构建只验证了其内置 `plugin.yml` 的元数据和 ZIP 完整性，**未逐一在所有版本执行服务器启动及真人游戏测试**。
- `folia-supported` 由各 JAR 的 `plugin.yml` 决定，不要把本仓库最新版本的说明直接套用到所有老版本。

| 版本系列 | 发布包 | 注意事项 |
| --- | --- | --- |
| 1.19 / 1.19.1–1.19.4 | `v1.0.0` | 此系列声明 `folia-supported: false` |
| 1.20 | `v1.0.0` | Minecraft 1.20 构建声明 `folia-supported: false` |
| 1.20.1–1.20.6 | `v1.0.0` | 元数据声明 `folia-supported: true` |
| 1.21 / 1.21.1–1.21.11 | `v1.0.0` | 含 `1.21.2-compat` 特别标记构建 |
| 26.1.2 / 26.2 | `v1.0.0` | 依赖对应 Minecraft 协议能力的 PacketEvents |
| 26.3 | `v1.0.1` | 当前源码仅适配 26.3、Java 25、PacketEvents 26.3 协议 |

## 所有历史 JAR（v1.0.0）

| 下载文件名 | SHA-256（前 12 位） |
| --- | --- |
| `HideArmor-1.0.0-MC-1.19.1.jar` | `1b9a676571c7` |
| `HideArmor-1.0.0-MC-1.19.2.jar` | `1b9a676571c7` |
| `HideArmor-1.0.0-MC-1.19.3.jar` | `1b9a676571c7` |
| `HideArmor-1.0.0-MC-1.19.4.jar` | `1b9a676571c7` |
| `HideArmor-1.0.0-MC-1.19.jar` | `1b9a676571c7` |
| `HideArmor-1.0.0-MC-1.20.1.jar` | `8b1c67e47d4a` |
| `HideArmor-1.0.0-MC-1.20.2.jar` | `8b1c67e47d4a` |
| `HideArmor-1.0.0-MC-1.20.3.jar` | `8b1c67e47d4a` |
| `HideArmor-1.0.0-MC-1.20.4.jar` | `8b1c67e47d4a` |
| `HideArmor-1.0.0-MC-1.20.5.jar` | `be67c18c601f` |
| `HideArmor-1.0.0-MC-1.20.6.jar` | `be67c18c601f` |
| `HideArmor-1.0.0-MC-1.20.jar` | `09edef59801f` |
| `HideArmor-1.0.0-MC-1.21.1.jar` | `bcefb60fdc3c` |
| `HideArmor-1.0.0-MC-1.21.10.jar` | `f9b5003af5d4` |
| `HideArmor-1.0.0-MC-1.21.11.jar` | `f12fa7f77085` |
| `HideArmor-1.0.0-MC-1.21.2-compat.jar` | `94e633e4e765` |
| `HideArmor-1.0.0-MC-1.21.3.jar` | `516e8bba09c2` |
| `HideArmor-1.0.0-MC-1.21.4.jar` | `70f06e8f461a` |
| `HideArmor-1.0.0-MC-1.21.5.jar` | `cde6c57097b5` |
| `HideArmor-1.0.0-MC-1.21.6.jar` | `abd902dd230b` |
| `HideArmor-1.0.0-MC-1.21.7.jar` | `9aab2c577a2f` |
| `HideArmor-1.0.0-MC-1.21.8.jar` | `563e69613388` |
| `HideArmor-1.0.0-MC-1.21.9.jar` | `3e008b38fa9f` |
| `HideArmor-1.0.0-MC-1.21.jar` | `4c5c0746b329` |
| `HideArmor-1.0.0-MC-26.1.2.jar` | `0944694f1c96` |
| `HideArmor-1.0.0-MC-26.2.jar` | `76b7a6499911` |

## 校验

完整 SHA-256 见 `docs/SHA256-v1.0.0.txt` 与 `docs/SHA256-v1.0.1.txt`，Release 附件也提供各自清单。使用 SHA-256 核对下载文件与清单一致，不依靠文件名或体积猜测兼容性。
