# Project Overview

This is the **official Termux Android terminal emulator application** — an open-source terminal emulator that provides a Linux-like environment on Android devices. The project enables users to run shell commands, install packages via APT, and access a full suite of command-line tools (Bash, Zsh, Python, Git, SSH, etc.) without requiring root access.

The codebase is written in **Java** (Android application layer) with **C** (JNI native layer for PTY management), built using **Gradle** with the **Android Gradle Plugin**. It targets Android SDK 28 (Android 9.0 Pie) as `targetSdkVersion` — permanently locked due to Google's restrictions on native executable execution from apps targeting SDK 29+. The minimum SDK is 21 (Android 5.0 Lollipop).

**License:** GPLv3 (root app), Apache 2.0 (terminal-emulator and terminal-view libraries), MIT (termux-shared core).

**Version:** 0.118.0 (versionCode 118).

---

# Complete Project Structure

```
Termux/
├── .editorconfig                          # Editor config (4-space indent, LF, UTF-8)
├── .gitattributes                         # Line ending rules per file type
├── .gitignore                             # Build artifacts, IDE files, devnotes.md
├── .github/
│   ├── dependabot.yml                     # Daily GitHub Actions dependency updates
│   ├── FUNDING.yml                        # https://termux.dev/donate
│   ├── ISSUE_TEMPLATE/
│   │   ├── 01-bug-report.yml              # Structured bug report template
│   │   ├── 02-feature-request.yml         # Feature request template
│   │   └── config.yml                     # Disables blank issues
│   └── workflows/                         # 7 CI/CD workflows
│       ├── attach_debug_apks_to_release.yml
│       ├── debug_build.yml
│       ├── dependency-submission.yml
│       ├── gradle-wrapper-validation.yml
│       ├── release.yml                    # NEW: auto-versioning + GitHub Releases
│       ├── run_tests.yml
│       └── trigger_library_builds_on_jitpack.yml
├── app/                                   # Main Android application module
│   ├── build.gradle                       # App build config, bootstrap download, APK signing
│   ├── proguard-rules.pro                 # -dontobfuscate
│   ├── testkey_untrusted.jks             # PUBLIC shared test signing key
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml        # 18 permissions, 5 activities, 2 services
│       │   ├── cpp/                       # Native bootstrap extraction layer
│       │   │   ├── Android.mk             # Builds libtermux-bootstrap.so
│       │   │   ├── termux-bootstrap.c     # JNI: getZip() returns embedded ZIP bytes
│       │   │   └── termux-bootstrap-zip.S # Assembly: .incbin embeds bootstrap ZIP
│       │   ├── java/com/termux/app/       # 35 Java source files
│       │   │   ├── TermuxApplication.java
│       │   │   ├── TermuxActivity.java
│       │   │   ├── TermuxService.java
│       │   │   ├── TermuxOpenReceiver.java
│       │   │   ├── TermuxInstaller.java
│       │   │   ├── RunCommandService.java
│       │   │   ├── activities/
│       │   │   │   ├── SettingsActivity.java
│       │   │   │   └── HelpActivity.java
│       │   │   ├── api/file/
│       │   │   │   └── FileReceiverActivity.java
│       │   │   ├── event/
│       │   │   │   └── SystemEventReceiver.java
│       │   │   ├── fragments/settings/   # 12 preference fragments
│       │   │   ├── models/
│       │   │   │   └── UserAction.java
│       │   │   └── terminal/
│       │   │       ├── TermuxTerminalSessionActivityClient.java
│       │   │       ├── TermuxTerminalSessionServiceClient.java
│       │   │       ├── TermuxTerminalViewClient.java
│       │   │       ├── TermuxSessionsListViewController.java
│       │   │       ├── TermuxActivityRootView.java
│       │   │       └── io/
│       │   │           ├── TermuxTerminalExtraKeys.java
│       │   │           ├── TerminalToolbarViewPager.java
│       │   │           ├── KeyboardShortcut.java
│       │   │           └── FullScreenWorkAround.java
│       │   └── res/                       # 44 resource files across 11 directories
│       └── test/
│           └── java/com/termux/app/       # 2 unit test files
├── terminal-emulator/                     # Core VT100 terminal emulator library
│   ├── build.gradle                       # Published to JitPack as com.termux:terminal-emulator
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml        # Empty stub
│       │   ├── jni/
│       │   │   ├── Android.mk             # Builds libtermux.so
│       │   │   └── termux.c               # PTY creation, process spawning (230 lines)
│       │   └── java/com/termux/terminal/  # 15 Java source files
│       │       ├── ByteQueue.java
│       │       ├── JNI.java
│       │       ├── KeyHandler.java
│       │       ├── Logger.java
│       │       ├── TerminalBuffer.java
│       │       ├── TerminalColors.java
│       │       ├── TerminalColorScheme.java
│       │       ├── TerminalEmulator.java  # 2617 lines — the VT100 engine
│       │       ├── TerminalOutput.java
│       │       ├── TerminalRow.java
│       │       ├── TerminalSession.java
│       │       ├── TerminalSessionClient.java
│       │       ├── TextStyle.java
│       │       └── WcWidth.java
│       └── test/                          # 18 unit test files
├── terminal-view/                         # Android View widget for terminal display
│   ├── build.gradle                       # Published to JitPack as com.termux:terminal-view
│   └── src/main/java/com/termux/view/    # 8 Java source files
│       ├── GestureAndScaleRecognizer.java
│       ├── TerminalRenderer.java
│       ├── TerminalView.java              # 1500 lines — the main rendering View
│       ├── TerminalViewClient.java
│       ├── support/
│       │   └── PopupWindowCompatGingerbread.java
│       └── textselection/
│           ├── CursorController.java
│           ├── TextSelectionCursorController.java
│           └── TextSelectionHandleView.java
├── termux-shared/                         # Shared library for all Termux apps
│   ├── build.gradle                       # Published to JitPack as com.termux:termux-shared
│   ├── LICENSE.md                         # MIT (with GPLv3 exceptions for termux/*)
│   └── src/main/java/com/termux/shared/  # ~121 Java source files across 20 subdirs
│       ├── activities/
│       ├── activity/
│       ├── android/
│       ├── crash/
│       ├── data/
│       ├── errors/
│       ├── file/
│       ├── interact/
│       ├── jni/
│       ├── logger/
│       ├── markdown/
│       ├── models/
│       ├── net/
│       ├── notification/
│       ├── reflection/
│       ├── settings/
│       │   └── properties/               # TermuxAppSharedProperties, TermuxPropertyConstants
│       ├── shell/
│       │   ├── command/
│       │   │   ├── ExecutionCommand.java
│       │   │   ├── runner/
│       │   │   │   ├── app/AppShell.java
│       │   │   │   └── terminal/TermuxSession.java
│       │   │   └── ...
│       │   └── ...
│       ├── termux/
│       │   ├── TermuxConstants.java       # 1338 lines — single source of truth for all constants
│       │   ├── TermuxUtils.java           # 730 lines — utility methods
│       │   ├── am/
│       │   │   └── TermuxAmSocketServer.java
│       │   ├── extrakeys/
│       │   │   ├── ExtraKeysView.java     # 681 lines — extra keys rendering
│       │   │   ├── ExtraKeysInfo.java     # 213 lines — JSON config parser
│       │   │   └── ExtraKeyButton.java
│       │   ├── plugins/
│       │   │   └── TermuxPluginUtils.java
│       │   ├── shell/
│       │   │   ├── TermuxShellManager.java
│       │   │   └── TermuxShellEnvironment.java
│       │   └── ...
│       ├── theme/
│       └── view/
├── art/                                   # Icon/artwork generation scripts and SVGs
├── docs/                                  # Minimal docs placeholder
├── fastlane/                              # F-Droid/Play Store metadata
├── site/                                  # Sponsor logos
├── build.gradle                           # Root build: AGP 8.13.2
├── settings.gradle                        # 4 modules
├── gradle.properties                      # SDK versions, NDK, JVM args
├── gradle/wrapper/                        # Gradle 9.2.1
├── gradlew / gradlew.bat                  # Gradle wrapper scripts
├── devnotes.md                            # Developer notes (gitignored)
├── LICENSE.md                             # GPLv3 with Apache 2.0 exceptions
├── README.md                              # Comprehensive project docs
└── SECURITY.md                            # Links to termux.dev/security
```

---

# Architecture Explanation

## Module Dependency Graph

```
app
├── terminal-view (implementation)
│   └── terminal-emulator (api)
└── termux-shared (implementation)
    └── terminal-view (implementation)
        └── terminal-emulator (api)
```

The `app` module depends on both `terminal-view` and `termux-shared`. The `termux-shared` module depends on `terminal-view`, which depends on `terminal-emulator`. This creates a layered architecture:

1. **terminal-emulator** — Pure Java/C terminal emulation engine (no Android UI dependencies beyond JNI)
2. **terminal-view** — Android `View` subclass that renders terminal content and handles touch/key input
3. **termux-shared** — Shared utilities, constants, settings, and Termux-specific abstractions used by all Termux apps
4. **app** — The main Termux Android application that ties everything together

All three library modules are published to JitPack as Maven artifacts for use by Termux plugin apps.

## Architectural Layers

```
┌─────────────────────────────────────────────────────┐
│                    APPLICATION LAYER                 │
│  TermuxApplication → TermuxActivity → TermuxService  │
│  RunCommandService, FileReceiverActivity, Settings    │
├─────────────────────────────────────────────────────┤
│                   CLIENT LAYER                       │
│  TermuxTerminalSessionActivityClient                 │
│  TermuxTerminalViewClient                            │
│  TermuxTerminalSessionServiceClient                  │
├─────────────────────────────────────────────────────┤
│                    VIEW LAYER                        │
│  TerminalView → TerminalRenderer                     │
│  TextSelectionCursorController → HandleViews         │
│  ExtraKeysView → ExtraKeysInfo                       │
│  TerminalToolbarViewPager                            │
├─────────────────────────────────────────────────────┤
│                EMULATOR LAYER                        │
│  TerminalEmulator (VT100 state machine)              │
│  TerminalBuffer (circular screen memory)             │
│  TerminalRow (variable-width character storage)      │
│  TerminalColors / TerminalColorScheme                │
│  KeyHandler (key → escape sequence mapping)          │
│  WcWidth (Unicode character width)                   │
│  TextStyle (bit-packed cell style)                   │
├─────────────────────────────────────────────────────┤
│                   SYSTEM LAYER                       │
│  TerminalSession (PTY management)                    │
│  JNI → termux.c (fork/exec, PTY, TIOCSWINSZ)        │
│  ByteQueue (lock-free I/O queues)                    │
├─────────────────────────────────────────────────────┤
│                   SHARED LAYER                       │
│  TermuxConstants (paths, packages, URLs)             │
│  TermuxShellManager (session/task tracking)          │
│  TermuxShellEnvironment (env variables)              │
│  TermuxAppSharedProperties (runtime config)          │
│  TermuxPluginUtils (plugin result delivery)          │
│  TermuxAmSocketServer (local socket server)          │
│  ExecutionCommand (command lifecycle model)          │
└─────────────────────────────────────────────────────┘
```

