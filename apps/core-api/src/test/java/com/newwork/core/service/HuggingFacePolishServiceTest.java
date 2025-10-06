package com.newwork.core.service;

import com.newwork.core.ai.HfClient;
import com.newwork.core.ai.HfRetryProps;
import com.newwork.core.service.impl.HuggingFacePolishService;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClientResponseException;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class HuggingFacePolishServiceTest {

    @Test
    void retries_then_succeeds() {
        HfClient client = mock(HfClient.class);
        HfRetryProps props = new HfRetryProps();
        props.setMaxAttempts(3);
        props.setInitialDelayMs(1);
        props.setMultiplier(1.0);
        props.setMaxDelayMs(2);
        props.setJitterMs(0);

        var svc = new HuggingFacePolishService("vennify/t5-base-grammar-correction", "hf_token", client, props);

        var ex429 = new RestClientResponseException("429", 429, "Too Many", null, new byte[0], StandardCharsets.UTF_8);
        when(client.infer(any(), any(), any()))
                .thenThrow(ex429) // attempt 1
                .thenThrow(ex429) // attempt 2
                .thenReturn(List.of(Map.of("generated_text", "Fixed."))); // attempt 3

        String out = svc.polish("bad txt");
        assertEquals("Fixed.", out);
        verify(client, times(3)).infer(any(), any(), any());
    }

    @Test
    void nonRetryable_400_bubbles_immediately() {
        HfClient client = mock(HfClient.class);
        HfRetryProps props = new HfRetryProps();
        var svc = new HuggingFacePolishService("vennify/t5-base-grammar-correction", "hf_token", client, props);

        var ex400 = new RestClientResponseException("400", 400, "Bad Request", null, new byte[0], StandardCharsets.UTF_8);
        when(client.infer(any(), any(), any())).thenThrow(ex400);

        var ex = assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> svc.polish("x"));
        assertEquals(400, ex.getStatusCode().value());
        verify(client, times(1)).infer(any(), any(), any());
    }

    @Test
    void gives_502_after_exhausted() {
        HfClient client = mock(HfClient.class);
        HfRetryProps props = new HfRetryProps();
        props.setMaxAttempts(2);
        props.setInitialDelayMs(1);
        props.setMultiplier(1.0);
        props.setMaxDelayMs(2);
        props.setJitterMs(0);

        var svc = new HuggingFacePolishService("vennify/t5-base-grammar-correction", "hf_token", client, props);

        var ex503 = new RestClientResponseException("503", 503, "Service Unavailable", null, new byte[0], StandardCharsets.UTF_8);
        when(client.infer(any(), any(), any())).thenThrow(ex503).thenThrow(ex503);

        var ex = assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> svc.polish("x"));
        assertEquals(503, ex.getStatusCode().value()); // last non-retryable return is 503 path
        verify(client, times(2)).infer(any(), any(), any());
    }
}
