package org.rod.kaizen_api.configs;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.net.URI;

@Configuration
public class DataSourceConfig {

    // Railway injeta DATABASE_URL como postgresql://user:pass@host:port/db
    @Value("${DATABASE_URL:}")
    private String databaseUrl;

    // Fallbacks para desenvolvimento local
    @Value("${PGHOST:localhost}")
    private String pgHost;

    @Value("${PGPORT:5432}")
    private int pgPort;

    @Value("${PGDATABASE:kaizen_db}")
    private String pgDatabase;

    @Value("${PGUSER:rrxx}")
    private String pgUser;

    @Value("${PGPASSWORD:}")
    private String pgPassword;

    @Bean
    @Primary
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        config.setConnectionTimeout(30000);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(5);

        if (databaseUrl != null && !databaseUrl.isBlank()) {
            // Parseia postgresql://user:pass@host:port/database
            String normalized = databaseUrl
                    .replace("postgresql://", "//")
                    .replace("postgres://", "//");
            URI uri = URI.create(normalized);
            String host = uri.getHost();
            int port = uri.getPort() > 0 ? uri.getPort() : 5432;
            String db = uri.getPath().replaceFirst("^/", "");
            String userInfo = uri.getUserInfo() != null ? uri.getUserInfo() : "postgres:";
            String user = userInfo.split(":", 2)[0];
            String pass = userInfo.contains(":") ? userInfo.substring(userInfo.indexOf(':') + 1) : "";

            config.setJdbcUrl("jdbc:postgresql://" + host + ":" + port + "/" + db);
            config.setUsername(user);
            config.setPassword(pass);
        } else {
            // Desenvolvimento local
            config.setJdbcUrl("jdbc:postgresql://" + pgHost + ":" + pgPort + "/" + pgDatabase);
            config.setUsername(pgUser);
            config.setPassword(pgPassword);
        }

        return new HikariDataSource(config);
    }
}
