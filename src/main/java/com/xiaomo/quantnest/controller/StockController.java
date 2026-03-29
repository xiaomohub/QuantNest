package com.xiaomo.quantnest.controller;

import com.alibaba.fastjson2.JSON;
import com.xiaomo.quantnest.service.StockFilterService;
import kotlin.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/stock")
@CrossOrigin  // 开发阶段允许跨域
public class StockController {

    @Autowired
    private StockFilterService stockFilterService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 获取今日选股结果（优先读Redis缓存）
     */
    @GetMapping("/filter/today")
    public Result<List<StockScore>> getTodayFilter() {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String cacheKey = "stock:filter:result:" + today;

        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            List<StockScore> scores = JSON.parseArray(cached.toString(), StockScore.class);
            return Result.ok(scores);
        }

        // 缓存没有，实时计算
        // ...（实际上不太可能到这里，因为定时任务会写缓存）
        return Result.fail("今日数据尚未生成，请等待收盘后定时任务执行");
    }

    /**
     * 手动触发选股（测试用）
     */
    @PostMapping("/filter/run")
    public Result<String> runFilter(@RequestBody List<String> stockPool) {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        stockFilterService.runFilter(stockPool, today);
        return Result.ok("选股任务已触发，请稍后查询结果");
    }
}