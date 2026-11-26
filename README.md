# PSP BT Gamepad

Turn an Android phone into a wireless game controller for a Windows PC or another compatible Bluetooth host.

This project has two separate ways to send controller input:

1. **Windows Wi-Fi mode:** the Android app sends button and joystick information to a Python program running on your Windows PC. The PC program creates a virtual Xbox 360 controller, which games and emulators such as PPSSPP can detect.
2. **Bluetooth HID mode:** the Android app can act as a Bluetooth HID gamepad when the Android device supports the required HID Device profile. This mode does not use the Windows Python receiver.

> **Project status:** Prototype / active development. The Windows Wi-Fi mode has been tested with PPSSPP, but compatibility, latency and button mapping can vary by device, network and game. A successful build does not guarantee that every connection mode works on every Android phone.

## Contents

- [How it works](#how-it-works)
- [Features](#features)
- [Controller layout and controls](#controller-layout-and-controls)
- [Choose a connection mode](#choose-a-connection-mode)
- [Requirements](#requirements)
- [Build and install the Android app](#build-and-install-the-android-app)
- [Set up the Windows companion](#set-up-the-windows-companion)
- [Connect to PPSSPP](#connect-to-ppsspp)
- [Customise the on-screen layout](#customise-the-on-screen-layout)
- [Bluetooth HID mode](#bluetooth-hid-mode)
- [Troubleshooting](#troubleshooting)
- [Security, limitations and project status](#security-limitations-and-project-status)
- [Development checks](#development-checks)

## How it works

### Windows Wi-Fi mode

The phone does not directly pretend to be an Xbox controller. Instead, it sends the state of its controls to a small receiver program on the PC. That receiver translates the information into virtual Xbox 360 controller input.

```text
Android phone
  PSP BT Gamepad app
  Reads buttons and analog stick
          |
          | Local Wi-Fi network
          | UDP messages to port 26760
          v
Windows PC
  Python receiver (receiver.py)
  Checks the pairing PIN and reads input
          |
          v
  vgamepad + ViGEmBus
  Creates a virtual Xbox 360 controller
          |
          v
  PPSSPP or another compatible game
  Reads the virtual controller input
```

In this setup:

- **Android app:** displays the controller and tracks which buttons or directions are being pressed.
- **Wi-Fi:** carries the controller information between the phone and PC. Both devices need to be on the same reachable local network.
- **UDP:** is the network communication method used by the receiver. The project uses UDP port **26760**.
- **Python receiver:** runs on Windows, checks incoming messages and translates them into controller actions.
- **vgamepad:** is the Python package used to send virtual Xbox 360 controller input.
- **ViGEmBus:** is the Windows virtual gamepad driver required by vgamepad to expose the virtual controller to other programs.
- **PPSSPP:** or another compatible application reads the virtual controller as an input device.

The receiver prints a six-digit PIN when it starts. The Android app must send the current PIN when connecting. This is a basic local-network pairing check; it is not encryption or strong authentication.

### Bluetooth HID mode

Bluetooth HID is a separate mode. When supported by the Android phone, the app can present itself as a Bluetooth gamepad to a compatible host. It does not need the Windows Python receiver, UDP port 26760, vgamepad or the Windows virtual gamepad driver.

## Features

- Landscape Android controller interface.
- Cross, Circle, Square and Triangle face buttons.
- L and R shoulder buttons, Select and Start.
- Four-way D-pad and spring-return analog stick.
- On-device layout editor to move and resize controls.
- Automatic saving of the on-screen layout on the phone.
- Windows companion connection over local Wi-Fi.
- Six-digit pairing PIN generated when the Windows receiver starts.
- Virtual Xbox 360 controller output through vgamepad.
- Separate Bluetooth HID connection option.
- Android APK build workflow through GitHub Actions.

## Controller layout and controls

The controller is designed for landscape orientation. The face buttons are on the **left**, the D-pad is on the **right**, and the analog stick sits between the two control groups.

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

The Windows receiver converts the Android screen's downward-positive Y direction into the opposite Y direction expected by the virtual Xbox controller.

The layout editor changes where controls appear and how large they are on the phone. It does **not** change the button mapping sent to the connected host.

## Choose a connection mode

| If you want to… | Use |
|---|---|
| Connect the phone to a Windows PC through your local Wi-Fi | Windows Wi-Fi mode |
| Use the Windows virtual Xbox controller with PPSSPP | Windows Wi-Fi mode |
| Connect to a compatible host directly over Bluetooth | Bluetooth HID mode |
| Use Bluetooth HID without running a PC receiver | Bluetooth HID mode |

The two modes have different requirements. Follow only the setup instructions for the mode you intend to use.

## Requirements

### Android app

- Android Studio.
- Android SDK Platform 34.
- JDK 17.
- Android Gradle Plugin 8.2.2 and Kotlin Android plugin 1.9.22, as declared in the project build files.
- Android 9 (API 28) or newer for the current minimum SDK.
- A physical Android device is recommended for testing touch controls and connectivity.
- Bluetooth HID Device profile support on the phone if you want to use Bluetooth mode. Not every Android device exposes this profile.

### Windows Wi-Fi mode

- Windows 10 or Windows 11.
- Python 3.10 or newer.
- The Python package listed in `windows-companion/requirements.txt` (`vgamepad`).
- A compatible ViGEmBus virtual gamepad driver installation.
- Phone and PC connected to the same trusted local Wi-Fi network.

## Build and install the Android app

1. Clone or download this repository.
2. Open the project root in Android Studio.
3. Allow Gradle to sync and install any missing Android SDK components.
4. Select the Android app run configuration.
5. Connect an Android phone with USB debugging enabled, or use an Android emulator for interface checks.
6. Build and run the app.

The repository currently does not include a Gradle wrapper. Use Android Studio's Gradle integration or a locally installed compatible Gradle distribution.

To build from a terminal after configuring Gradle and the Android SDK:

```bash
gradle assembleDebug
```

The debug APK is normally written to:

```text
app/build/outputs/apk/debug/app-debug.apk
```

### Download a CI-built APK

1. Open the repository's **Actions** tab on GitHub.
2. Select a successful **Build Android APK** workflow run for the branch you want.
3. Download the **PSP-Controller-APK** artifact.
4. Extract the artifact and install the APK on your Android phone. Android may ask you to allow installation from the browser or file manager.

A successful CI build confirms that the project compiled. It does not prove that Bluetooth HID, Wi-Fi connectivity or every game works on every device.

## Set up the Windows companion

The Windows receiver is in the `windows-companion/` directory. It listens for controller messages from the Android app and creates virtual Xbox controller input.

For the complete Windows installation and connection walkthrough, see **[Windows companion setup](windows-companion/README.md)**.

Quick overview:

1. Install Python and the required virtual gamepad driver.
2. Open Command Prompt or PowerShell in `windows-companion/`.
3. Create a Python virtual environment and install the requirements.
4. Start `receiver.py`.
5. Note the PC's active Wi-Fi IPv4 address and the six-digit PIN printed by the receiver.
6. Enter both values in the Android app and connect.

Keep the receiver terminal open while playing. The detailed guide explains each command and how to troubleshoot connection problems.

## Connect to PPSSPP

This section applies to **Windows Wi-Fi mode**.

1. Start the Windows receiver and connect the Android app.
2. Open PPSSPP on Windows.
3. Open **Settings → Controls → Control mapping**. The wording may vary by PPSSPP version.
4. Select the virtual Xbox 360 controller if PPSSPP has not selected it automatically.
5. Map the PSP controls to the corresponding virtual Xbox buttons and analog stick.
6. Test the D-pad, face buttons, shoulder buttons and analog stick in a game or the PPSSPP control test screen.

You normally only need to map the controls once, unless you change the input device or reset PPSSPP's control settings.

## Customise the on-screen layout

The Android app includes a built-in layout editor:

1. Tap **Edit Layout** on the controller screen.
2. Drag a control to reposition it.
3. Tap a control to select it.
4. Use **−** and **+** in the editor bar to make the selected control smaller or larger.
5. Tap **Done** to return to normal play.
6. Tap **Reset** to restore the original positions and sizes.

Layout changes are saved on the phone automatically and remain after closing and reopening the app. Each control can be adjusted independently. The editor changes only the on-screen layout; it does not remap the buttons or change the input sent to the host.

## Bluetooth HID mode

Bluetooth mode is independent of the Windows Wi-Fi receiver.

1. Enable Bluetooth on the phone and host device.
2. In the Android app, tap **Pair Bluetooth**.
3. Follow the Android discoverability prompt.
4. Open Bluetooth settings on the host and pair with the phone/controller when it appears.
5. Select the gamepad/controller input in the game or emulator.

Bluetooth HID Device support depends on the Android phone's hardware and firmware. If the phone does not expose the required profile, this mode may not be available on that device. Use Windows Wi-Fi mode if you want to connect through the Windows companion and it is working on your network.

## Troubleshooting

| Problem | What it may mean / what to check |
|---|---|
| The phone cannot connect | The phone and PC may be on different networks, the IPv4 address may be wrong, the receiver may not be running, or Windows Firewall may be blocking Python. |
| The PIN is rejected | The receiver creates a new PIN each time it starts. Enter the PIN currently shown in the open receiver window. |
| The receiver cannot start | Check that the Python virtual environment was created, requirements were installed, and the virtual gamepad driver is available. |
| Windows does not show a controller | Check the virtual gamepad driver installation and restart the receiver after resolving driver issues. |
| PPSSPP does not respond | Select the virtual Xbox controller in PPSSPP and map the controls manually. |
| Buttons stop responding | Keep the receiver open, check Wi-Fi stability and reconnect. The receiver releases all controls after 1.5 seconds without a valid state packet. |
| The PC's IP address changed | Run `ipconfig` again and use the IPv4 address of the active Wi-Fi adapter, not the Default Gateway. |
| Bluetooth pairing is unavailable | The phone may not support or expose the Bluetooth HID Device profile. |

## Security, limitations and project status

- The Windows receiver listens on `0.0.0.0:26760`, so it can receive packets through the PC's network interfaces.
- The six-digit PIN is a basic local-network pairing check, not encrypted authentication. Anyone who can see the PIN and reach the port on your LAN may be able to send input.
- Use this prototype only on a trusted private network. Do not expose UDP port 26760 to the public internet or configure router port forwarding for it.
- If Windows Firewall asks, allow Python on **Private networks only**, and only when you trust the network.
- Only one phone is accepted as the active controller at a time.
- The receiver releases all controls after 1.5 seconds without a valid state packet.
- Bluetooth HID availability depends on Android device hardware and firmware.
- The app has been tested on a limited set of devices and games. Compatibility and latency are not guaranteed.
- The current build does not provide per-game profiles, in-app button remapping or a configurable mapping editor.

### Virtual gamepad driver note

The Windows virtual controller uses `vgamepad` with ViGEmBus. The upstream ViGEmBus project is archived and retired, so it may not receive future fixes or security updates. Treat it as a prototype dependency, not a guaranteed long-term driver solution. Install drivers only from trusted official sources and do not disable Windows security features to force installation.

## Repository layout

```text
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
│   └── README.md               # Detailed Windows setup guide
├── .github/workflows/build.yml # Android build and Python syntax check
├── build.gradle
├── settings.gradle
└── gradle.properties
```

## Development checks

The GitHub Actions workflow:

1. Sets up JDK 17 and the Android SDK.
2. Runs Python bytecode compilation on `windows-companion/receiver.py`.
3. Builds the Android debug APK.
4. Uploads the APK as the `PSP-Controller-APK` workflow artifact.

For a local Python syntax check from the repository root:

```powershell
python -m py_compile windows-companion/receiver.py
```

For a local import check after installing the Windows companion requirements:

```powershell
python -c "import vgamepad; print('vgamepad import OK')"
```

## License

No license file is currently included. Unless a license is added to the repository, all rights remain with the copyright holder and reuse/redistribution permissions are not explicitly granted.
