package pl.szybkimarcin.netauditpanel.bluetooth.api

enum class TransportKind {
    BLE,
    CLASSIC,
}

sealed interface ConnectionState {
    data object Idle : ConnectionState
    data object Scanning : ConnectionState
    data class Connecting(val deviceId: String) : ConnectionState
    data class Connected(val deviceId: String) : ConnectionState
    data class Failed(val reason: String) : ConnectionState
}

data class DiscoveredDevice(
    val id: String,
    val name: String?,
    val transport: TransportKind,
    val rssi: Int?,
)

