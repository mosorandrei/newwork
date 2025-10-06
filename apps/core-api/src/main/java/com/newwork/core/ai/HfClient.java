package com.newwork.core.ai;

import java.util.List;
import java.util.Map;

public interface HfClient {
    List<Map<String,Object>> infer(String model, String token, String input);
}
