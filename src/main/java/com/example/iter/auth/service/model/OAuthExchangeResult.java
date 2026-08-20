package com.example.iter.auth.service.model;

import com.example.iter.auth.dto.response.OAuthActionRequiredResponse;

public sealed interface OAuthExchangeResult {

    record Authenticated(IssuedTokenPair tokenPair) implements OAuthExchangeResult {
    }

    record ActionRequired(OAuthActionRequiredResponse response) implements OAuthExchangeResult {
    }
}
