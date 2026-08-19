package com.ledger.modules.export.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 导出配置（读取 application.yml 中的 export 配置）
 */
@Data
@Component
@ConfigurationProperties(prefix = "export")
public class ExportProperties {

    /**
     * 导出文件存储路径
     */
    private String filePath = "./exports/";

    /**
     * 同步导出阈值（行数），超过则异步导出
     */
    private int syncThreshold = 10000;

    /**
     * 最大导出行数
     */
    private int maxRows = 1000000;

    /**
     * 文件保留天数
     */
    private int retainDays = 7;
}
