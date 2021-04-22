package com.gusaini.payment.controller;

import com.gusaini.dto.AddBalanceDto;
import com.gusaini.payment.entity.UserBalance;
import com.gusaini.payment.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.query.Param;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("payment")
public class PaymentController {
    @Autowired
    private PaymentService paymentService;

    @PostMapping("/add")
    public UserBalance addBalance(@RequestBody AddBalanceDto addBalanceDto){
        return this.paymentService.addBalance(addBalanceDto);
    }

    @PostMapping("/remove")
    public UserBalance removeBalance(@RequestBody AddBalanceDto addBalanceDto){
        return this.paymentService.removeBalance(addBalanceDto);
    }

    @GetMapping("/balance")
    public UserBalance getBalance(@Param("userId") Integer userId){
        return this.paymentService.getBalance(userId);
    }

    @GetMapping("/all")
    public List<UserBalance> getAll(){
        return this.paymentService.getAll();
    }
}
