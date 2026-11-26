# PSP BT Gamepad — Windows companion

This folder contains the Windows receiver for the Android controller. It listens for controller state over local Wi-Fi and exposes it as a virtual Xbox 360 controller using vgamepad.

## Requirements

- Windows 10 or Windows 11.
- Python 3.10 or newer.
- A compatible ViGEmBus virtual gamepad driver installation.
- Phone and PC on the same trusted Wi-Fi network.

## First-time setup

Open Command Prompt in this folder (File Explorer address bar → type cmd → Enter), then run:

~~~cmd
py -m venv .venv
.venv\Scripts\python.exe -m pip install -r requirements.txt
~~~

If ViGEmBus is already installed and you need to skip the package's bundled driver installer, run:

~~~cmd
set VGAMEPAD_SKIP_VIGEMBUS_INSTALL=true
.venv\Scripts\python.exe -m pip install -r requirements.txt
~~~

Only use the skip option if a compatible driver is already installed. Do not download drivers from unofficial sites or bypass Windows driver-security controls.

## Start the receiver

After setup, either double-click **Start-Controller.bat** or run:

~~~cmd
.venv\Scripts\python.exe receiver.py
~~~

Keep the receiver window open while playing. It prints a fresh six-digit pairing PIN each time it starts.

## Connect the Android app

1. Connect the phone and PC to the same trusted Wi-Fi network.
2. On Windows, run ipconfig and find the IPv4 Address under the active Wi-Fi adapter. Do not use the Default Gateway.
3. Enter that IPv4 address and the current six-digit PIN in the Android app.
4. Tap **Connect to PC**.
5. If Windows Firewall asks, allow Python on private networks only, if you trust the network.

The receiver listens on UDP port **26760**. Do not expose this port to the public internet. The PIN is a basic LAN pairing safeguard, not encrypted authentication.

## Controls

| Android control | Virtual Xbox control |
|---|---|
| Cross | A |
| Circle | B |
| Square | X |
| Triangle | Y |
| L / R | Left / Right shoulder |
| Select / Start | Back / Start |
| D-pad | D-pad |
| Analog stick | Left analog stick |

## PPSSPP setup

1. Start the receiver and connect the phone.
2. Open PPSSPP → Settings → Controls → Control mapping.
3. Select the virtual Xbox 360 controller if it is not selected automatically.
4. Map the PSP controls to the corresponding Xbox buttons and stick.
5. Test all controls in a game or the PPSSPP control test screen.

## Troubleshooting

- **No controller appears:** check that the virtual gamepad driver is installed and that the receiver is running.
- **Phone cannot connect:** confirm both devices are on the same Wi-Fi, check the PC's Wi-Fi IPv4 address, and use the current PIN.
- **PIN rejected:** restart the receiver and enter the newly generated PIN.
- **PPSSPP ignores input:** select the virtual Xbox controller and map the buttons manually.
- **Input stops:** keep the receiver window open and check Wi-Fi stability. The receiver releases all buttons after 1.5 seconds without a valid packet.

## Driver status

This project uses vgamepad with ViGEmBus. The upstream ViGEmBus project is archived and retired, so future compatibility and fixes are not guaranteed. Treat this as a prototype dependency. Install drivers only from trusted official sources.
