package com.bark.twitter.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;

/**
 * Configuration for AWS DynamoDB client.
 */
@Configuration
public class DynamoDbConfig {

    @Bean
    public DynamoDbAsyncClient dynamoDbAsyncClient(
            @Value("${aws.region:us-east-1}") String region
    ) {
        return DynamoDbAsyncClient.builder()
                .region(Region.of(region))
                .build();
    }
}
