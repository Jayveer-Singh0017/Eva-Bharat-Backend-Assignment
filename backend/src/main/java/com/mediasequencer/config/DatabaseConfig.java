package com.mediasequencer.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.net.URI;

@Configuration
public class DatabaseConfig {

    private static final Logger log = LoggerFactory.getLogger(DatabaseConfig.class);

    @Value("${DATABASE_URL:}")
    private String databaseUrlEnv;

    @Value("${spring.datasource.url:}")
    private String springDatasourceUrl;

    @Value("${spring.datasource.username:}")
    private String springDatasourceUsername;

    @Value("${spring.datasource.password:}")
    private String springDatasourcePassword;

    @Bean
    @Primary
    @ConditionalOnMissingBean
    public DataSource dataSource() {
        String rawUrl = databaseUrlEnv != null && !databaseUrlEnv.isBlank()
                ? databaseUrlEnv.trim()
                : (springDatasourceUrl != null ? springDatasourceUrl.trim() : "");

        if (!rawUrl.isBlank()) {
            // 1. MySQL URL format (mysql://user:password@host:port/dbname or jdbc:mysql://...)
            if (rawUrl.startsWith("mysql://")) {
                try {
                    log.info("Configuring MySQL datasource from DATABASE_URL (mysql://...)");
                    URI uri = URI.create(rawUrl);
                    String[] creds = extractCredentials(uri.getUserInfo());

                    int port = uri.getPort() > 0 ? uri.getPort() : 3306;
                    String path = extractDatabaseName(uri.getPath());

                    StringBuilder jdbcUrl = new StringBuilder("jdbc:mysql://")
                            .append(uri.getHost()).append(":").append(port).append("/").append(path);

                    if (uri.getQuery() != null && !uri.getQuery().isBlank()) {
                        jdbcUrl.append("?").append(uri.getQuery());
                    } else {
                        jdbcUrl.append("?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
                    }

                    HikariConfig config = new HikariConfig();
                    config.setJdbcUrl(jdbcUrl.toString());
                    if (creds[0] != null) config.setUsername(creds[0]);
                    if (creds[1] != null) config.setPassword(creds[1]);
                    config.setDriverClassName("com.mysql.cj.jdbc.Driver");
                    configureHikariPool(config);
                    return new HikariDataSource(config);
                } catch (Exception e) {
                    log.warn("Failed to parse MySQL DATABASE_URL: {}. Falling back to default H2.", e.getMessage());
                }
            } else if (rawUrl.startsWith("jdbc:mysql:")) {
                log.info("Configuring MySQL datasource from JDBC URL");
                HikariConfig config = new HikariConfig();
                config.setJdbcUrl(rawUrl);
                if (springDatasourceUsername != null && !springDatasourceUsername.isBlank()) {
                    config.setUsername(springDatasourceUsername);
                }
                if (springDatasourcePassword != null) {
                    config.setPassword(springDatasourcePassword);
                }
                config.setDriverClassName("com.mysql.cj.jdbc.Driver");
                configureHikariPool(config);
                return new HikariDataSource(config);
            }

            // 2. PostgreSQL URL format (postgres:// or postgresql:// or jdbc:postgresql:)
            if (rawUrl.startsWith("postgres://") || rawUrl.startsWith("postgresql://")) {
                try {
                    log.info("Configuring PostgreSQL datasource from DATABASE_URL");
                    URI uri = URI.create(rawUrl);
                    String[] creds = extractCredentials(uri.getUserInfo());

                    int port = uri.getPort() > 0 ? uri.getPort() : 5432;
                    String path = extractDatabaseName(uri.getPath());

                    StringBuilder jdbcUrl = new StringBuilder("jdbc:postgresql://")
                            .append(uri.getHost()).append(":").append(port).append("/").append(path);

                    if (uri.getQuery() != null && !uri.getQuery().isBlank()) {
                        jdbcUrl.append("?").append(uri.getQuery());
                    }

                    HikariConfig config = new HikariConfig();
                    config.setJdbcUrl(jdbcUrl.toString());
                    if (creds[0] != null) config.setUsername(creds[0]);
                    if (creds[1] != null) config.setPassword(creds[1]);
                    config.setDriverClassName("org.postgresql.Driver");
                    configureHikariPool(config);
                    return new HikariDataSource(config);
                } catch (Exception e) {
                    log.warn("Failed to parse PostgreSQL DATABASE_URL: {}. Falling back to default H2.", e.getMessage());
                }
            } else if (rawUrl.startsWith("jdbc:postgresql:")) {
                log.info("Configuring PostgreSQL datasource from JDBC URL");
                HikariConfig config = new HikariConfig();
                config.setJdbcUrl(rawUrl);
                if (springDatasourceUsername != null && !springDatasourceUsername.isBlank()) {
                    config.setUsername(springDatasourceUsername);
                }
                if (springDatasourcePassword != null) {
                    config.setPassword(springDatasourcePassword);
                }
                config.setDriverClassName("org.postgresql.Driver");
                configureHikariPool(config);
                return new HikariDataSource(config);
            } else if (rawUrl.startsWith("jdbc:")) {
                HikariConfig config = new HikariConfig();
                config.setJdbcUrl(rawUrl);
                if (springDatasourceUsername != null && !springDatasourceUsername.isBlank()) {
                    config.setUsername(springDatasourceUsername);
                }
                if (springDatasourcePassword != null) {
                    config.setPassword(springDatasourcePassword);
                }
                configureHikariPool(config);
                return new HikariDataSource(config);
            }
        }

        // 3. Fallback: In-memory H2 with MySQL/PostgreSQL compatibility
        log.info("No external MySQL or PostgreSQL DATABASE_URL provided; using embedded H2 database (MySQL/PostgreSQL compatible mode).");
        HikariConfig h2Config = new HikariConfig();
        h2Config.setJdbcUrl("jdbc:h2:mem:media;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1");
        h2Config.setDriverClassName("org.h2.Driver");
        h2Config.setUsername("sa");
        h2Config.setPassword("");
        return new HikariDataSource(h2Config);
    }

    private static String[] extractCredentials(String userInfo) {
        String username = null;
        String password = null;
        if (userInfo != null && userInfo.contains(":")) {
            String[] parts = userInfo.split(":", 2);
            username = parts[0];
            password = parts[1];
        } else if (userInfo != null) {
            username = userInfo;
        }
        return new String[]{username, password};
    }

    private static String extractDatabaseName(String path) {
        if (path != null && path.startsWith("/")) {
            return path.substring(1);
        }
        return path != null ? path : "media";
    }

    private static void configureHikariPool(HikariConfig config) {
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(1);
        config.setMaxLifetime(30 * 60 * 1000); // 30 minutes
        config.setIdleTimeout(5 * 60 * 1000);   // 5 minutes
    }
}
