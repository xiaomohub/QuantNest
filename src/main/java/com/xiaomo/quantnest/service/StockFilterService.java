package com.xiaomo.quantnest.service;

import com.alibaba.fastjson2.JSON;
import com.xiaomo.quantnest.entity.StockKline;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Slf4j
public class StockFilterService {

    @Autowired
    private StockKlineMapper klineMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 执行选股，返回Top20候选股票
     */
    public List<StockScore> runFilter(List<String> stockPool, String endDate) {
        log.info("开始选股, 股票池大小: {}, 截止日期: {}", stockPool.size(), endDate);

        List<StockScore> scores = stockPool.parallelStream()
                .map(tsCode -> calcScore(tsCode, endDate))
                .filter(Objects::nonNull)
                // 过滤条件1: 波动率不能太高
                .filter(s -> s.getVolatility().compareTo(new BigDecimal("0.05")) < 0)
                // 过滤条件2: 近期涨幅不能过大（避免追高）
                .filter(s -> s.getRecentGain().compareTo(new BigDecimal("0.3")) < 0)
                // 综合评分排序，取Top20
                .sorted(Comparator.comparing(StockScore::getScore).reversed())
                .limit(20)
                .collect(Collectors.toList());

        log.info("选股完成，候选股票: {}", scores.size());

        // 缓存到Redis，前端直接读
        redisTemplate.opsForValue().set(
                "stock:filter:result:" + endDate,
                JSON.toJSONString(scores),
                Duration.ofHours(24)
        );

        return scores;
    }

    /**
     * 计算单只股票的综合评分
     */
    private StockScore calcScore(String tsCode, String endDate) {
        // 取最近60个交易日的K线
        List<StockKline> klines = klineMapper.getRecentKline(tsCode, endDate, 60);
        if (klines == null || klines.size() < 20) {
            return null;  // 数据不足，跳过
        }

        // 1. 计算波动率（日收益率标准差）
        BigDecimal volatility = calcVolatility(klines);

        // 2. 计算近20日涨幅
        BigDecimal recentGain = calcRecentGain(klines, 20);

        // 3. 计算均线趋势（MA20 vs MA60）
        BigDecimal ma20 = calcMA(klines, 20);
        BigDecimal ma60 = calcMA(klines, 60);
        boolean trendUp = ma20.compareTo(ma60) > 0;

        // 4. 计算成交量趋势（近5日均量 vs 近20日均量）
        BigDecimal vol5 = calcAvgVol(klines, 5);
        BigDecimal vol20 = calcAvgVol(klines, 20);
        boolean volExpand = vol5.compareTo(vol20) > 0;

        // 5. 综合评分（可根据你的策略调整权重）
        BigDecimal score = BigDecimal.ZERO;
        if (trendUp) score = score.add(new BigDecimal("30"));
        if (volExpand) score = score.add(new BigDecimal("20"));
        // 波动率越低，加分越多（最多30分）
        BigDecimal volScore = new BigDecimal("30").subtract(
                volatility.multiply(new BigDecimal("600"))
        ).max(BigDecimal.ZERO);
        score = score.add(volScore);
        // 涨幅适中加分
        if (recentGain.compareTo(new BigDecimal("0.05")) > 0
                && recentGain.compareTo(new BigDecimal("0.2")) < 0) {
            score = score.add(new BigDecimal("20"));
        }

        StockScore result = new StockScore();
        result.setTsCode(tsCode);
        result.setScore(score);
        result.setVolatility(volatility);
        result.setRecentGain(recentGain);
        result.setMa20(ma20);
        result.setMa60(ma60);
        result.setTrendUp(trendUp);
        return result;
    }

    private BigDecimal calcVolatility(List<StockKline> klines) {
        // 日收益率列表
        List<Double> returns = new ArrayList<>();
        for (int i = 1; i < klines.size(); i++) {
            BigDecimal prev = klines.get(i).getClose();
            BigDecimal curr = klines.get(i - 1).getClose();
            if (prev.compareTo(BigDecimal.ZERO) > 0) {
                double r = curr.subtract(prev).divide(prev, 6, RoundingMode.HALF_UP).doubleValue();
                returns.add(r);
            }
        }
        // 标准差
        double mean = returns.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double variance = returns.stream()
                .mapToDouble(r -> Math.pow(r - mean, 2)).average().orElse(0);
        return BigDecimal.valueOf(Math.sqrt(variance)).setScale(6, RoundingMode.HALF_UP);
    }

    private BigDecimal calcMA(List<StockKline> klines, int period) {
        return klines.stream()
                .limit(period)
                .map(StockKline::getClose)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(new BigDecimal(period), 4, RoundingMode.HALF_UP);
    }

    private BigDecimal calcRecentGain(List<StockKline> klines, int period) {
        if (klines.size() < period) return BigDecimal.ZERO;
        BigDecimal now = klines.get(0).getClose();
        BigDecimal before = klines.get(period - 1).getClose();
        if (before.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return now.subtract(before).divide(before, 4, RoundingMode.HALF_UP);
    }

    private BigDecimal calcAvgVol(List<StockKline> klines, int period) {
        return klines.stream()
                .limit(period)
                .map(k -> new BigDecimal(k.getVol()))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(new BigDecimal(period), 0, RoundingMode.HALF_UP);
    }
}