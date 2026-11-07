package sa.factory.warshanowservices1.di

import sa.factory.warshanowservices1.data.repository.WarshaNowRepositoryImpl
import sa.factory.warshanowservices1.domain.repository.WarshaNowRepository
import sa.factory.warshanowservices1.domain.usecase.*
import sa.factory.warshanowservices1.hardware.HardwareSensorDriver
import sa.factory.warshanowservices1.ui.viewmodel.WarshaNowViewModel

class DiContainer {
    val sensorDriver = HardwareSensorDriver()
    val repository: WarshaNowRepository = WarshaNowRepositoryImpl(sensorDriver)

    val getCatalogUseCase = GetCatalogUseCase(repository)
    val manageCartUseCase = ManageCartUseCase(repository)
    val processCheckoutUseCase = ProcessCheckoutUseCase(repository)
    val observeHardwareTelemetryUseCase = ObserveHardwareTelemetryUseCase(repository)

    fun provideViewModel(): WarshaNowViewModel = WarshaNowViewModel(
        getCatalogUseCase,
        manageCartUseCase,
        processCheckoutUseCase,
        observeHardwareTelemetryUseCase
    )
}
