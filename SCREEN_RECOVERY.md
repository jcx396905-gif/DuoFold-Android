# Screen-off recovery: issues #4 and #6

Baseline: `86a3578c340e1e9744d6f09e04a7a3316296cb7b` / v0.6.1.
Prepared version: 0.6.2, versionCode 9. Inspection date: 2026-10-24.

## Evidence and likely cause

Both [#4](https://github.com/jcx396905-gif/DuoFold-Android/issues/4) and
[#6](https://github.com/jcx396905-gif/DuoFold-Android/issues/6) report a Samsung
S24 Ultra losing the effect after screen off/lock. Neither issue specifies the
installed APK version, ROM build, Shizuku state, or logs. They remain open;
device confirmation is required before closing them.

v0.6.1 already listens for SCREEN_OFF and USER_PRESENT. Its recovery is lossy:

- SCREEN_OFF derives `resumeAfterUnlock` from `active`, then calls a stop method
  that clears the flag. A second screen-off or interruption while suspended
  erases the pending request.
- USER_PRESENT clears the flag **before** posting a single 400 ms attempt. If
  Shizuku is unavailable then, that attempt returns without preserving or
  retrying the request. A subsequent bridge connection never resumes it.
- The flag exists only in memory. Service teardown/recreation loses it, and
  onServiceConnected has no restore path.
- Capture/heartbeat failures can precede the SCREEN_OFF receiver. These failures
  use the same final stop path, so the receiver later sees `active=false`.
- A delayed heartbeat failure lacks a generation check and can stop a newer
  session.

These are confirmed code defects and the most plausible common explanation.
They do not establish which sequence occurred on the reporter's phone. There
is no evidence to justify disabling battery protection, adding a wake lock,
forcing accessibility settings, or changing the screen-capture backend.

## Resulting behavior

- Explicit global-mode intent is persisted separately from active GPU/capture
  resources. A successful first frame consumes the pending resume, not intent.
- Non-interactive or keyguard-locked devices release overlay, sensors, capture,
  and touch interception. Repeated suspension preserves intent.
- Screen-on, unlock, accessibility connection/window events, and Shizuku-ready
  notifications request recovery. Screen-on while locked checks again every
  second while interactive, covering a missed USER_PRESENT without capturing
  or intercepting the lock screen.
- Failed resume starts are limited to three attempts. Ordinary unlocked runtime
  failures still stop. Manual stop, trial mode, and disabling accessibility
  cancel intent. The app's stop button also cancels a pending reconnect.
- Service disconnect removes its observer/receiver once, clears only its own
  singleton, and retains intent only while the service remains enabled in
  system settings. Reconnection rebuilds the overlay and capture surface.
- Old heartbeat/probe/timeout results are invalidated. Observer registration is
  idempotent; an old service teardown cannot stop another live service's capture.

Android owns accessibility-service rebinding. A force-stopped app, revoked
permission, or OEM policy that prevents rebinding still requires user action.

## Validation

JDK 17, Android SDK 36, AGP 8.11.1, Gradle 8.14.

Unit test coverage includes delayed bridge readiness after unlock, repeated
screen-off/interruption, queued resume cancellation by manual stop, trial
exclusion, state restoration after recreation, bounded retries, and first-frame
completion. Existing motion and display-token tests are retained. All 44 test executions passed
(11 app tests in each flavor, plus 22 motion tests). Both Release and Debug
APKs compiled for both flavors. Release Lint completed without errors
(standard: 28 warnings; compat: 16 warnings).

Run:

```sh
./gradlew :app:testStandardDebugUnitTest :app:testCompatDebugUnitTest :motion:test
./gradlew :app:assembleStandardRelease :app:assembleCompatRelease
./gradlew :app:lintStandardRelease :app:lintCompatRelease
```

No Android device was attached during this work. Samsung S24 Ultra / One UI
screen-off, AOD, fingerprint/PIN/swipe/no-lock, rapid off/on, long sleep,
Shizuku reconnect, service recreation, and manual-stop regression checks remain
device acceptance work. JVM tests do not exercise real broadcasts, Binder, GPU,
or vendor power management.

## Signing and publication

The public v0.6.1 standard APK's signing-certificate SHA-256 is:

```text
b03d36688a1442148ac20c4dee953d7daf0bc842e0446e4a5fa96ea1dda40e02
```

It is an Android debug certificate, but its private key is specific to the old
build environment. The private key cannot be recovered from the APK. No matching
keystore was available here. Release builds therefore remain unsigned until
`-PduofoldKeystore=/path/to/original.keystore` is supplied. Debug builds generated
here use a different certificate and are test packages, not in-place upgrades.

Do not mark a new release as upgrade-compatible without verifying the signing
certificate. Do not close the issues or claim Samsung device verification based
only on the build and JVM tests.

## Platform references

- [Android Intent: SCREEN_ON, SCREEN_OFF, USER_PRESENT](https://developer.android.com/reference/android/content/Intent)
- [PowerManager.isInteractive](https://developer.android.com/reference/android/os/PowerManager#isInteractive())
- [KeyguardManager.isKeyguardLocked](https://developer.android.com/reference/android/app/KeyguardManager#isKeyguardLocked())
- [AccessibilityService lifecycle](https://developer.android.com/reference/android/accessibilityservice/AccessibilityService)
