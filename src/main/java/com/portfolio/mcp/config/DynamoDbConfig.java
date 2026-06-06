package com.portfolio.mcp.config;

import com.portfolio.mcp.tasks.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;

import java.net.URI;

/**
 * Wires the AWS DynamoDB clients used by the Task tools.
 *
 * <p>In AWS (EC2 / Lambda) the default credential + region providers are used,
 * so no configuration is needed beyond the IAM role attached to the instance.
 * For local development we point at <a href="https://hub.docker.com/r/amazon/dynamodb-local">DynamoDB
 * Local</a> by setting the {@code DYNAMODB_ENDPOINT} environment variable
 * (see {@code docker-compose.yml}).
 */
@Configuration
public class DynamoDbConfig {

    private static final Logger log = LoggerFactory.getLogger(DynamoDbConfig.class);

    @Value("${app.dynamodb.endpoint:}")
    private String endpoint;

    @Value("${app.dynamodb.region:us-east-1}")
    private String region;

    @Value("${app.dynamodb.table-name:mcp-tasks}")
    private String tableName;

    @Bean
    public DynamoDbClient dynamoDbClient() {
        var builder = DynamoDbClient.builder().region(Region.of(region));
        if (endpoint != null && !endpoint.isBlank()) {
            // Local / test mode: talk to DynamoDB Local instead of AWS. The SDK still
            // signs requests, so supply dummy credentials (DynamoDB Local ignores them).
            log.info("Using DynamoDB endpoint override: {}", endpoint);
            builder.endpointOverride(URI.create(endpoint))
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create("local", "local")));
        }
        // In AWS the default credential provider chain resolves the EC2 instance role.
        return builder.build();
    }

    @Bean
    public DynamoDbEnhancedClient dynamoDbEnhancedClient(DynamoDbClient client) {
        return DynamoDbEnhancedClient.builder().dynamoDbClient(client).build();
    }

    /**
     * Typed handle to the {@code mcp-tasks} table. In production the table is
     * created by Terraform; for local/test we create it on first use so the
     * demo works out of the box.
     */
    @Bean
    public DynamoDbTable<Task> taskTable(DynamoDbEnhancedClient enhancedClient) {
        DynamoDbTable<Task> table =
                enhancedClient.table(tableName, TableSchema.fromBean(Task.class));
        if (endpoint != null && !endpoint.isBlank()) {
            createTableIfMissing(table);
        }
        return table;
    }

    private void createTableIfMissing(DynamoDbTable<Task> table) {
        try {
            table.describeTable();
            log.info("DynamoDB table '{}' already exists", tableName);
        } catch (DynamoDbException e) {
            log.info("Creating local DynamoDB table '{}'", tableName);
            table.createTable();
        }
    }
}
