# PSP BT Gamepad

Turn an Android phone into a wireless game controller for a Windows PC or a supported Bluetooth host. The project currently provides two connection modes:

- **Windows over Wi-Fi:** the Android app sends button and analog-stick state over your local network. A small Python receiver on Windows exposes it as a virtual Xbox 360 controller, so games that support XInput can use it.
- **Bluetooth HID:** the Android app can register as a Bluetooth HID gamepad on Android devices and hosts that support the required Bluetooth HID Device profile.

The Android app is designed in landscape orientation. The face buttons are on the **left**, the D-pad is on the **right**, and the analog stick sits between the two control groups.

> **Project status:** prototype / active development. The Wi-Fi controller has been exercised with PPSSPP, but compatibility, latency and button mapping can vary by device and game. Please test before relying on it.

## Features

- On-screen Cross, Circle, Square and Triangle buttons.
- L and R shoulder buttons, plus Select and Start.
- Four-way D-pad and a spring-return analog stick.
- Local Wi-Fi connection to the Windows companion receiver.
- Six-digit pairing PIN generated when the receiver starts.
- Virtual Xbox 360 controller output through vgamepad.
- Separate Bluetooth HID connection option.
- Android APK build workflow through GitHub Actions.

## Repository layout

~~~text
.
├── app/
│   ├── build.gradle
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/example/pspcontroller/
│       │   ├── MainActivity.kt
│       │   └── AnalogStickView.kt
│       └── res/
│           ├── drawable/       # Controller backgrounds and button states
│           └── layout/         # Landscape controller layout
├── windows-companion/
│   ├── receiver.py             # UDP receiver and virtual Xbox gamepad
│   ├── requirements.txt
│   └── README.md               # Windows-specific setup
├── .github/workflows/build.yml # Android build and Python syntax check
├── build.gradle
├── settings.gradle
└── gradle.properties
~~~

## Requirements

### Android development

- Android Studio with Android SDK Platform 34.
- JDK 17.
- Android Gradle Plugin 8.2.2 and Kotlin Android plugin 1.9.22 (declared in the root Gradle build file).
- A phone running Android 9 (API 28) or newer for the current app minimum SDK.
- Bluetooth HID Device profile support on the phone if you want to use Bluetooth mode. Not every Android device exposes this profile.

### Windows Wi-Fi companion

- Windows 10 or Windows 11.
- Python 3.10 or newer (the receiver uses only standard-library networking plus vgamepad).
- The vgamepad Python package.
- A compatible ViGEmBus virtual gamepad driver installation. See the driver note below.
- Phone and PC connected to the same trusted local Wi-Fi network.

## Build the Android app

1. Clone or download this repository.
2. Open the project root in Android Studio.
3. Allow Android Studio to sync the Gradle project and install any missing Android SDK components.
4. Select the app run configuration and build/run it on your Android device.

The repository currently does not include a Gradle wrapper, so use Android Studio's Gradle integration or a locally installed compatible Gradle distribution. The GitHub Actions workflow uses Gradle 8.5 when it needs to generate a wrapper for CI.

To build from a terminal after configuring Gradle and the Android SDK, run:

~~~bash
gradle assembleDebug
~~~

The debug APK is normally written to:

~~~text
app/build/outputs/apk/debug/app-debug.apk
~~~

### Download a CI-built APK

1. Open the repository's **Actions** tab on GitHub.
2. Select the latest successful **Build Android APK** workflow run for the branch you want.
3. Download the **PSP-Controller-APK** artifact.
4. Extract the artifact and install the APK on your Android phone. Android may ask you to allow installation from the browser or file manager you used.

A successful CI build confirms that the project compiled; it does not prove that Bluetooth HID, Wi-Fi connectivity or every game works on every device.

## Windows Wi-Fi setup

