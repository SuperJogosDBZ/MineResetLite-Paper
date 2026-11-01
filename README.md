# MineResetLite

Modern Paper port of [jjkoletar/MineResetLite](https://github.com/jjkoletar/MineResetLite).

- Supports Paper 1.21.11 and newer; tested on 1.21.11 and 26.3.
- Supports modern block names and TeamVK zero-data entries such as `STONE:0`.
- Fixes mine selection, composition, reset warnings, teleportation, and saving issues.
- Optimizes mine resets and adds `/mrl stagger <spacing seconds> <interval minutes>` to distribute automatic resets for mines with the specified interval. Extra mines share time slots.
- Updates messages and command help in eight languages, with UTF-8 support for custom messages.
- Uses optional WorldEdit/FAWE cuboid selections. Resets use Bukkit/Paper; WorldEdit 7.4.2 was tested, FAWE was not.
- Removes legacy metrics and the update checker.

Build with Java 21 and `mvn package`. The JAR is generated in `target/`.

Resets are synchronous; large mines can cause pauses.
