package com.ast.scheduler.common.entity;

import java.time.LocalDateTime;

/**
 * 执行器实体
 */
public class Executor {

    private Integer id;
    private String executorName;
    private String executorType;
    private String executorDesc;
    private String connectionType;
    private String connectionConfig;
    private String healthCheckCommand;
    private Integer status;
    private Integer isFavorite;
    private Integer favoriteOrder;
    private LocalDateTime lastCheckTime;
    private String lastCheckResult;
    private String groupName;
    private Integer sortOrder;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getExecutorName() {
        return executorName;
    }

    public void setExecutorName(String executorName) {
        this.executorName = executorName;
    }

    public String getExecutorType() {
        return executorType;
    }

    public void setExecutorType(String executorType) {
        this.executorType = executorType;
    }

    public String getExecutorDesc() {
        return executorDesc;
    }

    public void setExecutorDesc(String executorDesc) {
        this.executorDesc = executorDesc;
    }

    public String getConnectionType() {
        return connectionType;
    }

    public void setConnectionType(String connectionType) {
        this.connectionType = connectionType;
    }

    public String getConnectionConfig() {
        return connectionConfig;
    }

    public void setConnectionConfig(String connectionConfig) {
        this.connectionConfig = connectionConfig;
    }

    public String getHealthCheckCommand() {
        return healthCheckCommand;
    }

    public void setHealthCheckCommand(String healthCheckCommand) {
        this.healthCheckCommand = healthCheckCommand;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getIsFavorite() {
        return isFavorite;
    }

    public void setIsFavorite(Integer isFavorite) {
        this.isFavorite = isFavorite;
    }

    public Integer getFavoriteOrder() {
        return favoriteOrder;
    }

    public void setFavoriteOrder(Integer favoriteOrder) {
        this.favoriteOrder = favoriteOrder;
    }

    public LocalDateTime getLastCheckTime() {
        return lastCheckTime;
    }

    public void setLastCheckTime(LocalDateTime lastCheckTime) {
        this.lastCheckTime = lastCheckTime;
    }

    public String getLastCheckResult() {
        return lastCheckResult;
    }

    public void setLastCheckResult(String lastCheckResult) {
        this.lastCheckResult = lastCheckResult;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}