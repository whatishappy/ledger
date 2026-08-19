package com.ledger.modules.export.controller;

import com.ledger.common.context.UserContext;
import com.ledger.common.result.Result;
import com.ledger.modules.export.dto.ExportRequest;
import com.ledger.modules.export.dto.ExportTaskVO;
import com.ledger.modules.export.service.IExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 导出控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/export")
@RequiredArgsConstructor
@Tag(name = "导出模块", description = "Excel同步/异步导出、任务状态管理、文件清理")
public class ExportController {

    private final IExportService exportService;

    /**
     * Excel 导出（E-01，按详细设计 §8.3）
     * ≤10000 行同步返回文件流；>10000 行返回 taskId
     */
    @PostMapping("/excel")
    @Operation(summary = "Excel导出（自动判断同步/异步）")
    public Result<String> exportExcel(@RequestBody ExportRequest request, HttpServletResponse response) {
        Long userId = UserContext.requireUserId();
        String taskId = exportService.exportExcel(userId, request, response);
        if (taskId == null) {
            // 同步导出已完成，直接写入响应流
            return Result.success("同步导出已完成");
        }
        return Result.success(taskId);
    }

    /**
     * 查询导出任务状态（E-02）
     */
    @GetMapping("/status/{taskId}")
    @Operation(summary = "查询导出任务状态")
    public Result<ExportTaskVO> getExportStatus(@PathVariable String taskId) {
        Long userId = UserContext.requireUserId();
        ExportTaskVO vo = exportService.getExportStatus(userId, taskId);
        return Result.success(vo);
    }

    /**
     * 下载导出文件（E-03）
     */
    @GetMapping("/download/{taskId}")
    @Operation(summary = "下载导出文件")
    public ResponseEntity<Resource> downloadExportFile(@PathVariable String taskId) {
        Long userId = UserContext.requireUserId();
        Resource resource = exportService.downloadExportFile(userId, taskId);
        String fileName = URLEncoder.encode("账目导出.xlsx", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename*=utf-8''" + fileName)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(resource);
    }
}
