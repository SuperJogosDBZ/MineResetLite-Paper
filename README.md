# MineResetLite

Changes compared with the original [jjkoletar/MineResetLite](https://github.com/jjkoletar/MineResetLite):

- Updated for modern Paper, starting with 1.21.11; tested on 1.21.11 and 26.3.
- Modernized block handling and added support for reading TeamVK composition and surface entries such as `STONE:0`.
- Fixed saving after removing composition entries and corrected delayed-save task handling.
- Optimized resets for a single block at 100%, reused block lookups, and limited player checks to the mine's world.
- Updated optional WorldEdit/FAWE selection integration.
- Removed obsolete code, legacy metrics, and the update checker.
- Validated functionality and persistence on local Paper servers.
