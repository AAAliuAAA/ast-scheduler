package com.ast.scheduler.common.entity;

import java.time.LocalDateTime;

/**
 * Prompt模板实体
 */
public class PromptTemplate {

    private Integer id;
    private String templateName;
    private String templateDesc;
    private String agentType;
    private String roleDefinition;
    private String rulesTemplate;
    private String promptTemplate;
    private String variables;
    private Integer isSystem;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public String getTemplateDesc() {
        return templateDesc;
    }

    public void setTemplateDesc(String templateDesc) {
        this.templateDesc = templateDesc;
    }

    public String getAgentType() {
        return agentType;
    }

    public void setAgentType(String agentType) {
        this.agentType = agentType;
    }

    public String getRoleDefinition() {
        return roleDefinition;
    }

    public void setRoleDefinition(String roleDefinition) {
        this.roleDefinition = roleDefinition;
    }

    public String getRulesTemplate() {
        return rulesTemplate;
    }

    public void setRulesTemplate(String rulesTemplate) {
        this.rulesTemplate = rulesTemplate;
    }

    public String getPromptTemplate() {
        return promptTemplate;
    }

    public void setPromptTemplate(String promptTemplate) {
        this.promptTemplate = promptTemplate;
    }

    public String getVariables() {
        return variables;
    }

    public void setVariables(String variables) {
        this.variables = variables;
    }

    public Integer getIsSystem() {
        return isSystem;
    }

    public void setIsSystem(Integer isSystem) {
        this.isSystem = isSystem;
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