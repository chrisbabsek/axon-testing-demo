package de.babsek.demo.axontesting.domain.commands

import org.axonframework.modelling.command.TargetAggregateIdentifier

data class TakeProductsCommand(
        @TargetAggregateIdentifier
        val warehouseId: String,
        // Article Number -> Amount
        val products: Map<String, Int>
)
