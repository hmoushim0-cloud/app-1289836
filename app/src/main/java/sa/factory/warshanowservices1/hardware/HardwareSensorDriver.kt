package sa.factory.warshanowservices1.hardware

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import sa.factory.warshanowservices1.engine.MadaCardScheme
import sa.factory.warshanowservices1.engine.MadaContactlessEmvL2ProtocolEngine
import sa.factory.warshanowservices1.engine.MobileWorkshopDispatchRoutingEngine
import sa.factory.warshanowservices1.engine.ServiceSpecialization

data class HardwareTelemetryFrame(
    val peripheralName: String,
    val connectionState: String,
    val livePayload: String,
    val timestamp: Long = System.currentTimeMillis()
)

class HardwareSensorDriver(
    val madaEngine: MadaContactlessEmvL2ProtocolEngine = MadaContactlessEmvL2ProtocolEngine(),
    val routingEngine: MobileWorkshopDispatchRoutingEngine = MobileWorkshopDispatchRoutingEngine()
) {
    private val peripheral = "Mada Contactless EMV L2 NFC + GPS Dispatch"

    fun observePeripheralStream(): Flow<HardwareTelemetryFrame> = flow {
        emit(HardwareTelemetryFrame(peripheral, "CONNECTING", "Mada EMV L2 Kernel & GPS Telemetry initializing..."))
        delay(300)
        emit(HardwareTelemetryFrame(peripheral, "ONLINE", "Mada Contactless EMV L2 NFC + GPS Dispatch: ONLINE | READY"))

        var frameIndex = 0
        while (true) {
            delay(1500)
            frameIndex++

            val amount = 120.0 + (frameIndex * 15.0)
            val txn = madaEngine.processContactlessTransaction(
                pan = "5888501234567890",
                amountSar = amount,
                scheme = MadaCardScheme.MADA,
                pinEntered = amount > 300.0
            )

            val availableVans = routingEngine.getAvailableFleetCount()
            val totalVans = routingEngine.getTotalFleetSize()
            val totalVol = madaEngine.getTotalProcessedVolumeSar()

            val payload = "MADA: ${txn.status} [AUTH: ${txn.approvalCode}] | VOL: ${String.format("%.0f", totalVol)} SAR | FLEET: $availableVans/$totalVans AVAILABLE | GPS: LOCK (24.7742N, 46.7385E)"

            emit(HardwareTelemetryFrame(
                peripheralName = peripheral,
                connectionState = "ACTIVE_STREAMING",
                livePayload = payload
            ))
        }
    }
}
