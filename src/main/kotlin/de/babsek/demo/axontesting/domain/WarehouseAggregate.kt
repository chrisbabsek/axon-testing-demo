package de.babsek.demo.axontesting.domain

import de.babsek.demo.axontesting.domain.commands.*
import de.babsek.demo.axontesting.domain.events.ProductArrivedInWarehouseEvent
import de.babsek.demo.axontesting.domain.events.ProductTakenEvent
import de.babsek.demo.axontesting.domain.events.WarehouseClosedEvent
import de.babsek.demo.axontesting.domain.events.WarehouseOpenedEvent
import de.babsek.demo.axontesting.domain.exceptions.OutOfStockException
import de.babsek.demo.axontesting.domain.services.ShippingService
import org.axonframework.commandhandling.CommandHandler
import org.axonframework.eventsourcing.EventSourcingHandler
import org.axonframework.modelling.command.AggregateCreationPolicy
import org.axonframework.modelling.command.AggregateIdentifier
import org.axonframework.modelling.command.AggregateLifecycle
import org.axonframework.modelling.command.CreationPolicy
import org.axonframework.spring.stereotype.Aggregate

@Aggregate
class WarehouseAggregate {

    @AggregateIdentifier
    lateinit var warehouseId: String

    // Article Number -> Amount
    lateinit var stock: MutableMap<String, Int>

    @CreationPolicy(AggregateCreationPolicy.CREATE_IF_MISSING)
    @CommandHandler
    fun handleOpenWarehouseCommand(command: OpenWarehouseCommand) {
        if (!this::warehouseId.isInitialized) {
            AggregateLifecycle.apply(
                WarehouseOpenedEvent(warehouseId = command.warehouseId)
            )
        }
    }

    @EventSourcingHandler
    fun on(event: WarehouseOpenedEvent) {
        warehouseId = event.warehouseId
        stock = mutableMapOf()
    }

    @CommandHandler
    fun handleCloseWarehouseCommand(command: CloseWarehouseCommand) {
        require(stock.values.all { it == 0 }) {
            "Warehouse must be completely empty to be closed!"
        }
        AggregateLifecycle.apply(
            WarehouseClosedEvent(warehouseId = command.warehouseId)
        )
    }

    @EventSourcingHandler
    fun on(event: WarehouseClosedEvent) {
        AggregateLifecycle.markDeleted()
    }

    @CommandHandler
    fun handleStoreProductsInWarehouseCommand(command: StoreProductsInWarehouseCommand) {
        command.products.forEach { articleNumber, amount ->
            require(amount > 0) {
                "Arriving amount for product must be greater than 0!"
            }
            AggregateLifecycle.apply(
                ProductArrivedInWarehouseEvent(
                    warehouseId = command.warehouseId,
                    articleNumber = articleNumber,
                    amount = amount
                )
            )
        }
    }

    @EventSourcingHandler
    fun on(event: ProductArrivedInWarehouseEvent) {
        stock[event.articleNumber] = stock.getOrDefault(event.articleNumber, 0) + event.amount
    }

    fun takeProducts(products: Map<String, Int>) {
        products.forEach { articleNumber, requestedAmount ->
            if ((stock[articleNumber] ?: 0) < requestedAmount) {
                throw OutOfStockException(articleNumber, requestedAmount, stock[articleNumber] ?: 0)
            } else {
                AggregateLifecycle.apply(
                    ProductTakenEvent(
                        warehouseId = warehouseId,
                        articleNumber = articleNumber,
                        amount = requestedAmount
                    )
                )
            }
        }
    }

    @CommandHandler
    fun handleTakeProductsCommand(command: TakeProductsCommand) {
        takeProducts(command.products)
    }

    @CommandHandler
    fun handleShipProductsCommand(command: ShipProductsCommand, shippingService: ShippingService) {
        takeProducts(command.products)
        shippingService.shipProducts(
            address = command.deliveryAddress,
            products = command.products
        )
    }

    @EventSourcingHandler
    fun on(event: ProductTakenEvent) {
        stock[event.articleNumber] = stock.getOrDefault(event.articleNumber, 0) - event.amount
    }
}
