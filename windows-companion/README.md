# PSP BT Gamepad — Windows companion setup

This guide explains how to prepare a Windows PC, start the companion receiver, connect the Android app and use the phone as a controller in PPSSPP.

You do **not** need to install the Android project or Android Studio on the Windows PC just to run the receiver. You need the Android app installed on your phone, plus the Windows companion files from this repository.

## 1. Understand what the Windows companion does

The Android app reads your touches on the on-screen controller. It sends the current button and analog-stick state over your local Wi-Fi network to this Windows program.

The Windows program is called `receiver.py`. It receives those messages and uses `vgamepad` to send input to a virtual Xbox 360 controller. Windows games and emulators that support XInput can then read that virtual controller.

```text
Android phone
  Controller app
      |
      | Wi-Fi: UDP port 26760
      v
Windows PC
  receiver.py (Python)
      |
      | vgamepad
      v
Virtual Xbox 360 controller
      |
      v
PPSSPP / compatible game
```

A few terms you'll see in this guide:

- **Python:** the programming language used to run the receiver.
- **Virtual environment (`.venv`):** a separate Python environment for this project, so its installed packages do not interfere with other Python projects.
- **vgamepad:** the Python package that sends button and joystick input to a virtual Xbox controller.
- **ViGEmBus:** the Windows virtual gamepad driver that vgamepad depends on.
- **IPv4 address:** the PC's local network address. The phone uses it to know which PC to contact.
- **UDP port 26760:** the network endpoint where the receiver waits for controller messages.
- **Pairing PIN:** a six-digit code printed by the receiver. The Android app must send the current code when it connects.

## 2. What you need

Before starting, make sure you have:

- Windows 10 or Windows 11.
- Python 3.10 or newer installed.
- The `windows-companion` folder from this repository.
- A compatible ViGEmBus virtual gamepad driver installation.
- The Android PSP BT Gamepad app installed on your phone.
- Your phone and PC connected to the same trusted Wi-Fi network.

### Check that Python is installed

1. Open **Command Prompt**.
2. Run:

   ```cmd
   py --version
   ```

3. You should see a Python version printed.

If Windows says that `py` is not recognised, Python may not be installed or the Python launcher may not be available. Install Python from its official distribution and reopen Command Prompt before continuing.

## 3. Open the correct folder in Command Prompt

The commands in this guide must be run from the `windows-companion` directory, because that is where `receiver.py` and `requirements.txt` are located.

An easy way to open Command Prompt in the correct folder:

1. Open the repository folder in File Explorer.
2. Open the `windows-companion` folder.
3. Click the File Explorer address bar.
4. Type `cmd` and press Enter.

A Command Prompt window should open with `windows-companion` as its current directory.

You can check by running:

```cmd
dir
```

You should see files such as `receiver.py` and `requirements.txt`.

## 4. Create the project's Python environment

Run:

```cmd
py -m venv .venv
```

This creates a folder named `.venv`. It contains a project-specific Python environment. You normally only need to create it once.

Next, install the Python package required by the receiver:

```cmd
.venv\Scripts\python.exe -m pip install -r requirements.txt
```

The requirements file lists `vgamepad`. Pip downloads and installs it into this project's `.venv` environment.

### If the vgamepad installer tries to install ViGEmBus

Only if a compatible ViGEmBus driver is **already installed** and you need to skip the package's bundled driver-install step, use these commands in **Command Prompt**:

```cmd
set VGAMEPAD_SKIP_VIGEMBUS_INSTALL=true
.venv\Scripts\python.exe -m pip install -r requirements.txt
```

The `set` command applies to the current Command Prompt window. It tells the package installer to skip its bundled driver-install step; it does not install the driver for you.

Do not use this option as a substitute for the required driver. Do not download drivers from unofficial sites or bypass Windows driver-security controls.

> **Driver status:** ViGEmBus is an archived and retired upstream project. Future compatibility and fixes are not guaranteed. Treat this as a prototype dependency.

## 5. Start the receiver

In the same Command Prompt window and folder, run:

```cmd
.venv\Scripts\python.exe receiver.py
```

You can also double-click `Start-Controller.bat` after completing the setup, if that file is present in your copy of the folder.

When the receiver starts, it prints information similar to:

```text
PSP BT Gamepad receiver listening on UDP 26760
Enter this pairing PIN in the Android app: 123456
Keep this window open. Press Ctrl+C to stop.
```

The PIN shown on your computer will be different. **Use the PIN displayed by your receiver, not the example PIN in this guide.**

Keep this window open while playing. Closing it or pressing Ctrl+C stops the receiver.

## 6. Find your PC's IPv4 address

The Android app needs your PC's local IPv4 address so it knows where to send controller messages.

1. Open Command Prompt on the PC.
2. Run:

   ```cmd
   ipconfig
   ```

3. Find the section for the network adapter currently connected to your Wi-Fi.
4. Look for **IPv4 Address**.

Example:

```text
Wireless LAN adapter Wi-Fi:

   IPv4 Address. . . . . . . . . . . : 192.168.1.25
   Default Gateway . . . . . . . . . : 192.168.1.1
```

In this example, the PC's IPv4 address is `192.168.1.25`. The Default Gateway is the router's address, so do **not** enter that instead.

