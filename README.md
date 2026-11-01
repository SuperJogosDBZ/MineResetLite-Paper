# MineResetLite

Fork of [jjkoletar/MineResetLite](https://github.com/jjkoletar/MineResetLite) with the following changes:

- Updated for modern Paper, starting with 1.21.11.
- Replaced legacy block handling with modern block names and states, including support for TeamVK entries such as `STONE:0`.
- Fixed mine selection, name matching, composition updates, air distribution, reset warnings, teleportation, and saving issues.
- Optimized resets for single-material mines and empty compositions, and limited player checks to the mine's world.
- Added `/mrl stagger <spacing seconds> <interval minutes>` to distribute automatic resets for matching mines.
- Revised messages and command help in all eight bundled languages, standardized formatting, and added UTF-8 support for custom messages.
- Updated optional WorldEdit/FAWE selection integration.
- Removed obsolete code, legacy metrics, and the update checker.
