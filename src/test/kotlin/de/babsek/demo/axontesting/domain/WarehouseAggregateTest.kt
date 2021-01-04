package de.babsek.demo.axontesting.domain

import de.babsek.demo.axontesting.domain.commands.*
import de.babsek.demo.axontesting.domain.events.ProductArrivedInWarehouseEvent
import de.babsek.demo.axontesting.domain.events.ProductTakenEvent
import de.babsek.demo.axontesting.domain.events.WarehouseClosedEvent
import de.babsek.demo.axontesting.domain.events.WarehouseOpenedEvent
import de.babsek.demo.axontesting.domain.exceptions.OutOfStockException
import de.babsek.demo.axontesting.domain.services.ShippingService
import de.babsek.demo.axontesting.domain.value.DeliveryAddress
import io.mockk.*
import org.axonframework.eventsourcing.AggregateDeletedException
import org.axonframework.test.aggregate.AggregateTestFixture
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

internal class WarehouseAggregateTest {

    val fixture: AggregateTestFixture<WarehouseAggregate> by lazy {
        AggregateTestFixture(WarehouseAggregate::class.java).apply {
            // Initialize aggregate test
        }
    }

    val warehouse01Id = "warehouse01"

    @Nested
    @DisplayName("Open and Close Warehouse")
    inner class OpenAndCloseWarehouse {

        @Test
        fun `can open a new warehouse`() {
            fixture
                .`when`(
                    OpenWarehouseCommand(warehouseId = warehouse01Id)
                )
                .expectSuccessfulHandlerExecution()
                .expectEvents(
                    WarehouseOpenedEvent(warehouseId = warehouse01Id)
                )
        }

        @Test
        fun `create no new warehouse when aggregate already exists`() {
            fixture
                .given(
                    WarehouseOpenedEvent(warehouseId = warehouse01Id)
                )
                .`when`(
                    OpenWarehouseCommand(warehouseId = warehouse01Id)
                )
                .expectSuccessfulHandlerExecution()
                .expectNoEvents()
        }

        @Test
        fun `close warehouse`() {
            fixture
                .given(
                    WarehouseOpenedEvent(warehouseId = warehouse01Id)
                )
                .`when`(
                    CloseWarehouseCommand(warehouseId = warehouse01Id)
                )
                .expectSuccessfulHandlerExecution()
                .expectEvents(
                    WarehouseClosedEvent(warehouseId = warehouse01Id)
                )
                .expectMarkedDeleted()
        }

        @Test
        fun `deny to close warehouse without empty stock`() {
            fixture
                .given(
                    WarehouseOpenedEvent(warehouseId = warehouse01Id),
                    ProductArrivedInWarehouseEvent(warehouseId = warehouse01Id, articleNumber = "001", amount = 7)
                )
                .`when`(
                    CloseWarehouseCommand(warehouseId = warehouse01Id)
                )
                .expectException(IllegalArgumentException::class.java)
        }
    }

    @Nested
    @DisplayName("Products Arriving")
    inner class ProductsArriving {

        val warehouseOpenedEvent = WarehouseOpenedEvent(warehouseId = warehouse01Id)

        @Test
        fun `new products arrive in warehouse`() {
            fixture
                .given(warehouseOpenedEvent)
                .`when`(
                    StoreProductsInWarehouseCommand(
                        warehouseId = warehouse01Id,
                        products = mapOf(
                            "001" to 7,
                            "002" to 13
                        )
                    )
                )
                .expectSuccessfulHandlerExecution()
                .expectEvents(
                    ProductArrivedInWarehouseEvent(
                        warehouseId = warehouse01Id,
                        articleNumber = "001",
                        amount = 7,
                    ),
                    ProductArrivedInWarehouseEvent(
                        warehouseId = warehouse01Id,
                        articleNumber = "002",
                        amount = 13,
                    )
                )
        }

        @Test
        fun `deny new products on closed warehouse`() {
            fixture
                .given(warehouseOpenedEvent)
                .andGiven(WarehouseClosedEvent(warehouseId = warehouse01Id))
                .`when`(
                    StoreProductsInWarehouseCommand(
                        warehouseId = warehouse01Id,
                        products = mapOf(
                            "001" to 7,
                            "002" to 13
                        )
                    )
                )
                .expectException(AggregateDeletedException::class.java)
        }
    }

    @Nested
    @DisplayName("Take Products")
    inner class TakeProducts {

        @Test
        fun `can take available products from warehouse`() {
            fixture
                .given(
                    WarehouseOpenedEvent(warehouseId = warehouse01Id)
                )
                .andGiven(
                    ProductArrivedInWarehouseEvent(
                        warehouseId = warehouse01Id,
                        articleNumber = "001",
                        amount = 3
                    )
                )
                .`when`(
                    TakeProductsCommand(
                        warehouseId = warehouse01Id,
                        products = mapOf(
                            "001" to 3
                        )
                    )
                )
                .expectSuccessfulHandlerExecution()
                .expectEvents(
                    ProductTakenEvent(
                        warehouseId = warehouse01Id,
                        articleNumber = "001",
                        amount = 3
                    )
                )
        }

        @Test
        fun `fail on taking product that is not in warehouse`() {
            fixture
                .given(
                    WarehouseOpenedEvent(warehouseId = warehouse01Id)
                )
                .andGiven(
                    ProductArrivedInWarehouseEvent(
                        warehouseId = warehouse01Id,
                        articleNumber = "001",
                        amount = 3
                    )
                )
                .`when`(
                    TakeProductsCommand(
                        warehouseId = warehouse01Id,
                        products = mapOf(
                            "002" to 3
                        )
                    )
                )
                .expectException(OutOfStockException::class.java)
        }

        @Test
        fun `fail on taking higher amount of product than is available in warehouse`() {
            fixture
                .given(
                    WarehouseOpenedEvent(warehouseId = warehouse01Id)
                )
                .andGiven(
                    ProductArrivedInWarehouseEvent(
                        warehouseId = warehouse01Id,
                        articleNumber = "001",
                        amount = 3
                    )
                )
                .`when`(
                    TakeProductsCommand(
                        warehouseId = warehouse01Id,
                        products = mapOf(
                            "001" to 4
                        )
                    )
                )
                .expectException(OutOfStockException::class.java)
        }
    }

    @Nested
    @DisplayName("Ship Products")
    inner class ShipProducts {
        lateinit var shippingService: ShippingService

        @BeforeEach
        fun initShipServiceMock() {
            shippingService = mockk()
            fixture
                .registerInjectableResource(shippingService)
        }

        @Test
        fun `can ship a product`() {
            every { shippingService.shipProducts(any(), any()) } just runs

            fixture
                .given(
                    WarehouseOpenedEvent(warehouseId = warehouse01Id)
                )
                .andGiven(
                    ProductArrivedInWarehouseEvent(
                        warehouseId = warehouse01Id,
                        articleNumber = "001",
                        amount = 3
                    )
                )
                .`when`(
                    ShipProductsCommand(
                        warehouseId = warehouse01Id,
                        products = mapOf(
                            "001" to 3
                        ),
                        deliveryAddress = DeliveryAddress(
                            streetAndHouseNumber = "street 1",
                            postCodeAndTown = "12345 Town"
                        )
                    )
                )
                .expectSuccessfulHandlerExecution()

            verifySequence {
                shippingService
                    .shipProducts(
                        address = DeliveryAddress(
                            streetAndHouseNumber = "street 1",
                            postCodeAndTown = "12345 Town"
                        ),
                        products = mapOf(
                            "001" to 3
                        )
                    )
            }
        }
    }
}
