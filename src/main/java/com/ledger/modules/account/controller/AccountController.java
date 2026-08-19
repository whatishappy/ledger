package com.ledger.modules.account.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ledger.common.context.UserContext;
import com.ledger.common.result.Result;
import com.ledger.modules.account.dto.AccountAddRequest;
import com.ledger.modules.account.dto.AccountPageRequest;
import com.ledger.modules.account.dto.AccountUpdateRequest;
import com.ledger.modules.account.dto.AccountVO;
import com.ledger.modules.account.service.IAccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 账目控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/account")
@RequiredArgsConstructor
@Tag(name = "账目模块", description = "账目CRUD、分页查询、幂等记账、跨月缓存管理")
public class AccountController {

    private final IAccountService accountService;

    /**
     * 新增记账（A-01）：含幂等设计，Key由服务端生成
     */
    @PostMapping("/add")
    @Operation(summary = "新增记账（幂等）")
    public Result<Long> addAccount(@Valid @RequestBody AccountAddRequest request) {
        Long userId = UserContext.requireUserId();
        Long accountId = accountService.addAccount(userId, request);
        return Result.success(accountId);
    }

    /**
     * 分页条件查询（A-02）
     */
    @PostMapping("/page")
    @Operation(summary = "分页查询账目")
    public Result<IPage<AccountVO>> pageQuery(@RequestBody AccountPageRequest request) {
        Long userId = UserContext.requireUserId();
        IPage<AccountVO> page = accountService.pageQuery(userId, request);
        return Result.success(page);
    }

    /**
     * 修改记账（A-03）：含跨月缓存双清、乐观锁
     */
    @PutMapping("/update")
    @Operation(summary = "修改记账")
    public Result<Void> updateAccount(@Valid @RequestBody AccountUpdateRequest request) {
        Long userId = UserContext.requireUserId();
        accountService.updateAccount(userId, request);
        return Result.success();
    }

    /**
     * 删除记账（A-04）：逻辑删除
     */
    @DeleteMapping("/delete/{id}")
    @Operation(summary = "删除记账")
    public Result<Void> deleteAccount(@PathVariable Long id) {
        Long userId = UserContext.requireUserId();
        accountService.deleteAccount(userId, id);
        return Result.success();
    }
}
