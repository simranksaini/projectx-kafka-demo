package com.gusaini.inventory.service;

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

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@Service
public class InventoryService {

    @Autowired
    private OrderInventoryRepository inventoryRepository;

    @Autowired
    private OrderInventoryConsumptionRepository consumptionRepository;

    private Map<Integer, Integer> productInventoryMap;

    @PostConstruct
    private void init(){
        this.productInventoryMap = new HashMap<>();
        this.productInventoryMap.put(1, 5);
        this.productInventoryMap.put(2, 5);
        this.productInventoryMap.put(3, 5);
        this.productInventoryMap.put(4, 5);
        this.productInventoryMap.put(5, 5);
    }

    @HystrixCommand(fallbackMethod = "getDataFallBack")
    @Transactional
    public InventoryEvent newOrderInventory(OrderEvent orderEvent){
        if(orderEvent.getPurchaseOrder().getProductId().equals(4)){
            throw new RuntimeException();
        }
        InventoryDto dto = InventoryDto.of(orderEvent.getPurchaseOrder().getOrderId(), orderEvent.getPurchaseOrder().getProductId());
        int quantity = this.productInventoryMap.getOrDefault(orderEvent.getPurchaseOrder().getProductId(), 0);
        if(quantity > 0){
            this.productInventoryMap.put(orderEvent.getPurchaseOrder().getProductId(), quantity - 1);
            consumptionRepository.save(OrderInventoryConsumption.of(orderEvent.getPurchaseOrder().getOrderId(), orderEvent.getPurchaseOrder().getProductId(), 1));
            return new InventoryEvent(dto, InventoryStatus.RESERVED);
        }
        return new InventoryEvent(dto, InventoryStatus.REJECTED);
//        return inventoryRepository.findById(orderEvent.getPurchaseOrder().getProductId())
//                .filter(i -> i.getAvailableInventory() > 0 )
//                .map(i -> {
//                    i.setAvailableInventory(i.getAvailableInventory() - 1);
//                    consumptionRepository.save(OrderInventoryConsumption.of(orderEvent.getPurchaseOrder().getOrderId(), orderEvent.getPurchaseOrder().getProductId(), 1));
//                    return new InventoryEvent(dto, InventoryStatus.RESERVED);
//                })
//                .orElse(new InventoryEvent(dto, InventoryStatus.REJECTED));
    }

    public InventoryEvent getDataFallBack(OrderEvent orderEvent){
        InventoryDto dto = InventoryDto.of(orderEvent.getPurchaseOrder().getOrderId(), orderEvent.getPurchaseOrder().getProductId());
        return new InventoryEvent(dto, InventoryStatus.REJECTED);
    }

    @Transactional
    public void cancelOrderInventory(OrderEvent orderEvent){
        consumptionRepository.findById(orderEvent.getPurchaseOrder().getOrderId())
                .ifPresent(ci -> {
//                    inventoryRepository.findById(ci.getProductId())
//                            .ifPresent(i ->
//                                i.setAvailableInventory(i.getAvailableInventory() + ci.getQuantityConsumed())
//                            );
                    consumptionRepository.delete(ci);
                    this.productInventoryMap
                            .computeIfPresent(orderEvent.getPurchaseOrder().getProductId(), (k, v) -> v + 1);

                });
    }

}
