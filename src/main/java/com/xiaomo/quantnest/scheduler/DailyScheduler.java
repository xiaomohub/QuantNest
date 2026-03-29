package com.xiaomo.quantnest.scheduler;

import com.xiaomo.quantnest.service.DataSyncService;
import com.xiaomo.quantnest.service.StockFilterService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

@Component
@Slf4j
public class DailyScheduler {

    @Autowired
    private DataSyncService dataSyncService;

    @Autowired
    private StockFilterService stockFilterService;

    // 每天收盘后 16:30 执行
    @Scheduled(cron = "0 30 16 * * MON-FRI")
    public void dailyJob() {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        log.info("========== 每日任务开始: {} ==========", today);

        // TODO: 从数据库读取你的股票池
        List<String> stockPool = Arrays.asList(
                "000001.SZ", "600519.SH", "000858.SZ" // 先写死，后续做成可配置
        );

        // 1. 同步今日K线
        stockPool.forEach(code ->
                dataSyncService.syncKlineData(code, today, today)
        );

        // 2. 执行选股
        stockFilterService.runFilter(stockPool, today);

        log.info("========== 每日任务完成 ==========");
    }
}