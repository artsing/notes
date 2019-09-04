package com.demo.config;

import com.demo.cluster.Client;
import com.demo.cluster.Master;
import com.demo.cluster.Worker;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

/**
 * Auto Configuration of Master
 * @author artsing
 */
@Configuration
public class AutoConfiguration {
    @Value("${host}")
    private String host;

    @Bean
    @ConditionalOnMissingBean(Master.class)
    public Master masterBean() throws IOException {
        Master m = new Master(host);
        m.startZK();
        return m;
    }

    @Bean
    @ConditionalOnMissingBean(Worker.class)
    public Worker workerBean() throws IOException {
        Worker worker = new Worker(host);
        worker.startZK();
        return worker;
    }

    @Bean
    @ConditionalOnMissingBean(Client.class)
    public Client clientBean() throws IOException {
        Client client = new Client(host);
        client.startZK();
        return client;
    }
}
