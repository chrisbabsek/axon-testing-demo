package de.babsek.demo.axontesting.domain.commands

import de.babsek.demo.axontesting.domain.value.DeliveryAddress
import org.axonframework.modelling.command.TargetAggregateIdentifier

data class ShipProductsCommand(
    @TargetAggregateIdentifier
    val warehouseId: String,
    // Article Number -> Amount
    val products: Map<String, Int>,
    val deliveryAddress: DeliveryAddress
)
