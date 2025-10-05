package com.newwork.core.ai;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.ai.hf.retry")
@Getter
@Setter
public class HfRetryProps {
    private int maxAttempts = 3;
    private long initialDelayMs = 200;
    private double multiplier = 2.0;
    private long maxDelayMs = 2000;
    private long jitterMs = 100;
    private int[] retryOnStatus = new int[]{408,429,500,502,503,504};
}
