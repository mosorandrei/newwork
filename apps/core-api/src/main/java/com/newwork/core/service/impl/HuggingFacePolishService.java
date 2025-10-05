package com.newwork.core.service.impl;

import com.newwork.core.ai.HfClient;
import com.newwork.core.ai.HfRetryProps;
import com.newwork.core.service.AiPolishService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.util.List;
import java.util.Map;

@Service
public class HuggingFacePolishService implements AiPolishService {
    private final String model;
    private final String token;
    private final HfClient client;
    private final HfRetryProps retry;
    private final SecureRandom rnd = new SecureRandom();

    public HuggingFacePolishService(
            @Value("${app.ai.hf.model}") String model,
            @Value("${app.ai.hf.token}") String token,
            HfClient client,
            HfRetryProps retry
    ) {
        if (model == null || model.isBlank()) throw new IllegalStateException("app.ai.hf.model required");
        if (token == null || token.isBlank()) throw new IllegalStateException("HF_API_TOKEN required");
        this.model = model;
        this.token = token;
        this.client = client;
        this.retry = retry;
    }

    @Override public String modelId() { return model; }

    @Override
    public String polish(String input) {
        if (input == null || input.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "text_required");

        int attempts = 0;
        long delay = retry.getInitialDelayMs();

        while (true) {
            attempts++;
            try {
                List<Map<String,Object>> res = client.infer(model, token, input);
                if (res == null || res.isEmpty()) throw new IllegalStateException("empty_response");
                Object gt = res.getFirst().get("generated_text");
                if (gt instanceof String s && !s.isBlank()) return s.trim();
                throw new IllegalStateException("bad_response");
            } catch (RestClientResponseException ex) {
                int sc = ex.getStatusCode().value();
                if (!shouldRetry(sc, attempts)) {
                    throw new ResponseStatusException(ex.getStatusCode(), ex.getResponseBodyAsString(), ex);
                }
                sleep(delayWithJitter(delay));
                delay = nextDelay(delay);
            } catch (RestClientException | IllegalStateException ex) {
                if (!shouldRetry(-1, attempts)) throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "hf_unavailable", ex);
                sleep(delayWithJitter(delay));
                delay = nextDelay(delay);
            }
        }
    }

    private boolean shouldRetry(int status, int attempts) {
        if (attempts >= retry.getMaxAttempts()) return false;
        if (status < 0) return true;
        for (int s : retry.getRetryOnStatus()) if (s == status) return true;
        return false;
    }

    private long nextDelay(long current) {
        double nd = current * retry.getMultiplier();
        long capped = (long)Math.min(nd, retry.getMaxDelayMs());
        return Math.max(0, capped);
    }

    private long delayWithJitter(long base) {
        long j = retry.getJitterMs() <= 0 ? 0 : rnd.nextLong(retry.getJitterMs()+1);
        return base + j;
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
    }
}