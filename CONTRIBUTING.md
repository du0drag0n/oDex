# Contributing to oDex

Thanks for taking a look. oDex is a free, open-source Pokédex made in spare time, so small focused changes are easiest to
review.

## Before you start

- **Bugs and ideas:** open an issue first for anything bigger than a typo, so we can agree on it before you spend time.
- **Game data** (stats, moves, locations, item and place numbers) comes from the openDex API
  (`https://api.opendex.lemoncore.io/v1/game-data`) through `tools/seedgen`, not from hand-edited files. If something is
  wrong or missing, say so in an issue rather than patching `assets/seed/*.json`.
- **Save formats** are worked out from the community's documentation (such as Project Pokémon and the pret
  decompilations) and checked against real saves. Please don't copy code or tables out of other save editors, and never
  attach a real save to an issue: they hold trainer names and IDs.

## Setting up

You need JDK 17+ and the Android SDK (compileSdk 37). Then:

```bash
./gradlew :app:testDebugUnitTest   # unit tests, including the data and save-reader tests
./gradlew :app:assembleDebug       # installs as io.lemoncore.odex.debug, beside a release build
```

The README explains how to regenerate the seed and how to add a game.

## Pull requests

- Keep each pull request to one change, and say what you tested and on what.
- Add or update a test where there is one to add: save readers have fake-save tests in `app/src/test`.
- Match the code around yours: comment density, naming and idiom.
- By contributing you agree your work is licensed under the GPL-3.0, like the rest of oDex.

## Translations

Interface strings live in `app/src/main/res/values*/strings.xml`. Native speakers are very welcome to correct them.
