package sa.factory.warshanowservices1.domain.repository

import sa.factory.warshanowservices1.model.WarshaNowItem
import sa.factory.warshanowservices1.model.WarshaNowOrder
import sa.factory.warshanowservices1.model.WarshaNowTelemetry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface WarshaNowRepository {
    val catalogFlow: Flow<List<WarshaNowItem>>
    val activeCart: StateFlow<List<WarshaNowItem>>
    val telemetryFlow: Flow<WarshaNowTelemetry>
    
    fun addToCart(item: WarshaNowItem)
    fun removeFromCart(item: WarshaNowItem)
    fun clearCart()
    suspend fun checkout(paymentMethod: String): WarshaNowOrder
    suspend fun refreshCatalog()
}
