# Findings & Decisions: MotoNav

## Requirements
- **100% Free**: No recurring cloud API costs, no Google Maps / Mapbox metered API billing.
- **Offline First**: Map tiles and detailed routing calculations must work in areas without mobile connectivity.
- **Motorcycle Focused**: Routing must favor winding/curvy roads, avoid boring multi-lane highways, handle road surface preferences, and provide large, high-contrast, glove-friendly touch controls.
- **POI Search**: Offline search for petrol pumps (Indian Oil, Bharat Petroleum, HP, Shell), food/dhabas, mechanics/puncture repair, ATMs, and emergency services.
- **Regional Coverage**: Pre-packaged West Bengal out of the box; remaining Indian states available on-demand.
- **Spotify Controls**: Floating mini-player to play/pause, skip track, view song/artist. User has Spotify Premium.
- **WhatsApp Location Peek**: Detect incoming WhatsApp messages containing location links (Google Maps, geo URIs, OSM) and offer immediate one-tap navigation.
- **Battery Optimization**: Limit background apps, manage screen brightness, lock screen on, whitelist Spotify & WhatsApp.
- **Helmet Intercom Audio**: Voice guidance over Bluetooth A2DP with audio ducking.
- **Dark Mode**: Automatic day/night switching + manual override button in Settings.

## Research Findings
- **MapLibre Native Android (11.7.0+)**: Natively supports PMTiles using `pmtiles://file:///path/to/region.pmtiles`. Single-file vector format eliminates SQLite locking overhead.
- **GraphHopper 9.x/11.x**: Custom Models allow flexible JSON weighting without recompiling Java source code:
  - `road_class == MOTORWAY` -> `multiply_by 0.2`
  - `road_class == SECONDARY / TERTIARY` -> `multiply_by 1.5`
  - `curvature > 3` -> `multiply_by 1.6`
- **Ferrostar Navigation SDK**: Rust core with native Kotlin & Jetpack Compose bindings; router-agnostic adapter architecture works cleanly with GraphHopper.
- **Android NotificationListenerService**: Intercepts `com.whatsapp` and `com.whatsapp.w4b` notifications, extracts text via `Notification.EXTRA_TEXT`, regex-matches `maps.google.com`, `maps.app.goo.gl`, `geo:`, etc.
- **Spotify App Remote SDK**: Connects over local IPC to `com.spotify.music`. Doesn't require active internet for local music playback control.
- **Android MediaSessionManager**: Can fall back to controlling any active media player if Spotify App Remote is disconnected.

## Technical Decisions
| Decision | Rationale |
|----------|-----------|
| Kotlin 1.9.22 + AGP 8.2.2 + Gradle 8.2 | Compatible with Java 17 installed in environment, modern Android standards |
| MapLibre Native + PMTiles | Zero cost, completely offline, high performance GPU rendering |
| GraphHopper Embedded Java Core | Reliable, no C++ compilation friction, native motorcycle custom models |
| Room SQLite for POI storage | Instant spatial bounding box queries and name search offline |
| Foreground Location Service | Prevents Android OS from killing GPS tracking while in navigation mode |
| Android TTS with `USAGE_ASSISTANCE_NAVIGATION_GUIDANCE` | Automatically ducks Spotify music during turn guidance over Bluetooth helmet headset |

## Resources
- Geofabrik India OSM: https://download.geofabrik.de/asia/india/
- Planetiler: https://github.com/onthegomap/planetiler
- OpenFreeMap: https://openfreemap.org/
- Ferrostar Docs: https://stadiamaps.github.io/ferrostar/
