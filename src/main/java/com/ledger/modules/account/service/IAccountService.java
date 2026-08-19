package com.ledger.modules.account.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ledger.modules.account.dto.AccountAddRequest;
import com.ledger.modules.account.dto.AccountPageRequest;
import com.ledger.modules.account.dto.AccountUpdateRequest;
import com.ledger.modules.account.dto.AccountVO;

/**
 * 账目服务接口
 */
public interface IAccountService {

    /**
     * 新增记账（A-01）：含幂等设计
     * @return 新生成的账目ID（或重复提交时返回已有ID）
     */
    Long addAccount(Long userId, AccountAddRequest request);

    /**
     * 分页条件查询（A-02）
     */
    IPage<AccountVO> pageQuery(Long userId, AccountPageRequest request);

    /**
     * 修改记账（A-03）：含跨月双清、乐观锁
     */
    void updateAccount(Long userId, AccountUpdateRequest request);

    /**
     * 删除记账（A-04）：逻辑删除
     */
    void deleteAccount(Long userId, Long id);
}
