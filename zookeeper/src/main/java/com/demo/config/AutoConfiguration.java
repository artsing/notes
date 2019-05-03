package com.demo.config;

import com.demo.client.Client;
import com.demo.client.Master;
import com.demo.client.Worker;
import org.apache.zookeeper.KeeperException;
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

    @Bean
    @ConditionalOnMissingBean(Master.class)
    public Master masterBean() throws IOException {
        return new Master("1001");
    }

    @Bean
    @ConditionalOnMissingBean(Worker.class)
    public Worker workerBean() throws IOException {
        return new Worker();
    }

    @Bean
    @ConditionalOnMissingBean(Client.class)
    public Client clientBean() throws IOException,
            KeeperException.NodeExistsException, InterruptedException {
        Client client = new Client();
        client.queueCommand("ls");
        return client;
    }
}
