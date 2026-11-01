# MineResetLite

Changes compared with the original [jjkoletar/MineResetLite](https://github.com/jjkoletar/MineResetLite):

- Updated for modern Paper, starting with 1.21.11; tested on 1.21.11 and 26.3.
- Modernized block handling and added support for reading TeamVK composition and surface entries such as `STONE:0`.
- Fixed composition updates, air distribution, reset-warning validation, and saving after removing composition entries or clearing warnings.
- Preserved loaded mine filenames when saving and deleting, reported failed deletions, and grouped pending saves without repeatedly postponing them.
- Optimized resets for a single block at 100%, filled empty compositions directly with air, reused block lookups, and limited player checks to the mine's world.
- Fixed mine creation with partially similar names, selection handling, and filename conflicts.
- Preserved player direction during reset teleportation, stopped block changes when a teleport is cancelled, and corrected local reset announcements.
- Added `/mrl stagger <spacing seconds> <interval minutes>` to distribute resets for matching mines; extra mines share time slots, and other intervals stay unchanged.
- Revised messages and command help in all eight bundled languages, standardized the `&8[&4MRL&8]` prefix and status colors, and limited displayed percentages to two decimal places without changing stored precision. Custom messages are read as UTF-8.
- Updated optional WorldEdit/FAWE integration for cuboid selections. Resets use Bukkit/Paper, independently of these integrations.
- Simplified configuration handling and cleaned up player selections on disconnect.
- Removed obsolete code, legacy metrics, and the update checker.
- Validated commands, automatic timers, broadcasts, persistence, and migration from older mine files on local Paper servers.

Build with Java 21 and `mvn package`. The plugin JAR is generated in `target/`.

Resets remain synchronous; large mines can cause pauses. WorldEdit 7.4.2 was tested; FAWE was not tested. Custom message overrides take precedence over bundled messages.