## Threading Model

| Component | Thread | Notes |
|-----------|--------|-------|
| `TermuxApplication.onCreate` | Main | App startup |
| `TermuxActivity` lifecycle | Main | UI operations |
| `TermuxService.onStartCommand` | Main | Synchronized |
| `TermuxSession` PTY reader | Background | Reads from PTY fd |
| `TermuxSession` PTY writer | Background | Writes to PTY fd |
| `TermuxSession` waitpid | Background | Blocking wait |
| `TerminalSession.MainThreadHandler` | Main | Processes MSG_NEW_INPUT |
| `AppShell` execution | Background | Runtime.exec() |
| `StreamGobbler` (stdout/stderr) | Background | Reads subprocess output |
| Bootstrap installation | Background Thread | Extracts ZIP |
| Storage symlink creation | Background Thread | Creates ~/storage |
| Cursor blinker | Main (Handler) | Periodic invalidate |
| ExtraKeysView repeat | ScheduledExecutor | Background |
| TermuxAmSocketServer | Dedicated listener | LocalSocket |

---

# File-by-File Deep Analysis

## Root Configuration Files

### build.gradle (root)

**Purpose:** Top-level build configuration. Defines Android Gradle Plugin version and repository sources.

- **AGP Version:** `com.android.tools.build:gradle:8.13.2`
- **Repositories:** `mavenCentral()`, `google()`, `maven { url "https://jitpack.io" }` — JitPack is used for the `termux-am-library` dependency
- **No plugins applied** at root level — plugins are applied per module

### settings.gradle

**Content:** `include ':app', ':termux-shared', ':terminal-emulator', ':terminal-view'`

Four modules. No `rootProject.name` set.

### gradle.properties

| Property | Value | Significance |
|----------|-------|-------------|
| `org.gradle.jvmargs` | `-Xmx2048M` | 2GB heap for Gradle daemon |
| `android.useAndroidX` | `true` | AndroidX migration complete |
| `minSdkVersion` | `21` | Android 5.0 Lollipop — oldest supported |
| `targetSdkVersion` | `28` | **PERMANENT** — Google restricts native exec from SDK 29+ |
| `ndkVersion` | `29.0.14206865` | Latest stable NDK |
| `compileSdkVersion` | `36` | Compile against latest SDK |
| `markwonVersion` | `4.6.2` | Markdown rendering library |

### gradle/wrapper/gradle-wrapper.properties

- **Gradle version:** 9.2.1 (`gradle-9.2.1-bin.zip`)
- Network timeout: 10000ms
- Distribution URL validation enabled

### .editorconfig

- LF line endings, UTF-8 encoding
- 4-space indent for all files
- 2-space indent for YAML files
- Final newline required

### .gitignore

Ignores: `build/`, `release/`, `*.apk`, `*.so`, `.externalNativeBuild`, `.cxx`, `*.zip`, `local.properties`, `.gradle/`, `.signing/`, `.idea/`, `*.iml`, Vim swap files, OS files, `devnotes.md`.

### .gitattributes

- `*` — text=auto (auto-detect binary/text)
- `*.bat` — CRLF
- `*.gradle`, `*.mk`, `*.sh` — LF

### jitpack.yml

- JDK: OpenJDK 17
- NDK: `JITPACK_NDK_VERSION: "29.0.14206865"` — matches `gradle.properties`

---

## app/build.gradle

**Lines:** 243

This is the most complex build file in the project. Key sections:

### Package Variant

```groovy
ext.packageVariant = System.getenv("TERMUX_PACKAGE_VARIANT") ?: "apt-android-7"
```

Supports two bootstrap variants: `apt-android-7` (current, version 2026.02.12-r1) and `apt-android-5` (legacy, version 2022.04.28-r6).

### Dependencies

```groovy
implementation "androidx.annotation:annotation:1.9.0"
implementation "androidx.core:core:1.13.1"
implementation "androidx.drawerlayout:drawerlayout:1.2.0"
implementation "androidx.preference:preference:1.2.1"
implementation "androidx.viewpager:viewpager:1.0.0"
implementation "com.google.android.material:material:1.12.0"
implementation "com.google.guava:guava:24.1-jre"
implementation "io.noties.markwon:core:$markwonVersion"
// ... more markwon modules
implementation project(":terminal-view")
implementation project(":termux-shared")
```

### Version Validation

At configuration time, `versionName` is validated against Semantic Version 2.0.0 regex. This enforces semver compliance.

### Signing

Debug builds use `testkey_untrusted.jks` with a **publicly known** password (`xrj45yWGLbsO7W0v`). Release builds have **NO signing config** — they are unsigned. F-Droid signs its own builds.

### APK Splits

Per-ABI APK splitting: `x86`, `x86_64`, `armeabi-v7a`, `arm64-v8a`, plus universal. Controlled by env vars `TERMUX_SPLIT_APKS_FOR_DEBUG_BUILDS` (default "1") and `TERMUX_SPLIT_APKS_FOR_RELEASE_BUILDS` (default "0").

### Bootstrap Download Task

Custom Gradle task `downloadBootstrap` downloads ZIP files from `https://github.com/termux/termux-packages/releases/` with SHA-256 checksum verification. All 4 architectures × 2 variants = 8 possible ZIP files. Downloaded files are placed in `app/src/main/cpp/` for embedding via the assembly `.incbin` directive.

### NDK Build

```groovy
android.externalNativeBuild.ndkBuild {
    path "src/main/cpp/Android.mk"
}
```

C flags: `-std=c11 -Wall -Wextra -Werror -Os -fno-stack-protector -Wl,--gc-sections`

### Release Build

- `minifyEnabled true` (ProGuard/R8)
- `shrinkResources false` — explicitly disabled for reproducible builds
- ProGuard rules: `-dontobfuscate` only

---

## terminal-emulator/build.gradle

**Lines:** 85

Library module published as `com.termux:terminal-emulator:0.118.0`.

- NDK build: `src/main/jni/Android.mk`
- `unitTests.returnDefaultValues = true` — allows testing Android APIs without Robolectric
- Dependencies: `annotation:1.9.0`, `junit:4.13.2`
- Java 1.8 compatibility

### terminal-view/build.gradle

**Lines:** 63

Library module published as `com.termux:terminal-view:0.118.0`.

- API dependency on `project(":terminal-emulator")` — transitive
- No NDK build
- Dependencies: `annotation:1.9.0`, `junit:4.13.2`

### termux-shared/build.gradle

**Lines:** 100

Library module published as `com.termux:termux-shared:0.118.0`.

Key dependencies:
- `androidx.appcompat:appcompat:1.6.1`
- `com.google.android.material:material:1.12.0`
- `com.google.guava:guava:24.1-jre`
- `io.noties.markwon` (multiple modules)
- `org.lsposed.hiddenapibypass:hiddenapibypass:6.1` — bypasses hidden API restrictions
- `androidx.window:window:1.1.0`
- `commons-io:commons-io:2.5` — **pinned** at 2.5 to avoid runtime exceptions on Android < 8
- `com.termux:termux-am-library:v2.0.0` — Android Messaging socket client library
- `coreLibraryDesugaring "com.android.tools:desugar_jdk_libs:1.1.5"` — Java 8+ API backport

---

## AndroidManifest.xml (app)

**Lines:** 235

### Permissions (18)

```
INTERNET, READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE,
MANAGE_EXTERNAL_STORAGE, WAKE_LOCK, VIBRATE, FOREGROUND_SERVICE,
REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, SYSTEM_ALERT_WINDOW,
READ_LOGS, DUMP, WRITE_SECURE_SETTINGS, REQUEST_INSTALL_PACKAGES,
RECEIVE_BOOT_COMPLETED, PACKAGE_USAGE_STATS, SET_ALARM
```

Plus custom permission: `com.termux.permission.RUN_COMMAND` (dangerous).

### Components

**Activities (5 + 2 aliases):**
- `TermuxActivity` — main launcher, singleTask, exported
- `HelpActivity` — embedded WebView for wiki
- `SettingsActivity` — preference fragments
- `ReportActivity` — crash/error reports
- `FileReceiverActivity` — handles shared files/URLs
- `.HomeActivity` — IOT (Internet of Things) launcher alias
- `FileShareReceiverActivity` — SEND intent alias
- `FileViewReceiverActivity` — VIEW intent alias

**Services (2):**
- `TermuxService` — not exported, manages terminal sessions
- `RunCommandService` — exported, protected by RUN_COMMAND permission

**Providers (2):**
- `TermuxDocumentsProvider` — SAF documents provider
- `TermuxOpenReceiver$ContentProvider` — file sharing with external apps

**Receivers (3):**
- `TermuxOpenReceiver` — ACTION_VIEW/SEND handling
- `SystemEventReceiver` — BOOT_COMPLETED, PACKAGE events
- `ReportActivityBroadcastReceiver`

### Samsung DeX Support

Metadata for: keepalive, multidisplay, multiwindow. `android.max_aspect = 10.0` for Galaxy S8 full-screen.

---

## app/src/main/cpp/ — Native Bootstrap Layer

### Android.mk

Builds `libtermux-bootstrap.so` from:
- `termux-bootstrap-zip.S` — assembly file
- `termux-bootstrap.c` — JNI glue

### termux-bootstrap-zip.S (Assembly)

Uses `.incbin` directive to embed pre-compiled bootstrap ZIP directly into the `.rodata` section of the shared library. Architecture-conditional selection:

```asm
#if defined(__aarch64__)
  .incbin "bootstrap-aarch64.zip"
#elif defined(__arm__)
  .incbin "bootstrap-arm.zip"
#elif defined(__i386__)
  .incbin "bootstrap-i686.zip"
#elif defined(__x86_64__)
  .incbin "bootstrap-x86_64.zip"
#endif
```

