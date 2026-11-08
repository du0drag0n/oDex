# Releasing oDex

One signing key, one package name and one rising `versionCode` serve Google Play, GitHub releases and Obtainium, so
people can move between them and keep their data.

## The signing key

The v1 key lives at `~/.odex-signing/odex-app-key.jks`, with its alias and password in `~/.gradle/gradle.properties`.
Back both up somewhere safe: if they're lost, nobody who installed from GitHub can update again. Its certificate's
SHA-256 is in the README. To make a new one (only before anything is published):

```bash
keytool -genkeypair -v -keystore odex-app-key.jks -alias odex -keyalg RSA -keysize 4096 -validity 10000
```

Tell Gradle where it is in `~/.gradle/gradle.properties` (your user folder, not this repository), or with
`ODEX_SIGNING_STOREFILE`, `ODEX_SIGNING_STOREPASSWORD`, `ODEX_SIGNING_KEYALIAS` and `ODEX_SIGNING_KEYPASSWORD`:

```properties
odex.signing.storeFile=C:/path/to/odex-app-key.jks
odex.signing.storePassword=...
odex.signing.keyAlias=odex
odex.signing.keyPassword=...
```

Without them a release build comes out unsigned, never signed with the debug key.

### Giving the same key to Google Play

In Play Console, App integrity → Play app signing, choose to **export and upload a key from a Java keystore** and follow
the steps there (they use Google's `pepk.jar` to wrap your key). Play then signs what it serves with your key, so the
GitHub APK and the Play install have the same signature. Do this before the first release goes to a track: afterwards
the key can only be changed through Play's upgrade-key process. Also set a separate **upload key** for your bundles.

## Version plan

- **0.9.0** (versionCode 1) is the closed-testing build: Play's 12 testers for 14 days, and GitHub as a pre-1.0 release.
- **1.0.0** (versionCode 2 or higher) follows once Play grants production access.
- For each release raise `versionCode` by at least 1: Play refuses one it has seen.

## Screenshots without artwork

`./gradlew assembleDebug -Podex.silhouettes=true` draws every Pokémon as a flat shape, so store and README screenshots
don't reproduce Nintendo's artwork. Import public sample saves rather than your own for these.

## Building

```bash
./gradlew assembleRelease bundleRelease -Podex.kofiUrl=
```

- `app/build/outputs/bundle/release/app-release.aab` goes to Play.
- `app/build/outputs/apk/release/app-release.apk` goes on the GitHub release and is what Obtainium reads.
- `-Podex.kofiUrl=` hides the Ko-fi button. Leave it off the APK if you want the button on GitHub's build only; the
  Play build should never have it (see below).
- Raise `versionName` and `versionCode` in `app/build.gradle.kts` for every release. Play refuses a `versionCode` it has
  seen, and Obtainium compares `versionName`.
- Check the APK before uploading: `apksigner verify --print-certs`, and `zipalign -c -P 16 4` for 16 KB alignment (Play
  requires it for apps targeting Android 15+).

## Play Console

- **Testing first:** a personal developer account made after 13 November 2023 needs a closed test with at least 12
  testers opted in for 14 days before it can apply for production.
- **Target API:** new apps and updates must target API 36 from 31 August 2026. oDex does.
- **Donations:** Google's payments policy has stopped apps over in-app links to donation pages that aren't tax-exempt
  organisations, so build Play without the Ko-fi link. The Support screen's website row still opens the openDex site's
  support page; check that page before submitting.
- **Developer verification:** Android is starting to block apps from unregistered developers outside the Play Store.
  Register `io.lemoncore.odex`'s signing key in Play Console so GitHub installs stay installable.

## Store listing (draft)

**Name:** oDex

**Short description (80):** A Pokédex that tracks your own games, saves and catches. Free, no ads.

**Full description:**

oDex is a Pokédex for tracking your playthroughs game by game, from Red and Blue to Scarlet and Violet and Legends: Z-A.

- Every game's own Pokédex, 1,025 Pokémon and the forms that matter, with that game's types, abilities, moves, evolutions and locations.
- Keep a separate caught checklist for each copy of a game you own, including emulator copies.
- Import a save file from Generation I to VII, Let's Go and Brilliant Diamond and Shining Pearl: caught Pokémon, party, boxes, badges and play time. Link a save and oDex updates it each time you open the app.
- Plan a playthrough with catch lists, track shinies, build teams and see type matchups.
- Works offline once Pokémon pictures have loaded. Nine languages.
- No ads, no account, nothing locked.

oDex is an unofficial fan project, not affiliated with or endorsed by Nintendo, Creatures Inc., GAME FREAK inc. or The Pokémon Company.

## Data safety answers

These describe what the app does today. Check them against the live privacy page before submitting.

- Data collected or shared with others: none. oDex has no account, analytics, crash reporting or ads.
- Your checklists, teams, imported saves and settings stay on the device. Android's own backup may copy them to the user's Google account.
- Network: the app downloads Pokémon pictures and cries from the openDex API over HTTPS, which receives the device's IP address like any web request. If Play's form counts an IP address held only in short-lived logs, declare it as collected for app functionality and not shared.
- Data is encrypted in transit. Users can delete their data by deleting copies in the app or uninstalling.
- Permissions: Internet only. Save files are opened through Android's file picker.
- Content rating: answer the IARC questionnaire honestly; oDex has no violence, chat, purchases or user-generated content.
