package com.ast.scheduler.common.mapper;

import com.ast.scheduler.common.entity.UserConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 用户配置 Mapper
 */
@Mapper
public interface UserConfigMapper {

    UserConfig selectById(@Param("id") Integer id);

    int insert(UserConfig userConfig);

    int updateById(UserConfig userConfig);

    int deleteById(@Param("id") Integer id);
}