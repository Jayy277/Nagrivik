package org.nagrivic.health;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final ObjectProvider<DataSource> dataSourceProvider;

    public HealthController(ObjectProvider<DataSource> dataSourceProvider) {
        this.dataSourceProvider = dataSourceProvider;
    }

    @GetMapping
    public ResponseEntity<HealthResponse> checkHealth() {
        String dbStatus = checkDatabaseHealth();
        return ResponseEntity.ok(new HealthResponse("UP", "nagrivic-backend", dbStatus));
    }

    private String checkDatabaseHealth() {
        DataSource dataSource = dataSourceProvider.getIfAvailable();
        if (dataSource == null) {
            return "NOT_CONFIGURED";
        }
        try (Connection conn = dataSource.getConnection()) {
            if (conn.isValid(2)) {
                return "UP";
            }
            return "DOWN";
        } catch (Exception e) {
            return "DOWN";
        }
    }
}
