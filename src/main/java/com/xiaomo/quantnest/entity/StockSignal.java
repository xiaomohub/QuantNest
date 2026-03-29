package com.xiaomo.quantnest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

// StockSignal.java  ← 策略输出的买卖信号
@Data
@TableName("stock_signal")
public class StockSignal {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String tsCode;
    private String signalDate;
    private String signalType;  // BUY / SELL
    private String strategyName;
    private BigDecimal score;   // 综合评分
    private String reason;      // 信号原因
    private Date createTime;
}