Exports `blob` (raw ZIP bytes) and `blob_size` (length). This is the mechanism that provides the initial `$PREFIX` filesystem without requiring network access.

### termux-bootstrap.c (JNI)

Single JNI function: `Java_com_termux_app_TermuxInstaller_getZip` — returns the embedded bootstrap ZIP blob as a `jbyteArray`. Called by `TermuxInstaller.loadZipBytes()`.

---

## terminal-emulator/src/main/jni/termux.c

**Lines:** 230

The core native layer that manages PTY creation and process spawning.

### Helper Functions

#### `throw_runtime_exception(JNIEnv*, char const*)`
Finds `java/lang/RuntimeException` class and throws it. Returns -1 for convenience (allows `return throw_runtime_exception(...)` in error paths).

#### `free_string_array(char**)`
NULL-safe helper that frees a `malloc()`'d NULL-terminated array of strings, then frees the array itself. Iterates with `for (char** tmp = arr; *tmp; ++tmp) free(*tmp)`.

### `create_subprocess()` — The PTY Fork

This is the most critical function in the native layer:

**Step 1: Open PTY Master**
```c
int ptm = open("/dev/ptmx", O_RDWR | O_CLOEXEC);
```

**Step 2: Configure Slave**
```c
grantpt(ptm);
unlockpt(ptm);
ptsname_r(ptm, devname, sizeof(devname));  // or ptsname() on macOS
```

**Step 3: Set Terminal Flags**
```c
tcgetattr(pts, &term);
term.c_iflag |= IUTF8;          // UTF-8 mode
term.c_iflag &= ~(IXON | IXOFF); // Disable Ctrl+S/Ctrl+Q flow control
tcsetattr(pts, TCSANOW, &term);
```

**Step 4: Set Window Size**
```c
struct winsize ws = {rows, columns, columns*cell_width, rows*cell_height};
ioctl(pts, TIOCSWINSZ, &ws);
```

**Step 5: Fork**
```c
pid_t pid = fork();
```

**Parent process (pid > 0):** Stores child PID, returns master fd.

**Child process (pid == 0):**
1. Unblock all signals: `sigfillset()` + `sigprocmask(SIG_UNBLOCK)`
2. Close master fd
3. Create new session: `setsid()` — detaches from controlling terminal
4. Open slave PTY: `open(devname, O_RDWR)`
5. Redirect stdio: `dup2(pts, 0/1/2)`
6. Close all fds > 2: iterates `/proc/self/fd`
7. Set environment: `clearenv()` + `putenv()` for each var
8. Change directory: `chdir(cwd)`
9. Exec: `execvp(cmd, argv)`

### JNI Functions

#### `createSubprocess`
Converts Java arrays to C arrays via `GetStringUTFChars()` + `strdup()`. Uses `free_string_array()` for cleanup. Writes PID via `GetPrimitiveArrayCritical()` (zero-copy).

**Memory leak fix (commit 10b01ead):** Error paths now properly free `argv` and `envp` before returning. Previously, allocation failures leaked previously allocated arrays.

**Bug fix (commit 10b01ead):** `ReleaseStringUTFChars(env, cmd, cmd_cwd)` was incorrectly using `cmd` instead of `cwd`. This caused undefined behavior per JNI spec.

#### `setPtyWindowSize`
```c
struct winsize ws = {rows, cols, cell_width*cols, cell_height*rows};
ioctl(fd, TIOCSWINSZ, &ws);
```

#### `waitFor`
```c
waitpid(pid, &status, 0);
// Returns WEXITSTATUS(status) for normal exit
// Returns -WTERMSIG(status) for signal kill
```

#### `close`
```c
close(fd);
```

---

## terminal-emulator Module — Complete Java Source Analysis

### ByteQueue.java (108 lines)

**Purpose:** Lock-free circular byte buffer for producer-consumer I/O between PTY threads.

**Algorithm:** Classic ring buffer with `mHead` (read cursor) and `mStoredBytes` (fill level). Uses `synchronized` + `wait()`/`notify()` for blocking. When buffer is full, `write()` blocks. When data is read from full buffer, `notify()` wakes writers.

**Two instances per session:**
- `mProcessToTerminalIOQueue` — 64KB (PTY → emulator)
- `mTerminalToProcessIOQueue` — 4KB (keyboard → PTY)

**Two-phase copy handles wraparound:** If head is near end of array, copies in two parts: `[head, end]` then `[0, remaining]`.

### JNI.java (41 lines)

Loads `libtermux.so` via `System.loadLibrary("termux")`. Declares 4 native methods:
- `createSubprocess()` — returns master fd
- `setPtyWindowSize()` — sends TIOCSWINSZ
- `waitFor()` — waitpid wrapper
- `close()` — close fd wrapper

### KeyHandler.java (373 lines)

Maps Android key codes + modifier bitmasks to terminal escape sequences.

**Modifier constants:**
- `KEYMOD_ALT = 0x80000000`
- `KEYMOD_CTRL = 0x40000000`
- `KEYMOD_SHIFT = 0x20000000`
- `KEYMOD_NUM_LOCK = 0x10000000`

**Main method `getCode()`** is a giant switch over all Android `KeyEvent.KEYCODE_*` values. Handles:
- Arrow keys (application cursor mode `ESC O` vs normal `ESC [`)
- Function keys F1-F12
- Numpad with numlock dual behavior
- Insert/Delete/PageUp/PageDown
- Tab (back-tab when shifted)
- Enter (Alt+Enter prepends ESC)
- DEL (Ctrl+DEL → BS)

**`transformForModifiers()`** encodes modifier parameter per xterm spec:
- Shift=2, Alt=3, Shift+Alt=4, Ctrl=5, Ctrl+Shift=6, Ctrl+Alt=7, etc.
- Example: Ctrl+Right → `\033[1;5C`

**Termcap support:** `TERMCAP_TO_KEYCODE` maps termcap key names to Android keycodes for DCS termcap/terminfo responses. Recent fix (commit 30ebb2de) corrected inverted `kN`/`kP` mappings for Page Up/Down.

### Logger.java (80 lines)

Logging facade. If a `TerminalSessionClient` is available, delegates to it. Otherwise falls back to `android.util.Log`. Provides `logError`, `logWarn`, `logInfo`, `logDebug`, `logVerbose`, and stack trace formatting methods.

### TerminalBuffer.java (497 lines)

**Purpose:** Circular buffer of `TerminalRow` objects managing screen content and scrollback history.

**Core data structure:** `mLines` array of `TerminalRow` objects, indexed via circular mapping.

**Circular buffer mapping:**
```java
internalRow = (mScreenFirstRow + externalRow + mTotalRows) % mTotalRows
```
External row 0 is the first visible screen row. Negative rows are scrollback history.

**Key methods:**
- `getTranscriptText()` — returns full transcript as string
- `getSelectedText()` — extracts text from coordinate range
- `resize()` — handles column width changes (re-wraps all content)
- `scrollDownOneLine()` — shifts `mScreenFirstRow`, increments `mActiveTranscriptRows`
- `blockCopy()` / `blockSet()` — rectangular region operations
- `setChar()` — sets single character in row

**Resize algorithm:**
- Fast path: only rows changed → copy lines, skip blanks
- Slow path: columns changed → reconstruct entire screen character by character, re-wrapping lines and relocating cursor

### TerminalColors.java (96 lines)

Manages the 259-color palette (256 indexed + foreground/background/cursor). Colors can be dynamically changed via OSC 4 (set color) and OSC 104 (reset colors) escape sequences.

**Color parsing:** Supports `#RGB`, `#RRGGBB`, `rgb:R/G/B` formats.

**HSP brightness:** `getPerceivedBrightnessOfColor()` calculates Human Short-term Perceptual brightness for cursor auto-color.

### TerminalColorScheme.java (126 lines)

Defines the default 259-color palette. Xterm-compatible with brighter blue (`#5555FF` instead of `#0000AA`). Supports overriding colors from `termux.properties` via `updateWith(Properties)`. Auto-selects cursor color (white/black) based on background brightness.

### TerminalEmulator.java (2617 lines)

**The heart of the project.** A complete VT100/xterm terminal emulator engine that parses byte streams and interprets escape sequences.

**Escape state machine:** 24 states covering ESC, CSI, OSC, DCS, APC, and various intermediate states.

**Key features:**
- UTF-8 decoding with overlong validation
- VT100 line drawing character translation (G0/G1)
- SGR (Select Graphic Rendition) with 24-bit truecolor
- DECSET/DECRST with 17+ mode flags
- Mouse tracking (X10, button event, SGR protocols)
- Alternate screen buffer
- Scrolling regions (top/bottom/left/right margins)
- Insert/replace mode
- Tab stops
- Cursor styles (block/underline/bar)
- Bracketed paste mode
- Title stack (push/pop)

**`append(byte[], int)`** — Entry point. Feeds raw bytes from PTY.

**`processByte(byte)`** — UTF-8 state machine → `processCodePoint()`.

**`processCodePoint(int)`** — Main dispatch. Checks for string terminators and control characters, then dispatches to appropriate handler based on `mEscapeState`.

**`doCsi(int)`** — CSI handler. 40+ cases including: cursor movement (CUU/CUD/CUF/CUB/CUP), erase (ED/EL), scroll (SU/SD), SGR, margins (DECSTBM/DECSLRM), device status report, mode set/reset.

**`selectGraphicRendition()`** — SGR parser. Handles: bold, dim, italic, underline, blink, inverse, invisible, strikethrough, protected, foreground/background colors (standard 8, 256-indexed, 24-bit truecolor).

**`emitCodePoint(int)`** — VT100 line drawing translation, autowrap handling, insert mode, character placement in `TerminalRow`.

### TerminalRow.java (283 lines)

Single row of terminal cells. Handles variable-width characters, combining characters, and wide (CJK) characters in a compact `char[]` + `long[]` style array.

**`mText`** — Variable-length char array storing the row's character data.
**`mStyle`** — Fixed-length long array (one per display column) storing bit-packed styles.

**`setChar()`** algorithm:
1. Fast path: if no wide/surrogate chars, direct array write
2. Handles combining characters (added to existing column)
3. Handles replacing half of a wide character
4. Uses `findStartOfColumn()` for column→index mapping (handles wide chars and combining marks)
5. Shifts `mText` array when char count changes
6. Grows array by `mColumns` when needed

### TerminalSession.java (373 lines)

