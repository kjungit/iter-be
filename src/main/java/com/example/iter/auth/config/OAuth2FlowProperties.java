package com.example.iter.auth.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Getter
@Setter
@Validated
@Component
@ConfigurationProperties(prefix = "app.oauth2")
public class OAuth2FlowProperties {

    @NotBlank
    private String frontendCallbackUri;

    @NotNull
    private Duration exchangeTokenValidity;

    @NotNull
    private Duration actionTokenValidity;

    @NotNull
    private Duration pendingTokenRetention;
}
