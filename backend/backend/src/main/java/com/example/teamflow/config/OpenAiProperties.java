package com.example.teamflow.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * OpenAI 연동 설정. application.properties의 openai.* 값(환경변수 주입) 바인딩.
 * api-key가 비어 있으면 AI 기능은 비활성(호출 시 AI_DISABLED).
 */
@Component
@ConfigurationProperties(prefix = "openai")
@Getter
@Setter
public class OpenAiProperties {
    private String apiKey = "";
    private String model = "gpt-4o-mini";
    private String baseUrl = "https://api.openai.com/v1";

    public boolean isEnabled() {
        return apiKey != null && !apiKey.isBlank();
    }
}
