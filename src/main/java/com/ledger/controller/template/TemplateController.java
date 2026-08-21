package com.ledger.controller.template;

import com.ledger.common.context.UserContext;
import com.ledger.common.result.Result;
import com.ledger.dto.template.TemplateApplyDTO;
import com.ledger.dto.template.TemplateCreateDTO;
import com.ledger.dto.template.TemplateUpdateDTO;
import com.ledger.modules.account.dto.AccountVO;
import com.ledger.service.template.TemplateService;
import com.ledger.vo.template.TemplateVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/templates")
@RequiredArgsConstructor
@Tag(name = "交易模板模块", description = "模板CRUD、从模板生成账目（TP-01~TP-05）")
public class TemplateController {

    private final TemplateService templateService;

    @GetMapping
    @Operation(summary = "TP-01 查询模板列表（支持关键词/类型过滤/排序/分页）")
    public Result<List<TemplateVO>> listTemplates(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false, defaultValue = "createdAt") String sortBy,
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer size) {
        Long userId = UserContext.requireUserId();
        List<TemplateVO> list = templateService.listTemplates(userId, keyword, type, sortBy, page, size);
        return Result.success(list);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "TP-02 创建交易模板（同名校验）")
    public Result<TemplateVO> createTemplate(@Valid @RequestBody TemplateCreateDTO dto) {
        Long userId = UserContext.requireUserId();
        TemplateVO vo = templateService.createTemplate(userId, dto);
        return Result.success(vo);
    }

    @PutMapping("/{id}")
    @Operation(summary = "TP-03 更新交易模板")
    public Result<TemplateVO> updateTemplate(@PathVariable Long id,
                                              @Valid @RequestBody TemplateUpdateDTO dto) {
        Long userId = UserContext.requireUserId();
        TemplateVO vo = templateService.updateTemplate(userId, id, dto);
        return Result.success(vo);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "TP-04 删除交易模板")
    public Result<Void> deleteTemplate(@PathVariable Long id) {
        Long userId = UserContext.requireUserId();
        templateService.deleteTemplate(userId, id);
        return Result.success();
    }

    @PostMapping("/{id}/apply")
    @Operation(summary = "TP-05 应用模板生成账目（可选覆盖金额/备注/日期）")
    public Result<AccountVO> applyTemplate(@PathVariable Long id,
                                            @Valid @RequestBody(required = false) TemplateApplyDTO dto) {
        Long userId = UserContext.requireUserId();
        TemplateApplyDTO applyDto = dto != null ? dto : new TemplateApplyDTO();
        AccountVO accountVO = templateService.applyTemplate(userId, id, applyDto);
        return Result.success(accountVO);
    }
}
