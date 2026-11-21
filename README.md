# Custom Board — Alpha 0.1

A minimal black Android system keyboard with Russian and English layouts.

## Build
GitHub Actions builds a debug APK on every push to `main` and uploads it as the artifact **CustomBoard-Alpha-0.1**.

## Install and enable
1. Download the APK from the latest successful Actions run.
2. Install it on Android.
3. Open Android Settings → System → Languages & input → On-screen keyboard → Manage keyboards.
4. Enable **Custom Board**, then choose it from the keyboard selector.

This is an early alpha. Keyboard input is processed locally by Android's InputMethodService; no network permission or telemetry is included.