Your address will probably be different. Enter the IPv4 address shown on your own PC.

If you see several network adapters, use the one that is actually connected to the same Wi-Fi network as your phone. Do not use an address belonging to a disconnected adapter or a virtual network adapter.

## 7. Connect the Android app to Windows

Before connecting, check all of the following:

- The receiver is still running on Windows.
- The phone and PC are connected to the same trusted Wi-Fi network.
- You have the PC's current Wi-Fi IPv4 address.
- You have the current six-digit PIN displayed in the receiver window.

Then:

1. Open PSP BT Gamepad on your Android phone.
2. Find the Windows / PC connection area.
3. Enter the PC's IPv4 address.
4. Enter the six-digit PIN from the receiver window.
5. Tap **Connect to PC**.
6. Test the controller input.

The receiver should print a message when the phone pairs. If you restart the receiver, it generates a new PIN, so enter the new one in the app.

### If Windows Firewall asks for permission

Windows may show a firewall prompt when Python first listens for network connections.

Only allow Python on **Private networks** if you trust the Wi-Fi network you are using. Do not enable public-network access just to make the prototype work.

## 8. Use the controller in PPSSPP

Once the Android app is connected:

1. Open PPSSPP on Windows.
2. Open **Settings → Controls → Control mapping**. The exact wording may vary by version.
3. Find the input-device selection, if available, and choose the virtual Xbox 360 controller.
4. Map the PSP controls to the virtual controller buttons.
5. Test each button and the analog stick.

The Windows receiver exposes these mappings:

| Phone control | Virtual Xbox controller |
|---|---|
| Cross | A |
| Circle | B |
| Square | X |
| Triangle | Y |
| L | Left shoulder |
| R | Right shoulder |
| Select | Back |
| Start | Start |
| D-pad | D-pad |
| Analog stick | Left analog stick |

PPSSPP may not choose or map the virtual controller automatically. If the game does not respond, check the selected input device and complete the control mapping manually.

## 9. Troubleshooting

### The phone cannot connect to the PC

Check these in order:

1. Is the receiver window still open?
2. Are the phone and PC on the same Wi-Fi network?
3. Did you enter the PC's Wi-Fi **IPv4 Address**, rather than its Default Gateway?
4. Did you enter the PIN currently displayed by the receiver?
5. Did you allow Python on the trusted Private network if Windows Firewall asked?

If the PC changes Wi-Fi networks or reconnects to the router, its IPv4 address may change. Run `ipconfig` again and enter the current address.

### The PIN is rejected

The receiver generates a fresh six-digit PIN every time it starts. Use the PIN from the current receiver window. If you restart the receiver, replace the old PIN in the phone app.

### The receiver closes or reports an error

Check that you opened Command Prompt in the `windows-companion` folder and completed the Python environment setup. If the error mentions `vgamepad`, install the requirements again:

```cmd
.venv\Scripts\python.exe -m pip install -r requirements.txt
```

If the error relates to the virtual gamepad driver, check that a compatible ViGEmBus installation is available. Do not try to work around Windows driver-security warnings.

### Windows does not show a virtual controller

The receiver needs both the Python package and its compatible virtual gamepad driver. Check the driver installation and then restart the receiver. A successful Python import alone does not prove that Windows can create the virtual controller.

### PPSSPP opens but does not respond

The receiver may be working while PPSSPP is listening to a different input device. Open PPSSPP's control mapping, select the virtual Xbox controller and map the buttons manually.

### Buttons stop responding

Keep the receiver window open and check that the Wi-Fi connection is stable. The receiver releases all controls if it has not received a valid controller state for 1.5 seconds. Reconnect from the phone if necessary.

### Bluetooth pairing is unavailable

Bluetooth HID is separate from this Windows Wi-Fi receiver. Availability depends on whether the Android phone supports and exposes the Bluetooth HID Device profile. If it does not, try the Windows Wi-Fi mode instead.

## 10. Network and security notes

- The receiver listens on UDP port **26760**.
- It binds to `0.0.0.0` so it can receive packets through the PC's network interfaces.
- The phone and PC must be able to reach each other on the local network.
- The receiver accepts one active phone at a time.
- A six-digit PIN is a basic pairing check, not encrypted authentication. It does not protect controller messages from someone who can observe the PIN and reach the receiver on your local network.
- Use this prototype only on a trusted private network.
- Do not expose UDP port 26760 to the public internet or configure router port forwarding for it.
- After 1.5 seconds without a valid state packet, the receiver resets the virtual controller and releases all controls.

## 11. Stop the receiver

When you have finished playing, return to the receiver's Command Prompt window and press:

```text
Ctrl + C
```

The receiver stops listening, resets the virtual controller state and closes its network socket.

## 12. Developer checks

Run these commands from the `windows-companion` directory.

Check that the Python file has valid syntax:

```powershell
.venv\Scripts\python.exe -m py_compile receiver.py
```

Check that the vgamepad package can be imported:

```powershell
.venv\Scripts\python.exe -c "import vgamepad; print('vgamepad import OK')"
```

These checks are useful, but they do not replace testing the controller on the actual Windows PC, Android phone and game/emulator.

---

For the overall project description, Android build instructions, Bluetooth mode and repository structure, see the [main project README](../README.md).
