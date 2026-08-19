package com.ledger.modules.account.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ledger.common.cache.CacheService;
import com.ledger.common.enums.AccountCategoryEnum;
import com.ledger.common.enums.AccountTypeEnum;
import com.ledger.common.exception.BusinessException;
import com.ledger.common.result.ResultCode;
import com.ledger.common.utils.MD5Utils;
import com.ledger.modules.monitor.service.BusinessMetricsService;
import com.ledger.modules.account.dto.AccountAddRequest;
import com.ledger.modules.account.dto.AccountPageRequest;
import com.ledger.modules.account.dto.AccountUpdateRequest;
import com.ledger.modules.account.dto.AccountVO;
import com.ledger.modules.account.entity.Account;
import com.ledger.modules.account.event.AccountChangeEvent;
import com.ledger.modules.account.mapper.AccountMapper;
import com.ledger.modules.account.service.IAccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

/**
 * 账目服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements IAccountService {

    private final AccountMapper accountMapper;
    private final CacheService cacheService;
    private final ApplicationEventPublisher eventPublisher;
    private final BusinessMetricsService metricsService;

    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addAccount(Long userId, AccountAddRequest request) {
        // 步骤2：校验金额、分类、业务日期合法性
        validateAccountRequest(request.getType(), request.getCategory(), request.getAmount(), request.getAccountDate());

        // 步骤3：基于用户ID+分类+金额+业务日期生成幂等Key
        String idempotentKey = buildIdempotentKey(userId, request.getCategory(), request.getAmount(), request.getAccountDate());

        // 步骤4：查询Redis中是否已存在该幂等Key
        Long existingId = cacheService.getIdempotentAccount(idempotentKey);
        if (existingId != null) {
            // 步骤5：若存在，直接返回已有的账目ID
            log.info("幂等命中: userId={}, existingId={}", userId, existingId);
            return existingId;
        }

        // 步骤6：构建账目实体
        Account account = new Account();
        account.setUserId(userId);
        account.setType(request.getType());
        account.setCategory(request.getCategory());
        account.setAmount(request.getAmount());
        account.setAccountDate(request.getAccountDate());
        account.setRemark(request.getRemark());
        account.setExtraJson(request.getExtraJson());

        // 步骤7：保存账目记录
        accountMapper.insert(account);

        // 步骤8：以幂等Key为键，账目ID为值存入Redis（5分钟过期）
        cacheService.setIdempotentAccount(idempotentKey, account.getId());

        // 步骤9：发布账目变更事件（用于事务提交后清除缓存）
        String month = request.getAccountDate().format(MONTH_FORMATTER);
        eventPublisher.publishEvent(new AccountChangeEvent(userId, month));

        // 步骤10：返回新生成的账目ID
        log.info("新增记账成功: userId={}, accountId={}", userId, account.getId());
        metricsService.recordAccountSuccess();
        return account.getId();
    }

    @Override
    public IPage<AccountVO> pageQuery(Long userId, AccountPageRequest request) {
        // 步骤2~3：构建分页对象和查询条件
        Page<Account> page = new Page<>(request.getPageNum(), request.getPageSize());
        LambdaQueryWrapper<Account> wrapper = new LambdaQueryWrapper<Account>()
                // 强制拼接 user_id（数据隔离）
                .eq(Account::getUserId, userId);

        // 步骤4：按需添加type等值条件
        if (request.getType() != null) {
            wrapper.eq(Account::getType, request.getType());
        }
        // 步骤5：按需添加category等值条件
        if (StringUtils.hasText(request.getCategory())) {
            wrapper.eq(Account::getCategory, request.getCategory());
        }
        // 步骤5：按需添加日期范围条件
        if (request.getStartDate() != null) {
            wrapper.ge(Account::getAccountDate, request.getStartDate());
        }
        if (request.getEndDate() != null) {
            wrapper.le(Account::getAccountDate, request.getEndDate());
        }
        // 步骤6：按需添加keyword模糊匹配条件
        if (StringUtils.hasText(request.getKeyword())) {
            wrapper.like(Account::getRemark, request.getKeyword());
        }
        // 步骤7：排序规则（account_date倒序、create_time倒序）
        wrapper.orderByDesc(Account::getAccountDate)
               .orderByDesc(Account::getCreateTime);

        // 步骤8：执行分页查询
        IPage<Account> accountPage = accountMapper.selectPage(page, wrapper);

        // 转换为VO
        return accountPage.convert(this::toVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAccount(Long userId, AccountUpdateRequest request) {
        // 步骤2：根据账目ID查询原账目记录
        Account account = accountMapper.selectById(request.getId());
        // 步骤3：校验原账目存在且user_id匹配当前用户（数据隔离）
        if (account == null || !account.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.ACCOUNT_NOT_FOUND);
        }

        // 步骤2（再次校验请求参数）
        validateAccountRequest(request.getType(), request.getCategory(), request.getAmount(), request.getAccountDate());

        // 步骤5：记录旧业务日期，提取旧月份
        String oldMonth = account.getAccountDate().format(MONTH_FORMATTER);

        // 步骤6：更新账目实体字段
        account.setType(request.getType());
        account.setCategory(request.getCategory());
        account.setAmount(request.getAmount());
        account.setAccountDate(request.getAccountDate());
        account.setRemark(request.getRemark());
        account.setVersion(request.getVersion());

        // 步骤7：调用数据层更新（MP自动校验version乐观锁）
        int rows = accountMapper.updateById(account);
        // 步骤8：若更新影响行数为0，抛出乐观锁冲突异常
        if (rows == 0) {
            throw new BusinessException(ResultCode.OPTIMISTIC_LOCK_CONFLICT);
        }

        // 步骤9：提取新业务日期的月份
        String newMonth = request.getAccountDate().format(MONTH_FORMATTER);

        // 步骤10~11：判断新旧月份是否一致
        if (!oldMonth.equals(newMonth)) {
            // 跨月：发布两个账目变更事件（旧月份、新月份）
            eventPublisher.publishEvent(new AccountChangeEvent(userId, oldMonth));
            eventPublisher.publishEvent(new AccountChangeEvent(userId, newMonth));
        } else {
            // 同月：发布一个账目变更事件
            eventPublisher.publishEvent(new AccountChangeEvent(userId, newMonth));
        }

        log.info("修改记账成功: userId={}, accountId={}, oldMonth={}, newMonth={}", userId, request.getId(), oldMonth, newMonth);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAccount(Long userId, Long id) {
        // 步骤2：构建删除条件：id匹配 + user_id匹配（数据隔离）
        LambdaQueryWrapper<Account> wrapper = new LambdaQueryWrapper<Account>()
                .eq(Account::getId, id)
                .eq(Account::getUserId, userId);
        Account account = accountMapper.selectOne(wrapper);
        if (account == null) {
            // 步骤4：若影响行数为0，返回错误码2001
            throw new BusinessException(ResultCode.ACCOUNT_NOT_FOUND);
        }

        // 步骤3：执行逻辑删除（MP自动将is_deleted置为1）
        accountMapper.deleteById(id);

        // 步骤5~6：发布账目变更事件
        String month = account.getAccountDate().format(MONTH_FORMATTER);
        eventPublisher.publishEvent(new AccountChangeEvent(userId, month));

        log.info("删除记账成功: userId={}, accountId={}, month={}", userId, id, month);
    }

    /**
     * 校验请求参数
     */
    private void validateAccountRequest(Integer type, String category, java.math.BigDecimal amount, LocalDate accountDate) {
        if (!AccountTypeEnum.isValid(type)) {
            throw new BusinessException(ResultCode.PARAM_VALIDATION_FAILED, "收支类型无效");
        }
        if (!AccountCategoryEnum.isValid(category)) {
            throw new BusinessException(ResultCode.PARAM_VALIDATION_FAILED, "收支分类无效");
        }
        if (amount == null || amount.compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ResultCode.PARAM_VALIDATION_FAILED, "金额必须大于0");
        }
        // 业务日期不能晚于当前日期+7天
        LocalDate maxDate = LocalDate.now().plusDays(7);
        if (accountDate != null && accountDate.isAfter(maxDate)) {
            throw new BusinessException(ResultCode.PARAM_VALIDATION_FAILED, "业务日期不能晚于当前日期+7天");
        }
    }

    /**
     * 生成幂等Key
     */
    private String buildIdempotentKey(Long userId, String category, java.math.BigDecimal amount, LocalDate accountDate) {
        String raw = userId + "|" + category + "|" + amount.toPlainString() + "|" + accountDate.toString();
        return MD5Utils.md5(raw);
    }

    /**
     * 实体转VO
     */
    private AccountVO toVO(Account account) {
        return new AccountVO(
                account.getId(),
                account.getType(),
                account.getCategory(),
                account.getAmount(),
                account.getAccountDate(),
                account.getRemark(),
                account.getVersion(),
                account.getCreateTime(),
                account.getUpdateTime()
        );
    }
}
