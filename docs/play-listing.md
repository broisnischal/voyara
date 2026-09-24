# Play Console: what goes in every field

Paste-ready copy for Voyara's first submission. Character limits in brackets are
Play's hard caps.

---

## Release (Closed testing → Create new release)

**App bundle**: `dist/voyara-1.0-vc2-release.aab`

**Release name** [50]. Internal only, never shown to users. Play prefills it from
the bundle and the default is correct:

```
1.0 (2)
```

The convention is `versionName (versionCode)`. Naming a release after its content
("first closed test") is also fine; I just have to be able to tell releases apart
in the console six months from now.

**Release notes** [500 per language]. These *are* shown to users, inside the
`<en-US>` tags Play prefills. For a first release there is nothing to compare
against, so describe the app rather than the diff:

```
<en-US>
First release.

Simulate a location anywhere on the map, follow a route along real roads, replay a
GPX track, or roam within a radius. Walk, cycle and drive speeds, a floating
joystick for manual control, and optional GPS drift for realistic jitter.
</en-US>
```

On every later release these become a changelog. Write what changed, in the user's
terms, not the commit subjects:

```
<en-US>
- Routes now snap to footpaths as well as roads
- Fixed the joystick drifting after a screen rotation
</en-US>
```

---

## Store listing (Grow → Store presence → Main store listing)

**App name** [30]

```
Voyara: Mock Location
```

Bare `Voyara` is cleaner but nobody searches for it. The suffix is what makes the
listing findable.

**Short description** [80]. Shown above the fold, before anyone taps "read more".
This one line does most of the conversion work:

```
Simulate GPS location, follow routes and replay GPX tracks for app testing.
```

**Full description** [4000]

```
Voyara simulates your device location for development and testing.

Pick a point on the map and your device reports it as the current fix. Chain
several stops together and Voyara follows real roads between them. Load a GPX file
and it replays the track. Nothing else on the device has to know the difference,
which is the point: it lets you test location-aware features without walking the
route yourself.

WHAT IT DOES

• Tap the map, search an address, or paste coordinates or a map link
• Hold a fixed position, or move along a route through as many stops as you like
• Follow roads between stops, or draw straight lines
• Import a GPX file and replay its track, route or waypoints
• Roam at random inside a radius you choose
• Drive the position by hand with a floating joystick, over any other app
• Walk, run, cycle and drive speed presets
• Optional GPS drift, so fixes jitter the way a real receiver does
• Loop a route to run it continuously
• Monochrome and street map styles, in light or dark
• Save favourites, revisit recents
• Start and stop from a Quick Settings tile
• Light, dark and system themes

HOW TO TURN IT ON

Android will not accept simulated locations until you allow it explicitly:

1. Open Settings → About phone and tap Build number seven times
2. Open Settings → System → Developer options
3. Tap "Select mock location app" and choose Voyara

Voyara cannot do this for you, and no app can. Until you complete those steps
Voyara is just a map.

While a simulation is running, Voyara shows a permanent notification. It never
runs out of sight.

PRIVACY

No account, no analytics, no ads, no tracking. Saved places and settings stay in
the app's private storage on your device and are deleted when you uninstall.
Search, routing and map tile requests go to OpenFreeMap, komoot Photon and OSRM
carrying only the coordinates needed to answer them.

Voyara is open source: https://github.com/broisnischal/voyara
```

**App icon**: `dist/play-assets/icon-512.png`

**Feature graphic**: `dist/play-assets/feature-graphic-1024x500.png`

**Phone screenshots** - `dist/play-assets/screenshots/01.png` through `04.png`,
captured from a real device and already padded to 1350x2290 so they satisfy Play's
rule that the long side cannot exceed twice the short side (a raw 1080x2400 capture
is 2.22:1 and gets rejected). The status bar is cropped off. In order: a pin
dropped in Shibuya, a three-stop route snapped to roads, the route being followed
in dark mode, and the full control sheet.

**App category**: Tools. **Tags**: pick from Play's fixed list; "Maps &
Navigation" and "Developer tools" are the closest.

---

## App content (Policy → App content)

Every item here blocks the release until it is green.

| Section | Answer |
| --- | --- |
| Privacy policy | URL to the published `docs/privacy.md` |
| Ads | No, my app does not contain ads |
| App access | All functionality available without restrictions. No login exists |
| Content rating | Questionnaire, category Utility. Answer No throughout. Result: Everyone / PEGI 3 |
| Target audience | 18 and over. Anything under 13 pulls the app into Families Policy, which this is not |
| News app | No |
| COVID-19 contact tracing | No |
| Data safety | See below |
| Government apps | No |
| Financial features | None |
| Health | No |
| Foreground service permissions | See below |

### Foreground service permissions

Voyara declares `FOREGROUND_SERVICE_LOCATION`, so Play requires a justification.

- **Type:** Location
- **Core functionality:** "Voyara supplies simulated location fixes to Android's
  mock location provider. The service keeps the simulation running while the user
  switches to the app being tested, which is the entire purpose of the app. It
  cannot run in the background only, because the simulation must survive leaving
  Voyara."
- **Video link:** required. A 30 second unlisted YouTube clip showing the app
  start a simulation and the notification appear is enough.

### Data safety

The one section worth being careful with. My answers:

- **Does the app collect or share user data?** Yes.
- **Location → Approximate location** and **Precise location**: collected, not
  shared. Purpose: *App functionality*. Mark **optional**, because the app works
  fully without granting it; the permission only powers the "show my real
  location" button.
- Everything else (saved places, settings, recents) is stored only on the device.
  On-device storage is not "collection" under Play's definition, so it is not
  declared.
- **Encrypted in transit:** Yes. Every network call is HTTPS.
- **Users can request data deletion:** No deletion mechanism, because no data
  leaves the device. Uninstalling removes everything.

The judgment call: coordinates do travel to OpenFreeMap, Photon, OSRM and TomTom
to fetch tiles and results. Play exempts data that is processed ephemerally, which
these are, so they need not be declared as shared. Declaring location as collected
anyway is the conservative reading and costs nothing. Under-declaring is what draws
enforcement, so when torn, declare.

---

## The native debug symbols warning

Play warns that the bundle contains native code with no symbol file uploaded. It is
a recommendation, not a blocker, and it cannot be resolved here: MapLibre and
`androidx.graphics.path` ship pre-stripped `.so` files. Both carry only a `.dynsym`
section, with no `.symtab` and no `.debug_info`, so there are no symbols to
extract. Building MapLibre from source is the only way to change that, and it is
not worth it.

`ndk { debugSymbolLevel = "SYMBOL_TABLE" }` is set in the release build type
anyway. It produces nothing today and costs nothing, and it will start emitting
symbols automatically if a future dependency ships unstripped libraries.

Upload and ignore the warning.

## Publishing the privacy policy

Play needs a public URL, not a file. GitHub Pages off this repo is free:

```sh
git add docs/privacy.md && git commit -m "Add privacy policy" && git push
```

Then in the repo settings, **Pages → Source: Deploy from a branch → main → /docs**.
The policy lands at:

```
https://broisnischal.github.io/voyara/privacy
```

Confirm that URL loads in a private window before pasting it into Play. A privacy
policy URL that 404s is the single most common reason a first submission bounces.
