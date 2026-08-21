package com.ledger.modules.imports.parser;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class WechatBillParser implements BillParser {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public boolean supports(String source, String filename) {
        return "wechat".equalsIgnoreCase(source)
                && filename != null
                && filename.toLowerCase().endsWith(".csv");
    }

    @Override
    public List<RawBillRow> parse(InputStream is) throws IOException {
        List<RawBillRow> result = new ArrayList<>();
        Charset gbk = Charset.forName("GBK");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, gbk))) {
            List<String> headerColumns = null;
            Map<String, Integer> headerIndex = new HashMap<>();
            String line;
            int lineNum = 0;

            while ((line = reader.readLine()) != null) {
                lineNum++;
                if (!StringUtils.hasText(line)) {
                    continue;
                }
                String cleanLine = line.trim();
                if (cleanLine.startsWith("\uFEFF")) {
                    cleanLine = cleanLine.substring(1);
                }
                String[] cells = parseCsvLine(cleanLine);
                if (cells.length == 0) {
                    continue;
                }

                if (headerColumns == null) {
                    if (isHeaderRow(cells)) {
                        headerColumns = new ArrayList<>();
                        for (int i = 0; i < cells.length; i++) {
                            String col = cells[i].trim();
                            headerColumns.add(col);
                            headerIndex.put(col, i);
                        }
                    }
                    continue;
                }

                try {
                    RawBillRow row = parseDataRow(cells, headerIndex);
                    if (row != null) {
                        row.setSource("wechat");
                        result.add(row);
                    }
                } catch (Exception e) {
                    log.warn("解析微信账单第{}行失败: {}", lineNum, e.getMessage());
                }
            }
        }
        return result;
    }

    private boolean isHeaderRow(String[] cells) {
        boolean hasTradeTime = false;
        boolean hasTradeType = false;
        for (String cell : cells) {
            String c = cell.trim();
            if (c.contains("交易时间")) {
                hasTradeTime = true;
            }
            if (c.contains("交易类型")) {
                hasTradeType = true;
            }
        }
        return hasTradeTime && hasTradeType;
    }

    private RawBillRow parseDataRow(String[] cells, Map<String, Integer> headerIndex) {
        String status = safeGetCell(cells, headerIndex, "当前状态", "交易状态");
        if (status != null) {
            String s = status.trim();
            if (!"支付成功".equals(s) && !"已收钱".equals(s) && !"已转账".equals(s)) {
                return null;
            }
        }

        String tradeTimeStr = safeGetCell(cells, headerIndex, "交易时间");
        String tradeTypeStr = safeGetCell(cells, headerIndex, "交易类型", "收/支");
        String counterparty = safeGetCell(cells, headerIndex, "交易对方", "对方账号");
        String goods = safeGetCell(cells, headerIndex, "商品", "商品说明");
        String amountStr = safeGetCell(cells, headerIndex, "金额(元)", "金额（元）", "金额");
        String paymentMethod = safeGetCell(cells, headerIndex, "支付方式");
        String tradeNo = safeGetCell(cells, headerIndex, "交易单号", "交易订单号");
        if (!StringUtils.hasText(tradeNo)) {
            tradeNo = safeGetCell(cells, headerIndex, "商户单号");
        }

        if (!StringUtils.hasText(tradeTimeStr)) {
            return null;
        }

        RawBillRow row = new RawBillRow();
        row.setTradeNo(tradeNo != null ? tradeNo.trim() : null);

        try {
            row.setTradeTime(LocalDateTime.parse(tradeTimeStr.trim(), DATE_TIME_FORMATTER));
        } catch (Exception e) {
            return null;
        }

        Integer type = mapType(tradeTypeStr);
        if (type == null) {
            return null;
        }
        row.setType(type);

        row.setCounterparty(counterparty != null ? counterparty.trim() : "");
        row.setGoods(goods != null ? goods.trim() : "");

        if (StringUtils.hasText(amountStr)) {
            try {
                BigDecimal amount = new BigDecimal(amountStr.trim().replace(",", "").replace("¥", ""));
                row.setAmount(amount.abs());
            } catch (Exception e) {
                return null;
            }
        } else {
            return null;
        }

        row.setPaymentMethod(paymentMethod != null ? paymentMethod.trim() : "");
        return row;
    }

    private Integer mapType(String typeStr) {
        if (typeStr == null) {
            return null;
        }
        String t = typeStr.trim();
        if ("支出".equals(t) || "/".equals(t)) {
            return 1;
        } else if ("收入".equals(t)) {
            return 2;
        }
        return null;
    }

    private String safeGetCell(String[] cells, Map<String, Integer> headerIndex, String... names) {
        for (String name : names) {
            Integer idx = headerIndex.get(name);
            if (idx != null && idx < cells.length) {
                return cells[idx];
            }
        }
        for (Map.Entry<String, Integer> entry : headerIndex.entrySet()) {
            for (String name : names) {
                if (entry.getKey().contains(name) && entry.getValue() < cells.length) {
                    return cells[entry.getValue()];
                }
            }
        }
        return null;
    }

    private String[] parseCsvLine(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                result.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        result.add(current.toString());
        return result.toArray(new String[0]);
    }
}
