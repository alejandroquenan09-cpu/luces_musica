package com.alejandro.lucesmusica

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.os.Handler
import android.os.Looper
import java.util.UUID

@SuppressLint("MissingPermission")
class LedStrip(private val context: Context, private val onStatus: (String) -> Unit) {

    private val serviceUuid = UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb")
    private val charUuid = UUID.fromString("0000fff3-0000-1000-8000-00805f9b34fb")

    private val adapter: BluetoothAdapter? =
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
    private val main = Handler(Looper.getMainLooper())

    private var gatt: BluetoothGatt? = null
    private var characteristic: BluetoothGattCharacteristic? = null
    private var scanning = false
    private var lastWrite = 0L

    val isReady: Boolean get() = characteristic != null

    fun connect() {
        val scanner = adapter?.bluetoothLeScanner ?: run {
            onStatus("Bluetooth apagado")
            return
        }
        if (scanning) return
        scanning = true
        onStatus("Buscando luces...")
        scanner.startScan(scanCallback)
        main.postDelayed({
            if (scanning) {
                scanning = false
                scanner.stopScan(scanCallback)
                if (gatt == null) onStatus("No encontré luces. Cierra Lotus Lantern y reintenta")
            }
        }, 15000)
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val name = result.device.name ?: return
            val ok = name.startsWith("ELK-") || name.startsWith("ELK~") ||
                name.startsWith("LED LIGHT STRIP") || name.startsWith("XSL-") ||
                name.startsWith("BLEDOM")
            if (ok && scanning) {
                scanning = false
                adapter?.bluetoothLeScanner?.stopScan(this)
                onStatus("Conectando a $name...")
                gatt = result.device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
            }
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                g.discoverServices()
            } else {
                characteristic = null
                g.close()
                gatt = null
                onStatus("Desconectado, reconectando...")
                main.postDelayed({ connect() }, 2000)
            }
        }

        override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
            characteristic = g.getService(serviceUuid)?.getCharacteristic(charUuid)
            if (characteristic != null) {
                characteristic!!.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
                onStatus("Luces conectadas")
                setPower(true)
            } else {
                onStatus("No encontré el canal de control de la tira")
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun write(bytes: ByteArray) {
        val c = characteristic ?: return
        val g = gatt ?: return
        c.value = bytes
        g.writeCharacteristic(c)
    }

    fun setPower(on: Boolean) {
        val v = if (on) 0xF0 else 0x00
        write(byteArrayOf(0x7E, 0x04, 0x04, v.toByte(), 0x00, if (on) 0x01 else 0x00, 0xFF.toByte(), 0x00, 0xEF.toByte()))
    }

    fun setColor(r: Int, g: Int, b: Int) {
        val now = System.currentTimeMillis()
        if (now - lastWrite < 40) return
        lastWrite = now
        write(byteArrayOf(0x7E, 0x07, 0x05, 0x03, r.toByte(), g.toByte(), b.toByte(), 0x10, 0xEF.toByte()))
    }

    fun disconnect() {
        gatt?.close()
        gatt = null
        characteristic = null
    }
}
