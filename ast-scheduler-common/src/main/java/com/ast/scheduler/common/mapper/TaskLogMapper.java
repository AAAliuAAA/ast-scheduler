package com.ast.scheduler.common.mapper;

import com.ast.scheduler.common.entity.TaskLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 任务日志 Mapper
 */
@Mapper
public interface TaskLogMapper {

    TaskLog selectById(@Param("id") Integer id);

    List<TaskLog> selectByTaskId(@Param("taskId") Integer taskId);

    int insert(TaskLog taskLog);

    int deleteById(@Param("id") Integer id);
}
