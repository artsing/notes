package com.demo.config;

import com.demo.client.Master;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

/**
 * Auto Configuration of Master
 * @author artsing
 */
@Configuration
public class AutoConfig {

    @Bean("master")
    @ConditionalOnMissingBean(Master.class)
    public Master masterBean() throws IOException {
        return new Master("1001");
    }
}