The Windows receiver is in **windows-companion/**. For the full Windows walkthrough, see [windows-companion/README.md](windows-companion/README.md).

### First-time setup

Open Command Prompt or PowerShell in the windows-companion directory:

~~~powershell
py -m venv .venv
.venv\Scripts\python.exe -m pip install -r requirements.txt
~~~

If ViGEmBus is already installed and you need to prevent the package installer from attempting its bundled driver installation, set the environment variable before installing:

~~~cmd
set VGAMEPAD_SKIP_VIGEMBUS_INSTALL=true
.venv\Scripts\python.exe -m pip install -r requirements.txt
~~~

Only use that skip option when a compatible driver is already installed. Do not download drivers from unofficial sites or bypass Windows driver-security controls.

### Start the receiver

From the same windows-companion folder:

~~~cmd
.venv\Scripts\python.exe receiver.py
~~~

Or double-click **Start-Controller.bat** in that folder after the first-time setup is complete. Keep the receiver window open while playing. The receiver displays a new six-digit PIN each time it starts.

### Connect the phone

1. Make sure the phone and Windows PC are on the same trusted Wi-Fi network.
2. On Windows, run ipconfig and find the **IPv4 Address** under the active Wi-Fi adapter. Do not use the Default Gateway address.
3. In the Android app, enter that IPv4 address and the six-digit PIN shown in the receiver window.
4. Tap **Connect to PC**. Tap it again to disconnect.
5. If Windows Firewall prompts you, allow Python on **Private networks** only, if you trust the network.

The receiver listens on UDP port **26760**. The PIN is a basic local-network pairing check, not encrypted authentication. Do not expose this port to the public internet or use the prototype on an untrusted network.

## Controls and button mapping

| Android control | Bluetooth HID report | Windows virtual Xbox controller |
|---|---|---|
| Cross | Button 1 | A |
| Circle | Button 2 | B |
| Square | Button 3 | X |
| Triangle | Button 4 | Y |
| L | Button 5 | Left shoulder |
| R | Button 6 | Right shoulder |
| Select | Button 7 | Back |
| Start | Button 8 | Start |
| D-pad directions | Buttons 9–12 | D-pad |
| Analog stick | X/Y axes | Left analog stick |

The Windows receiver maps the Android screen's downward-positive Y axis to XInput's upward-positive Y axis.

## Configure PPSSPP

1. Start the Windows receiver and connect the Android app.
2. Open PPSSPP on Windows.
3. Open **Settings → Controls → Control mapping** (wording may vary by PPSSPP version).
4. Select the virtual Xbox 360 controller as the input device if PPSSPP has not selected it automatically.
5. Map the PSP controls to the corresponding virtual Xbox buttons and stick.
6. Test the D-pad, face buttons, shoulder buttons and analog stick in a game or the PPSSPP control test screen.

You normally only need to map the controls once, unless you change the input device or reset PPSSPP's control settings.

## Bluetooth HID mode

Bluetooth mode is separate from the Windows Wi-Fi receiver:

1. Enable Bluetooth on the phone and the host device.
2. In the Android app, tap **Pair Bluetooth**.
3. Follow the Android discoverability prompt and pair from the host device's Bluetooth settings.
4. Select the gamepad/controller input in the game or emulator.

Bluetooth HID Device support is device-dependent. If the phone does not support the HID Device profile, use the Windows Wi-Fi companion mode instead.

## Network and security notes

- The phone and PC must be reachable on the same local network.
- The Windows receiver binds to 0.0.0.0:26760 so it can receive packets from the LAN.
- Only one phone is accepted as the active controller at a time.
- The receiver releases all controls after 1.5 seconds without a valid state packet.
- A six-digit PIN is generated on every receiver start. Anyone who can see the PIN and reach the port on your LAN may be able to send input; the current protocol is not encrypted.
- Do not configure router port forwarding for UDP 26760.

## Troubleshooting

| Problem | What to check |
|---|---|
| The phone cannot connect | Confirm both devices use the same Wi-Fi, re-check the PC's Wi-Fi IPv4 address, and re-enter the current PIN. |
| The PIN is rejected | Restarting the receiver creates a new PIN. Use the PIN currently shown in the open receiver window. |
| The receiver cannot start | Confirm the virtual gamepad driver is installed and that the Python environment has vgamepad. |
| Windows does not show a controller | Check Device Manager for the virtual gamepad bus/driver and restart the receiver after resolving driver issues. |
| PPSSPP does not respond | Open control mapping, choose the virtual Xbox controller and map the buttons manually. |
| Buttons stop responding | Keep the receiver terminal open, check Wi-Fi stability, and reconnect the phone. The receiver automatically releases controls after a timeout. |
| The IP address keeps changing | Check the active Wi-Fi adapter with ipconfig; a router DHCP reservation can make the PC's local address more consistent. |
| Bluetooth pairing is unavailable | Your phone may not expose the Bluetooth HID Device profile. Use Wi-Fi mode if available. |

## Driver status and limitations

The Windows virtual controller uses vgamepad with ViGEmBus. The upstream ViGEmBus project has been archived and is retired, so it may not receive future fixes or security updates. Treat this as a prototype dependency, not a guaranteed long-term driver solution. Install drivers only from trusted official sources and do not disable Windows security features to force installation.

Other current limitations:

- Windows companion mode supports one active phone at a time.
- The local-network PIN is not a substitute for encrypted authentication.
- Bluetooth HID availability depends on Android device hardware and firmware.
- The app has been tested only on a limited set of devices and games.
- The current build does not provide per-game profiles, button remapping inside the Android app, or a configurable controller layout.

## Development checks

The GitHub Actions workflow:

1. Sets up JDK 17 and Android SDK.
2. Runs Python bytecode compilation on windows-companion/receiver.py.
3. Builds the Android debug APK.
4. Uploads the APK as the PSP-Controller-APK workflow artifact.

For a local Python syntax check:

~~~powershell
.venv\Scripts\python.exe -m py_compile receiver.py
~~~

For a local import check:

~~~powershell
.venv\Scripts\python.exe -c "import vgamepad; print('vgamepad import OK')"
~~~

## License

No license file is currently included. Unless a license is added to the repository, all rights remain with the copyright holder and reuse/redistribution permissions are not explicitly granted.
