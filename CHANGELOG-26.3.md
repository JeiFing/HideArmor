# HideArmor 1.0.1 / Minecraft 26.3

- Updated Lophine API target to 26.3.
- Updated plugin api-version to 26.3.
- Updated protocol tests to 26.3.
- Removed the PacketEvents 2.13.0 EQUIPPABLE slot-id workaround; 26.3-capable PacketEvents resolves component slot ids correctly.
- Runtime still requires PacketEvents; until an official 26.3 release is available, the verified test baseline is upstream PR #1582.
