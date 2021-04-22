package com.gusaini.payment.service;

import com.gusaini.payment.entity.UserTransaction;
import com.gusaini.payment.repository.UserBalanceRepository;
import com.gusaini.payment.repository.UserTransactionRepository;
import com.gusaini.dto.PaymentDto;
import com.gusaini.events.order.OrderEvent;
import com.gusaini.events.payment.PaymentEvent;
import com.gusaini.events.payment.PaymentStatus;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@Service
public class PaymentService {

    @Autowired
    private UserBalanceRepository balanceRepository;

    @Autowired
    private UserTransactionRepository transactionRepository;

    private Map<Integer, Double> userBalanceMap;

    @PostConstruct
    private void init(){
        this.userBalanceMap = new HashMap<>();
        this.userBalanceMap.put(1, 1000d);
        this.userBalanceMap.put(2, 1000d);
        this.userBalanceMap.put(3, 1000d);
        this.userBalanceMap.put(4, 1000d);
        this.userBalanceMap.put(5, 1000d);
    }

    @HystrixCommand(fallbackMethod = "getDataFallBack")
    @Transactional
    public PaymentEvent newOrderEvent(OrderEvent orderEvent) throws RuntimeException{
        if(orderEvent.getPurchaseOrder().getUserId()==4)
            throw new RuntimeException();
        var purchaseOrder = orderEvent.getPurchaseOrder();
        var dto = PaymentDto.of(purchaseOrder.getOrderId(), purchaseOrder.getUserId(), purchaseOrder.getPrice());
//        return this.balanceRepository.findById(purchaseOrder.getUserId())
//                .filter(ub -> ub.getBalance() >= purchaseOrder.getPrice())
//                .map(ub -> {
//                    ub.setBalance(ub.getBalance() - purchaseOrder.getPrice());
//                    System.out.println(ub.getBalance());
//                    System.out.println(purchaseOrder.getOrderId());
//                    System.out.println(purchaseOrder.getUserId());
//                    System.out.println(purchaseOrder.getPrice());
//                    System.out.println(purchaseOrder.getProductId());
//                    this.transactionRepository.save(UserTransaction.of(purchaseOrder.getOrderId(), purchaseOrder.getUserId(), purchaseOrder.getPrice()));
//                    return new PaymentEvent(dto, PaymentStatus.RESERVED);
//                })
//                .orElse(new PaymentEvent(dto, PaymentStatus.REJECTED));
        double balance = this.userBalanceMap.getOrDefault(purchaseOrder.getUserId(), 0d);
        if(balance >= purchaseOrder.getPrice()){
            this.userBalanceMap.put(purchaseOrder.getUserId(), balance - purchaseOrder.getPrice());
            this.transactionRepository.save(UserTransaction.of(purchaseOrder.getOrderId(), purchaseOrder.getUserId(), purchaseOrder.getPrice()));
            System.out.println(this.userBalanceMap.get(purchaseOrder.getUserId()));
                    System.out.println(purchaseOrder.getOrderId());
                    System.out.println(purchaseOrder.getUserId());
                    System.out.println(purchaseOrder.getPrice());
                    System.out.println(purchaseOrder.getProductId());
            return new PaymentEvent(dto, PaymentStatus.RESERVED);
        }
        return new PaymentEvent(dto, PaymentStatus.REJECTED);
    }

    public PaymentEvent getDataFallBack(OrderEvent orderEvent){
        var purchaseOrder = orderEvent.getPurchaseOrder();
        var dto = PaymentDto.of(purchaseOrder.getOrderId(), purchaseOrder.getUserId(), purchaseOrder.getPrice());
        return new PaymentEvent(dto, PaymentStatus.REJECTED);
    }

    @Transactional
    public void cancelOrderEvent(OrderEvent orderEvent){
        this.transactionRepository.findById(orderEvent.getPurchaseOrder().getOrderId())
                .ifPresent(ut -> {
                    this.transactionRepository.delete(ut);
                    this.userBalanceMap.computeIfPresent(ut.getUserId(), (k, v) -> v + ut.getAmount());
//                    this.balanceRepository.findById(ut.getUserId())
//                            .ifPresent(ub -> ub.setBalance(ub.getBalance() + ut.getAmount()));
                });
    }
}
