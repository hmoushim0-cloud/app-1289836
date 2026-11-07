package sa.factory.warshanowservices1.data.remote

import sa.factory.warshanowservices1.model.WarshaNowItem
import sa.factory.warshanowservices1.model.WarshaNowOrder

interface WarshaNowApiService {
    suspend fun fetchCatalog(): List<WarshaNowItem>
    suspend fun submitOrder(order: WarshaNowOrder): Boolean
}
