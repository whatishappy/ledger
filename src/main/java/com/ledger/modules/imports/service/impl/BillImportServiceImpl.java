package com.ledger.modules.imports.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ledger.common.cache.CacheService;
import com.ledger.common.enums.AccountTypeEnum;
import com.ledger.common.exception.BusinessException;
import com.ledger.common.result.ResultCode;
import com.ledger.modules.account.entity.Account;
import com.ledger.modules.account.event.AccountChangeEvent;
import com.ledger.modules.account.mapper.AccountMapper;
import com.ledger.modules.imports.dto.BillImportConfirmDTO;
import com.ledger.modules.imports.parser.BillParser;
import com.ledger.modules.imports.parser.RawBillRow;
import com.ledger.modules.imports.service.IBillImportService;
import com.ledger.modules.imports.vo.BillImportPreviewVO;
import com.ledger.modules.imports.vo.BillImportResultVO;
import com.ledger.modules.imports.vo.ImportedBillRowVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BillImportServiceImpl implements IBillImportService {

    private final List<BillParser> billParsers;
    private final CacheService cacheService;
    private final AccountMapper accountMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024L;
    private static final int MAX_ROW_LIMIT = 1000;
    private static final int SAMPLE_SIZE = 5;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    private static final Map<String, String> EXPENSE_KEYWORD_CATEGORY = new HashMap<>();
    private static final Map<String, String> INCOME_KEYWORD_CATEGORY = new HashMap<>();

    static {
        EXPENSE_KEYWORD_CATEGORY.put("美团", "餐饮");
        EXPENSE_KEYWORD_CATEGORY.put("饿了么", "餐饮");
        EXPENSE_KEYWORD_CATEGORY.put("餐饮", "餐饮");
        EXPENSE_KEYWORD_CATEGORY.put("火锅", "餐饮");
        EXPENSE_KEYWORD_CATEGORY.put("咖啡", "餐饮");
        EXPENSE_KEYWORD_CATEGORY.put("奶茶", "餐饮");
        EXPENSE_KEYWORD_CATEGORY.put("餐厅", "餐饮");
        EXPENSE_KEYWORD_CATEGORY.put("外卖", "餐饮");

        EXPENSE_KEYWORD_CATEGORY.put("滴滴", "交通");
        EXPENSE_KEYWORD_CATEGORY.put("出租", "交通");
        EXPENSE_KEYWORD_CATEGORY.put("高铁", "交通");
        EXPENSE_KEYWORD_CATEGORY.put("机票", "交通");
        EXPENSE_KEYWORD_CATEGORY.put("地铁", "交通");
        EXPENSE_KEYWORD_CATEGORY.put("公交", "交通");
        EXPENSE_KEYWORD_CATEGORY.put("打车", "交通");
        EXPENSE_KEYWORD_CATEGORY.put("加油", "交通");

        EXPENSE_KEYWORD_CATEGORY.put("京东", "购物");
        EXPENSE_KEYWORD_CATEGORY.put("淘宝", "购物");
        EXPENSE_KEYWORD_CATEGORY.put("拼多多", "购物");
        EXPENSE_KEYWORD_CATEGORY.put("天猫", "购物");
        EXPENSE_KEYWORD_CATEGORY.put("超市", "购物");
        EXPENSE_KEYWORD_CATEGORY.put("商场", "购物");
        EXPENSE_KEYWORD_CATEGORY.put("便利店", "购物");

        INCOME_KEYWORD_CATEGORY.put("工资", "工资");
        INCOME_KEYWORD_CATEGORY.put("薪资", "工资");
        INCOME_KEYWORD_CATEGORY.put("奖金", "工资");
        INCOME_KEYWORD_CATEGORY.put("转账", "其他");
        INCOME_KEYWORD_CATEGORY.put("红包", "其他");
        INCOME_KEYWORD_CATEGORY.put("退款", "其他");
    }

    @Override
    public BillImportPreviewVO preview(Long userId, String source, MultipartFile file) {
        validateFile(file);

        BillParser parser = findParser(source, file.getOriginalFilename());
        if (parser == null) {
            throw new BusinessException(ResultCode.IMPORT_FORMAT_UNSUPPORTED);
        }

        List<RawBillRow> rows;
        try {
            rows = parser.parse(file.getInputStream());
        } catch (Exception e) {
            log.error("账单解析失败: userId={}, source={}, file={}", userId, source, file.getOriginalFilename(), e);
            throw new BusinessException(ResultCode.IMPORT_PARSE_FAILED);
        }

        if (rows == null || rows.isEmpty()) {
            throw new BusinessException(ResultCode.IMPORT_PARSE_FAILED);
        }
        if (rows.size() > MAX_ROW_LIMIT) {
            throw new BusinessException(ResultCode.IMPORT_LIMIT_EXCEEDED);
        }

        long conflicts = countConflicts(userId, rows);

        BigDecimal amountSum = BigDecimal.ZERO;
        for (RawBillRow row : rows) {
            if (row.getType() == 1 || row.getType() == 2) {
                amountSum = amountSum.add(row.getAmount() != null ? row.getAmount() : BigDecimal.ZERO);
            }
        }

        List<ImportedBillRowVO> sampleRows = buildSampleRows(rows);

        String token = UUID.randomUUID().toString().replace("-", "");
        cacheRawRows(userId, token, rows);

        BillImportPreviewVO vo = new BillImportPreviewVO();
        vo.setToken(token);
        vo.setCount(rows.size());
        vo.setAmountSum(amountSum);
        vo.setConflicts(conflicts);
        vo.setSampleRows(sampleRows);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BillImportResultVO confirm(Long userId, BillImportConfirmDTO dto) {
        if (dto == null || !StringUtils.hasText(dto.getToken())) {
            throw new BusinessException(ResultCode.PARAM_VALIDATION_FAILED, "token不能为空");
        }

        List<RawBillRow> rows = getCachedRawRows(userId, dto.getToken());
        if (rows == null || rows.isEmpty()) {
            throw new BusinessException(ResultCode.IMPORT_PARSE_FAILED, "预览已过期，请重新上传");
        }

        boolean skipConflicts = dto.getSkipConflicts() == null || dto.getSkipConflicts();
        Map<String, String> categoryOverrides = dto.getCategoryOverrides() != null ? dto.getCategoryOverrides() : new HashMap<>();

        int imported = 0;
        int skipped = 0;
        BigDecimal amountSumIncome = BigDecimal.ZERO;
        BigDecimal amountSumExpense = BigDecimal.ZERO;

        for (RawBillRow row : rows) {
            if (row.getType() == null || row.getType() == 3) {
                skipped++;
                continue;
            }
            if (row.getAmount() == null || row.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                skipped++;
                continue;
            }
            if (row.getTradeTime() == null) {
                skipped++;
                continue;
            }
            if (isConflict(userId, row)) {
                if (skipConflicts) {
                    skipped++;
                    continue;
                }
            }

            String category = resolveCategory(row, categoryOverrides);

            Account account = new Account();
            account.setUserId(userId);
            Integer accountType = (row.getType() == 1) ? AccountTypeEnum.EXPENSE.getValue() : AccountTypeEnum.INCOME.getValue();
            account.setType(accountType);
            account.setCategory(category);
            account.setAmount(row.getAmount());
            account.setAccountDate(row.getTradeTime().toLocalDate());
            String remark = buildRemark(row);
            account.setRemark(remark);

            accountMapper.insert(account);

            if (AccountTypeEnum.EXPENSE.getValue() == accountType) {
                amountSumExpense = amountSumExpense.add(row.getAmount());
            } else {
                amountSumIncome = amountSumIncome.add(row.getAmount());
            }

            String month = row.getTradeTime().format(MONTH_FORMATTER);
            eventPublisher.publishEvent(new AccountChangeEvent(userId, month));

            imported++;
        }

        cacheService.evictImportPreview(userId, dto.getToken());

        BillImportResultVO vo = new BillImportResultVO();
        vo.setImported(imported);
        vo.setSkipped(skipped);
        vo.setAmountSumIncome(amountSumIncome);
        vo.setAmountSumExpense(amountSumExpense);
        return vo;
    }

    @Override
    public Map<String, Integer> getImportStatus() {
        Map<String, Integer> status = new HashMap<>();
        status.put("alipay", 1);
        status.put("wechat", 1);
        return status;
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.IMPORT_FORMAT_UNSUPPORTED);
        }
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".csv")) {
            throw new BusinessException(ResultCode.IMPORT_FORMAT_UNSUPPORTED);
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(ResultCode.IMPORT_FORMAT_UNSUPPORTED);
        }
    }

    private BillParser findParser(String source, String filename) {
        for (BillParser parser : billParsers) {
            if (parser.supports(source, filename)) {
                return parser;
            }
        }
        return null;
    }

    private long countConflicts(Long userId, List<RawBillRow> rows) {
        long count = 0;
        for (RawBillRow row : rows) {
            if (row.getType() == null || row.getType() == 3) {
                continue;
            }
            if (isConflict(userId, row)) {
                count++;
            }
        }
        return count;
    }

    private boolean isConflict(Long userId, RawBillRow row) {
        LambdaQueryWrapper<Account> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Account::getUserId, userId);
        if (row.getAmount() != null) {
            wrapper.eq(Account::getAmount, row.getAmount());
        } else {
            return false;
        }
        if (row.getTradeTime() != null) {
            LocalDate date = row.getTradeTime().toLocalDate();
            wrapper.eq(Account::getAccountDate, date);
        } else {
            return false;
        }
        String cp = row.getCounterparty() != null ? row.getCounterparty() : "";
        wrapper.apply("(remark LIKE CONCAT('%', {0}, '%') OR remark = {1})", cp, cp);
        wrapper.last("LIMIT 1");
        return accountMapper.selectCount(wrapper) > 0;
    }

    private List<ImportedBillRowVO> buildSampleRows(List<RawBillRow> rows) {
        List<ImportedBillRowVO> samples = new ArrayList<>();
        int end = Math.min(SAMPLE_SIZE, rows.size());
        for (int i = 0; i < end; i++) {
            RawBillRow row = rows.get(i);
            ImportedBillRowVO vo = new ImportedBillRowVO();
            if (row.getTradeTime() != null) {
                vo.setDate(row.getTradeTime().format(DATE_FORMATTER));
            }
            vo.setCounterparty(row.getCounterparty());
            vo.setAmount(row.getAmount());
            if (row.getType() != null) {
                vo.setType(row.getType() == 1 ? "支出" : row.getType() == 2 ? "收入" : "不计入");
            }
            vo.setSource(row.getSource() != null ? row.getSource() : "");
            vo.setPreCategory(defaultCategory(row));
            samples.add(vo);
        }
        return samples;
    }

    private String defaultCategory(RawBillRow row) {
        if (row.getType() == null) {
            return "其他";
        }
        String text = mergeText(row);
        if (row.getType() == 1) {
            for (Map.Entry<String, String> entry : EXPENSE_KEYWORD_CATEGORY.entrySet()) {
                if (text.contains(entry.getKey())) {
                    return entry.getValue();
                }
            }
            return "其他";
        } else if (row.getType() == 2) {
            for (Map.Entry<String, String> entry : INCOME_KEYWORD_CATEGORY.entrySet()) {
                if (text.contains(entry.getKey())) {
                    return entry.getValue();
                }
            }
            return "其他";
        }
        return "其他";
    }

    private String resolveCategory(RawBillRow row, Map<String, String> overrides) {
        String override = null;
        if (row.getTradeNo() != null) {
            override = overrides.get(row.getTradeNo());
        }
        if (StringUtils.hasText(override)) {
            return override;
        }
        return defaultCategory(row);
    }

    private String mergeText(RawBillRow row) {
        StringBuilder sb = new StringBuilder();
        if (row.getCounterparty() != null) {
            sb.append(row.getCounterparty());
        }
        if (row.getGoods() != null) {
            sb.append(" ").append(row.getGoods());
        }
        if (row.getPaymentMethod() != null) {
            sb.append(" ").append(row.getPaymentMethod());
        }
        return sb.toString();
    }

    private String buildRemark(RawBillRow row) {
        StringBuilder sb = new StringBuilder();
        if (row.getCounterparty() != null && !row.getCounterparty().isEmpty()) {
            sb.append(row.getCounterparty());
        }
        if (row.getGoods() != null && !row.getGoods().isEmpty()) {
            if (sb.length() > 0) {
                sb.append("-");
            }
            sb.append(row.getGoods());
        }
        return sb.toString();
    }

    private void cacheRawRows(Long userId, String token, List<RawBillRow> rows) {
        try {
            String json = objectMapper.writeValueAsString(rows);
            cacheService.setImportPreview(userId, token, json);
        } catch (Exception e) {
            log.error("缓存导入预览数据失败: userId={}, token={}", userId, token, e);
            throw new BusinessException(ResultCode.INTERNAL_SERVER_ERROR, "缓存预览数据失败");
        }
    }

    @SuppressWarnings("unchecked")
    private List<RawBillRow> getCachedRawRows(Long userId, String token) {
        try {
            Object raw = cacheService.getImportPreview(userId, token, Object.class);
            if (raw == null) {
                return null;
            }
            String json;
            if (raw instanceof String) {
                json = (String) raw;
            } else {
                json = objectMapper.writeValueAsString(raw);
            }
            return objectMapper.readValue(json.getBytes(StandardCharsets.UTF_8), new TypeReference<List<RawBillRow>>() {});
        } catch (Exception e) {
            log.warn("读取导入预览缓存失败: userId={}, token={}", userId, token, e);
            return null;
        }
    }
}
