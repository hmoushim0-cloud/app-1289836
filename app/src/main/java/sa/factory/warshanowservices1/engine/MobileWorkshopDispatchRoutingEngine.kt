package sa.factory.warshanowservices1.engine

import java.util.concurrent.ConcurrentHashMap
import kotlin.math.*

enum class ServiceSpecialization(val labelAr: String) {
    MECHANICAL_REPAIR("ميكانيكا عامة ومحركات"),
    BATTERY_ELECTRICAL("كهرباء وتبديل بطاريات"),
    TIRE_EMERGENCY("بنشر وإطارات متنقلة"),
    PERIODIC_MAINTENANCE("صيانة دورية وتغيير زيوت")
}

enum class DispatchPriority(val maxSlaMinutes: Int) {
    EMERGENCY_ROADSIDE(20),
    PRIORITY_HOME_SERVICE(45),
    SCHEDULED_STANDARD(120)
}

data class ServiceVanUnit(
    val vanId: String,
    val technicianName: String,
    val specialization: ServiceSpecialization,
    val currentLat: Double,
    val currentLon: Double,
    var isAvailable: Boolean = true,
    var jobsCompletedToday: Int = 0
)

data class DispatchAssignmentResult(
    val assignmentId: String,
    val customerRequestId: String,
    val assignedVan: ServiceVanUnit,
    val distanceKm: Double,
    val estimatedArrivalMinutes: Int,
    val withinSla: Boolean,
    val priority: DispatchPriority
)

class MobileWorkshopDispatchRoutingEngine {

    private val activeFleet = ConcurrentHashMap<String, ServiceVanUnit>()
    private val activeAssignments = ConcurrentHashMap<String, DispatchAssignmentResult>()

    init {
        // Seed mobile workshop fleet across Riyadh quadrants
        registerVan(ServiceVanUnit("VAN-RYD-01", "أحمد العتيبي", ServiceSpecialization.BATTERY_ELECTRICAL, 24.7742, 46.7385, true, 4))
        registerVan(ServiceVanUnit("VAN-RYD-02", "خالد الحربي", ServiceSpecialization.MECHANICAL_REPAIR, 24.7136, 46.6753, true, 2))
        registerVan(ServiceVanUnit("VAN-RYD-03", "محمد الدوسري", ServiceSpecialization.TIRE_EMERGENCY, 24.8125, 46.6341, true, 6))
        registerVan(ServiceVanUnit("VAN-RYD-04", "يوسف الغامدي", ServiceSpecialization.PERIODIC_MAINTENANCE, 24.6877, 46.7219, true, 3))
    }

    fun registerVan(van: ServiceVanUnit) {
        activeFleet[van.vanId] = van
    }

    fun computeHaversineDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0 // Earth radius in km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = sin(dLat / 2).pow(2.0) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2.0)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        val dist = r * c
        return Math.round(dist * 100.0) / 100.0
    }

    fun estimateEtaMinutes(distanceKm: Double, averageSpeedKmh: Double = 42.0): Int {
        val hours = distanceKm / averageSpeedKmh
        val minutes = (hours * 60.0).roundToInt()
        return minutes.coerceAtLeast(5)
    }

    fun dispatchNearestVan(
        customerRequestId: String,
        targetLat: Double,
        targetLon: Double,
        requiredSpecialization: ServiceSpecialization,
        priority: DispatchPriority = DispatchPriority.EMERGENCY_ROADSIDE
    ): DispatchAssignmentResult? {
        val eligibleVans = activeFleet.values
            .filter { it.isAvailable && it.specialization == requiredSpecialization }

        if (eligibleVans.isEmpty()) {
            // Fallback: any available van
            val fallbackVans = activeFleet.values.filter { it.isAvailable }
            if (fallbackVans.isEmpty()) return null
        }

        val pool = if (eligibleVans.isNotEmpty()) eligibleVans else activeFleet.values.filter { it.isAvailable }

        val bestVan = pool.minByOrNull { computeHaversineDistanceKm(it.currentLat, it.currentLon, targetLat, targetLon) }
            ?: return null

        val distance = computeHaversineDistanceKm(bestVan.currentLat, bestVan.currentLon, targetLat, targetLon)
        val eta = estimateEtaMinutes(distance)
        val withinSla = eta <= priority.maxSlaMinutes

        bestVan.isAvailable = false
        bestVan.jobsCompletedToday++

        val assignment = DispatchAssignmentResult(
            assignmentId = "DISP-${System.currentTimeMillis() % 100000}",
            customerRequestId = customerRequestId,
            assignedVan = bestVan,
            distanceKm = distance,
            estimatedArrivalMinutes = eta,
            withinSla = withinSla,
            priority = priority
        )

        activeAssignments[assignment.assignmentId] = assignment
        return assignment
    }

    fun completeAssignment(assignmentId: String): Boolean {
        val assignment = activeAssignments.remove(assignmentId) ?: return false
        val van = activeFleet[assignment.assignedVan.vanId]
        van?.isAvailable = true
        return true
    }

    fun getAvailableFleetCount(): Int = activeFleet.values.count { it.isAvailable }

    fun getTotalFleetSize(): Int = activeFleet.size
}
