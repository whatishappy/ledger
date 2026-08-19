package com.ledger.modules.imports.controller;

import com.ledger.common.context.UserContext;
import com.ledger.common.result.Result;
import com.ledger.modules.imports.dto.BillImportConfirmDTO;
import com.ledger.modules.imports.service.IBillImportService;
import com.ledger.modules.imports.vo.BillImportPreviewVO;
import com.ledger.modules.imports.vo.BillImportResultVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/imports")
@RequiredArgsConstructor
@Tag(name = "账单导入模块", description = "支付宝/微信账单CSV上传预览、确认导入、支持来源查询")
public class BillImportController {

    private final IBillImportService billImportService;

    @PostMapping(value = "/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "IM-01 账单解析预览")
    public Result<BillImportPreviewVO> preview(@RequestParam("file") MultipartFile file,
                                                @RequestParam("source") String source) {
        Long userId = UserContext.requireUserId();
        BillImportPreviewVO vo = billImportService.preview(userId, source, file);
        return Result.success(vo);
    }

    @PostMapping("/confirm")
    @Operation(summary = "IM-02 确认导入")
    public Result<BillImportResultVO> confirm(@RequestBody BillImportConfirmDTO dto) {
        Long userId = UserContext.requireUserId();
        BillImportResultVO vo = billImportService.confirm(userId, dto);
        return Result.success(vo);
    }

    @GetMapping("/support")
    @Operation(summary = "IM-03 查询支持的账单来源")
    public Result<Map<String, Integer>> getImportStatus() {
        Map<String, Integer> status = billImportService.getImportStatus();
        return Result.success(status);
    }
}
