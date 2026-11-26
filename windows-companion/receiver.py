"""
PSP BT Gamepad - Windows companion receiver

Receives controller state packets from the Android app over the same local
Wi-Fi network and exposes them as a virtual Xbox 360 controller.

Prerequisites:
  1. Windows 10/11
  2. Python 3.10+
  3. ViGEmBus driver installed (see README.md)
  4. pip install -r requirements.txt

Run:
  py receiver.py
"""

import json
import socket
import time

import vgamepad as vg

HOST = "0.0.0.0"
PORT = 26760
STALE_AFTER_SECONDS = 1.5

BUTTON_MAP = {
    0: vg.XUSB_BUTTON.XUSB_GAMEPAD_A,                 # Cross
    1: vg.XUSB_BUTTON.XUSB_GAMEPAD_B,                 # Circle
    2: vg.XUSB_BUTTON.XUSB_GAMEPAD_X,                 # Square
    3: vg.XUSB_BUTTON.XUSB_GAMEPAD_Y,                 # Triangle
    4: vg.XUSB_BUTTON.XUSB_GAMEPAD_LEFT_SHOULDER,      # L
    5: vg.XUSB_BUTTON.XUSB_GAMEPAD_RIGHT_SHOULDER,     # R
    6: vg.XUSB_BUTTON.XUSB_GAMEPAD_BACK,               # Select
    7: vg.XUSB_BUTTON.XUSB_GAMEPAD_START,              # Start
    8: vg.XUSB_BUTTON.XUSB_GAMEPAD_DPAD_UP,
    9: vg.XUSB_BUTTON.XUSB_GAMEPAD_DPAD_DOWN,
    10: vg.XUSB_BUTTON.XUSB_GAMEPAD_DPAD_LEFT,
    11: vg.XUSB_BUTTON.XUSB_GAMEPAD_DPAD_RIGHT,
}


def apply_state(gamepad, buttons, x_axis, y_axis):
    gamepad.reset()

    for bit, button in BUTTON_MAP.items():
        if buttons & (1 << bit):
            gamepad.press_button(button=button)

    # Android sends -127..127. XInput uses -32768..32767.
    x_value = max(-32768, min(32767, int(x_axis * 32767 / 127)))
    # Android screen coordinates grow downward; XInput Y grows upward.
    y_value = max(-32768, min(32767, int(-y_axis * 32767 / 127)))
    gamepad.left_joystick(x_value=x_value, y_value=y_value)
    gamepad.update()


def main():
    gamepad = vg.VX360Gamepad()
    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    sock.bind((HOST, PORT))
    sock.settimeout(0.25)

    last_packet_at = 0.0
    last_client = None

    print(f"PSP BT Gamepad receiver listening on UDP {PORT}")
    print("Keep this window open. Press Ctrl+C to stop.")

    try:
        while True:
            try:
                raw, client = sock.recvfrom(2048)
                message = json.loads(raw.decode("utf-8"))
                last_packet_at = time.monotonic()
                last_client = client[0]

                if message.get("type") == "hello":
                    print(f"Phone found: {last_client}")
                    continue

                if message.get("type") == "state":
                    apply_state(
                        gamepad,
                        int(message.get("buttons", 0)),
                        int(message.get("x", 0)),
                        int(message.get("y", 0)),
                    )
            except socket.timeout:
                if last_packet_at and time.monotonic() - last_packet_at > STALE_AFTER_SECONDS:
                    gamepad.reset()
                    last_packet_at = 0.0
                    print("Controller timed out; all buttons released.")
            except (UnicodeDecodeError, json.JSONDecodeError, ValueError, TypeError) as error:
                print(f"Ignored invalid controller packet: {error}")
    except KeyboardInterrupt:
        print("\nStopping receiver...")
    finally:
        gamepad.reset()
        sock.close()


if __name__ == "__main__":
    main()
