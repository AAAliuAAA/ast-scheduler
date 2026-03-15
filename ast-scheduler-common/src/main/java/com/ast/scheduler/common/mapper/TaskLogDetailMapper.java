package com.ast.scheduler.common.mapper;

import com.ast.scheduler.common.entity.TaskLogDetail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 任务日志详情 Mapper
 */
@Mapper
public interface TaskLogDetailMapper {

    TaskLogDetail selectByLogId(@Param("logId") Integer logId);

    int insert(TaskLogDetail taskLogDetail);

    int deleteByLogId(@Param("logId") Integer logId);
}
