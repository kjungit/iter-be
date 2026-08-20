package com.example.iter.auth.support;

import com.example.iter.common.exception.CustomException;
import com.example.iter.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;

@Component
public class OAuth2ExchangeSessionManager {

    public static final String ATTRIBUTE_NAME =
            OAuth2ExchangeSessionManager.class.getName() + ".EXCHANGE_CODE";

    public void store(HttpServletRequest request, String exchangeCode) {
        request.getSession(true).setAttribute(ATTRIBUTE_NAME, exchangeCode);
    }

    public String consume(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            throw new CustomException(ErrorCode.OAUTH_TOKEN_INVALID);
        }

        synchronized (session) {
            Object value;
            try {
                value = session.getAttribute(ATTRIBUTE_NAME);
                session.removeAttribute(ATTRIBUTE_NAME);
            } catch (IllegalStateException exception) {
                throw new CustomException(ErrorCode.OAUTH_TOKEN_INVALID);
            }

            if (!(value instanceof String exchangeCode) || exchangeCode.isBlank()) {
                throw new CustomException(ErrorCode.OAUTH_TOKEN_INVALID);
            }
            return exchangeCode;
        }
    }

    public void invalidate(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return;
        }
        try {
            session.invalidate();
        } catch (IllegalStateException ignored) {
            // 동시 교환 요청이 먼저 세션을 폐기한 경우에도 정리는 완료된 것으로 본다.
        }
    }
}
