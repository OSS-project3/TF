package com.example.teamflow.infra.google;

import com.example.teamflow.common.exception.BusinessException;
import com.example.teamflow.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
public class GoogleAuthService {

    private final RestTemplate restTemplate;

    @Value("${google.client-id:}")
    private String clientId;

    public GoogleAuthService(RestTemplateBuilder builder) {
        this.restTemplate = builder.build();
    }

    public GoogleTokenInfo verify(String idToken) {
        if (!StringUtils.hasText(clientId)) {
            throw new BusinessException(ErrorCode.GOOGLE_AUTH_DISABLED);
        }
        try {
            GoogleTokenInfo info = restTemplate.getForObject(
                    "https://oauth2.googleapis.com/tokeninfo?id_token=" + idToken,
                    GoogleTokenInfo.class);
            if (info == null || !clientId.equals(info.aud())) {
                throw new BusinessException(ErrorCode.GOOGLE_AUTH_INVALID);
            }
            if (!"true".equals(info.emailVerified())) {
                throw new BusinessException(ErrorCode.GOOGLE_AUTH_INVALID);
            }
            return info;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Google 토큰 검증 실패: {}", e.getMessage());
            throw new BusinessException(ErrorCode.GOOGLE_AUTH_INVALID);
        }
    }
}
