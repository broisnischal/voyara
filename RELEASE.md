# Releasing Voyara to Google Play

## What ships

Play takes an **Android App Bundle** (`.aab`), not an APK. Play splits the bundle
into per-device APKs itself, which is why the bundle is 22 MB and the universal
APK is 50 MB.

| File | Use |
| --- | --- |
| `dist/voyara-1.0-vc2-release.aab` | Upload this to Play Console |
| `dist/voyara-1.0-vc2-release.apk` | Sideload this to smoke-test the exact release build |
| `dist/play-assets/icon-512.png` | Store listing app icon |
| `dist/play-assets/feature-graphic-1024x500.png` | Store listing feature graphic |

## Signing

The upload key lives outside the repo at `~/.android-keystores/voyara-upload.jks`,
alias `voyara-upload`, RSA 4096, valid until 2054. `keystore.properties` at the repo
root points `app/build.gradle.kts` at it and is gitignored, as is `*.jks`.

**Back up both files somewhere I will still have in five years.** Play Console
signs the shipped APKs with its own app signing key, so a lost upload key is
recoverable through a key reset request, but a lost key plus a lost account is not.
Never commit either file.

Rebuild after changing signing config:

```sh
JAVA_HOME=/home/nees/.local/share/mise/installs/java/temurin-21.0.12+101.0.LTS \
  ./gradlew bundleRelease
```

Output lands at `app/build/outputs/bundle/release/app-release.aab`.

Confirm the right key signed it:

```sh
keytool -printcert -jarfile app/build/outputs/bundle/release/app-release.aab | grep Owner
```

It should print `CN=Voyara, OU=Voyara, O=Voyara, ...`. If it prints
`CN=Android Debug`, `keystore.properties` was not found and the build fell back to
the debug key. Play rejects debug-signed bundles.

## Build prerequisites

- JDK 21 (Gradle 9.7.1 and AGP 9.4.1 do not accept the system JDK 26)
- Android SDK Platform 37.0 and Build-Tools 37.0.0
- `local.properties` with `sdk.dir=/home/nees/Android/Sdk`
- Optional: `TOMTOM_API_KEY` in `local.properties`. Without it the map falls back
  to OpenFreeMap tiles.

## Play Console, first submission

1. **Create the developer account** at <https://play.google.com/console>. One-time
   $25 fee. Individual accounts need ID verification and, since 2023, a D-U-N-S
   number is required for organisation accounts only.
2. **Create the app.** App name `Voyara`, default language, type *App*, *Free*.
3. **Set up app signing.** Under *Release → Setup → App signing*, accept Play App
   Signing. Play generates the app signing key; the keystore above stays the
   *upload* key.
4. **Start with a closed test.** *Release → Testing → Closed testing → Create new
   release*. Upload `dist/voyara-1.0-vc2-release.aab`. An individual developer
   account has to run a closed test with a minimum tester count opted in for 14
   continuous days before the app can reach production. The console shows the
   current threshold on the track page (12 when I wrote this). Starting this
   track early is what unblocks the launch date.
5. **Fill the content declarations.** Play blocks the release until every one is
   green:
   - Privacy policy URL. Required, because the app requests location.
   - Data safety form. Voyara collects location and keeps it on device; declare
     what the app actually does with it.
   - Ads, content rating questionnaire, target audience, news app, COVID-19
     tracing, government app, financial features.
   - Permission declarations. Foreground `ACCESS_FINE_LOCATION` needs no separate
     form. `ACCESS_BACKGROUND_LOCATION` would, and Voyara does not request it.
6. **Store listing.** Short description (80 chars), full description (4000),
   `icon-512.png`, `feature-graphic-1024x500.png`, and at least 2 phone
   screenshots between 320 px and 3840 px on the long edge. Screenshots are the one
   asset I still have to capture from a real device.
7. **Countries and pricing**, then *Send for review*. First review typically runs
   a few days and can take longer for a new account.
8. **Promote to production** once the closed test has served its 14 days.

## Before uploading anything

Install the release APK on a real device and use it:

```sh
adb install -r dist/voyara-1.0-vc2-release.apk
```

The release build runs R8 with `isMinifyEnabled` and `isShrinkResources` on, which
debug builds do not. Reflection-heavy paths in MapLibre and OkHttp are where a
missing keep rule shows up, and it shows up as a runtime crash, not a build error.
The debug build passing proves nothing about this.

## Policy risk worth knowing up front

Voyara is a mock-location app. Play permits them, and several ship today, but two
policy lines decide how review goes:

- **Device and Network Abuse.** The listing has to describe the app as a developer
  and testing tool for simulating location. Marketing it for spoofing another
  app's location checks, and games in particular, is what gets apps pulled.
- **Deceptive behaviour.** Mocking only works when the user turns it on themselves
  in *Developer options → Select mock location app*. Saying that plainly in the
  listing helps, because it shows the app cannot spoof anything silently.

Write the store description around route simulation, GPX playback, and location
testing. That is what the app is.

## Package name

`applicationId` is `click.stroke.voyara`, matching the app created in Play Console.
It is permanent: Play identifies the app by it, and it cannot be changed after the
first upload without shipping a different app.

`namespace` stays `app.voyara`. That is only the Kotlin package for the generated
`R` and `BuildConfig` classes and has nothing to do with the Play identity, so the
source tree did not move.

## Shipping an update

Bump `versionCode` in `app/build.gradle.kts` for every upload. Play burns a version
code permanently the moment a bundle carrying it is uploaded, and deleting the
draft release does not give it back, so a code is never reusable. `versionName` is
the string users see and can stay put across several codes.

Currently at `versionCode = 2`, because 1 was consumed by the first upload attempt.

```kotlin
versionCode = 2
versionName = "1.1"
```
