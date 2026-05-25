package com.demo.test.openai.config;

import com.demo.test.openai.utils.CustomChatAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

import static com.demo.test.openai.contants.AppContants.*;

@Configuration
public class AppConfig {


    public AppConfig() {

    }

    @Bean (name = "mariadbDataSourceProperties")
    @Primary
    @ConfigurationProperties(prefix = "spring.datasource.mariadb")
    public DataSourceProperties mariadbDatasourceProperties() {
        return new DataSourceProperties();
    }

    @Bean (name = "mariadbDataSource")
    @Primary
    public DataSource mariadbDataSource(@Qualifier("mariadbDataSourceProperties") final DataSourceProperties dataSourceProperties) {
        return dataSourceProperties
                .initializeDataSourceBuilder()
                .build();
    }


    @Bean (name = "mysqlDataSourceProperties")
    @ConfigurationProperties(prefix = "spring.datasource.mysql")
    public DataSourceProperties mysqlDatasourceProperties() {
        return new DataSourceProperties();
    }

    @Bean (name = "mysqlDataSource")
    public DataSource mysqlDataSource(@Qualifier("mysqlDataSourceProperties") final DataSourceProperties dataSourceProperties) {
        return dataSourceProperties
                .initializeDataSourceBuilder()
                .build();
    }

    @Bean
    public JdbcChatMemoryRepository jdbcChatMemoryRepository(@Qualifier("mysqlDataSource") final DataSource dataSource) {

        return JdbcChatMemoryRepository.builder()
                .dataSource(dataSource)
                .build();
    }

    @Bean
    public ChatMemory chatMemory(
           final JdbcChatMemoryRepository repository) {

        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(repository)
                .maxMessages(MAX_CHAT_MEMORY_MESSAGES)
                .build();
    }


    @Bean
    public ChatClient customChatClient(final ChatMemory chatMemory, final ChatClient.Builder customChatClientBuilder) {


        return customChatClientBuilder
                .defaultAdvisors(advisorSpec -> advisorSpec.advisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        new CustomChatAdvisor(),
                        new SimpleLoggerAdvisor()))
                .defaultOptions(ChatOptions.builder()
                        .model(OPENAI_MODEL))
                .build();
    }
}