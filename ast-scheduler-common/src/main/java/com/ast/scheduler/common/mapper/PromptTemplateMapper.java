package com.ast.scheduler.common.mapper;

import com.ast.scheduler.common.entity.PromptTemplate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Prompt模板 Mapper
 */
@Mapper
public interface PromptTemplateMapper {

    PromptTemplate selectById(@Param("id") Integer id);

    List<PromptTemplate> selectAll();

    int insert(PromptTemplate promptTemplate);

    int updateById(PromptTemplate promptTemplate);

    int deleteById(@Param("id") Integer id);
}
