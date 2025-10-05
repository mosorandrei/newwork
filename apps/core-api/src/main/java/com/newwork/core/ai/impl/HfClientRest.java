package com.newwork.core.ai.impl;

import com.newwork.core.ai.HfClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
public class HfClientRest implements HfClient {
    @Override
    public List<Map<String, Object>> infer(String model, String token, String input) {
        var http = RestClient.builder()
                .baseUrl("https://api-inference.huggingface.co/models/" + model)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build();

        var payload = Map.of("inputs", "grammar: " + input);
        return http.post()
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }
}
