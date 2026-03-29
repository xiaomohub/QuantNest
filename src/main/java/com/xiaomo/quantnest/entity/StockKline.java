package com.xiaomo.quantnest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

// StockKline.java
@Data
@TableName("stock_kline")
public class StockKline {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String tsCode;
    private String tradeDate;   // 交易日 20240101
    private BigDecimal open;
    private BigDecimal high;
    private BigDecimal low;
    private BigDecimal close;
    private BigDecimal preClose;
    private BigDecimal pctChg;  // 涨跌幅
    private Long vol;           // 成交量（手）
    private BigDecimal amount;  // 成交额（千元）
    private Date createTime;
}