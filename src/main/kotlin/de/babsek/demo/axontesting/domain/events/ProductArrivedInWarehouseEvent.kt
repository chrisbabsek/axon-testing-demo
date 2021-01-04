package de.babsek.demo.axontesting.domain.events

data class ProductArrivedInWarehouseEvent(
    val warehouseId: String,
    val articleNumber: String,
    val amount: Int
)
