package com.example.pspcontroller

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.Executors

@SuppressLint("MissingPermission")
class MainActivity : AppCompatActivity() {

    private var hidDevice: BluetoothHidDevice? = null
    private var hostDevice: BluetoothDevice? = null

    // Report state: 12 buttons plus two signed analog axes.
    private var buttonState: Int = 0
    private var analogX: Byte = 0
    private var analogY: Byte = 0

    // User-customisable on-screen control layout.
    private var isLayoutEditing = false
    private var selectedControl: View? = null
    private var lastDragRawX = 0f
    private var lastDragRawY = 0f
    private val layoutPrefs by lazy {
        getSharedPreferences("controller_layout", Context.MODE_PRIVATE)
    }
    private val customizableControlIds = intArrayOf(
        R.id.btnCross, R.id.btnCircle, R.id.btnSquare, R.id.btnTriangle,
        R.id.btnL, R.id.btnR, R.id.btnSelect, R.id.btnStart,
        R.id.btnUp, R.id.btnDown, R.id.btnLeft, R.id.btnRight,
        R.id.analogStick
    )

    // Optional Wi-Fi connection to the Windows companion app.
    private val pcExecutor = Executors.newSingleThreadExecutor()
    @Volatile private var pcConnected = false
    @Volatile private var pcAddress: InetAddress? = null
    @Volatile private var pcPin: String? = null
    private var pcSocket: DatagramSocket? = null // accessed only on pcExecutor
    private val pcPort = 26760
    private val pcHeartbeatHandler = Handler(Looper.getMainLooper())
    private val pcHeartbeat = object : Runnable {
        override fun run() {
            if (pcConnected) {
                sendPcState()
                pcHeartbeatHandler.postDelayed(this, 500)
            }
        }
    }

    private val hidDescriptor = byteArrayOf(
        0x05.toByte(), 0x01.toByte(),
        0x09.toByte(), 0x05.toByte(),
        0xA1.toByte(), 0x01.toByte(),
        0x85.toByte(), 0x01.toByte(),
        0x05.toByte(), 0x09.toByte(),
        0x19.toByte(), 0x01.toByte(),
        0x29.toByte(), 0x0C.toByte(),
        0x15.toByte(), 0x00.toByte(),
        0x25.toByte(), 0x01.toByte(),
        0x75.toByte(), 0x01.toByte(),
        0x95.toByte(), 0x0C.toByte(),
        0x81.toByte(), 0x02.toByte(),
        0x75.toByte(), 0x04.toByte(),
        0x95.toByte(), 0x01.toByte(),
        0x81.toByte(), 0x03.toByte(),
        0x05.toByte(), 0x01.toByte(),
        0x09.toByte(), 0x30.toByte(),
        0x09.toByte(), 0x31.toByte(),
        0x15.toByte(), 0x81.toByte(),
        0x25.toByte(), 0x7F.toByte(),
        0x75.toByte(), 0x08.toByte(),
        0x95.toByte(), 0x02.toByte(),
        0x81.toByte(), 0x02.toByte(),
        0xC0.toByte()
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        requestBluetoothPermissions()
        setupButtons()
        setupLayoutEditor()
        restoreControlLayout()

        findViewById<Button>(R.id.btnConnect).setOnClickListener {
            initBluetoothHid()
            val discoverableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
                putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
            }
            startActivity(discoverableIntent)
        }

