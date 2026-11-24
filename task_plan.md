# Task Plan: MotoNav — Offline-First Motorcycle Navigation App for Android

## Goal
Build a 100% free, production-ready, personal-use Android motorcycle navigation application (MotoNav) with offline-first vector maps (pre-packaged West Bengal, on-demand other Indian states), detailed motorcycle-friendly routing (curvy roads, surface-aware, highway avoidance), turn-by-turn voice guidance routed to helmet Bluetooth, Spotify playback control widget, WhatsApp location link peek widget, battery preservation ride mode, POI database (petrol pumps, food, mechanics), and auto/manual dark mode.

## Current Phase
Phase 6: Data Pipeline Scripts & Testing

## Phases

### Phase 1: Project Setup & Core Foundation
- [x] Initialize Android project structure (`build.gradle.kts`, `settings.gradle.kts`, `libs.versions.toml`, `AndroidManifest.xml`)
- [x] Set up Hilt Dependency Injection, Room Database foundations, and DataStore preferences
- [x] Implement Map Style & Offline Tile Source manager (PMTiles / MapLibre Native, OpenFreeMap online fallback)
- [x] Add West Bengal regional pre-package structure and download management
- **Status:** complete

### Phase 2: Offline Routing & Navigation Engine
- [x] Implement GraphHopper motorcycle routing engine with custom curvature and surface weighting
- [x] Implement Ferrostar turn-by-turn state machine and navigation controller
- [x] Implement VoiceGuidanceManager with Android TTS (`en-IN` voice) and Bluetooth helmet audio routing
- [x] Implement Foreground NavigationService with ongoing notification for ride protection
- **Status:** complete

### Phase 3: POI System & Search
- [x] Implement Room POI entities (petrol pump, restaurant/dhaba, mechanic, ATM, hospital)
- [x] Implement spatial bounding box queries and proximity search
- [x] Implement offline POI data extractor and West Bengal POI database seeding
- [x] Build POI search bar and quick filter chips UI
- **Status:** complete

### Phase 4: Integrations (Spotify, WhatsApp, Battery)
- [x] Implement Spotify App Remote Manager + MediaSession fallback controller
- [x] Implement MiniSpotifyPlayer Jetpack Compose floating widget (glove-friendly touch targets)
- [x] Implement WhatsApp `NotificationListenerService` and regex location link parser
- [x] Implement WALocationPeek popup widget with one-tap "Navigate" action
- [x] Implement BatteryOptimizer with selective process restriction, screen brightness control, and whitelist
- **Status:** complete

### Phase 5: Motorcycle-Optimized UI & Settings
- [x] Implement Main MapScreen with MapLibre Compose, user location puck, and compass
- [x] Implement Active NavigationScreen with maneuver banner, lane guidance, ETA bar, and speed indicator
- [x] Implement SettingsScreen with Auto / Manual Dark Mode toggle, audio routing, and battery saver settings
- [x] Implement Region Download screen for on-demand state packages
- **Status:** complete

### Phase 6: Data Pipeline Scripts & Testing
- [ ] Create desktop pipeline scripts (`download_osm.sh`, `package_region.sh`, `extract_pois.py`)
- [ ] Write unit tests for routing calculation, POI spatial queries, WhatsApp link parser, and battery manager
- [ ] Verify build, package, and runtime readiness
- **Status:** in_progress

### Phase 6: Data Pipeline Scripts & Testing
- [ ] Create desktop pipeline scripts (`download_osm.sh`, `package_region.sh`, `extract_pois.py`)
- [ ] Write unit tests for routing calculation, POI spatial queries, WhatsApp link parser, and battery manager
- [ ] Verify build, package, and runtime readiness
- **Status:** pending

## Key Questions
1. How to ensure map tiles and routing graphs run completely offline? -> Pre-package West Bengal `.pmtiles` and `.ghz` in assets/storage, load via `pmtiles://` file URI and GraphHopper local storage.
2. How to ensure voice audio plays clearly in helmet intercom? -> AudioAttributes with `USAGE_ASSISTANCE_NAVIGATION_GUIDANCE` and request audio focus with ducking.
3. How to preserve battery during 6+ hour rides? -> Foreground service with `location` type, screen brightness lock at 70%, background process limits, and black/dark mode OLED optimization.

## Decisions Made
| Decision | Rationale |
|----------|-----------|
| Kotlin Native + Jetpack Compose | Maximum performance, direct sensor/GPS/foreground service control, 60fps map rendering |
| MapLibre Native + PMTiles | Free, open-source, no API keys, single-file archive with instant range reads |
| GraphHopper Embedded Core | Java-native on Android JVM (no NDK required), custom model JSON for curvy motorcycle roads |
| Ferrostar SDK | Modern TBT navigation state machine, Kotlin + Compose bindings |
| NotificationListenerService | Standard Android API to read WhatsApp notifications and parse location links safely |
| Spotify App Remote + MediaSession | Native playback control for Spotify Premium with universal fallback |
| Dual Dark Mode (Auto + Manual in Settings) | Auto by sunrise/sunset + manual override toggle requested by user |
| Pre-packaged West Bengal | Fast initial experience out-of-the-box, other states downloaded on demand |

## Errors Encountered
| Error | Attempt | Resolution |
|-------|---------|------------|
|       | 1       |            |
