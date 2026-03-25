package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import java.util.List;

@Configuration
public class RedisConfig {

    @Bean
    public RedisScript<List> fetchAndRemoveScript() {
        DefaultRedisScript<List> redisScript = new DefaultRedisScript<>();
        redisScript.setLocation(new ClassPathResource("scripts/fetch_and_remove.lua"));
        redisScript.setResultType(List.class);
        return redisScript;
    }
}
