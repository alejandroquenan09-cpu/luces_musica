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
