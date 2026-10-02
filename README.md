# Pool Aim Guide - Native Android App (Kotlin + Jetpack Compose)

A floating overlay practice tool for 8-ball pool and billiards games.

## Phase 1 Features
1. **Foreground Overlay Service with `SYSTEM_ALERT_WINDOW`**:
   - Small floating toggle bubble icon can be dragged anywhere.
   - Shows/hides transparent full-screen overlay above games.
   - Dynamic `FLAG_NOT_TOUCHABLE` touch pass-through so you can shoot the pool cue in 8 Ball Pool directly.
2. **Manual 2-Point Calibration**:
   - Tap 1: Cue Ball center.
   - Tap 2: Target Ball center.
   - Micro-drag fine tuning with magnifying glass preview.
3. **Accurate 3-Line Pool Geometry**:
   - **Line 1 (Long Aim Line)**: Straight line from cue ball center through target area and extended beyond.
   - **Line 2 (Object Ball Path)**: From contact point along line through object ball center (ghost-ball method).
   - **Line 3 (Cue Ball Deflection)**: 90° tangent line perpendicular to Line 2 on the correct side of impact.
4. **Floating Control Panel**:
   - Master on/off toggle.
   - Sliders for line length, line thickness, opacity, and ball radius.
   - High-contrast color presets.
   - Reset button.

## Building in Android Studio
1. Open the project folder in **Android Studio Ladybug or newer**.
2. Sync Gradle files.
3. Run on a physical Android device or emulator running Android 8.0+ (API 26+).
4. When prompted, grant "Display over other apps" permission in Android Settings.