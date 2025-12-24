# Mirage Manager

[![Download](https://img.shields.io/github/v/release/Kitsuri-Studios/Mirage?color=green&logoColor=green&label=Download&logo=DocuSign)](https://github.com/Kitsuri-Studios/Mirage/releases/latest) [![Total](https://shields.io/github/downloads/Kitsuri-Studios/Mirage/total?logo=github&label=Counts&logoColor=black&color=black)](https://github.com/Kitsuri-Studios/Mirage/releases) [![License: GPL v3](https://img.shields.io/badge/License-GPL%20v3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0.html)[![GitHub issues](https://img.shields.io/github/issues/Kitsuri-Studios/Mirage.svg) ](https://github.com/Kitsuri-Studios/Mirage/issues)[![GitHub stars](https://img.shields.io/github/stars/Kitsuri-Studios/Mirage.svg)](https://github.com/Kitsuri-Studios/Mirage/stargazers)[![IntelliJ IDEA](https://img.shields.io/badge/IntelliJIDEA-000000.svg?logo=intellij-idea&logoColor=white)](https://www.jetbrains.com/idea/)[![Firebase](https://img.shields.io/badge/Firebase-039BE5?logo=Firebase&logoColor=white)](https://firebase.google.com/)[![Android](https://img.shields.io/badge/Android-3DDC84?logo=android&logoColor=white)](https://developer.android.com/studio)  

## Introduction 

Mirage is a Manager App for hxo framework/tool for auto-injecting shared objects into target processes. 

# Preview
https://github.com/user-attachments/assets/32a9c128-141c-4334-b04f-26a7dba350dd

## Supported Versions

- Minimum of android 9 maximum is as long as google dosent disable side loading or pairip is implemented in every app (a work around to make  the apps compaitiable with pairip to a certain extent is being planned for future)


## Installation Instructions

Setting up Mirage Client is quick and straightforward. Follow these steps:

1. **Download the APK**: Head to our [Releases page](https://github.com/Kitsuri-Studios/Mirage/releases) and download the latest Mirrage APK.
2. **Allow Unknown Sources**: On your Android device, go to **Settings > Security** (or **Apps & Notifications** on newer versions) and enable **Install from Unknown Sources**.
3. **Install the APK**: Open the downloaded APK file using a file manager, tap to install, and follow the prompts.

If you encounter issues, visit our [Discord](https://discord.gg/tt25ff6WH6) for support.

## Development Setup

For developers looking to contribute or customize Mirage, here’s how to set up your environment:

1. **Clone the Repository**:

    ```bash
    git clone https://github.com/Kitsuri-Studios/Mirage.git
    ```

   This downloads the full source code to your local machine.

2. **Open in Android Studio**:

   - Launch Android Studio and select **Open an existing project**.
   - Choose the `M1rage` directory and open it.

3. **Sync Gradle**:

   - Click **Sync Project with Gradle Files** to fetch dependencies.
   - Ensure you have the Android SDK (API level 28 or higher) and Gradle installed.
   
4. **Build and Test**:

   - Connect an Android device or configure an emulator.
   - Select **Run > Run 'app'** to build and deploy Mirage.
   - Test changes on a local Minecraft Bedrock server to verify functionality.

Refer to inline comments, For development tips, join our [Discord](https://discord.gg/tt25ff6WH6).

---

### Prohibited Uses

- Do not distribute modified versions without sharing the source code, as required by the GPL.
- Do not claim Mirage as your own without crediting the Kitsuri team and its contributors.
- Selling Mirage or derivatives without adhering to the GPL is prohibited.
- Do not distribute Mirage as closed-source or under a non-GPL license.
- The authors are not responsible for bans, damages, or issues arising from Mirage’s use.

## Community and Support

Join our community to connect with other Mirage users and developers:

- **Discord**: Our [Discord server](https://discord.gg/tt25ff6WH6) is the best place for real-time support, bug reports, and feature discussions.


---

## Credits and Acknowledgments

We are deeply grateful to the open-source community and internal contributors whose tools and libraries make **Mirage** possible.


---

### UI, Jetpack Compose & Material

| Project                                                                                                                      | Description                                                                | Resources      |
| ---------------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------- | -------------- |
| **Tabler Icons (Project Module)**                                                                                            | Vector icon pack used throughout Mirage for consistent and clean UI icons. | Project module |
| **[Jetpack Compose](https://developer.android.com/jetpack/compose)**                                                         | Declarative UI toolkit used to build Mirage’s interface.                   | Documentation  |
| **[AndroidX Compose BOM](https://developer.android.com/jetpack/compose/bom)**                                                | Ensures consistent and compatible Compose dependency versions.             | Documentation  |
| **[AndroidX Compose UI](https://developer.android.com/jetpack/androidx/releases/compose-ui)**                                | Core UI primitives for Compose-based layouts.                              | Documentation  |
| **[Compose UI Graphics](https://developer.android.com/jetpack/androidx/releases/compose-ui)**                                | Graphics and rendering utilities for Compose.                              | Documentation  |
| **[Compose UI Tooling & Preview](https://developer.android.com/jetpack/compose/tooling)**                                    | Preview and inspection tools for UI development.                           | Documentation  |
| **[Material 3](https://developer.android.com/jetpack/androidx/releases/compose-material3)**                                  | Material You components for modern, adaptive UI.                           | Documentation  |
| **[Material Icons Extended](https://developer.android.com/reference/kotlin/androidx/compose/material/icons/Icons.Extended)** | Extended Material icon set used across the UI.                             | Documentation  |
| **[Compose Google Fonts](https://developer.android.com/jetpack/compose/text/fonts)**                                         | Google Fonts integration for Compose typography.                           | Documentation  |

---

### AndroidX Core & Lifecycle

| Project                                                                                                          | Description                                            | Resources     |
| ---------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------ | ------------- |
| **[AndroidX Core KTX](https://developer.android.com/jetpack/androidx/releases/core)**                            | Kotlin extensions for Android core APIs.               | Documentation |
| **[AndroidX Lifecycle Runtime KTX](https://developer.android.com/jetpack/androidx/releases/lifecycle)**          | Lifecycle-aware components for state management.       | Documentation |
| **[Lifecycle ViewModel Compose](https://developer.android.com/jetpack/androidx/releases/lifecycle)**             | ViewModel integration for Jetpack Compose.             | Documentation |
| **[AndroidX Activity Compose](https://developer.android.com/jetpack/androidx/releases/activity)**                | Compose support for Android activities.                | Documentation |
| **[AndroidX DocumentFile](https://developer.android.com/reference/androidx/documentfile/provider/DocumentFile)** | SAF-compatible file access for modern Android storage. | Documentation |

---

### APK, AXML & Patching Utilities

| Project                                                | Description                                                    | Resources |
| ------------------------------------------------------ | -------------------------------------------------------------- | --------- |
| **APK Signature (apksig)**                             | APK signing and signature verification utilities.              | AOSP      |
| **AXML Parser**                                        | Binary AndroidManifest.xml parsing and manipulation.           | AOSP      |
| **APK Utils**                                          | APK inspection, extraction, and metadata handling utilities.   | Internal  |
| **[Zip4j](https://github.com/srikanth-lingala/zip4j)** | ZIP handling with encryption and advanced compression support. | GitHub    |
| **[Baksmali](https://github.com/JesusFreke/smali)**    | Disassembler for Android DEX bytecode.                         | GitHub    |
| **[Smali](https://github.com/JesusFreke/smali)**       | Assembler for Android DEX bytecode.                            | GitHub    |

---

### Serialization, Data & Graphics

| Project                                                                           | Description                                             | Resources |
| --------------------------------------------------------------------------------- | ------------------------------------------------------- | --------- |
| **[Kotlinx Serialization JSON](https://github.com/Kotlin/kotlinx.serialization)** | JSON serialization for Kotlin-based data handling.      | GitHub    |
| **UI Graphics Utilities**                                                         | Shared graphics utilities used across Mirage UI layers. | Internal  |

---

### Internal Utilities & Modules

| Module               | Description                                              |
| -------------------- | -------------------------------------------------------- |
| **Crash Reporter**   | Internal crash reporting and diagnostics utility.        |
| **Adapters**         | Shared adapter utilities used across UI and data layers. |
| **Credits**          | Credits and attribution handling module.                 |
| **File Utils**       | File system utilities for safe file operations.          |
| **Installer Utils**  | APK installation and package management helpers.         |
| **Package Utils**    | Android package inspection utilities.                    |
| **Permission Utils** | Runtime permission handling helpers.                     |
| **Theme Utils**      | Theme and appearance customization utilities.            |
| **Translator Utils** | Localization and translation helpers.                    |

---

### Testing & Tooling

| Project                                                                            | Description                                   | Resources     |
| ---------------------------------------------------------------------------------- | --------------------------------------------- | ------------- |
| **[JUnit](https://junit.org/junit4/)**                                             | Unit testing framework.                       | Documentation |
| **[AndroidX JUnit](https://developer.android.com/jetpack/androidx/releases/test)** | Android-specific JUnit extensions.            | Documentation |
| **[Espresso](https://developer.android.com/training/testing/espresso)**            | UI testing framework for Android.             | Documentation |
| **Compose UI Test JUnit4**                                                         | Compose-specific UI testing utilities.        | Documentation |
| **Compose UI Test Manifest**                                                       | Test manifest utilities for Compose UI tests. | Documentation |

---

## License

Mirage is licensed under the [GNU General Public License v3.0](https://www.gnu.org/licenses/gpl-3.0.html). Earlier versions have separate freeware licenses; please review them if applicable. Key license terms:

- You may use, modify, and distribute Mirage, provided the source code is shared under the same GPL license.
- Derivative works must remain open-source and cannot impose additional restrictions.
- The software is provided "as is" without warranties, and the authors are not liable for any damages or misuse.

For complete details, see the [LICENSE](https://github.com/Kitsuri-Studios/Mirage/blob/main/LICENSE).

---


