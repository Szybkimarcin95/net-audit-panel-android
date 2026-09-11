package pl.szybkimarcin.netauditpanel.bluetooth.api

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/** Wspólny kontrakt transportu. Checkpoint 2 nie otwiera jeszcze połączeń radiowych. */
interface BluetoothTransport {
    val state: StateFlow<ConnectionState>
    val incomingData: Flow<ByteArray>

    suspend fun scan(): Flow<DiscoveredDevice>
    suspend fun connect(deviceId: String)
    suspend fun disconnect()
    suspend fun send(data: ByteArray)
}

