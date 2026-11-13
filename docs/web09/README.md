# Vault 0.9.0 — local HTML / Android WebView

## Implementation
- Launcher now hosts `WebVault`, not the old Compose screen tree. Every main screen, search/filter, account screen, cloud/admin screen and form is rendered by the new local `assets/vault.html`.
- New cobalt/lime visual identity, SVG folder illustration and outlined icons, responsive cards, bottom navigation, bottom-sheet forms, progress, empty and error states, light/dark/system appearance, reduced-motion handling. No blur or external CDNs.
- Android file/document-tree pickers and the final upload/overwrite confirmation remain native intentionally.
- Existing ViewModels/Room/WorkManager/streaming GitHub client still perform the actual work: public saged7 browsing, branches and folders, text preview, downloads, optional encrypted device-only write token, public repo creation, upload, local open/update/delete, transfer retry/cancel, private cloud login/password/configuration, cloud browsing/download/upload, admin repository linking, user/access management and audit.
- Token and password fields are cleared on form close/submit. They are not stored in browser storage or sent back in state. Only token-saved status is exposed. Transfer payloads, device file paths, session tokens and upload URIs are not passed to JavaScript.

## Bridge isolation
`WebViewAssetLoader` serves local assets at an HTTPS origin. `WebViewCompat.addWebMessageListener` only accepts that origin's main frame. File/content access, mixed content, popups, navigation, and non-asset resource loads are blocked. CSP blocks network requests, frames and form navigation. All dynamic strings are HTML escaped; state is passed with JSON quoting. Native code resolves file/repo/job IDs from current ViewModel state rather than accepting arbitrary local paths/URLs. Cloud authorization remains enforced server-side. Nothing was deployed to or modified in Supabase during this work.

## Build
```
cd android
./gradlew :app:assembleDebug -PwithoutTests=true --no-daemon --max-workers=1
```
Version 0.9.0 / code 10; debug-signed with the same certificate as previous deliveries. Tests and screenshot-test dependencies/variants are excluded by the build property. No JUnit, instrumentation, Robolectric, lint or live upload tests were run for this release.

## What was reviewed
The HTML was rendered in desktop Chromium at CSS viewports 320×640, 360×740, 393×801, 412×843, 480×820 and 768×1024. Repository/files screens, token sheet and dark settings were captured; navigation, repo opening and theme selection were exercised for those captures. `render-notes.json` records no page JS errors or document horizontal overflow in that capture sequence. These are browser render observations, not Android runtime or backend end-to-end results. Images contain clearly labeled fictional demo data and no live credentials.

393×801 CSS pixels approximates the application area of the user's Redmi Note 11 Pro 5G, assuming 440dpi, a 24dp status bar and 48dp three-button navigation on a 1080×2400 display. Actual device density/insets were not measured.

## Limitations
The APK was not installed or run on a physical phone/emulator. Android WebView bridge execution, keyboard/insets, file picking, live cloud authentication, and real GitHub uploads/downloads still need on-device acceptance. The page's standalone preview is an offline demo; only the installed APK uses native networking/storage. No universal-device guarantee or carry-over 90% SSIM claim is made for this redesign. An updated Android System WebView supporting WEB_MESSAGE_LISTENER is required; older unsupported engines show an update message.
