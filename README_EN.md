# HideArmor · Hide Armor

[简体中文](README.md) | **English**

Hide the appearance of your helmet, chestplate, leggings, and boots without removing them. **Armor stats, enchantments, durability, and protection remain unchanged. Elytra always stays visible.**

## Installation

1. Download the HideArmor JAR that matches your **Minecraft server version** from [Releases](https://github.com/JeiFing/HideArmor/releases).
2. Download and install the required [PacketEvents](https://github.com/retrooper/packetevents/releases) plugin, using a build compatible with your Minecraft version.
3. Put both JAR files in your server's `plugins` folder and **restart the server**.

Players don't need client-side mods. Install HideArmor on your Paper/Purpur/Folia server; Bedrock players can use it when connecting through Geyser.

## Usage

| Command | Result |
| --- | --- |
| `/hide all` | Hide all armor appearances |
| `/hide none` | Show all armor appearances |
| `/hide 0101` | Hide helmet and leggings; show chestplate and boots |

You can also use any four-digit `0/1` combination, such as `/hide 1010`.

**The four digits represent: Helmet → Chestplate → Leggings → Boots.** `1` means visible, `0` means hidden. `0000` is the same as `all`; `1111` is the same as `none`. Elytra is never hidden.

Permission: `hidearmor.use` (enabled for all players by default). Your visibility settings are saved automatically.

Having a problem? [Open an Issue](https://github.com/JeiFing/HideArmor/issues).
