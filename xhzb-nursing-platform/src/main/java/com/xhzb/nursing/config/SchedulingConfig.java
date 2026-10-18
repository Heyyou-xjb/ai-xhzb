package com.xhzb.nursing.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 定时任务配置
 * 开启Spring的定时任务支持，供预约过期检查等任务使用
 *
 * @author xujingbo
 * @date 2026-10-18
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