Top-level session object. Owns the subprocess, I/O threads, emulator, and bridges to the UI client.

**Thread architecture:**
1. **TermSessionInputReader** — reads from PTY fd → writes to `mProcessToTerminalIOQueue` → sends `MSG_NEW_INPUT`
2. **TermSessionOutputWriter** — reads from `mTerminalToProcessIOQueue` → writes to PTY fd
3. **TermSessionWaiter** — calls `JNI.waitFor(pid)` → sends `MSG_PROCESS_EXITED`
4. **Main thread** — processes `MSG_NEW_INPUT` (emulator append + screen update)

**`initializeEmulator()`** — Creates the emulator, opens the PTY via `JNI.createSubprocess()`, and starts the 3 background threads.

**`updateSize()`** — Called when terminal is resized. Calls `JNI.setPtyWindowSize()` + `mEmulator.resize()`.

### TextStyle.java (90 lines)

Bit-packed encoding of foreground color, background color, and effect flags into a 64-bit `long`.

**Bit layout:**
```
Bits 0-8:   Effect flags (11 bits: bold, italic, underline, blink, inverse, invisible, strikethrough, protected, dim, truecolor_fg, truecolor_bg)
Bits 9-15:  Unused
Bits 16-39: Background color (9-bit index or 24-bit truecolor)
Bits 40-63: Foreground color (9-bit index or 24-bit truecolor)
```

If color top byte is `0xff`, it's a true color stored in lower 24 bits. Otherwise it's a 9-bit index.

### WcWidth.java (566 lines)

Unicode character width calculation (0, 1, or 2 columns). Implements `wcwidth(3)` for Unicode 15.

**Algorithm:**
1. Returns 0 for: null, zero-width characters (U+200B-U+200F, etc.)
2. Returns 0 for control characters (< 32 or 0x7F-0x9F)
3. Binary search in `ZERO_WIDTH` table for combining characters → 0
4. Binary search in `WIDE_EASTASIAN` table → 2
5. Otherwise → 1

**Data:** 363 zero-width ranges, 123 wide East Asian ranges.

---

## terminal-view Module — Complete Java Source Analysis

### GestureAndScaleRecognizer.java (112 lines)

Wraps `GestureDetector` and `ScaleGestureDetector` into a single touch processor. Delegates scroll/fling/down/longPress/scale/tap events to a `Listener` interface. Used by `TerminalView` for touch input handling.

### TerminalRenderer.java (249 lines)

**Purpose:** Renders terminal content onto an Android `Canvas`.

**`render()` method:**
1. Gets emulator state: reverse video, cursor position/shape, palette colors, screen buffer
2. Draws reverse-video background if active
3. Iterates rows from `topRow` to `topRow + mRows`
4. For each row, groups characters into **runs** sharing: same style, cursor state, selection state
5. Handles surrogate pairs and wide characters via `WcWidth.width()`
6. Calls `drawTextRun()` for each completed run

**`drawTextRun()` method:**
1. Decodes `TextStyle` into foreground/background colors and effect flags
2. Bold brightens foreground colors 0-7 by adding 8
3. Applies reverse-video XOR
4. If measured width ≠ expected width, applies canvas scaling (non-monospace font fix)
5. Draws background rectangle (only if non-default)
6. Draws cursor: block (full height, inverts text), underline (¼ height), bar (¾ width)
7. Applies dim (2/3 RGB channels per libvte/xterm spec)
8. Draws text via `canvas.drawTextRun()`

### TerminalView.java (1500 lines)

The main Android `View` that displays terminal content.

**Key responsibilities:**
- Touch event handling → keyboard input
- Key event handling → escape sequence generation
- Mouse event handling → terminal mouse protocol
- Screen update rendering
- Text selection with drag handles
- Cursor blinking animation
- Pinch-to-zoom font size
- AutoFill support (API 26+)
- Soft keyboard management
- Scroll bar support

**`onKeyDown()` (~170 lines):**
Massive method handling: Ctrl+key combinations, Fn key mappings (WASD→arrows, etc.), virtual keys from ExtraKeysView, text selection mode keys, hardware keyboard shortcuts, Enter key behavior for finished sessions.

**`onCreateInputConnection()`:**
Creates a `BaseInputConnection` that intercepts `commitText()` and `finishComposingText()` to send text to the terminal. Handles `deleteSurroundingText()` by sending KEYCODE_DEL events.

**`inputCodePoint()`** — Central input method. Filters through:
1. `mClient.onCodePoint()` — allows client to intercept
2. `handleKeyCode()` — checks if codepoint is a special key mapping
3. `handleKeyCodeAction()` — executes key action
4. `mTermSession.writeCodePoint()` — sends to terminal

**Cursor blinking:**
`TerminalCursorBlinkerRunnable` toggles `mEmulator.setCursorBlinkState()`, calls `invalidate()`, and re-posts itself via `mTerminalCursorBlinkerHandler.postDelayed(this, mBlinkRate)`.

**Text selection:**
Delegates to `TextSelectionCursorController` which manages two `TextSelectionHandleView` instances as `PopupWindow`s. Supports word expansion, auto-scroll when dragging past edges, and wide-character boundary detection.

### TerminalViewClient.java (83 lines)

Pure interface — callback contract between `TerminalView` and its host Activity. Splits into: configuration queries, input interception, state notifications, and logging.

### TextSelectionCursorController.java (407 lines)

Manages text selection within `TerminalView`. Handles:
- Initial selection via word boundary expansion
- Two drag handles (LEFT/RIGHT) as `PopupWindow`s
- ActionMode with Copy/Paste/More menu
- Auto-scroll when dragging past top/bottom edge
- Wide-character-aware column clamping
- 300ms debounce in `hide()` to prevent accidental dismissal

### TextSelectionHandleView.java (352 lines)

Individual selection handle as a `PopupWindow`. Handles:
- Drag tracking via `onTouchEvent()` (DOWN/MOVE/UP)
- Auto-flip (LEFT↔RIGHT) near terminal view edges (throttled to 50ms)
- Smooth repositioning via `PopupWindow.update()`
- Position validation against terminal bounds

---

## app Module — Complete Java Source Analysis

### TermuxApplication.java (85 lines)

**Application entry point.** `onCreate()` initializes:
1. Crash handler via `TermuxCrashUtils.setDefaultCrashHandler()`
2. Log configuration from SharedPreferences
3. `TermuxBootstrap` package manager and variant from BuildConfig
4. `TermuxAppSharedProperties` (loads `termux.properties`)
5. `TermuxShellManager` (session/task tracking singleton)
6. Night mode from properties
7. Termux files directory accessibility check
8. `TermuxAmSocketServer` (local socket server for `am` command)
9. `TermuxShellEnvironment` constants and cache
10. Writes environment to file

### TermuxActivity.java (1013 lines)

The main terminal activity. Extends `AppCompatActivity`, implements `ServiceConnection`.

**Lifecycle:**
- `onCreate()`: Loads properties, sets theme, initializes views, starts+binds to `TermuxService`, sends "Termux opened" broadcast
- `onServiceConnected()`: If no sessions, runs bootstrap installer then creates first session. If sessions exist, restores stored session or creates from intent
- `onStart()`/`onResume()`/`onStop()`: Delegates to client classes, manages broadcast receiver registration

**Key fields:**
- `mTermuxService` — bound service reference
- `mTerminalView` — the terminal display
- `mTermuxTerminalViewClient` — input/key handling
- `mTermuxTerminalSessionActivityClient` — UI callbacks
- `mExtraKeysView` / `mTermuxTerminalExtraKeys` — extra keys
- `mTermuxSessionsListViewController` — session list adapter
- `mIsInvalidState` — prevents running if setup fails

**Context menu** (long-press): Select URL, share transcript, share text, paste, reset terminal, kill process, change style, toggle keep screen on, help, settings, report issue.

**Broadcast receiver:** Handles `ACTION_NOTIFY_APP_CRASH`, `ACTION_RELOAD_STYLE`, `ACTION_REQUEST_PERMISSIONS`.

### TermuxService.java (959 lines)

The foreground service that manages terminal sessions. Extends `Service`, implements `AppShell.AppShellClient` and `TermuxSession.TermuxSessionClient`.

**Session management:**
- `mShellManager.mTermuxSessions` — foreground sessions
- `mShellManager.mTermuxTasks` — background tasks
- All session/task methods are `synchronized`

**Intent handling (`onStartCommand`):**
- `ACTION_STOP_SERVICE` — kills all commands, stops service
- `ACTION_WAKE_LOCK` / `ACTION_WAKE_UNLOCK` — power management
- `ACTION_SERVICE_EXECUTE` — parses intent extras into `ExecutionCommand`, routes to session or task creation

**Client swap pattern:**
When activity binds: swaps all sessions to use `TermuxTerminalSessionActivityClient` (full UI).
When activity unbinds: swaps to `TermuxTerminalSessionServiceClient` (minimal fallback).

**Notification:** Shows session count, task count, wake lock status. Actions: exit service, toggle wake lock.

**Foreground service:** Returns `START_NOT_STICKY`. Must outlive the activity.

### TermuxInstaller.java (386 lines)

Handles bootstrap package installation.

**`setupBootstrapIfNeeded()`:**
1. Validates primary user
2. Checks if prefix directory exists and is non-empty (skip if so)
3. Shows ProgressDialog
4. Spawns background thread:
   - Deletes staging/prefix directories
   - Creates them
   - Extracts bootstrap ZIP from native library via `getZip()` JNI
   - Processes `SYMLINKS.txt` (creates symbolic links)
   - Renames staging to prefix
   - Writes environment file

**`setupStorageSymlinks()`:** Creates symlinks in `~/storage/` for shared, documents, downloads, dcim, pictures, music, movies, podcasts, audiobooks, external-N, media-N.

### RunCommandService.java (287 lines)

Entry point for `RUN_COMMAND` intents from third-party apps. Validates the intent, builds an `ExecutionCommand`, and forwards to `TermuxService` via intent.

**Validation pipeline:**
1. Action must be `ACTION_RUN_COMMAND`
2. Runner must be valid (TERMINAL_SESSION or APP_SHELL)
3. `allow-external-apps` policy check (forced notification on violation)
4. Executable must be set
5. Canonical path resolution
6. File permissions validation (read + execute)
7. Working directory validation (if set)
8. Symlink handling for applets (coreutils/busybox)

### TermuxOpenReceiver.java (235 lines)

BroadcastReceiver + ContentProvider for file sharing.

