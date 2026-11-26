# PSP BT Gamepad

An Android on-screen gamepad with two connection options:

- **Bluetooth HID:** Android exposes itself as a Bluetooth gamepad on supported phones.
- **Windows companion:** sends controller state over local Wi-Fi to a Windows receiver, which exposes a virtual Xbox 360 controller.

## Windows companion setup

1. Use a Windows 10/11 PC and install Python 3.10 or newer.
2. Open PowerShell in the `windows-companion` folder.
3. Install the Python dependency:

   ```powershell
   py -m pip install -r requirements.txt
   ```

   On Windows, the `vgamepad` package may launch its bundled ViGEmBus driver installer. Review the installer and approve it only if you're comfortable installing a system-level driver. Administrator approval may be required.
4. Start the receiver:

   ```powershell
   py receiver.py
   ```

5. The receiver displays a fresh six-digit pairing PIN. Keep the receiver window open.
6. Find your laptop's IPv4 address with `ipconfig` (use the Wi-Fi adapter's IPv4 address).
7. Connect the phone and laptop to the same trusted Wi-Fi network.
8. Open PSP BT Gamepad on Android, enter the laptop IPv4 address and the PIN, then tap **Connect to PC**.
9. If Windows Defender Firewall asks, allow Python on private networks only.

The receiver listens on UDP port **26760**. The pairing PIN is regenerated every time the receiver starts. Do not expose this UDP port to the public internet; this prototype is intended for a trusted local network.

## Controls

| Phone control | Virtual Xbox control |
|---|---|
| Cross | A |
| Circle | B |
| Square | X |
| Triangle | Y |
| L / R | Left / Right shoulder |
| Select / Start | Back / Start |
| D-pad | D-pad |
| Analog stick | Left analog stick |

## Driver note

The current virtual Xbox output uses ViGEmBus through `vgamepad`. The upstream ViGEmBus project was archived in November 2023, so it is retired and may not receive future fixes. This is a prototype choice, not a long-term driver guarantee. We should confirm your Windows version and test driver compatibility before relying on it.

## Current limitations

- This first companion version supports one active phone at a time.
- The pairing PIN is a basic local-network safeguard, not encrypted authentication.
- Android Bluetooth HID mode remains available separately.
- Successful compilation does not replace real-device testing. Check button mapping and connection stability with PPSSPP and your target PC games.
