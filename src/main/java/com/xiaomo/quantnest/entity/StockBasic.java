package com.xiaomo.quantnest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

// StockBasic.java
@Data
@TableName("stock_basic")
public class StockBasic {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String tsCode;      // 股票代码 000001.SZ
    private String name;        // 名称
    private String area;        // 地区
    private String industry;    // 行业
    private String market;      // 市场
    private String listStatus;  // 上市状态 L上市 D退市
    private Date listDate;
    private Date createTime;
}