**BroadcastReceiver:** Handles `ACTION_VIEW` (opens URLs) and `ACTION_SEND` (shares files/text via content URIs with `FLAG_GRANT_READ_URI_PERMISSION`).

**ContentProvider:** Serves files to external apps. Security checks:
- File must be under `TERMUX_FILES_DIR` or external storage
- `allow-external-apps` policy must be enabled
- Property files are forced read-only

### SystemEventReceiver.java (91 lines)

Singleton BroadcastReceiver for `BOOT_COMPLETED` and `PACKAGE_ADDED/REMOVED/REPLACED`.

**Boot completed:** Delegates to `TermuxShellManager.onActionBootCompleted()`.
**Package update:** If the updated package is a Termux plugin, rewrites the environment file (which includes plugin paths).

Registered dynamically in `TermuxService.onCreate()`, unregistered in `TermuxService.onDestroy()`.

### TermuxTerminalSessionActivityClient.java (528 lines)

Full UI client for terminal sessions. Handles:
- Session lifecycle (add/remove/switch/rename)
- Text changes and title updates
- Bell sound (vibrate, beep, or ignore)
- Font and color loading from properties
- Background color updates
- Session list notifications
- Max 8 sessions enforcement
- Auto-remove on exit 0 or 130

### TermuxTerminalViewClient.java (802 lines)

Input handling client. Handles:
- Volume keys as virtual Ctrl (volume down) and Fn (volume up)
- Fn key mappings: WASD→arrows, P/N→page, H→~, U→_, L→|, 1-9→F1-F9, E→Escape, V→volume
- Ctrl+Alt hardware shortcuts: N/P→sessions, arrows→drawer, K→keyboard, M→menu, etc.
- Session shortcuts loaded from `termux.properties`
- Soft keyboard management (enable/disable toggle and show/hide toggle modes)
- Cursor blinker state management
- Report generation from transcript

### TermuxSessionsListViewController.java (99 lines)

ArrayAdapter for the session list in the navigation drawer. Renders each session as `[N] sessionName\nsessionTitle` with bold for number+name, italic for title. Running sessions show normal text; exited sessions show strikethrough + red for non-zero exit status.

### TermuxActivityRootView.java (284 lines)

Custom LinearLayout implementing keyboard overlap detection. Uses a 1sp transparent "bottom space view" at the bottom of the layout. `onGlobalLayout()` compares its position to the window available rect — if hidden by keyboard, calculates pixel offset and adds bottom margin to push content up. Uses 40ms time-based throttling to prevent infinite layout loops.

### FullScreenWorkAround.java (68 lines)

Workaround for `SOFT_INPUT_ADJUST_RESIZE` not working in fullscreen. When keyboard appears (>25% height difference), manually resizes the content view to exclude keyboard + nav bar.

### TermuxTerminalExtraKeys.java (108 lines)

Handles extra key button clicks. Special keys: KEYBOARD (toggle keyboard), DRAWER (toggle nav drawer), PASTE (paste from clipboard), SCROLL (toggle auto-scroll). Falls back to default extra keys on JSON parse error.

### TerminalToolbarViewPager.java (117 lines)

2-page ViewPager at the bottom of the terminal:
- Page 0: Extra keys view (ExtraKeysView)
- Page 1: Text input (EditText that writes to terminal on enter)

### KeyboardShortcut.java (13 lines)

Simple data class holding `codePoint` → `shortcutAction` mapping.

### FileReceiverActivity.java (287 lines)

Transparent activity that handles shared files/URLs. Files are saved to `~/downloads/`, user is prompted for filename, and `~/bin/termux-file-editor` script is run. URLs are passed to `~/bin/termux-url-opener` script.

### SettingsActivity.java (169 lines)

Preferences UI. Loads `RootPreferencesFragment` which shows:
- Termux (navigates to sub-screens for debugging, I/O, view settings)
- Termux:API (hidden if not installed)
- Termux:Float (hidden if not installed)
- Termux:Tasker (hidden if not installed)
- Termux:Widget (hidden if not installed)
- About
- Donate (hidden for Google Play builds)

### HelpActivity.java (77 lines)

Embedded WebView for Termux wiki. Only loads URLs starting with the wiki URL inline; external URLs open in system browser.

### Preference Fragments (12 files)

All follow the same pattern: `PreferenceFragmentCompat` + singleton `PreferenceDataStore` subclass wrapping a specific `*AppSharedPreferences` class. Handle: log level, key logging, plugin error notifications, crash reports, soft keyboard settings, terminal margin adjustment.

---

## termux-shared Module — Key File Analysis

### TermuxConstants.java (1338 lines)

**Single source of truth** for ALL shared constants. Defines:
- Organization URLs and F-Droid/GitHub/Play Store links
- Package names for all 7 Termux apps
- APK signing certificate SHA-256 digests (F-Droid, GitHub, Play, Termux Devs)
- Complete directory tree paths (`/data/data/com.termux/files/usr/bin`, etc.)
- Properties file paths (primary: `~/.termux/termux.properties`, secondary: `~/.config/termux/termux.properties`)
- Notification channel IDs and names
- Intent action and extra constants
- `RUN_COMMAND` API extras
- Nested classes for each app's constants

All constants are `public static final`. Path constants are built compositionally. Both `String` and `File` variants provided for directory paths.

### TermuxUtils.java (730 lines)

Utility methods for:
- Package context retrieval (one method per plugin)
- App install/access checks
- APK reflection (reads BuildConfig fields via classloader)
- URI helpers
- Broadcast sending (Oreo+ workaround for explicit broadcasts)
- Markdown report generation (app info, plugin info, issue reports)
- APT info retrieval (runs embedded shell script)
- Debug logcat dump (runs `logcat -d -t 3000`)
- APK release identification (maps signing cert to release name)

### TermuxShellManager.java (123 lines)

Singleton managing session/task lists and counters.

**Fields:** `mTermuxSessions` (List<TermuxSession>), `mTermuxTasks` (List<AppShell>), `mPendingPluginExecutionCommands`, auto-incrementing `SHELL_ID`.

**Counters:** `APP_SHELL_NUMBER_SINCE_APP_START`, `TERMINAL_SESSION_NUMBER_SINCE_APP_START` — exported to shell environments. Reset on boot completed and app exit.

### TermuxShellEnvironment.java (117 lines)

Builds the complete environment for shell sessions. Extends `AndroidShellEnvironment` with Termux-specific additions.

**Environment construction:**
1. Android base environment
2. Termux app environment overlay
3. Termux API environment overlay
4. `HOME` and `PREFIX` set
5. Non-failsafe: `TMPDIR`, `PATH` (Termux `$PREFIX/bin`), `LD_LIBRARY_PATH`
6. Android 5 variant: includes `/applets` subdirectory

**Failsafe mode:** Preserves system PATH for `/system/bin/sh` fallback.

### ExtraKeysView.java (681 lines)

Custom `GridLayout` rendering configurable extra keys (ESC, CTRL, ALT, arrows, etc.).

**Core rendering:** `reload(ExtraKeysInfo, float heightPx)` — clears grid, iterates 2D button matrix, creates `MaterialButton` per key, sets click/touch listeners.

**Touch handling:** Active background color on press, long-press start/stop, popup show/dismiss on swipe up, click dispatch.

**Key repeat:** `ScheduledExecutorService` with `scheduleWithFixedDelay` for repetitive keys (arrows, space). Special buttons lock on long-press via `Handler(Looper.getMainLooper())`.

**Special buttons:** CTRL, ALT, SHIFT, FN — each has active/inactive/locked states.

**Haptic feedback:** Checks system haptic setting, handles DND mode pre-API 28.

### ExtraKeysInfo.java (213 lines)

Parses JSON configuration for extra keys into a 2D matrix of `ExtraKeyButton` objects.

**JSON format:** Array of arrays (rows of keys). Each key can be a string (`"ESC"`) or object (`{"key": "CTRL", "popup": {"macro": "CTRL f d", "display": "tmux exit"}}`).

**Display maps:** Style-based alias resolution: "arrows-only", "arrows-all", "all", "none".

### TermuxAppSharedProperties.java (42 lines)

Thin singleton wrapper. Loads from first found file in `TERMUX_PROPERTIES_FILE_PATHS_LIST` (primary `~/.termux/termux.properties`, fallback `~/.config/termux/termux.properties`). Actual parsing/validation in parent `TermuxSharedProperties`.

### TermuxPropertyConstants.java (481 lines)

Defines ALL property key names, value constants, bidirectional maps, defaults, and valid sets.

**30+ properties** including:
- Booleans: `allow-external-apps`, `hide-soft-keyboard-on-startup`, `fullscreen`, etc.
- Integers: `bell-character`, `terminal-cursor-blink-rate`, `terminal-cursor-style`, etc.
- Strings: `back-key`, `extra-keys`, `extra-keys-style`, `night-mode`, `volume-keys`, etc.
- Hardware key shortcuts: `shortcut.create-session`, `shortcut.next-session`, etc.

### TermuxSession.java (296 lines)

Foreground interactive terminal sessions. Wraps `TerminalSession` with `ExecutionCommand` lifecycle.

**Shell selection logic:**
- Non-failsafe: `bash` → `zsh` → `sh` in `$PREFIX/bin`
- Failsafe: `/system/bin/sh` (mksh), NOT a login shell
- Login shell name prefixed with `-` (e.g., `-bash`)

**Static factory `execute()`:** Resolves executable, sets up args, builds environment, creates `TerminalSession`, returns wrapped `TermuxSession`.

### AppShell.java (349 lines)

Background shell execution using `Runtime.exec()`. Supports synchronous and async execution with stdout/stderr capture via `StreamGobbler` threads.

**Execution flow:**
1. `Runtime.getRuntime().exec(commandArray, environmentArray, workDir)`
2. Creates `DataOutputStream` for STDIN, `StreamGobbler` for STDOUT/STDERR
3. Writes stdin (handles EPIPE gracefully)
4. `process.waitFor()`
5. Joins gobbler threads, destroys process
6. Calls `processAppShellResult()`

### TermuxPluginUtils.java (469 lines)

Plugin command result delivery. Two mechanisms:
1. **PendingIntent** — result sent back to caller app via Bundle extras
2. **Directory** — result written to files in specified directory

Also handles: error reporting, error notifications, external app policy checking.

### TermuxAmSocketServer.java (232 lines)

Local filesystem socket server providing a fast alternative to Android's `am` command. Uses Unix domain sockets instead of starting a new dalvik VM per command.

