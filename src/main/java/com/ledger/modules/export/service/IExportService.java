package com.ledger.modules.export.service;

import com.ledger.modules.export.dto.ExportRequest;
import com.ledger.modules.export.dto.ExportTaskVO;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.io.Resource;

/**
 * 导出服务接口
 */
public interface IExportService {

    /**
     * Excel 导出（E-01，按详细设计 §8.3）
     * ≤10000 行同步返回文件流；>10000 行返回 taskId
     *
     * @param userId   当前用户ID
     * @param request  导出请求
     * @param response HTTP响应（同步导出时写入文件流）
     * @return 同步导出返回 null；异步导出返回 taskId
     */
    String exportExcel(Long userId, ExportRequest request, HttpServletResponse response);

    /**
     * 查询导出任务状态（E-02）
     */
    ExportTaskVO getExportStatus(Long userId, String taskId);

    /**
     * 下载导出文件（E-03）
     *
     * @return 文件资源
     */
    Resource downloadExportFile(Long userId, String taskId);
}
