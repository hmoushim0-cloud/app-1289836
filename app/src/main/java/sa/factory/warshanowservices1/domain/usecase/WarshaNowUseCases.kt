package sa.factory.warshanowservices1.domain.usecase

import sa.factory.warshanowservices1.domain.repository.WarshaNowRepository
import sa.factory.warshanowservices1.model.WarshaNowItem
import sa.factory.warshanowservices1.model.WarshaNowOrder
import sa.factory.warshanowservices1.model.WarshaNowTelemetry
import kotlinx.coroutines.flow.Flow

class GetCatalogUseCase(private val repository: WarshaNowRepository) {
    operator fun invoke(): Flow<List<WarshaNowItem>> = repository.catalogFlow
}

class ManageCartUseCase(private val repository: WarshaNowRepository) {
    val cart = repository.activeCart
    fun add(item: WarshaNowItem) = repository.addToCart(item)
    fun remove(item: WarshaNowItem) = repository.removeFromCart(item)
    fun clear() = repository.clearCart()
}

class ProcessCheckoutUseCase(private val repository: WarshaNowRepository) {
    suspend operator fun invoke(paymentMethod: String = "Mada Contactless EMV"): WarshaNowOrder =
        repository.checkout(paymentMethod)
}

class ObserveHardwareTelemetryUseCase(private val repository: WarshaNowRepository) {
    operator fun invoke(): Flow<WarshaNowTelemetry> = repository.telemetryFlow
}
