package de.babsek.demo.axontesting.domain.exceptions

class OutOfStockException(
    val articleNumber: String,
    val requestedAmount: Int,
    val availableAmount: Int
) : RuntimeException() {

    override val message: String?
        get() = "Product $articleNumber out of stock! requested amount $requestedAmount, available amount $availableAmount"

}