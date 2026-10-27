# Audio Tiles

Universal Noise Control Quick Settings Tiles, Home Widgets & Parametric EQ Studio for **Compatible Wireless ANC Headphones and Earbuds**.

A lightweight, zero-telemetry Android companion app targeting **Android 14+ (API 34+)** with specialized optimizations for **ColorOS / Oppo Find X9 Ultra**. Communicates directly with your headset over Bluetooth Classic RFCOMM (SPP) using native MDR protocol frames — delivering sub-50ms mode switching without the bloat, battery drain, accounts, or background data collection of the proprietary manufacturer app.

[**Download Signed APK (v2.1.1)**](https://github.com/TheSwagInDisguise/Audio-Tiles/releases/download/v2.1.1/Audio-Tiles-v2.1.1.apk) • [**Interactive Web Showcase**](https://theswagindisguise.github.io/Audio-Tiles/) • [**Release Notes**](https://github.com/TheSwagInDisguise/Audio-Tiles/releases/tag/v2.1.1)

```text
Official v2.1.1 Release Verification:
Asset:   Audio-Tiles-v2.1.1.apk (11.90 MB)
Package: me.audiotiles (Build 20101)
SHA-256: a27e46adda92941ebb326f47e276f7f0384c5ddf11a530a1eae155ad986246cf
```

---

## Supported Devices

*Headphone and earbud model designations are listed strictly for compatibility identification under nominative fair use:*

- **Over-Ear Flagships**: WH-1000XM6, WH-1000XM5, WH-1000XM4, WH-1000XM3, WH-1000XM2, MDR-1000X.
- **Bass & Mid-Range Over-Ear**: WH-ULT900N (ULT WEAR), WH-XB910N, WH-XB900N, WH-CH720N, WH-CH520.
- **True Wireless Stereo (TWS)**: WF-1000XM5, WF-1000XM4, WF-1000XM3, LinkBuds (WF-L900), LinkBuds S (WF-LS900N), LinkBuds Open / Fit, WF-C700N, WF-C510, WF-C500.
- **In-Ear & Neckband**: WI-1000XM2, WI-1000X, WI-C100.

---

## Key Features

### 1. Instant Quick Settings Tiles
Switch modes and audio enhancements directly from your Android notification shade without opening the app:
- **Primary Noise Control Tile**: Instant one-tap cycling between Noise Cancelling, Ambient Sound, and Off.
  - Supports configurable cycle schemes: 3-Way (`NC → Ambient → Off`), 2-Way (`NC ↔ Ambient`), or Direct (`NC ↔ Off`).
- **Speak-to-Chat Tile**: One-tap toggle for conversational voice passthrough.
- **Clear Bass Stepper Tile**: Step through hardware sub-bass boost levels (-10 to +10 dB) directly from quick settings.
- **Voice Focus Tile**: Emphasize human speech frequencies while attenuating low-frequency ambient rumble.
- **Sound Quality Priority Tile**: Switch instantly between LDAC (990 kbps high-res priority) and connection stability priority.

### 2. Home Screen Widgets
- **2x1 Compact Widget**: Real-time connection badge, single-tap ANC mode switcher, and live battery gauge.
- **4x2 Studio Dashboard Widget**: Independent Left, Right, and Smart Case battery gauges with charging indicators, segmented ANC selector, Clear Bass stepper, and Speak-to-Chat toggle.

### 3. 10-Band Graphic Equalizer, Interactive Spline & AutoEq Studio
- **Touch-Draggable Spline Canvas**: Directly touch-drag frequency nodes with tactile haptics and real-time biquad response curve rendering.
- **Audiophile Target Curve Overlays**: Dotted target curve overlays for Harman Target Over-Ear 2020, Harman In-Ear 2019, Oratory1990 Hi-Fi, and Rtings Neutral.
- **10 ISO Center Frequencies**: 31Hz, 63Hz, 125Hz, 250Hz, 500Hz, 1kHz, 2kHz, 4kHz, 8kHz, 16kHz ($\pm 12\text{ dB}$).
- **Hardware DSP Fitting**: Automatically downsamples 10-band adjustments and continuous curves to hardware DSP (Clear Bass + 5 hardware bands: 400Hz, 1kHz, 2.5kHz, 6.3kHz, 16kHz).
- **A/B Audition (Bypass)**: Toggle between Flat 0 dB and your active EQ profile with one tap to compare sound signatures in real time.
- **Built-in AutoEq & PEQDB Direct Catalog**: 26 pre-computed reference compensation curves derived from lab acoustic measurements (oratory1990 GRAS 43AG, Rtings Bruel & Kjaer 5128 anthropomorphic fixtures, Kuulokenurkka, DHRME). Automatically filters to your connected model on launch.
- **Universal Import**: Load `.peqdb`, JSON, and AutoEq / EqualizerAPO parametric text files.

### 4. Smart Audio Automations & Health
- **ISO 226 Dynamic Bass (Equal-Loudness)**: Automatically compensates human hearing sub-bass roll-off at low listening levels (10%–40% volume) according to ISO 226 equal-loudness contours, tapering back to flat at louder volumes.
- **Acoustic Seal & Fit Verification Tool**: Real-time diagnostic tone generator producing calibrated $40\text{Hz}$ deep sub-bass, $100\text{Hz}$ punch, $250\text{Hz}$ low-mid, and $1\text{kHz}$ reference sine waves to verify silicone/foam ear tip canal seal.
- **Wind Noise Watchdog**: Automatically activates physical microphone baffling during outdoor breezes.
- **App-Linked Sound Profiles**: Automatically loads custom EQ and ANC presets when specified media apps launch.

### 5. Curated Hardware Themes & Clean UI
- **Unified Daily-Driver Dashboard**: Telemetry, animated battery rings, direct Noise Control buttons, quick ambient slider, and Clear Bass subwoofer slider in one view.
- **Progressive Disclosure**: Power-user features (10-Band EQ & AutoEq, Headphone Hardware Features, Smart Audio Automations, and Live Diagnostics) are neatly tucked into collapsible accordions.
- **Dynamic Palette Reactivity**: Card borders, buttons, and visual accents dynamically adapt to your selected theme:
  1. `Black & Brushed Copper` (Obsidian finish)
  2. `Platinum Silver & Ice Blue` (Brushed metal finish)
  3. `Smoky Pink & Rose Gold` (Warm matte finish)
  4. `Midnight Navy & Brass` (Deep navy finish)
  5. `Cyberpunk Emerald & Mint` (Vibrant high-contrast dark palette)
  6. `Warm Espresso & Amber` (Deep organic coffee tone)
  7. `Pure OLED Stealth` (Zero-emission deep pitch black for AMOLED displays)

### 6. Full OEM Feature Parity
- **Clear Bass Subwoofer Level**: Direct hardware slider from -10 to +10 dB.
- **Speak-to-Chat**: Hardware-level voice detection sensitivity and auto-close timer.
- **DSEE Extreme**: AI high-frequency audio upscaling restoration.
- **Wearing Detection**: Capacitive sensor automatic playback pause when removed.
- **Auto Power-Off**: Configurable idle standby timeout (5m, 30m, 1h, 3h, Never).
- **Multi-Cell Battery**: Left earbud %, Right earbud %, and Case % telemetry for TWS models with live charging status.

---

## Comparison: Audio Tiles vs Official OEM Companion App

| Feature | Audio Tiles (This App) | Official OEM App |
| :--- | :--- | :--- |
| **Quick Settings Tile** | **Yes** (Native Android 14+ Quick Tile) | No (Not Supported) |
| **Home Screen Widgets** | **Yes** (2x1 Compact + 4x2 Studio Dashboard) | Limited |
| **Equalizer Resolution** | **10-Band ISO + Parametric Curve Spline** | Limited 5-Band Only |
| **Target Curve Overlays** | **Yes** (Harman 2020, Oratory1990, Rtings 5128) | No |
| **AutoEq & PEQDB Import** | **Yes** (26 Built-in Profiles + `.peqdb` import) | No |
| **Equal-Loudness Bass** | **Yes** (ISO 226 volume-compensated dynamic bass) | No |
| **Earbud Seal Tester** | **Yes** (Calibrated multi-frequency acoustic test) | No |
| **Toggle Latency** | **< 50 ms** (Instant RFCOMM direct frame) | 3 to 8 seconds (App launch required) |
| **App Size & RAM** | **~11 MB lean build** | 120+ MB + heavy background services |
| **Privacy & Telemetry** | **0 Trackers • 100% Offline** | Mandatory analytics & account required |
| **Battery Impact** | **120s Smart Idle Sleep & ACL hooks** | Continuous socket polling & background drain |
| **ColorOS / Oppo Support** | **Optimized** (WakeLock + Insecure RFCOMM fallback) | Frequently killed by aggressive memory management |
| **License** | **MIT License (Open Source)** | Proprietary Closed Source |

---

## Clean Wire-Level MDR Protocol

Audio Tiles communicates directly with compatible Bluetooth audio devices over Bluetooth Classic RFCOMM (SPP) using native MDR protocol frames without proprietary closed-source SDKs:

```text
MDR v2 Escaped Frame Structure:
[Header: 0x3E] [Type: 1B] [Seq: 1B] [Payload Length: 4B BE] [Payload Bytes...] [Checksum: 1B] [Trailer: 0x3C]
Escape Byte: 0x3D (Escaped value = original ^ 0xEF)

Common MDR Command Payloads:
- Noise Cancelling ON:  0x68 0x17 0x01 0x01 0x00 0x02 0x00 0x01
- Ambient Sound Level:  0x68 0x17 0x01 0x02 0x01 0x02 <VoiceFocus: 0|1> <Level: 0x01..0x14>
- Noise Control OFF:    0x68 0x17 0x01 0x00 0x00 0x02 0x00 0x00
- Speak-to-Chat Toggle: 0xF8 0x0C <Enable: 0x00|0x01> 0x01
- DSEE Extreme Toggle:  0xE8 0x01 <Auto: 0x01|Off: 0x00>
- Hardware Custom EQ:   0x58 0x00 0xA0 0x06 <CB+10> <400Hz+10> <1kHz+10> <2.5kHz+10> <6.3kHz+10> <16kHz+10>
- TWS Battery Query:    0x22 0x00 (V2 devices) / 0x23 (V1 safe multi-battery query)
```

---

## Quick Installation & Setup

> **⚠️ Upgrading from v2.0.x (Old Package `com.sony.xm6controller`)**:
> Because the package identifier was migrated to `me.audiotiles` for trademark compliance, Android installs this version alongside the legacy build rather than replacing it. **Please uninstall any previous version (`com.sony.xm6controller` or `Sony XM6 Controller`) before connecting**, otherwise both apps will contend for the single Bluetooth control channel and drop connections.

1. **Download APK**: Download `Audio-Tiles-v2.1.1.apk` from [Releases](https://github.com/TheSwagInDisguise/Audio-Tiles/releases/tag/v2.1.1).
2. **Pair Headset**: Pair your headphones or earbuds in Android's Bluetooth settings.
3. **Add Tile**: Swipe down your notification shade twice, tap the Edit (Pencil) icon, and drag **Audio Control** into your active tiles grid.
4. **ColorOS / Oppo Users**: Open the app once, tap the ⚙️ Settings cog in the top bar, and verify **"Ignore Battery Optimizations"** to ensure background persistence.

---

## Building from Source

Prerequisites: JDK 17 and Android SDK (API 34).

```bash
git clone https://github.com/TheSwagInDisguise/Audio-Tiles.git
cd Audio-Tiles

# Run all unit and protocol regression tests
./gradlew testDebugUnitTest

# Build debug APK
./gradlew assembleDebug

# Output APK path:
# app/build/outputs/apk/debug/app-debug.apk
```

---

## ⚖️ Legal & Trademark Notice

- **Trademark Notice**: Sony® and Xperia® are registered trademarks of Sony Group Corporation.
- **Non-Affiliation**: This project is an independent open-source utility and is not developed by, affiliated with, sponsored by, or endorsed by Sony Group Corporation, Oppo, or any of their subsidiaries.
- **Fair Use**: All product names, logos, brands, and registered trademarks mentioned are property of their respective owners. Headphone and earbud model designations (e.g. WH-1000XM5, WF-1000XM5) are used strictly to identify hardware compatibility under nominative fair use.
- **License**: [MIT License](LICENSE)
- **Protocol References & Prior Art**:
  - `mos9527/SonyHeadphonesClient`
  - `marconvcm/sony-device-center`
  - `Freeyourgadget/Gadgetbridge`
  - `snizovtsev/xm5-mode-switch`
  - `AutoEq / jaakkopasanen` (Acoustic measurement databases)

---

## AI Use Disclaimer

Portions of this codebase, protocol parsers, visual components, unit tests, and documentation were generated, refactored, and maintained with the assistance of artificial intelligence tools and large language models, followed by human architecture review and verification against hardware captures.
