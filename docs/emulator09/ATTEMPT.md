# Android emulator attempt — Vault 0.9.0

## Outcome
**Blocked by emulator/system startup instability. APK not installed; application never launched. No app tap, swipe, navigation, keyboard, bridge, or live backend validation was completed.**

## Environment
- x86_64 Linux sandbox, approximately 1982 MiB RAM, no swap, no `/dev/kvm`.
- Android Emulator 37.2.13.0, Android 11 / API 30 Google APIs x86_64 image.
- Software CPU emulation (`-accel off`), headless SwiftShader rendering.
- Display 720×1600, 294dpi; a resource-saving approximation, NOT the user's physical Redmi device.

## Attempts
1. Standard launch with requested RAM 768 MiB: emulator increased RAM to 2048 MiB and exited with `Insufficient RAM free for launching emulator`.
2. Retried with QEMU RAM override `-qemu -m 768` and Vulkan disabled. Guest reported 739928 kB RAM, ADB connected, services started. Emulator host RSS reached approximately 1.2 GB; guest execution saturated the available CPU.
3. Framework startup was extremely slow. Logcat recorded `WATCHDOG KILLING SYSTEM PROCESS: Blocked in handler on main thread (main)` and subsequent system/zygote restarts. A boot-completed event was logged at approximately 779 seconds, but the package service subsequently disappeared; the system was not usable/stable.
4. Streamed APK install failed: `Failure calling service package: Broken pipe (32)`.
5. Push/non-streamed install transferred the APK, but installation failed: `cmd: Can't find service: package`.
6. A captured emulator framebuffer was black. It is not an application screenshot.
7. Emulator was stopped to release resources. No GitHub writes, cloud changes or supplied credentials were used. Existing APK/source were not changed by this attempt.

## Interpretation
This run neither confirms nor disproves application correctness: failure occurred before application launch. Previous Chromium captures remain browser-only observations, not Android interaction validation.

## Next viable environment
Use a hardware-accelerated emulator on a machine with adequate memory, or the user's real phone through local ADB. Install the APK, start `com.saged.githubvault/.app.MainActivity`, clear/log logcat, and exercise repository open/back/search, local/transfer/account navigation, theme, token-sheet cancel, keyboard, file picker cancel, read-only download/preview, and offline recovery. Authenticated writes/overwrites should only be attempted with an authorized test account/repository and explicit confirmation.
