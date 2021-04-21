package com.gusaini.inventory.service;

import com.gusaini.dto.PaymentDto;
import com.gusaini.events.payment.PaymentEvent;
import com.gusaini.events.payment.PaymentStatus;
import com.gusaini.inventory.repository.OrderInventoryConsumptionRepository;
import com.gusaini.inventory.repository.OrderInventoryRepository;
import com.gusaini.dto.InventoryDto;
import com.gusaini.events.inventory.InventoryEvent;
import com.gusaini.events.inventory.InventoryStatus;
import com.gusaini.events.order.OrderEvent;
import com.gusaini.inventory.entity.OrderInventoryConsumption;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryService {

    @Autowired
    private OrderInventoryRepository inventoryRepository;

    @Autowired
    private OrderInventoryConsumptionRepository consumptionRepository;

    @Transactional
    @HystrixCommand(fallbackMethod = "getDataFallBack")
    public InventoryEvent newOrderInventory(OrderEvent orderEvent){
        InventoryDto dto = InventoryDto.of(orderEvent.getPurchaseOrder().getOrderId(), orderEvent.getPurchaseOrder().getProductId());
        if(dto.getProductId().equals(4)){
            throw new RuntimeException();
        }
        return inventoryRepository.findById(orderEvent.getPurchaseOrder().getProductId())
                .filter(i -> i.getAvailableInventory() > 0 )
                .map(i -> {
                    i.setAvailableInventory(i.getAvailableInventory() - 1);
                    consumptionRepository.save(OrderInventoryConsumption.of(orderEvent.getPurchaseOrder().getOrderId(), orderEvent.getPurchaseOrder().getProductId(), 1));
                    return new InventoryEvent(dto, InventoryStatus.RESERVED);
                })
                .orElse(new InventoryEvent(dto, InventoryStatus.REJECTED));
    }

    public InventoryEvent getDataFallBack(OrderEvent orderEvent){
        InventoryDto dto = InventoryDto.of(orderEvent.getPurchaseOrder().getOrderId(), orderEvent.getPurchaseOrder().getProductId());
        return new InventoryEvent(dto, InventoryStatus.REJECTED);
    }

    @Transactional
    public void cancelOrderInventory(OrderEvent orderEvent){
        consumptionRepository.findById(orderEvent.getPurchaseOrder().getOrderId())
                .ifPresent(ci -> {
                    inventoryRepository.findById(ci.getProductId())
                            .ifPresent(i ->
                                i.setAvailableInventory(i.getAvailableInventory() + ci.getQuantityConsumed())
                            );
                    consumptionRepository.delete(ci);
                });
    }

}
