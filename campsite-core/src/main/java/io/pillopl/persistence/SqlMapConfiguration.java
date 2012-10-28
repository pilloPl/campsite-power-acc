package io.pillopl.persistence;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * The map of hand-written SQL, loaded from {@code classpath:sql/sqls.xml}. DAOs pick their statements by name:
 * {@code @Value("#{sqls['findManageableGroupIds']}")}.
 */
@Configuration(proxyBeanMethods = false)
public class SqlMapConfiguration {

    public static final String SQL_MAP_LOCATION = "sql/sqls.xml";

    @Bean(name = "sqls")
    public Map<String, String> sqls() {
        return loadSqlMap(SQL_MAP_LOCATION);
    }

    public static Map<String, String> loadSqlMap(String location) {
        Properties properties = new Properties();
        try (InputStream in = new ClassPathResource(location).getInputStream()) {
            properties.loadFromXML(in);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot load SQL map " + location, e);
        }
        Map<String, String> sqls = new HashMap<>();
        for (String name : properties.stringPropertyNames()) {
            sqls.put(name, properties.getProperty(name).strip());
        }
        return sqls;
    }
}
