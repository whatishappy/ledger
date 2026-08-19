package com.ledger.modules.export.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ledger.modules.export.entity.ExportTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 导出任务 Mapper
 */
@Mapper
public interface ExportTaskMapper extends BaseMapper<ExportTask> {

    /**
     * 查询处理中超时的任务（status=1 且 update_time 超过30分钟）
     * 用于启动恢复机制
     */
    @Select("SELECT * FROM export_task WHERE status = 1 AND update_time < #{threshold}")
    List<ExportTask> findTimeoutProcessingTasks(@Param("threshold") LocalDateTime threshold);

    /**
     * 查询已过期的任务（expire_time < NOW）
     */
    @Select("SELECT * FROM export_task WHERE status IN (0,1,2) AND expire_time < NOW()")
    List<ExportTask> findExpiredTasks();

    /**
     * 查询7天前的任务记录（用于物理删除）
     */
    @Select("SELECT * FROM export_task WHERE create_time < #{threshold}")
    List<ExportTask> findOldTasks(@Param("threshold") LocalDateTime threshold);
}
