package com.xiaomo.quantnest.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class DataSyncService {

    @Value("${tushare.token}")
    private String token;

    @Value("${tushare.api-url}")
    private String apiUrl;

    @Autowired
    private StockKlineMapper klineMapper;

    private final OkHttpClient httpClient = new OkHttpClient();

    /**
     * 拉取某只股票的日K线数据
     * @param tsCode  股票代码 如 000001.SZ
     * @param startDate  开始日期 如 20230101
     * @param endDate    结束日期 如 20241231
     */
    public void syncKlineData(String tsCode, String startDate, String endDate) {
        log.info("开始同步K线: {} {} ~ {}", tsCode, startDate, endDate);

        // 构造Tushare请求体
        JSONObject params = new JSONObject();
        params.put("ts_code", tsCode);
        params.put("start_date", startDate);
        params.put("end_date", endDate);
        params.put("adj", "qfq");  // 前复权

        JSONObject body = new JSONObject();
        body.put("api_name", "daily");
        body.put("token", token);
        body.put("params", params);
        body.put("fields", "ts_code,trade_date,open,high,low,close,pre_close,pct_chg,vol,amount");

        String result = post(body.toJSONString());
        if (result == null) return;

        // 解析结果
        JSONObject json = JSON.parseObject(result);
        JSONObject data = json.getJSONObject("data");
        if (data == null) return;

        JSONArray items = data.getJSONArray("items");
        List<String> fields = data.getJSONArray("fields").toJavaList(String.class);

        List<StockKline> klines = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            JSONArray row = items.getJSONArray(i);
            StockKline kline = new StockKline();
            kline.setTsCode(row.getString(fields.indexOf("ts_code")));
            kline.setTradeDate(row.getString(fields.indexOf("trade_date")));
            kline.setOpen(row.getBigDecimal(fields.indexOf("open")));
            kline.setHigh(row.getBigDecimal(fields.indexOf("high")));
            kline.setLow(row.getBigDecimal(fields.indexOf("low")));
            kline.setClose(row.getBigDecimal(fields.indexOf("close")));
            kline.setPreClose(row.getBigDecimal(fields.indexOf("pre_close")));
            kline.setPctChg(row.getBigDecimal(fields.indexOf("pct_chg")));
            kline.setVol(row.getLong(fields.indexOf("vol")));
            kline.setAmount(row.getBigDecimal(fields.indexOf("amount")));
            kline.setCreateTime(new Date());
            klines.add(kline);
        }

        // 批量插入（MyBatis-Plus）
        if (!klines.isEmpty()) {
            klineMapper.batchInsertOrUpdate(klines);
            log.info("K线同步完成: {} 共{}条", tsCode, klines.size());
        }
    }

    private String post(String bodyStr) {
        RequestBody requestBody = RequestBody.create(
                bodyStr, MediaType.parse("application/json")
        );
        Request request = new Request.Builder()
                .url(apiUrl)
                .post(requestBody)
                .build();
        try (Response response = httpClient.newCall(request).execute()) {
            return response.body() != null ? response.body().string() : null;
        } catch (Exception e) {
            log.error("Tushare请求失败", e);
            return null;
        }
    }
}