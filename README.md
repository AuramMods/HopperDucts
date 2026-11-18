# Hopper Ducts

Hopper Ducts v1.5 by FyberOptic, ported to Minecraft 1.20.1 on Forge. Ducts move items in any direction. Grated hoppers use five real items as filters for intake from above and for their output.

- Place a duct next to another duct or hopper to auto-orient it. Sneak to use the clicked face instead. Right-click with a stick to cycle its output backward. In survival, left-click with a stick to cycle it forward (backward while sneaking).
- Craft four ducts from three iron ingots and a plank. Craft a grated hopper from a hopper and iron bars.
- Change `cooldownTime` in the world's `serverconfig/hopperducts-server.toml` to adjust the default eight-tick transfer interval.

## Build

Use JDK 17 and run `./gradlew build` (`gradlew.bat build` on Windows). The reobfuscated mod is written to `build/libs/hopperducts-1.5-1.20.1.jar`.

Run `./gradlew runGameTestServer` to exercise item transfer, filtering, redstone, and persistence on a headless Forge server.

The [original release README](src/main/resources/README.txt) documents the 1.12-era mod. The reconstructed Forge 1.12.2 source is available on the [1.12.2 branch](https://github.com/AuramMods/HopperDucts/tree/1.12.2).
