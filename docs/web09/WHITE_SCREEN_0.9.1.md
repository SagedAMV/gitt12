# 0.9.1 — white-screen recovery and diagnostics

User reported a white screen on initial launch of 0.9.0 on their Redmi Note 11 Pro 5G. No device logcat/engine version was available. **Root cause remains unconfirmed; this is a mitigation/diagnostic release, not a demonstrated on-device fix.**

Changes:
- Explicit MATCH_PARENT layout parameters for the embedded WebView; allow navigation only to the exact packaged entry page while blocking other navigation.
- Separate document loading, bridge readiness and successful HTML render acknowledgement. Do not assume onPageFinished proves the UI is ready.
- A native loading/recovery overlay, 12-second startup watchdog, retry with fresh WebView instance, engine version and short diagnostic code.
- Native error handlers for navigation/HTTP failures, renderer termination and startup JS console errors. Do not log message contents or credentials.
- HTML static boot content and an early JS error reporter; theme-listener compatibility fallback for older engines; successful state render reports `painted`.
- A native UI fallback button. While startup/recovery overlay is present, hardware Back switches to the original native UI. Once the page reports ready, Back on the public root opens a native recovery/exit menu, which also permits recovery if the WebView claims successful painting but its framebuffer remains blank.
- Renderer retry/disposal protects the new browser reference against stale view release.

Codes: W01=document did not finish loading, W02=bridge not ready, W03=no confirmed render, W04=navigation error, W05=HTTP asset error, W06=renderer exit, W07=console error (line number), W08=required WebView feature unsupported, W09=WebView initialization exception, W10=HTML startup error (line number). These identify stages, not necessarily root causes.

Version code 11, name 0.9.1. `:app:assembleDebug -PwithoutTests=true` succeeded. APK signature verified; same debug certificate as earlier deliveries. Packaged HTML compared byte-for-byte with the delivered HTML. JavaScript syntax checked with Node. No successful emulator or physical-device runtime validation; see prior emulator attempt report.

Do not uninstall/clear app data for diagnosis: local downloads and device-only token could be lost. Install as an update, try opening, capture any W-code/engine version screen, and use native fallback if needed. Updating Android System WebView/Chrome is an additional troubleshooting step, not a confirmed cause/fix.
