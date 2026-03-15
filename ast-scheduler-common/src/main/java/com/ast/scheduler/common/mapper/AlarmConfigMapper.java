package com.ast.scheduler.common.mapper;

import com.ast.scheduler.common.entity.AlarmConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 告警配置 Mapper
 */
@Mapper
public interface AlarmConfigMapper {

    AlarmConfig selectById(@Param("id") Integer id);

    List<AlarmConfig> selectAll();

    int insert(AlarmConfig alarmConfig);

    int updateById(AlarmConfig alarmConfig);

    int deleteById(@Param("id") Integer id);
}
