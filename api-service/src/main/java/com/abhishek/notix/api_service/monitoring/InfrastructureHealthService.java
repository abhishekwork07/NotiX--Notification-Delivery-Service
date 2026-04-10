package com.abhishek.notix.api_service.monitoring;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.File;
import java.net.HttpURLConnection;
import java.net.URI;
import java.sql.Connection;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

@Service
public class InfrastructureHealthService {

    private static final String UP = "UP";
    private static final String DOWN = "DOWN";
    private static final String UNKNOWN = "UNKNOWN";

    private final DataSource dataSource;

    @Value("${notix.infrastructure.docker.socket-path:/var/run/docker.sock}")
    private String dockerSocketPath;

    @Value("${notix.infrastructure.kafka.bootstrap-servers:localhost:9092}")
    private String kafkaBootstrapServers;

    @Value("${notix.infrastructure.postgres.validation-timeout-seconds:2}")
    private int postgresValidationTimeoutSeconds;

    @Value("${notix.infrastructure.prometheus.health-url:http://localhost:9090/-/ready}")
    private String prometheusHealthUrl;

    @Value("${notix.infrastructure.grafana.health-url:http://localhost:3000/api/health}")
    private String grafanaHealthUrl;

    public InfrastructureHealthService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public InfrastructureHealthResponse checkInfrastructure() {
        Map<String, ComponentHealth> components = new LinkedHashMap<>();
        components.put("docker", checkDocker());
        components.put("kafka", checkKafka());
        components.put("postgresql", checkPostgres());
        components.put("prometheus", checkHttpEndpoint(prometheusHealthUrl));
        components.put("grafana", checkHttpEndpoint(grafanaHealthUrl));

        boolean anyDown = components.values().stream().anyMatch(component -> DOWN.equals(component.status()));
        boolean anyUnknown = components.values().stream().anyMatch(component -> UNKNOWN.equals(component.status()));
        String overallStatus = anyDown ? DOWN : anyUnknown ? UNKNOWN : UP;

        return new InfrastructureHealthResponse(overallStatus, Instant.now(), components);
    }

    private ComponentHealth checkDocker() {
        File dockerSocket = new File(dockerSocketPath);
        if (dockerSocket.exists()) {
            return new ComponentHealth(UP, Map.of("socketPath", dockerSocketPath, "readable", dockerSocket.canRead()));
        }

        String dockerHost = System.getenv("DOCKER_HOST");
        if (dockerHost != null && !dockerHost.isBlank()) {
            return new ComponentHealth(UNKNOWN, Map.of("dockerHost", dockerHost, "message", "DOCKER_HOST is configured, but daemon reachability is not probed from the JVM"));
        }

        return new ComponentHealth(DOWN, Map.of("socketPath", dockerSocketPath, "message", "Docker socket was not found"));
    }

    private ComponentHealth checkKafka() {
        Properties properties = new Properties();
        properties.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrapServers);
        properties.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, "2000");
        properties.put(AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, "2000");

        try (AdminClient adminClient = AdminClient.create(properties)) {
            int nodeCount = adminClient.describeCluster().nodes().get(2, TimeUnit.SECONDS).size();
            return new ComponentHealth(UP, Map.of("bootstrapServers", kafkaBootstrapServers, "nodeCount", nodeCount));
        } catch (Exception ex) {
            return new ComponentHealth(DOWN, Map.of("bootstrapServers", kafkaBootstrapServers, "error", rootMessage(ex)));
        }
    }

    private ComponentHealth checkPostgres() {
        try (Connection connection = dataSource.getConnection()) {
            boolean valid = connection.isValid(postgresValidationTimeoutSeconds);
            return new ComponentHealth(valid ? UP : DOWN, Map.of("url", connection.getMetaData().getURL(), "valid", valid));
        } catch (Exception ex) {
            return new ComponentHealth(DOWN, Map.of("error", rootMessage(ex)));
        }
    }

    private ComponentHealth checkHttpEndpoint(String url) {
        try {
            HttpURLConnection connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
            connection.setConnectTimeout(2000);
            connection.setReadTimeout(2000);
            connection.setRequestMethod("GET");

            int statusCode = connection.getResponseCode();
            boolean healthy = statusCode >= 200 && statusCode < 400;
            return new ComponentHealth(healthy ? UP : DOWN, Map.of("url", url, "statusCode", statusCode));
        } catch (Exception ex) {
            return new ComponentHealth(DOWN, Map.of("url", url, "error", rootMessage(ex)));
        }
    }

    private String rootMessage(Exception ex) {
        Throwable current = ex;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
    }

    public record InfrastructureHealthResponse(String status, Instant checkedAt, Map<String, ComponentHealth> components) {
    }

    public record ComponentHealth(String status, Map<String, Object> details) {
    }
}
