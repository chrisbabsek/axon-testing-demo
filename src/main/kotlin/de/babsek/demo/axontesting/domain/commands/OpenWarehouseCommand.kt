package de.babsek.demo.axontesting.domain.commands

import org.axonframework.modelling.command.TargetAggregateIdentifier

data class OpenWarehouseCommand(
    @TargetAggregateIdentifier
    val warehouseId: String
)
