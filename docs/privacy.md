# Voyara privacy policy

Last updated: 24 September 2026

Voyara is a location simulation tool for Android. This policy covers what the app
does with data. It is short because the app does very little.

## What Voyara stores

Everything Voyara saves stays on the device, in the app's own private storage:

- Saved places and recent searches
- Map style, theme and speed preferences
- The simulated position currently in use

None of it leaves the device, and none of it is sent to me. Uninstalling Voyara
deletes all of it. Saved places and recents can also be cleared from inside the app
at any time.

I do not operate a server. Voyara has no account system, no analytics, no
advertising and no crash reporting SDK.

## What Voyara sends, and where

Voyara requests map data over HTTPS from third parties. These requests carry the
coordinates needed to answer them, and nothing else. No identifier for the device
or the person using it is attached.

| Service | What is sent | Purpose |
| --- | --- | --- |
| [OpenFreeMap](https://openfreemap.org) | Map tile coordinates | Base map tiles |
| [TomTom](https://www.tomtom.com/privacy/) | Map tile coordinates, API key | Map tiles and live traffic, when an API key is configured |
| [Photon](https://photon.komoot.io) (komoot) | Search text, map centre | Place search |
| [OSRM on FOSSGIS](https://routing.openstreetmap.de) | Route waypoints | Road routing between stops |

Each service handles those requests under its own privacy policy. Voyara sends
nothing to them beyond what is listed above.

## Location permission

Voyara requests `ACCESS_FINE_LOCATION` and `ACCESS_COARSE_LOCATION` for one
feature: the "show my real location" button, which centres the map on the device's
current position. That reading is used to move the map and is not stored or
transmitted.

Voyara does not request background location access.

## Mock location

Voyara supplies simulated coordinates to Android's mock location provider. This
only works after the device owner enables Developer options and selects Voyara
under *Select mock location app*. Android cannot be made to accept mock locations
without that explicit step, and Voyara cannot enable it on the user's behalf.

While mocking is active, Voyara runs a foreground service with a persistent
notification, so the simulation is never running invisibly.

## Children

Voyara is a developer and testing tool and is not directed at children.

## Contact

Questions about this policy: nischal.dahal@aitc.ai

Source code: <https://github.com/broisnischal/voyara>
