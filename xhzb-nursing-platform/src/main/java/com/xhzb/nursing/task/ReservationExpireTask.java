package com.xhzb.nursing.task;

import com.xhzb.nursing.service.IReservationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 预约过期检查任务
 * 每小时的第1分钟和第31分钟执行：未在预约时间来访的预约（仍为待报道状态且预约时间已过）改为过期
 *
 * @author xujingbo
 * @date 2026-10-18
 */
@Component
public class ReservationExpireTask {

    private static final Logger log = LoggerFactory.getLogger(ReservationExpireTask.class);

    @Autowired
    private IReservationService reservationService;

    /**
     * 定时把过时的待报道预约标记为过期
     */
    @Scheduled(cron = "0 1,31 * * * ?")
    public void expireOverdueReservations() {
        try {
            int expiredCount = reservationService.expireOverdueReservations();
            log.info("预约过期检查执行完成，本次标记过期预约 {} 条", expiredCount);
        } catch (Exception e) {
            log.error("预约过期检查执行失败", e);
        }
    }
}
