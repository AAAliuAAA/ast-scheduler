package com.ast.scheduler.common.mapper;

import com.ast.scheduler.common.entity.Executor;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 执行器 Mapper
 */
@Mapper
public interface ExecutorMapper {

    Executor selectById(@Param("id") Integer id);

    List<Executor> selectAll();

    int insert(Executor executor);

    int updateById(Executor executor);

    int deleteById(@Param("id") Integer id);
}
