package com.back;

import java.util.HashMap;
import java.util.Map;

// "삭제?id=1" 같은 명령을 행동 이름("삭제")과 파라미터(id=1)로 나눈다.
public class Rq {

    private final String actionName;
    private final Map<String, String> params = new HashMap<>();

    public Rq(String cmd) {
        String[] cmdBits = cmd.trim().split("\\?", 2);
        actionName = cmdBits[0].trim();

        if (cmdBits.length < 2) return;

        for (String paramStr : cmdBits[1].split("&")) {
            String[] paramBits = paramStr.split("=", 2);
            if (paramBits.length != 2) continue;

            String key = paramBits[0].trim();
            String value = paramBits[1].trim();
            if (key.isEmpty() || value.isEmpty()) continue;

            params.put(key, value);
        }
    }

    public String getActionName() {
        return actionName;
    }

    public String getParam(String name, String defaultValue) {
        return params.getOrDefault(name, defaultValue);
    }

    public int getParamAsInt(String name, int defaultValue) {
        String value = getParam(name, null);
        if (value == null) return defaultValue;

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
