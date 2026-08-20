package com.ledger.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ledger.modules.account.dto.AccountAddRequest;
import com.ledger.modules.account.service.IAccountService;
import com.ledger.entity.ScheduledTransaction;
import com.ledger.mapper.ScheduledTransactionMapper;
import com.ledger.service.ScheduledTransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduledTransactionServiceImpl implements ScheduledTransactionService {

    private final ScheduledTransactionMapper scheduledMapper;
    private final IAccountService accountService;

    @Override
    @Transactional
    public ScheduledTransaction create(ScheduledTransaction transaction) {
        transaction.setEnabled(1);
        transaction.setCreatedAt(LocalDateTime.now());
        transaction.setUpdatedAt(LocalDateTime.now());
        transaction.setNextRunAt(computeNextRun(transaction.getCron()));
        scheduledMapper.insert(transaction);
        log.info("创建定时交易: userId={}, id={}, cron={}", transaction.getUserId(), transaction.getId(), transaction.getCron());
        return transaction;
    }

    @Override
    @Transactional
    public ScheduledTransaction update(Long id, ScheduledTransaction transaction) {
        ScheduledTransaction existing = scheduledMapper.selectById(id);
        if (existing == null) {
            throw new RuntimeException("定时交易不存在: " + id);
        }
        if (transaction.getCron() != null) {
            existing.setCron(transaction.getCron());
            existing.setNextRunAt(computeNextRun(transaction.getCron()));
        }
        if (transaction.getType() != null) existing.setType(transaction.getType());
        if (transaction.getCategory() != null) existing.setCategory(transaction.getCategory());
        if (transaction.getAmount() != null) existing.setAmount(transaction.getAmount());
        if (transaction.getRemark() != null) existing.setRemark(transaction.getRemark());
        existing.setUpdatedAt(LocalDateTime.now());
        scheduledMapper.updateById(existing);
        return existing;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        scheduledMapper.deleteById(id);
        log.info("删除定时交易: id={}", id);
    }

    @Override
    @Transactional
    public ScheduledTransaction toggleEnabled(Long id, Integer enabled) {
        ScheduledTransaction existing = scheduledMapper.selectById(id);
        if (existing == null) {
            throw new RuntimeException("定时交易不存在: " + id);
        }
        existing.setEnabled(enabled);
        existing.setNextRunAt(enabled == 1 ? computeNextRun(existing.getCron()) : null);
        existing.setUpdatedAt(LocalDateTime.now());
        scheduledMapper.updateById(existing);
        return existing;
    }

    @Override
    public List<ScheduledTransaction> listByUserId(Long userId) {
        LambdaQueryWrapper<ScheduledTransaction> wrapper = new LambdaQueryWrapper<ScheduledTransaction>()
                .eq(ScheduledTransaction::getUserId, userId)
                .orderByDesc(ScheduledTransaction::getCreatedAt);
        return scheduledMapper.selectList(wrapper);
    }

    @Override
    public List<ScheduledTransaction> listDueTransactions() {
        LocalDateTime now = LocalDateTime.now();
        LambdaQueryWrapper<ScheduledTransaction> wrapper = new LambdaQueryWrapper<ScheduledTransaction>()
                .eq(ScheduledTransaction::getEnabled, 1)
                .le(ScheduledTransaction::getNextRunAt, now);
        return scheduledMapper.selectList(wrapper);
    }

    @Override
    @Transactional
    public void executeScheduledTransaction(Long id) {
        ScheduledTransaction st = scheduledMapper.selectById(id);
        if (st == null || st.getEnabled() == 0) return;

        AccountAddRequest request = new AccountAddRequest();
        request.setType(st.getType());
        request.setCategory(st.getCategory());
        request.setAmount(st.getAmount());
        request.setAccountDate(LocalDate.now());
        request.setRemark((st.getRemark() != null ? st.getRemark() : "") + " [定时交易]");
        accountService.addAccount(st.getUserId(), request);

        st.setNextRunAt(computeNextRun(st.getCron()));
        st.setUpdatedAt(LocalDateTime.now());
        scheduledMapper.updateById(st);

        log.info("定时交易执行完成: id={}, userId={}", id, st.getUserId());
    }

    private LocalDateTime computeNextRun(String cron) {
        try {
            CronExpression expression = CronExpression.parse(cron);
            Object result = expression.next(LocalDateTime.now());
            if (result instanceof java.time.Duration duration) {
                return LocalDateTime.now().plus(duration);
            } else if (result instanceof java.time.temporal.TemporalAmount amount) {
                return LocalDateTime.now().plus(amount);
            }
            return LocalDateTime.now().plusHours(1);
        } catch (Exception e) {
            log.warn("Cron表达式解析失败，默认1小时后执行: cron={}", cron);
            return LocalDateTime.now().plusHours(1);
        }
    }
}
