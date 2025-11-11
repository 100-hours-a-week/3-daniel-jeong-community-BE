package com.kakaotechbootcamp.community.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "email.password-reset")
public class EmailProperties {
    private long minIntervalMs;
    private int codeExpirationMinutes;
}