**Socket path:** `.../apps/com.termux/termux-am/am.sock`
**Security:** Only termux user and root can connect.

Started by `TermuxApplication`. State exported to shell environments via `TERMUX_APP__AM_SOCKET_SERVER_ENABLED`.

---

# Execution Flow

## Application Startup

```
1. Android creates TermuxApplication process
2. TermuxApplication.onCreate()
   ├── Sets crash handler
   ├── Configures logging
   ├── Sets TermuxBootstrap constants
   ├── Loads TermuxAppSharedProperties (termux.properties)
   ├── Initializes TermuxShellManager singleton
   ├── Sets night mode
   ├── Checks/files files directory accessibility
   ├── Starts TermuxAmSocketServer (if enabled)
   ├── Initializes TermuxShellEnvironment constants
   └── Writes environment to file
```

## Terminal Session Creation

```
1. User opens Termux or requests new session
2. TermuxActivity.onCreate()
   ├── Loads properties, sets theme
   ├── Initializes views (TerminalView, ExtraKeysView, Drawer)
   ├── Starts + binds to TermuxService
   └── Sends "Termux opened" broadcast

3. TermuxActivity.onServiceConnected()
   ├── If no sessions exist:
   │   ├── TermuxInstaller.setupBootstrapIfNeeded()
   │   │   ├── Validates primary user
   │   │   ├── Shows ProgressDialog
   │   │   ├── Background: extracts bootstrap ZIP via JNI
   │   │   ├── Processes SYMLINKS.txt
   │   │   └── Writes environment file
   │   └── Creates first session
   └── If sessions exist:
       └── Restores stored session or creates from intent

4. TermuxTerminalSessionActivityClient.addNewSession()
   ├── Enforces max 8 sessions
   ├── Gets working directory from current session
   ├── Calls TermuxService.createTermuxSession()
   │   ├── TermuxSession.execute()
   │   │   ├── Resolves shell (bash/zsh/sh)
   │   │   ├── Sets up command args
   │   │   ├── Builds environment
   │   │   └── Creates TerminalSession (actual PTY)
   │   ├── Adds to ShellManager.mTermuxSessions
   │   ├── Notifies activity list
   │   └── Updates notification
   └── Sets as current session
```

## PTY I/O Flow

```
PTY → Screen (reading):
  PTY fd → TermSessionInputReader thread → mProcessToTerminalIOQueue
  → MSG_NEW_INPUT → Main thread → mEmulator.append()
  → processByte() → processCodePoint() → escape state machine
  → emitCodePoint() → mScreen.setChar() → TerminalRow.mText/mStyle

Keyboard → PTY (writing):
  Key event → TerminalView.onKeyDown() → mTermSession.write()
  → mTerminalToProcessIOQueue → TermSessionOutputWriter thread → PTY fd
```

## Escape Sequence Processing

```
Raw bytes → TerminalEmulator.append()
  → processByte() — UTF-8 state machine
  → processCodePoint() — main dispatch
    ├── Control chars: BEL, BS, HT, LF, CR, ESC, etc.
    ├── ESC states: ESC, CSI, OSC, DCS, APC
    ├── CSI handler: cursor movement, erase, scroll, SGR, margins
    └── SGR: bold, colors, effects → TextStyle encoding
```

---

# Data Flow

## Configuration Flow

```
termux.properties (disk)
  → TermuxAppSharedProperties (loaded at startup)
    → TermuxPropertyConstants (validated)
      → UI components read via getProperties()
        → TerminalView, ExtraKeysView, Session settings
```

## Environment Variable Flow

```
Android base environment
  → TermuxAppShellEnvironment overlay
    → TermuxAPIShellEnvironment overlay
      → HOME, PREFIX, TMPDIR, PATH, LD_LIBRARY_PATH
        → Written to TERMUX_ENV_FILE_PATH (for sourcing)
          → Each TermuxSession/AppShell gets merged env
```

## Plugin Command Flow

```
Third-party app → RUN_COMMAND intent → RunCommandService
  → Validates (action, runner, permissions, executable)
    → Builds ExecutionCommand
      → Forwards to TermuxService via intent
        → TermuxService.actionServiceExecute()
          → Parses into ExecutionCommand
            → Routes to:
              ├── executeTermuxSessionCommand() → TermuxSession.execute()
              └── executeTermuxTaskCommand() → AppShell.execute()
                  → After completion:
                    → TermuxPluginUtils.processPluginExecutionCommandResult()
                      → ResultSender → PendingIntent / File directory
```

---

# Internal Design

## Key Design Patterns

### 1. Client Swap Pattern
`TermuxService` maintains two `TerminalSessionClient` implementations:
- `TermuxTerminalSessionActivityClient` — full UI (when activity bound)
- `TermuxTerminalSessionServiceClient` — minimal fallback (when no activity)

When activity binds/unbinds, the service swaps clients on all active sessions. This ensures sessions survive activity destruction.

### 2. Singleton Pattern
All `PreferenceDataStore` subclasses use `synchronized static getInstance()`. `TermuxShellManager` is a singleton. `SystemEventReceiver` is a singleton.

### 3. Local Binder Pattern
Both `TermuxService` and `RunCommandService` use inner `LocalBinder` classes for in-process binding without IPC. The binder directly exposes the service reference.

### 4. Observer Pattern
`TerminalSessionClient` and `TerminalViewClient` interfaces serve as observer callbacks from the terminal emulator back to the UI layer.

### 5. Factory Pattern
`TermuxSession.execute()` and `AppShell.execute()` are static factories that encapsulate the complex initialization logic.

### 6. Workaround Patterns
- `TermuxActivityRootView` — keyboard overlap via dynamic margins
- `FullScreenWorkAround` — manual content view resize for fullscreen
- Both address Android keyboard handling bugs that Google hasn't fixed.

## Memory Management

- **TerminalRow.mText** — Variable-length `char[]` with lazy allocation and 1.5× over-allocation factor
- **TextStyle** — 64-bit packed encoding, one long per display column
- **ByteQueue** — Fixed-size ring buffers (64KB + 4KB)
- **JNI** — All strings `strdup()`'d and freed via `free_string_array()`. Critical section for PID write (zero-copy)
- **Bootstrap ZIP** — Embedded in `.rodata` section of shared library via assembly `.incbin`

## Error Handling

- **JNI errors** — Throw `RuntimeException` with descriptive message
- **Bootstrap errors** — Show dialog with retry/abort, send crash report notification
- **Plugin errors** — Log errors, optionally show notification, deliver error result via PendingIntent
- **Socket errors** — `TermuxAmSocketServer` shows error notification if server is running
- **Memory leaks** — Fixed in commit 10b01ead: error paths in `createSubprocess()` now properly free `argv`/`envp`

---

# Security Analysis

## Permissions

The app requests 18 permissions, many of which are powerful:
- `READ_LOGS`, `DUMP` — can read system logs
- `WRITE_SECURE_SETTINGS` — can modify system settings
- `MANAGE_EXTERNAL_STORAGE` — full storage access
- `SYSTEM_ALERT_WINDOW` — draw over other apps
- `REQUEST_INSTALL_PACKAGES` — install APKs

These are necessary for Termux's functionality as a terminal emulator but create a significant attack surface.

## Signing

- Debug builds use a **publicly shared** test key with known passwords
- Release builds are **unsigned** (F-Droid signs its own)
- All Termux apps use `sharedUserId="com.termux"` requiring identical signing keys

## External App Communication

- `allow-external-apps` property controls whether external apps can send commands
- `RunCommandService` validates executable existence and permissions
- `FileReceiverActivity` validates file paths are under allowed directories
- `TermuxOpenReceiver$ContentProvider` forces read-only on property files

## Bootstrap Integrity

Bootstrap ZIPs are verified via SHA-256 checksums during download. The checksums are hardcoded in `app/build.gradle`.

## Potential Vulnerabilities

1. **Shared test key** — Anyone can sign APKs with the test key, potentially creating malicious "Termux" APKs
2. **`READ_LOGS` permission** — Can read other apps' logs on older Android versions
3. **`WRITE_SECURE_SETTINGS`** — Can modify system settings
4. **Native code** — JNI layer handles PTY creation; buffer overflows in C code could be exploitable
5. **`allow-external-apps`** — When enabled, any app can execute commands in Termux

---

# Performance Analysis

## Rendering Performance

- **TerminalRenderer** groups characters into runs sharing the same style, minimizing Canvas draw calls
- **TextStyle** bit-packed into 64-bit longs for cache-friendly storage
- **TerminalRow** uses lazy allocation (`allocateFullLineIfNecessary()`) for sparse rows
- **WcWidth** uses binary search for character width lookup
- **TerminalView.onDraw()** only redraws visible rows

## I/O Performance

- **ByteQueue** uses lock-free-ish circular buffer with `wait()`/`notify()` for minimal contention
- **PTY reader** uses 64KB buffer for high throughput
- **Keyboard writer** uses 4KB buffer (sufficient for keystrokes)

## Memory Performance

- **TerminalRow.mText** uses 1.5× over-allocation to minimize array copies
- **TerminalBuffer** reuses `TerminalRow` objects during scroll (ring buffer)
- **JNI critical section** for PID write (zero-copy access)
- **Bootstrap ZIP** is memory-mapped via shared library `.rodata`

## Startup Performance

- Bootstrap installation is the main startup bottleneck (ZIP extraction on first run)
- Properties loading happens on main thread (could be moved to background)
- Environment file is written on every startup (atomic write pattern)

---

# Possible Improvements

## Architecture

1. **Move properties loading to background thread** — Currently blocks main thread during `TermuxApplication.onCreate()`
2. **Replace SharedPreferences with DataStore** — Modern Android persistence library
3. **Add dependency injection** — Replace manual singleton management with Hilt/Dagger
4. **Separate concerns in TermuxTerminalViewClient** — 802 lines handling too many responsibilities

## Performance

1. **Batch screen updates** — Currently invalidates on every `MSG_NEW_INPUT`; could batch updates with `postDelayed()`
2. **Reduce object allocation in TerminalRow.setChar()** — Creates intermediate objects during wide character handling
3. **Cache WcWidth results** — Binary search per character is expensive for large buffers
4. **Use RenderScript/GPU for rendering** — TerminalRenderer uses Canvas which is CPU-based

## Security

