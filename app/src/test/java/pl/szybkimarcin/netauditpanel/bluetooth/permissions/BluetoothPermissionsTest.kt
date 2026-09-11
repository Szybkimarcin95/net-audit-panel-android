package pl.szybkimarcin.netauditpanel.bluetooth.permissions

import android.Manifest
import org.junit.Assert.assertArrayEquals
import org.junit.Test

class BluetoothPermissionsTest {
    @Test
    fun `Android 12 and newer require nearby-device permissions`() {
        assertArrayEquals(
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_ADVERTISE,
            ),
            BluetoothPermissions.runtimePermissions(31),
        )
    }

    @Test
    fun `Android 11 and older require location for discovery`() {
        assertArrayEquals(
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            BluetoothPermissions.runtimePermissions(30),
        )
    }
}

