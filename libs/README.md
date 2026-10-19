# PacketEvents compile dependency

For the 26.3 development build, place a 26.3-capable PacketEvents Spigot JAR at:

libs/packetevents-spigot-2.14.0-dev-26.3.jar

The verified test baseline is PacketEvents upstream PR #1582 while official 26.3 support is not yet released.
If the local JAR is absent, Gradle falls back to the CodeMC 2.14.0-SNAPSHOT coordinate.

PacketEvents remains compile-only and is not bundled in HideArmor.
