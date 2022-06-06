Litematica Printer
==================
This fork adds printing functionality for Litematica fabric 1.18 and 1.17 versions. Printer allows players to build
big structures more quickly by automatically placing the correct blocks around you.

The main branch (printing) is dedicated to latest version of Minecraft, while printing_1.17 and printing_1.16 are
for the older versions respectively. If you have issues with the printer, **do not** bother the original creator of
Litematica (maruohon) with them. Contact me instead. Feature requests or bugs can be reported via github issues.

For downloads check out [releases](https://github.com/aleksilassila/litematica-printing/releases/latest).
To install the mod, first download the original Litematica and MaLiLib from [here](https://www.curseforge.com/minecraft/mc-mods/litematica).
You will also need [Fabric API](https://www.curseforge.com/minecraft/mc-mods/fabric-api/).
Finally, move the printer's .jar from [releases](https://github.com/aleksilassila/litematica-printing/releases/latest) to your mods folder.

![Demo](printer_demo.gif)

How To Use
----------
Using the printer is straightforward: You can toggle the feature by pressing `CAPS_LOCK` by default. To configure variables such as
printing speed and range, open Litematica's settings by pressing `M + C` and navigate to "Generic" tab. Printer's configuration can be
found at the bottom of the page. You can also rebind the printing toggle under "Hotkeys" tab. Holding down `V` by default will also
print regardless if the printer is toggled on or off.

### List of blacklisted blocks
These blocks have not been implemented yet for various reasons and the printer will skip them instead of placing them wrong. If any
other blocks are placed incorrectly, try to lower the printing speed. If certain block is still placed incorrectly, you can create
[an issue](https://github.com/aleksilassila/litematica-printer/issues).
 - Grindstones
 - Skulls placed on the ground
 - Signs
 - Glow lichen and vines
 - Entities, including item frames and armor stands

----------

[![Curseforge](http://cf.way2muchnoise.eu/full_litematica_downloads.svg)](https://minecraft.curseforge.com/projects/litematica) [![Curseforge](http://cf.way2muchnoise.eu/versions/For%20MC_litematica_all.svg)](https://minecraft.curseforge.com/projects/litematica)

# Litematica
Litematica is a client-side schematic mod for Minecraft, with also lots of extra functionality
especially for creative mode (such as schematic pasting, area cloning, moving, filling, deletion).

It's primarily developed on MC 1.12.2 for LiteLoader. It has also been ported to Rift on MC 1.13.2,
and for Fabric on MC 1.14 and later. There are also Forge versions for 1.12.2, and Forge ports for 1.14.4+
are also planned, but Forge will need to start shipping the Mixin library before those can happen.

Litematica was started as an alternative for [Schematica](https://minecraft.curseforge.com/projects/schematica),
for players who don't want to have Forge installed on their client, and that's why it was developed for Liteloader.

For compiled builds (= downloads), see:
* CurseForge: http://minecraft.curseforge.com/projects/litematica
* For more up-to-date development builds: https://masa.dy.fi/mcmods/client_mods/
* **Note:** Litematica also requires the malilib library mod! But on the other hand Fabric API is not needed.

## Compiling
* Clone the repository
* Open a command prompt/terminal to the repository directory
* On 1.12.x you will first need to run `gradlew setupDecompWorkspace`
  (unless you have already done it once for another project on the same 1.12.x MC version
  and mappings and the same mod loader, Forge or LiteLoader)
* Run `gradlew build` to build the mod
* The built jar file will be inside `build/libs/`

## YourKit
![](https://www.yourkit.com/images/yklogo.png)

We appreciate YourKit for providing the project developers licenses of its profiler to help us improve performance! 

YourKit supports open source projects with innovative and intelligent tools
for monitoring and profiling Java and .NET applications.
YourKit is the creator of [YourKit Java Profiler](https://www.yourkit.com/java/profiler/),
[YourKit .NET Profiler](https://www.yourkit.com/.net/profiler/) and
[YourKit YouMonitor](https://www.yourkit.com/youmonitor), tools for profiling Java and .NET applications.
