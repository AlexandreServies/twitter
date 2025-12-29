package com.bark.twitter.config;

import com.bark.twitter.provider.SynopticDataProvider;
import com.bark.twitter.provider.TwitterApiDataProvider;
import com.bark.twitter.provider.TwitterDataProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Configuration for selecting the active Twitter data source.
 * Switch between providers by changing the 'twitter.data-source' property in application.yml.
 */
@Configuration
public class TwitterDataSourceConfig {

    public enum DataSource {
        SYNOPTIC,
        TWITTERAPI
    }

    @Value("${twitter.data-source:SYNOPTIC}")
    private DataSource dataSource;

    @Bean
    @Primary
    public TwitterDataProvider activeTwitterDataProvider(
            SynopticDataProvider synopticDataProvider,
            TwitterApiDataProvider twitterApiDataProvider) {

        TwitterDataProvider selected = switch (dataSource) {
            case SYNOPTIC -> synopticDataProvider;
            case TWITTERAPI -> twitterApiDataProvider;
        };

        System.out.println("[CONFIG] Active Twitter data source: " + selected.getProviderName());
        return selected;
    }
}
