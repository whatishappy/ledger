package com.ledger.service;

import com.ledger.entity.ScheduledTransaction;

import java.util.List;

public interface ScheduledTransactionService {

    ScheduledTransaction create(ScheduledTransaction transaction);

    ScheduledTransaction update(Long id, ScheduledTransaction transaction);

    void delete(Long id);

    ScheduledTransaction toggleEnabled(Long id, Integer enabled);

    List<ScheduledTransaction> listByUserId(Long userId);

    List<ScheduledTransaction> listDueTransactions();

    void executeScheduledTransaction(Long id);
}
