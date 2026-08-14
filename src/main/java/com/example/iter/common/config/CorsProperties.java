package com.example.iter.common.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Validated
@Component
@ConfigurationProperties(prefix = "app.cors")
public class CorsProperties {

    @NotEmpty
    private List<String> allowedOrigins = new ArrayList<>();

    @AssertTrue(message = "Credential CORS에서는 와일드카드 Origin을 사용할 수 없습니다.")
    public boolean isWildcardOriginAbsent() {
        return !allowedOrigins.contains("*");
    }
}
