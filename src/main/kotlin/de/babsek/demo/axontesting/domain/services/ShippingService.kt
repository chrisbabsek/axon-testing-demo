package de.babsek.demo.axontesting.domain.services

import de.babsek.demo.axontesting.domain.value.DeliveryAddress
import org.springframework.stereotype.Service

@Service
class ShippingService {

    fun shipProducts(address: DeliveryAddress, products: Map<String, Int>) {
    }
}
