package de.babsek.demo.axontesting.domain.events

data class ProductTakenEvent(
        val warehouseId: String,
        val articleNumber: String,
        val amount: Int
)
