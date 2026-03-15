package com.ast.scheduler.common.mapper;

import com.ast.scheduler.common.entity.Task;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 任务 Mapper
 */
@Mapper
public interface TaskMapper {

    Task selectById(@Param("id") Integer id);

    List<Task> selectAll();

    int insert(Task task);

    int updateById(Task task);

    int deleteById(@Param("id") Integer id);
}
