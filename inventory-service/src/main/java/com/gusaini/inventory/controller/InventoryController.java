package com.gusaini.inventory.controller;

import com.gusaini.inventory.entity.OrderInventory;
import com.gusaini.inventory.service.InventoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("inventory")
public class InventoryController {

    @Autowired
    private InventoryService inventoryService;

    @Value("${config.server.inventory}")
    private String inventory;

    @GetMapping("/all")
    public List<OrderInventory> getAll(){
        return this.inventoryService.getAll();
        System.out.println(inventory);
    }
}
