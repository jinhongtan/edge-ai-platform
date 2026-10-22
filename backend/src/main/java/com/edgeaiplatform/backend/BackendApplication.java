package com.edgeaiplatform.backend;

import com.edgeaiplatform.backend.config.InspectorProperties;
import com.edgeaiplatform.backend.config.StorageProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
@EnableConfigurationProperties({StorageProperties.class, InspectorProperties.class})
public class BackendApplication {
    public static void main(String[] args) { SpringApplication.run(BackendApplication.class, args); }
}