        findViewById<Button>(R.id.btnConnectPc).setOnClickListener {
            if (pcConnected) {
                disconnectPc()
            } else {
                connectPc()
            }
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
                    hidDevice?.registerApp(
                        sdp, null, null, Executors.newSingleThreadExecutor(), hidCallback
                    )
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
                if (registered) {
                    Toast.makeText(
                        this@MainActivity,
                        "Bluetooth gamepad ready to pair",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        override fun onConnectionStateChanged(device: BluetoothDevice, state: Int) {
            runOnUiThread {
                if (state == BluetoothProfile.STATE_CONNECTED) {
                    hostDevice = device
                    Toast.makeText(
                        this@MainActivity,
                        "Connected to ${device.name}",
                        Toast.LENGTH_SHORT
                    ).show()
                    sendHidReport()
                } else if (state == BluetoothProfile.STATE_DISCONNECTED) {
                    hostDevice = null
                    Toast.makeText(this@MainActivity, "Bluetooth disconnected", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupButtons() {
        bindButton(R.id.btnCross, 0)
        bindButton(R.id.btnCircle, 1)
        bindButton(R.id.btnSquare, 2)
        bindButton(R.id.btnTriangle, 3)
        bindButton(R.id.btnL, 4)
        bindButton(R.id.btnR, 5)
        bindButton(R.id.btnSelect, 6)
        bindButton(R.id.btnStart, 7)
        bindButton(R.id.btnUp, 8)
        bindButton(R.id.btnDown, 9)
        bindButton(R.id.btnLeft, 10)
        bindButton(R.id.btnRight, 11)

        val analog = findViewById<AnalogStickView>(R.id.analogStick)
        analog.onMoveListener = { x, y ->
            analogX = x
            analogY = y
            sendHidReport()
            sendPcState()
        }
        analog.setOnTouchListener { view, event ->
            if (isLayoutEditing) {
                handleLayoutTouch(view, event)
                true
            } else {
                false
            }
        }
    }

    private fun bindButton(viewId: Int, bitIndex: Int) {
        findViewById<View>(viewId).setOnTouchListener { view, event ->
            if (isLayoutEditing) {
                handleLayoutTouch(view, event)
                true
            } else {
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                        view.isPressed = true
                        buttonState = buttonState or (1 shl bitIndex)
                        sendHidReport()
                        sendPcState()
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_POINTER_UP -> {
                        view.isPressed = false
                        buttonState = buttonState and (1 shl bitIndex).inv()
                        sendHidReport()
                        sendPcState()
                    }
                }
                true
            }
        }
    }

    private fun setupLayoutEditor() {
        findViewById<Button>(R.id.btnEditLayout).setOnClickListener {
            isLayoutEditing = true
            selectedControl = null
            findViewById<Button>(R.id.btnEditLayout).visibility = View.GONE
            findViewById<View>(R.id.layoutEditorPanel).visibility = View.VISIBLE
            updateSelectedControlLabel()
            Toast.makeText(
                this,
                "Drag controls to move them. Select one, then use − / + to resize.",
                Toast.LENGTH_LONG
            ).show()
        }

        findViewById<Button>(R.id.btnDoneLayout).setOnClickListener {
            isLayoutEditing = false
            selectedControl = null
            findViewById<View>(R.id.layoutEditorPanel).visibility = View.GONE
            findViewById<Button>(R.id.btnEditLayout).visibility = View.VISIBLE
        }

        findViewById<Button>(R.id.btnSizeDown).setOnClickListener {
            resizeSelectedControl(-0.1f)
        }
        findViewById<Button>(R.id.btnSizeUp).setOnClickListener {
            resizeSelectedControl(0.1f)
        }
        findViewById<Button>(R.id.btnResetLayout).setOnClickListener {
            resetControlLayout()
        }
    }

    private fun handleLayoutTouch(view: View, event: MotionEvent) {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                selectedControl = view
                lastDragRawX = event.rawX
                lastDragRawY = event.rawY
                updateSelectedControlLabel()
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.rawX - lastDragRawX
                val dy = event.rawY - lastDragRawY
                view.translationX += dx
                view.translationY += dy
                lastDragRawX = event.rawX
                lastDragRawY = event.rawY
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                saveControlLayout(view)
                view.performClick()
            }
        }
    }

    private fun resizeSelectedControl(change: Float) {
        val view = selectedControl ?: run {
            Toast.makeText(this, "Tap a control first", Toast.LENGTH_SHORT).show()
            return
        }
        val newScale = (view.scaleX + change).coerceIn(0.6f, 1.8f)
        view.scaleX = newScale
        view.scaleY = newScale
        saveControlLayout(view)
    }

    private fun saveControlLayout(view: View) {
        layoutPrefs.edit()
            .putFloat("${view.id}_x", view.translationX)
            .putFloat("${view.id}_y", view.translationY)
            .putFloat("${view.id}_scale", view.scaleX)
            .apply()
    }

    private fun restoreControlLayout() {
        customizableControlIds.forEach { id ->
            val view = findViewById<View>(id)
            view.translationX = layoutPrefs.getFloat("${id}_x", 0f)
            view.translationY = layoutPrefs.getFloat("${id}_y", 0f)
            val scale = layoutPrefs.getFloat("${id}_scale", 1f).coerceIn(0.6f, 1.8f)
            view.scaleX = scale
            view.scaleY = scale
        }
    }

    private fun resetControlLayout() {
        layoutPrefs.edit().clear().apply()
        customizableControlIds.forEach { id ->
            findViewById<View>(id).apply {
                translationX = 0f
                translationY = 0f
                scaleX = 1f
                scaleY = 1f
            }
        }
        selectedControl = null
        updateSelectedControlLabel()
        Toast.makeText(this, "Controller layout restored to default", Toast.LENGTH_SHORT).show()
    }

    private fun updateSelectedControlLabel() {
        val label = selectedControl?.let { view ->
            when (view.id) {
                R.id.analogStick -> "ANALOG"
                R.id.btnCross -> "CROSS"
                R.id.btnCircle -> "CIRCLE"
                R.id.btnSquare -> "SQUARE"
                R.id.btnTriangle -> "TRIANGLE"
                R.id.btnL -> "L"
                R.id.btnR -> "R"
                R.id.btnSelect -> "SELECT"
                R.id.btnStart -> "START"
                R.id.btnUp -> "D-PAD UP"
                R.id.btnDown -> "D-PAD DOWN"
                R.id.btnLeft -> "D-PAD LEFT"
                R.id.btnRight -> "D-PAD RIGHT"
                else -> "CONTROL"
            }
        } ?: "Select a control"
        findViewById<TextView>(R.id.textSelectedControl).text = label
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

    private fun connectPc() {
        val ipText = findViewById<EditText>(R.id.inputPcIp).text.toString().trim()
        if (ipText.isEmpty()) {
            Toast.makeText(this, "Enter your laptop's local IPv4 address", Toast.LENGTH_SHORT).show()
            return
        }

        val pin = findViewById<EditText>(R.id.inputPcPin).text.toString().trim()
        if (pin.length != 6 || pin.any { !it.isDigit() }) {
            Toast.makeText(this, "Enter the 6-digit PIN shown on your laptop", Toast.LENGTH_SHORT).show()
            return
        }

        val address = try {
            InetAddress.getByName(ipText)
        } catch (_: Exception) {
            Toast.makeText(this, "That IP address is not valid", Toast.LENGTH_SHORT).show()
            return
        }

        pcAddress = address
        pcPin = pin
        pcConnected = true
        pcHeartbeatHandler.removeCallbacks(pcHeartbeat)
        pcHeartbeatHandler.postDelayed(pcHeartbeat, 500)
        pcExecutor.execute {
            try {
                if (pcSocket == null || pcSocket?.isClosed == true) {
                    pcSocket = DatagramSocket()
                }
                sendPcPacket(
                    JSONObject()
                        .put("type", "hello")
                        .put("app", "PSP BT Gamepad")
                        .put("pin", pin)
                        .toString(),
                    address
                )
            } catch (error: Exception) {
                pcConnected = false
                pcHeartbeatHandler.removeCallbacks(pcHeartbeat)
                runOnUiThread {
                    findViewById<Button>(R.id.btnConnectPc).text = "Connect to PC"
                    Toast.makeText(
                        this@MainActivity,
                        "Could not send to laptop: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        findViewById<Button>(R.id.btnConnectPc).text = "Disconnect PC"
        Toast.makeText(this, "Sending to $ipText:$pcPort", Toast.LENGTH_SHORT).show()
    }

    private fun disconnectPc() {
        pcConnected = false
        pcHeartbeatHandler.removeCallbacks(pcHeartbeat)
        val address = pcAddress
        val pin = pcPin
        pcAddress = null
        pcPin = null
        pcExecutor.execute {
            try {
                if (address != null && pin != null && pcSocket != null && pcSocket?.isClosed == false) {
                    sendPcPacket(
                        JSONObject()
                            .put("type", "state")
                            .put("buttons", 0)
                            .put("x", 0)
                            .put("y", 0)
                            .put("pin", pin)
                            .toString(),
                        address
                    )
                }
            } catch (_: Exception) {
                // Closing the socket below still releases the local connection.
            } finally {
                pcSocket?.close()
                pcSocket = null
            }
        }
        findViewById<Button>(R.id.btnConnectPc).text = "Connect to PC"
        Toast.makeText(this, "PC connection stopped", Toast.LENGTH_SHORT).show()
    }

    private fun sendPcState() {
        if (!pcConnected) return
        val address = pcAddress ?: return
        val pin = pcPin ?: return
        val packet = JSONObject()
            .put("type", "state")
            .put("buttons", buttonState)
            .put("x", analogX.toInt())
            .put("y", analogY.toInt())
            .put("pin", pin)
            .toString()

        pcExecutor.execute {
            try {
                if (pcSocket == null || pcSocket?.isClosed == true) {
                    pcSocket = DatagramSocket()
                }
                sendPcPacket(packet, address)
            } catch (error: Exception) {
                pcConnected = false
                pcHeartbeatHandler.removeCallbacks(pcHeartbeat)
                runOnUiThread {
                    findViewById<Button>(R.id.btnConnectPc).text = "Connect to PC"
                    Toast.makeText(
                        this@MainActivity,
                        "PC send failed: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun sendPcPacket(message: String, address: InetAddress) {
        val bytes = message.toByteArray(Charsets.UTF_8)
        val packet = DatagramPacket(bytes, bytes.size, address, pcPort)
        pcSocket?.send(packet)
    }

    override fun onDestroy() {
        pcConnected = false
        pcHeartbeatHandler.removeCallbacks(pcHeartbeat)
        pcExecutor.execute {
            pcSocket?.close()
            pcSocket = null
        }
        pcExecutor.shutdown()
        super.onDestroy()
    }
}
