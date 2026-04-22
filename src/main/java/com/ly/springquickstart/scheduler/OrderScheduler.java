package com.ly.springquickstart.scheduler;

import com.ly.springquickstart.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 订单定时任务：每分钟扫描并取消超时未付款订单
 */
@Component
public class OrderScheduler {

    @Autowired
    private OrderService orderService;

    @Scheduled(fixedDelay = 60_000)
    public void cancelExpiredOrders() {
        orderService.autoCancelExpired();
    }
}
