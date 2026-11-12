# 0.8.1 — APK without test components

Build: `cd android && ./gradlew :app:assembleDebug -PwithoutTests=true --no-daemon --max-workers=1`

The property disables Android unit/instrumentation variants, omits test dependencies (including the debug Compose test manifest), and does not register screenshot tasks. Test source files remain in the repository, but are not compiled or packaged in this delivery. No tests or lint were run for this build.

Version code 9. Debug signing certificate unchanged from previous delivery. APK signature and version metadata were inspected, not runtime-tested.