1. **Remove `READ_LOGS` permission** — Unnecessary on modern Android
2. **Remove `WRITE_SECURE_SETTINGS`** — Consider alternatives
3. **Add certificate pinning** — For bootstrap download
4. **Implement app sandboxing** — Restrict command execution scope
5. **Add SELinux policy** — Additional containment layer

## Code Quality

1. **Add Kotlin migration** — Gradually convert Java to Kotlin
2. **Increase test coverage** — Only 2 unit tests in app module
3. **Add integration tests** — Test PTY creation, escape sequence parsing
4. **Document public APIs** — Many public methods lack documentation
5. **Remove dead code** — Some commented-out resources and unused methods

## User Experience

1. **Add Material 3 motion** — Transitions between activities (M3 theme/colors/typography are complete; motion transitions remain)
2. **Add haptic feedback for special keys** — Currently only for long-press
3. **Add keyboard shortcuts help** — Document all Fn/Ctrl+Alt mappings
4. **Add session search** — Find text in session list
5. **Add themes/presets** — Pre-configured color schemes

## Completed Improvements (Material 3 Migration)

The following improvements have been implemented as part of the Material 3 migration:

- ✅ Dynamic Color support on Android 12+
- ✅ MaterialAlertDialogBuilder for all dialogs
- ✅ M3 typography scale in XML layouts
- ✅ Theme-resolved colors (no hardcoded Java colors)
- ✅ Night mode variants for markdown backgrounds
- ✅ MaterialToolbar imports in Java
- ✅ M3 text selection handle colors
- ✅ ViewPager2 (replaced deprecated ViewPager)
- ✅ RecyclerView (replaced deprecated ListView)
- ✅ Cleaned up hardcoded drawable tints

---

# Git History Analysis

## Recent Commits (Material 3 Migration Completion)

### Commit: `0c730379` — feat(ui): add Material 3 Dynamic Color support

- **Date:** 2026-07-22
- **Files:** `app/src/main/java/com/termux/app/TermuxApplication.java` (+4, -0)
- **Change:** Added `DynamicColors.applyToActivitiesIfAvailable(this)` in `TermuxApplication.onCreate()` after night mode setup. Enables wallpaper-based theming on Android 12+ with graceful fallback on older devices.
- **Why:** Completes M3 dynamic color support as part of the comprehensive Material 3 migration.

### Commit: `aa0c173c` — feat(ui): migrate AlertDialog.Builder to MaterialAlertDialogBuilder

- **Date:** 2026-07-22
- **Files:** 7 files (+16, -12)
- **Change:** Replaced `android.app.AlertDialog.Builder` and `androidx.appcompat.app.AlertDialog.Builder` with `com.google.android.material.dialog.MaterialAlertDialogBuilder` across all dialog creation sites.
- **Files:** TermuxActivity, TermuxInstaller, TermuxTerminalViewClient, TermuxTerminalSessionActivityClient, MessageDialogUtils, TextInputDialogUtils
- **Why:** MaterialAlertDialogBuilder is the recommended M3 dialog builder with proper theming.

### Commit: `34b8d7a0` — feat(ui): replace hardcoded Java colors with theme attributes

- **Date:** 2026-07-22
- **Files:** 6 files (+16, -12)
- **Change:** Replaced hardcoded `Color.RED`, `Color.BLACK`, and `0xFF607D8B` with dynamically resolved theme attributes (`?attr/colorError`, `?attr/colorPrimary`, `?android:attr/textColorPrimary`).
- **Why:** Ensures proper dark mode support and M3 color system compliance.

### Commit: `18813e34` — feat(ui): migrate XML layouts to Material 3 typography

- **Date:** 2026-07-22
- **Files:** 4 files (+5, -5)
- **Change:** Replaced framework text appearances (`?android:attr/textAppearanceLarge`, `?android:attr/textAppearanceMedium`) with M3 typography scale (`TextAppearance.Material3.BodyLarge`, `TextAppearance.Material3.BodyMedium`). Replaced `?android:attr/textColorPrimary` with `?attr/colorOnSurface`.
- **Why:** Consistent M3 typography across all text elements.

### Commit: `261055c8` — feat(ui): replace hardcoded Material 2 blue with M3 primary in text selection handles

- **Date:** 2026-07-22
- **Files:** 2 files (+2, -2)
- **Change:** Replaced `#2196F3` (Material 2 Blue 500) with `?attr/colorPrimary` in text selection handle drawables.
- **Why:** M3 color system compliance.

### Commit: `dfce2a9b` — feat(ui): add night mode variants for markdown code backgrounds

- **Date:** 2026-07-22
- **Files:** 1 new file (+6)
- **Change:** Created `termux-shared/src/main/res/values-night/colors.xml` with semi-transparent white variants for markdown code backgrounds. Previous semi-transparent black was nearly invisible on dark backgrounds.
- **Why:** Dark mode compatibility for markdown rendering.

### Commit: `fc358800` — refactor(ui): update Java Toolbar imports to MaterialToolbar

- **Date:** 2026-07-22
- **Files:** 3 files (+8, -8)
- **Change:** Replaced `androidx.appcompat.widget.Toolbar` imports with `com.google.android.material.appbar.MaterialToolbar` in AppCompatActivityUtils, TextIOActivity, and ReportActivity.
- **Why:** Consistency with XML layouts that already use MaterialToolbar.

### Commit: `12b5f129` — fix(ui): remove hardcoded black tint from ic_settings drawable

- **Date:** 2026-07-22
- **Files:** 1 file (+1, -1)
- **Change:** Removed `android:tint="#FF000000"` from `ic_settings.xml`. Layout already overrides with `?attr/termuxActivityDrawerImageTint`.
- **Why:** Remove hardcoded color that would be black on dark backgrounds.

### Commit: `9b7a85ad` — feat(ui): migrate ViewPager to ViewPager2

- **Date:** 2026-07-22
- **Files:** 4 files (+51, -34)
- **Change:** Replaced deprecated `androidx.viewpager.widget.ViewPager` with `androidx.viewpager2.widget.ViewPager2` for the terminal toolbar. Rewrote `TerminalToolbarViewPager.PageAdapter` from `PagerAdapter` to `RecyclerView.Adapter` with ViewHolder pattern.
- **Why:** ViewPager2 is the modern replacement with better performance and lifecycle support.

### Commit: `9bef0bce` — feat(ui): migrate ListView to RecyclerView for session list

- **Date:** 2026-07-22
- **Files:** 4 files (+43, -34)
- **Change:** Replaced `android.widget.ListView` with `androidx.recyclerview.widget.RecyclerView` for the terminal sessions list. Rewrote `TermuxSessionsListViewController` from `ArrayAdapter` to `RecyclerView.Adapter` with `SessionViewHolder` pattern.
- **Why:** RecyclerView is the modern replacement with better performance and animation support.

---

## Previous Commits

### Commit 1: `7f77c3da` — fix: migrate primary toolbar to Material3 component

- **Author:** Y-0x
- **Date:** 2026-07-21 17:56:17 UTC
- **Files:** `termux-shared/src/main/res/layout/partial_primary_toolbar.xml` (+2, -3)
- **Change:** Replaced `androidx.appcompat.widget.Toolbar` with `com.google.android.material.appbar.MaterialToolbar`. Removed `android:theme="@style/ThemeOverlay.Material3.Toolbar"` (no longer needed with MaterialToolbar).
- **Why:** Part of the Material 3 migration. MaterialToolbar is the recommended M3 toolbar widget with built-in M3 theming.
- **Impact:** Cosmetic only — proper M3 toolbar rendering.
- **Relationship:** Follow-up to commit 56acb262 (Material 3 migration).

## Commit 2: `e7d737ae` — fix(ci): correct Material3 style references and gradle wrapper validation action

- **Author:** Y-0x
- **Date:** 2026-07-21 17:39:48 UTC
- **Files:** 3 files (+4, -4)
  - `.github/workflows/gradle-wrapper-validation.yml` — `@5` → `@v6`
  - `termux-shared/src/main/res/layout/dialog_show_message.xml` — `Title.Large` → `TitleLarge`
  - `termux-shared/src/main/res/values/styles.xml` — Same fix for title/subtitle
- **Why:** Material 3 style names don't use dots (e.g., `TextAppearance.Material3.TitleLarge` not `Title.Large`). The CI action reference also needed updating.
- **Impact:** Fixes build-breaking M3 style references.
- **Relationship:** Follow-up to commit 56acb262 (Material 3 migration).

## Commit 3: `a11af0ab` — ci: add release workflow for automatic versioning and GitHub Releases

- **Author:** Y-0x
- **Date:** 2026-07-21 17:29:26 UTC
- **Files:** 2 files (+133, +3)
  - `.github/workflows/release.yml` — **New** 130-line workflow
  - `.gitignore` — Added `devnotes.md`
- **Change:** Adds a complete CI/CD pipeline that:
  1. Auto-increments patch version from latest git tag
  2. Builds debug APKs for both bootstrap variants
  3. Validates all 5 ABI APKs exist
  4. Generates SHA-256 checksums
  5. Creates GitHub release with auto-generated notes
- **Why:** Automates the release process that was previously manual.
- **Impact:** Enables automated releases on push to master.
- **Relationship:** New feature — no predecessor.

## Commit 4: `56acb262` — feat(ui): migrate from Material Components to Material 3

- **Author:** Y-0x
- **Date:** 2026-07-21 06:28:08 UTC
- **Files:** 22 files (+237, -289)
- **Change:** Major UI migration:
  - **Theme:** `Theme.MaterialComponents` → `Theme.Material3` with full M3 color token set
  - **Colors:** Replaced hardcoded colors with M3 theme attributes (`?attr/colorSurface`, `?attr/colorPrimary`, etc.)
  - **Styles:** Updated to M3 text appearances and button styles
  - **Drawables:** Removed 5 redundant light/dark drawable files, unified to theme attributes
  - **Java:** Removed manual dark theme checking in `TermuxSessionsListViewController`
  - **Dialogs:** Switched from `Theme_AppCompat_Light_Dialog` to M3 `materialAlertDialogTheme`
- **Why:** Modernize UI to Material 3 (Material You) design system with dynamic color support.
- **Impact:** Significant visual update. Terminal background stays black. Drawer and extra keys use M3 surface colors.
- **Relationship:** Largest single commit in the history. Foundation for commits 1 and 2.

## Commit 5: `3fc8252c` — fix(extrakeys): validate parameter instead of field in setLongPressRepeatDelay

