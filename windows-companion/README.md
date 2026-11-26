# PSP BT Gamepad

An Android on-screen gamepad with two connection options:

- **Bluetooth HID:** Android Bluetooth HID Device mode, where the phone itself is exposed as a gamepad (supported Android devices only).
- **Windows companion:** sends controller state over local Wi-Fi to a Windows receiver, which exposes a virtual Xbox 360 controller through ViGEmBus.

## Windows companion setup

1. Install Python 3.10 or newer.
2. Install the ViGEmBus driver from the official Nefarius project releases: https://github.com/nefarius/ViGEmBus/releases
3. Open PowerShell in the `windows-companion` folder.
4. Install the Python dependency:

   ```powershell
   py -m pip install -r requirements.txt
   ```

5. Start the receiver:

   ```powershell
   py receiver.py
   ```

6. Find your laptop's IPv4 address with `ipconfig` (use the Wi-Fi adapter's IPv4 address).
7. Connect the phone and laptop to the same trusted Wi-Fi network.
8. Open PSP BT Gamepad on Android, enter the laptop IPv4 address, and tap **Connect to PC**.
9. In Windows, allow Python through Windows Defender Firewall on private networks if prompted.

The receiver listens on UDP port **26760**. Keep the receiver window open while playing. Do not expose this UDP port to the public internet; this prototype is intended for a trusted local network.

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

## Notes

- The Wi-Fi companion requires the ViGEmBus virtual gamepad driver on Windows.
- The first companion version is a local-network prototype. It has no pairing PIN yet.
- Android Bluetooth HID mode remains available as a separate option.
- Successful compilation does not replace real-device testing. Check button mapping and connection stability with PPSSPP and your target PC games.
