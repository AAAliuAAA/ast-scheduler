package com.ast.scheduler.web.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.File;

@Slf4j
@Configuration
public class DataDirectoryConfig {

    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    @PostConstruct
    public void init() {
        // 从 JDBC URL 中提取数据库文件路径
        if (datasourceUrl != null && datasourceUrl.startsWith("jdbc:sqlite:")) {
            String dbPath = datasourceUrl.substring("jdbc:sqlite:".length());
            File dbFile = new File(dbPath);
            File parentDir = dbFile.getParentFile();

            if (parentDir != null && !parentDir.exists()) {
                if (parentDir.mkdirs()) {
                    log.info("Created data directory: {}", parentDir.getAbsolutePath());
                } else {
                    log.warn("Failed to create data directory: {}", parentDir.getAbsolutePath());
                }
            }
        }
    }
}