- **Author:** Y-0x
- **Date:** 2026-07-21 06:27:42 UTC
- **Files:** `termux-shared/src/main/java/com/termux/shared/termux/extrakeys/ExtraKeysView.java` (+1, -1)
- **Change:** Fixed `if (mLongPressRepeatDelay >= ...)` to `if (longPressRepeatDelay >= ...)`.
- **Why:** The validation was checking the current field value instead of the new parameter. This inverted the logic: valid new values were rejected if the current value was out of range, and invalid values were accepted if the current value was in range.
- **Impact:** Bug fix — extra key repeat delay now correctly validated.
- **Relationship:** Independent bug fix.

## Commit 6: `10b01ead` — fix(jni): correct ReleaseStringUTFChars argument and fix memory leaks on error paths

- **Author:** Y-0x
- **Date:** 2026-07-21 06:27:29 UTC
- **Files:** `terminal-emulator/src/main/jni/termux.c` (+24, -12)
- **Changes:**
  1. Fixed `ReleaseStringUTFChars(env, cmd, cmd_cwd)` → `ReleaseStringUTFChars(env, cwd, cmd_cwd)` — was passing wrong JNI string reference
  2. Added `free_string_array()` helper for clean C memory management
  3. Error paths in `createSubprocess()` now properly free `argv` and `envp` before returning
- **Why:** Two bugs: (a) wrong argument caused undefined behavior per JNI spec, (b) memory leaks on allocation failure paths.
- **Impact:** Critical bug fixes — undefined behavior and memory leaks in the core PTY creation path.
- **Relationship:** Independent bug fixes.

## Commit 7: `3df69d1d` — Revert: Add Warp sponsors logo

- **Author:** agnostic-apollo
- **Date:** 2026-07-15 01:47:01 UTC
- **Files:** `README.md` (-5 lines)
- **Change:** Removed Warp sponsor logo and link from README.
- **Why:** Sponsorship ended after 3 months.
- **Impact:** Documentation only.
- **Relationship:** Reverts a previous sponsorship addition.

## Commit 8: `401bbe54` — Fixed: Do not add BigTextStyle to notification if big text is null

- **Author:** agnostic-apollo
- **Date:** 2026-05-13 02:57:05 UTC
- **Files:** `termux-shared/src/main/java/com/termux/shared/notification/NotificationUtils.java` (+3, -1)
- **Change:** Added null check: `if (notificationBigText != null) { builder.setStyle(new Notification.BigTextStyle().bigText(notificationBigText)); }`
- **Why:** `BigTextStyle` with null text caused `NullPointerException` on Android 6. The stack trace shows `Notification$Builder.processLegacyText()` calling `.toString()` on null.
- **Impact:** Crash fix for Android 6 devices when notification big text is null.
- **Relationship:** Independent bug fix. Closes termux/termux-app#5113.

## Commit 9: `30ebb2de` — Fixed: Fix inverted typo in termcap values for KEYCODE_PAGE_UP and KEYCODE_PAGE_DOWN

- **Author:** agnostic-apollo
- **Date:** 2026-04-07 04:11:27 UTC
- **Files:** `terminal-emulator/src/main/java/com/termux/terminal/KeyHandler.java` (+2, -2)
- **Change:** Swapped `kN`→`KEYCODE_PAGE_UP` to `kP`→`KEYCODE_PAGE_UP` and vice versa.
- **Why:** The termcap names were inverted: `kN` is "key Next" (Page Down), `kP` is "key Previous" (Page Up). The code had them backwards.
- **Impact:** Fixed Page Up/Down key mapping for programs using termcap/terminfo (e.g., vim, less).
- **Relationship:** Independent bug fix. Closes #5052.

## Commit 10: `3f0dec35` — Fixed: Remove deprecated attribute extractNativeLibs

- **Author:** Fredrik Fornwall
- **Date:** 2026-02-21 11:37:36 UTC
- **Files:** `app/src/main/AndroidManifest.xml` (-1 line)
- **Change:** Removed `android:extractNativeLibs="true"` from the manifest.
- **Why:** The attribute is deprecated when `useLegacyPackaging = true` is set in `app/build.gradle` (which it is). AGP warns about this during builds.
- **Impact:** Removes deprecation warning. No functional change since `useLegacyPackaging` already controls this behavior.
- **Relationship:** Independent cleanup.

---

# Project Evolution

## Recent Development Pattern

The 10 most recent commits show three distinct development threads:

### Thread 1: Material 3 Migration (Completed)
- **56acb262** — Major migration (22 files, +237/-289) — theme, colors, styles, drawables, dialogs
- **e7d737ae** — Fix broken M3 style references (3 files)
- **7f77c3da** — Finalize toolbar migration (1 file)
- **0c730379** — Add Dynamic Color support (1 file)
- **aa0c173c** — Migrate AlertDialog.Builder → MaterialAlertDialogBuilder (7 files)
- **34b8d7a0** — Replace hardcoded Java colors with theme attributes (6 files)
- **18813e34** — Migrate XML layouts to M3 typography (4 files)
- **261055c8** — Replace hardcoded M2 blue in text selection handles (2 files)
- **dfce2a9b** — Add night mode variants for markdown code backgrounds (1 file)
- **fc358800** — Update Java Toolbar imports to MaterialToolbar (3 files)
- **12b5f129** — Remove hardcoded black tint from ic_settings drawable (1 file)
- **9b7a85ad** — Migrate ViewPager → ViewPager2 (4 files)
- **9bef0bce** — Migrate ListView → RecyclerView for session list (4 files)

The Material 3 migration is now **complete**. The project uses:
- `Theme.Material3.Light.NoActionBar` / `Theme.Material3.Dark.NoActionBar` as base themes
- Full M3 color token system (primary, secondary, tertiary, error, surface hierarchy)
- Dynamic Colors on Android 12+ with fallback to static M3 scheme
- MaterialToolbar, MaterialButton, MaterialTextView, MaterialAlertDialogBuilder
- M3 typography scale (TextAppearance.Material3.*)
- ViewPager2 and RecyclerView as modern widget replacements
- Proper dark mode support with night-mode color variants

### Thread 2: CI/CD Automation (Y-0x)
- **a11af0ab** — New release workflow with auto-versioning

This adds automated release pipeline that was previously manual or handled by external processes.

### Thread 3: Bug Fixes (mixed authors)
- **3fc8252c** — ExtraKeysView validation bug
- **10b01ead** — JNI memory leaks and wrong argument
- **401bbe54** — Notification NPE on Android 6
- **30ebb2de** — Termcap key mapping inversion
- **3f0dec35** — Deprecated manifest attribute

These span 5 months (Feb-Jul 2026) and address issues across different layers: UI, JNI, notifications, key handling, and build configuration.

## Architectural Milestones

1. **Terminal emulator engine** — The core VT100 emulator (`TerminalEmulator.java`, 2617 lines) is mature and stable. Recent changes are bug fixes, not feature additions.

2. **Material 3 migration** — The most significant recent architectural change, modernizing the entire UI layer. **Now complete** with Dynamic Color support, M3 typography, MaterialAlertDialogBuilder, ViewPager2, and RecyclerView.

3. **Bootstrap system** — The native ZIP embedding approach (assembly `.incbin`) is a unique and efficient solution for providing the initial Linux environment.

4. **Plugin architecture** — The shared library (`termux-shared`) enables 6 companion apps to share code, constants, and UI components.

## Future Risks

1. **targetSdkVersion lock** — Stuck at 28 due to Google's native executable restrictions. May eventually become impossible to update as Google Play enforces higher minimums.

2. **Java-only codebase** — No Kotlin adoption yet. May face developer onboarding challenges as Android development increasingly assumes Kotlin.

3. **Large monolithic files** — `TerminalEmulator.java` (2617 lines), `TermuxActivity.java` (1013 lines), `TermuxService.java` (959 lines), `TerminalView.java` (1500 lines) are all very large files that are difficult to maintain.

4. **Limited test coverage** — Only 2 unit tests in the app module, 18 in terminal-emulator, 0 in terminal-view and termux-shared.

5. **Dependency on JitPack** — Three library modules are published to JitPack for plugin consumption. JitPack availability is a single point of failure for the plugin ecosystem.

---

# Final Professional Review

## Overall Assessment

This is a **mature, well-architected Android application** that has evolved over many years. The codebase demonstrates deep understanding of Android internals, terminal emulation, and Unix system programming.

## Strengths

1. **Robust terminal emulation** — The VT100/xterm emulator is comprehensive, handling UTF-8, wide characters, 24-bit color, mouse tracking, and dozens of escape sequences.

2. **Elegant native layer** — The PTY management in `termux.c` is clean and correct. The bootstrap ZIP embedding via assembly `.incbin` is clever and efficient.

3. **Clean module separation** — The four-module architecture (`terminal-emulator` → `terminal-view` → `termux-shared` → `app`) provides good separation of concerns and enables library reuse by plugins.

4. **Comprehensive constant management** — `TermuxConstants.java` provides a single source of truth for all shared values, preventing inconsistencies across the ecosystem.

5. **Active maintenance** — Recent commits show attention to detail (JNI memory leaks, null checks, deprecated APIs) and modernization (Material 3 migration).

## Weaknesses

1. **Large files** — Several files exceed 1000 lines (`TerminalEmulator`: 2617, `TermuxConstants`: 1338, `TermuxActivity`: 1013, `TermuxService`: 959, `TerminalView`: 1500). These should be decomposed.

2. **Limited testing** — The app module has only 2 unit tests. The terminal-emulator has 18 tests but no integration tests. Terminal-view and termux-shared have no tests.

3. **Java-only** — No Kotlin adoption. The codebase uses Java 1.8 compatibility with desugaring, which limits access to modern language features.

4. **Hardcoded values** — Some magic numbers and hardcoded colors remain despite the Material 3 migration.

5. **Documentation** — Public APIs are inconsistently documented. Some classes have extensive Javadoc, others have none.

## Code Quality Score

| Category | Score | Notes |
|----------|-------|-------|
| Architecture | 9/10 | Clean layered design, good separation |
| Code Organization | 8/10 | Well-structured packages, some large files |
| Testing | 3/10 | Very limited test coverage |
| Documentation | 6/10 | Inconsistent Javadoc, good README |
| Security | 7/10 | Good practices, but powerful permissions |
| Performance | 8/10 | Efficient rendering, good I/O design |
| Maintainability | 7/10 | Large files hinder modification |
| **Overall** | **7/10** | **Mature, well-built project with room for improvement** |
