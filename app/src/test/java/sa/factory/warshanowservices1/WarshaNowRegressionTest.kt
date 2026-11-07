package sa.factory.warshanowservices1

import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import sa.factory.warshanowservices1.data.repository.WarshaNowRepositoryImpl
import sa.factory.warshanowservices1.engine.*
import sa.factory.warshanowservices1.hardware.HardwareSensorDriver
import sa.factory.warshanowservices1.model.WarshaNowItem

class WarshaNowRegressionTest {

    @Test
    fun testCheckoutCalculationAndTax() = runBlocking {
        val sensorDriver = HardwareSensorDriver()
        val repository = WarshaNowRepositoryImpl(sensorDriver)

        val item1 = WarshaNowItem("t1", "خدمة تجريبية أ", "الوصف", 100.0, "معتمد", "عام")
        val item2 = WarshaNowItem("t2", "خدمة تجريبية ب", "الوصف", 200.0, "معتمد", "عام")

        repository.addToCart(item1)
        repository.addToCart(item2)

        val order = repository.checkout("Mada EMV")

        assertEquals(300.0, order.subtotalSar, 0.01)
        assertEquals(45.0, order.vatSar, 0.01)
        assertEquals(345.0, order.totalSar, 0.01)
        assertEquals(0, repository.activeCart.value.size)
    }

    @Test
    fun testMadaPanMasking() {
        val engine = MadaContactlessEmvL2ProtocolEngine()

        val masked = engine.maskPan("5888501234567890")
        assertEquals("588850******7890", masked)
    }

    @Test
    fun testMadaUnderLimitOfflineApproval() {
        val engine = MadaContactlessEmvL2ProtocolEngine()

        val txn = engine.processContactlessTransaction(
            pan = "5888501234567890",
            amountSar = 150.0,
            pinEntered = false
        )

        assertEquals(EmvTransactionStatus.APPROVED_OFFLINE, txn.status)
        assertTrue(txn.approvalCode.startsWith("MADA-"))
        assertEquals(8, txn.arqcHex.length)
    }

    @Test
    fun testMadaAboveLimitRequiresPin() {
        val engine = MadaContactlessEmvL2ProtocolEngine()

        val txn = engine.processContactlessTransaction(
            pan = "5888501234567890",
            amountSar = 450.0,
            pinEntered = false
        )

        assertEquals(EmvTransactionStatus.DECLINED_PIN_REQUIRED, txn.status)
        assertTrue(txn.approvalCode.startsWith("REJECT-"))
    }

    @Test
    fun testMadaAboveLimitApprovedWithPin() {
        val engine = MadaContactlessEmvL2ProtocolEngine()

        val txn = engine.processContactlessTransaction(
            pan = "5888501234567890",
            amountSar = 450.0,
            pinEntered = true
        )

        assertEquals(EmvTransactionStatus.APPROVED_ONLINE, txn.status)
        assertTrue(txn.approvalCode.startsWith("MADA-"))
    }

    @Test
    fun testMadaExceededLimitRejection() {
        val engine = MadaContactlessEmvL2ProtocolEngine()

        val txn = engine.processContactlessTransaction(
            pan = "5888501234567890",
            amountSar = 1500.0,
            pinEntered = true
        )

        assertEquals(EmvTransactionStatus.DECLINED_EXCEEDED_LIMIT, txn.status)
    }

    @Test
    fun testHaversineDistanceCalculation() {
        val engine = MobileWorkshopDispatchRoutingEngine()

        // Distance between Olaya (24.7136, 46.6753) and Al-Malqa (24.7742, 46.7385) in Riyadh
        val distKm = engine.computeHaversineDistanceKm(24.7136, 46.6753, 24.7742, 46.7385)
        assertTrue(distKm > 5.0 && distKm < 15.0)
    }

    @Test
    fun testNearestMobileWorkshopDispatch() {
        val engine = MobileWorkshopDispatchRoutingEngine()

        val assignment = engine.dispatchNearestVan(
            customerRequestId = "REQ-101",
            targetLat = 24.7750,
            targetLon = 46.7390,
            requiredSpecialization = ServiceSpecialization.BATTERY_ELECTRICAL,
            priority = DispatchPriority.EMERGENCY_ROADSIDE
        )

        assertNotNull(assignment)
        assertEquals("VAN-RYD-01", assignment?.assignedVan?.vanId) // Near Al-Malqa (24.7742, 46.7385)
        assertTrue(assignment?.distanceKm ?: 99.0 < 2.0)
        assertTrue(assignment?.withinSla == true)
    }

    @Test
    fun testFleetAssignmentAndCompletion() {
        val engine = MobileWorkshopDispatchRoutingEngine()

        val initialAvailable = engine.getAvailableFleetCount()

        val assignment = engine.dispatchNearestVan(
            customerRequestId = "REQ-102",
            targetLat = 24.7130,
            targetLon = 46.6750,
            requiredSpecialization = ServiceSpecialization.MECHANICAL_REPAIR
        )

        assertNotNull(assignment)
        assertEquals(initialAvailable - 1, engine.getAvailableFleetCount())

        val completeOk = engine.completeAssignment(assignment!!.assignmentId)
        assertTrue(completeOk)
        assertEquals(initialAvailable, engine.getAvailableFleetCount())
    }

    @Test
    fun testHardwareSensorDriverEmission() = runBlocking {
        val driver = HardwareSensorDriver()
        val frames = driver.observePeripheralStream().take(2).toList()

        assertEquals(2, frames.size)
        assertEquals("Mada Contactless EMV L2 NFC + GPS Dispatch", frames[0].peripheralName)
        assertEquals("CONNECTING", frames[0].connectionState)
        assertEquals("ONLINE", frames[1].connectionState)
    }
}
