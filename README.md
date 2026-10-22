# Sony Audio Tiles

A lightweight, open-source Android app targeting Android 14+ (API 34+) that provides an **Android Quick Settings Tile**, a **10-Band Graphic Equalizer**, and an **AutoEq PEQ Studio** for Sony wireless headphones and earbuds.

It communicates directly with your headset over Bluetooth Classic RFCOMM (SPP) using reverse-engineered Sony MDR protocol frames, providing sub-50ms switching with zero analytics, no telemetry, and no account requirements.

---

## Supported Devices

- **Over-Ear**: WH-1000XM6, WH-1000XM5, WH-1000XM4, WH-1000XM3, WH-ULT900N (ULT WEAR), WH-XB910N, WH-CH720N, WH-CH520, MDR-1000X.
- **True Wireless (TWS)**: WF-1000XM5, WF-1000XM4, WF-1000XM3, LinkBuds (WF-L900), LinkBuds S (WF-LS900N), LinkBuds Open / Fit, WF-C700N, WF-C510.
- **In-Ear & Neckband**: WI-1000XM2, WI-1000X, WI-C100.

---

## Features

### 1. Instant Quick Settings Tile
- Toggle noise control modes directly from your notification shade:
  $$\text{Noise Cancelling (NC On)} \longrightarrow \text{Ambient Sound (Ambient)} \longrightarrow \text{Off (ANC Off)} \longrightarrow \text{Noise Cancelling}$$
- Supports customizable cycle schemes: 3-way (`NC → Ambient → Off`), 2-way (`NC ↔ Ambient`), or direct (`NC ↔ Off`).
- Syncs state in real time with physical headphone button presses (`0x69` notification frames).

### 2. 10-Band Graphic Equalizer & PEQ Studio
- **10 ISO Center Frequencies**: 31Hz, 63Hz, 125Hz, 250Hz, 500Hz, 1kHz, 2kHz, 4kHz, 8kHz, 16kHz ($\pm 12\text{ dB}$).
- **Hardware Downsampling**: Automatically downsamples 10-band adjustments and continuous curves to Sony's hardware DSP (Clear Bass + 5 hardware bands: 400Hz, 1kHz, 2.5kHz, 6.3kHz, 16kHz).
- **A/B Audition (Bypass)**: Toggle between Flat 0 dB and your active EQ profile with one tap to compare sound signatures in real-time.
- **Built-in AutoEq & PEQDB Direct Catalog**: 26 pre-computed reference compensation curves directly from official acoustic measurements (oratory1990 GRAS 43AG, Rtings Bruel & Kjaer 5128 anthropomorphic fixtures, Kuulokenurkka, DHRME) covering WH-1000XM6, XM5, XM4, XM3, XM2, MDR-1000X, WF-1000XM5, WF-1000XM4, WF-1000XM3, LinkBuds S, LinkBuds (WF-L900), LinkBuds Fit, ULT WEAR (WH-ULT900N), WH-XB910N, WH-CH720N, WH-CH520, WF-C700N, WF-C500, and WI-1000XM2. Automatically filters to your connected headphone model on launch.
- **Custom Profile Import**: Load `.peqdb`, JSON, and AutoEq / EqualizerAPO parametric text files.
- **Local Preset Library**: Save, load, and manage unlimited custom sound profiles locally.

### 3. Sound Connect Parity
- **Clear Bass**: Dedicated sub-woofer level control (-10 to +10).
- **Speak-to-Chat**: Automatic ambient passthrough when speaking.
- **DSEE Extreme**: AI high-frequency audio restoration.
- **Wearing Detection**: Automatic playback pause when headphones are removed.
- **Auto Power-Off**: Configurable idle standby countdown.
- **Multi-Cell Battery**: Left earbud %, Right earbud %, and Case % telemetry for TWS models.

### 4. Diagnostics & Testing
- **Real-Time Packet Stream**: Live monitor of TX, RX, and ACK frames with hex inspection.
- **Diagnostic Dump Export**: One-tap report generator bundling device name, address, capabilities, active telemetry, and recent packet logs for troubleshooting.

### 5. ColorOS / Oppo Find X9 Ultra Optimizations
- CPU WakeLock safeguard prevents process suspension when closing the notification shade.
- Multi-tier RFCOMM fallback (Standard SPP &rarr; Sony V2 UUID &rarr; Insecure RFCOMM &rarr; Direct channel reflection).
- 120-second idle power-saving socket tear-down.

---

## Installation & Setup

1. **Download APK**: Download `Sony-Audio-Tiles-vX.X.X.apk` from the [GitHub Releases](https://github.com/TheSwagInDisguise/Sony-Audio-Tiles/releases) page.
2. **Pair Headset**: Pair your Sony headphones or earbuds in Android's Bluetooth settings.
3. **Add Tile**: Swipe down your notification shade twice, tap Edit (Pencil), and drag **Sony Audio Control** into your active tiles grid.
4. **ColorOS / Oppo Users**: Open the app once and tap **"Ignore Battery Optimizations"** to ensure smooth background operation.

---

## Development & Branches

- **`main`**: Production-ready code. Official signed releases (`Sony-Audio-Tiles-vX.X.X.apk`) and GitHub Pages deployment are triggered exclusively from tags on this branch.
- **`develop`**: Active development and experimental testing branch. Builds on this branch are not published as public releases.

---

## Building from Source

Prerequisites: JDK 17 and Android SDK (API 34).

```bash
git clone https://github.com/TheSwagInDisguise/Sony-Audio-Tiles.git
cd Sony-Audio-Tiles

# Run unit tests
./gradlew testDebugUnitTest

# Build debug APK
./gradlew assembleDebug

# Output APK path:
# app/build/outputs/apk/debug/app-debug.apk
```

---

## License & Credits

- **License**: MIT License
- **Protocol References**:
  - `mos9527/SonyHeadphonesClient`
  - `marconvcm/sony-device-center`
  - `Freeyourgadget/Gadgetbridge`
  - `snizovtsev/xm5-mode-switch`

*Disclaimer: This is an independent open-source project and is not affiliated with, endorsed by, or connected to Sony Corporation or Oppo.*

---

## AI Use Disclaimer

Portions of this codebase, protocol parsers, visual components, unit tests, and documentation were generated, refactored, and maintained with the assistance of artificial intelligence tools and large language models, followed by human architecture review and verification against Sony hardware captures.
