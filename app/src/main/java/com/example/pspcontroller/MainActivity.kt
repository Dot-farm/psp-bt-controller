package com.example.pspcontroller

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.util.concurrent.Executors

@SuppressLint("MissingPermission")
class MainActivity : AppCompatActivity() {

    private var hidDevice: BluetoothHidDevice? = null
    private var hostDevice: BluetoothDevice? = null

    // Report state: [Buttons_Low_Byte, Buttons_High_Byte, Analog_X, Analog_Y]
    private var buttonState: Int = 0
    private var analogX: Byte = 0
    private var analogY: Byte = 0

    // Standard HID Gamepad Descriptor (12 Buttons + X/Y Axis)
    private val hidDescriptor = byteArrayOf(
        0x05.toByte(), 0x01.toByte(), // Usage Page (Generic Desktop)
        0x09.toByte(), 0x05.toByte(), // Usage (Game Pad)
        0xA1.toByte(), 0x01.toByte(), // Collection (Application)
        0x85.toByte(), 0x01.toByte(), //   Report ID (1)
        0x05.toByte(), 0x09.toByte(), //   Usage Page (Button)
        0x19.toByte(), 0x01.toByte(), //   Usage Minimum (Button 1)
        0x29.toByte(), 0x0C.toByte(), //   Usage Maximum (Button 12)
        0x15.toByte(), 0x00.toByte(), //   Logical Minimum (0)
        0x25.toByte(), 0x01.toByte(), //   Logical Maximum (1)
        0x75.toByte(), 0x01.toByte(), //   Report Size (1 bit)
        0x95.toByte(), 0x0C.toByte(), //   Report Count (12 buttons)
        0x81.toByte(), 0x02.toByte(), //   Input (Data, Variable, Absolute)
        0x75.toByte(), 0x04.toByte(), //   Report Size (4 bits padding)
        0x95.toByte(), 0x01.toByte(), //   Report Count (1)
        0x81.toByte(), 0x03.toByte(), //   Input (Constant)
        0x05.toByte(), 0x01.toByte(), //   Usage Page (Generic Desktop)
        0x09.toByte(), 0x30.toByte(), //   Usage (X)
        0x09.toByte(), 0x31.toByte(), //   Usage (Y)
        0x15.toByte(), 0x81.toByte(), //   Logical Minimum (-127)
        0x25.toByte(), 0x7F.toByte(), //   Logical Maximum (127)
        0x75.toByte(), 0x08.toByte(), //   Report Size (8 bits)
        0x95.toByte(), 0x02.toByte(), //   Report Count (2 axes)
        0x81.toByte(), 0x02.toByte(), //   Input (Data, Variable, Absolute)
        0xC0.toByte()                 // End Collection
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        requestBluetoothPermissions()
        setupButtons()

        findViewById<Button>(R.id.btnConnect).setOnClickListener {
            initBluetoothHid()
            val discoverableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
                putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
            }
            startActivity(discoverableIntent)
        }
    }

    private fun requestBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_ADVERTISE,
                    Manifest.permission.BLUETOOTH_SCAN
                ),
                101
            )
        }
    }

    private fun initBluetoothHid() {
        val btManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val btAdapter = btManager.adapter

        btAdapter.getProfileProxy(this, object : BluetoothProfile.ServiceListener {
            override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                if (profile == BluetoothProfile.HID_DEVICE) {
                    hidDevice = proxy as BluetoothHidDevice
                    val sdp = BluetoothHidDeviceAppSdpSettings(
                        "PSP Virtual Gamepad",
                        "Bluetooth HID Controller",
                        "Custom",
                        BluetoothHidDevice.SUBCLASS1_COMBO,
                        hidDescriptor
                    )
                    hidDevice?.registerApp(sdp, null, null, Executors.newSingleThreadExecutor(), hidCallback)
                }
            }

            override fun onServiceDisconnected(profile: Int) {
                hidDevice = null
            }
        }, BluetoothProfile.HID_DEVICE)
    }

    private val hidCallback = object : BluetoothHidDevice.Callback() {
        override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
            runOnUiThread {
                if (registered) Toast.makeText(this@MainActivity, "Gamepad Ready! Pair from target device.", Toast.LENGTH_SHORT).show()
            }
        }

        override fun onConnectionStateChanged(device: BluetoothDevice, state: Int) {
            runOnUiThread {
                if (state == BluetoothProfile.STATE_CONNECTED) {
                    hostDevice = device
                    Toast.makeText(this@MainActivity, "Connected to ${device.name}", Toast.LENGTH_SHORT).show()
                } else if (state == BluetoothProfile.STATE_DISCONNECTED) {
                    hostDevice = null
                    Toast.makeText(this@MainActivity, "Disconnected", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupButtons() {
        // Map each button to a bit position (0 to 11)
        bindButton(R.id.btnCross, 0)    // Button 1
        bindButton(R.id.btnCircle, 1)   // Button 2
        bindButton(R.id.btnSquare, 2)   // Button 3
        bindButton(R.id.btnTriangle, 3) // Button 4
        bindButton(R.id.btnL, 4)        // Button 5
        bindButton(R.id.btnR, 5)        // Button 6
        bindButton(R.id.btnSelect, 6)   // Button 7
        bindButton(R.id.btnStart, 7)    // Button 8
        bindButton(R.id.btnUp, 8)       // Button 9
        bindButton(R.id.btnDown, 9)     // Button 10
        bindButton(R.id.btnLeft, 10)    // Button 11
        bindButton(R.id.btnRight, 11)   // Button 12

        val analog = findViewById<AnalogStickView>(R.id.analogStick)
        analog.onMoveListener = { x, y ->
            analogX = x
            analogY = y
            sendHidReport()
        }
    }

    private fun bindButton(viewId: Int, bitIndex: Int) {
        findViewById<View>(viewId).setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.isPressed = true
                    buttonState = buttonState or (1 shl bitIndex)
                    sendHidReport()
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.isPressed = false
                    buttonState = buttonState and (1 shl bitIndex).inv()
                    sendHidReport()
                }
            }
            true
        }
    }

    private fun sendHidReport() {
        val device = hostDevice ?: return
        val report = byteArrayOf(
            (buttonState and 0xFF).toByte(),
            ((buttonState shr 8) and 0xFF).toByte(),
            analogX,
            analogY
        )
        hidDevice?.sendReport(device, 1, report)
    }
}
