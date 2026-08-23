package com.example.iter.payment.client;

import com.example.iter.payment.config.TossProperties;
import com.example.iter.payment.dto.toss.TossCancelApiRequest;
import com.example.iter.payment.dto.toss.TossConfirmApiRequest;
import com.example.iter.payment.dto.toss.TossConfirmApiResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Slf4j
@Component
public class TossPaymentClient {
    private static final String BASE_URL = "https://api.tosspayments.com";
    private static final String CONFIRM_URI = "/v1/payments/confirm";
    private static final String LOOKUP_URI = "/v1/payments/{paymentKey}";
    private static final String CANCEL_URI = "/v1/payments/{paymentKey}/cancel";

    private final RestClient restClient;

    public TossPaymentClient( TossProperties tossProperties ) {
        this.restClient = RestClient.builder()
                .baseUrl(BASE_URL)
                .defaultHeader(HttpHeaders.AUTHORIZATION, basicAuthHeader(tossProperties.secretKey())).build();
    }

    // idempotencyKey는 같은 Payment(같은 orderId)에 대한 confirm 재시도라면 항상 동일한 값을 넘겨야 한다 —
    // 토스가 (키+API키+URL+메서드) 조합으로 중복 요청을 판별해서, 같은 조합이면 실제로는 재실행하지 않고
    // 첫 요청의 응답을 그대로 돌려준다 (https://docs.tosspayments.com "멱등키 사용하기").
    public TossConfirmApiResponse confirm( String paymentKey, String orderId, BigDecimal amount, String idempotencyKey )  {
        TossConfirmApiRequest request = new TossConfirmApiRequest(paymentKey, orderId, amount.longValueExact());
        try {
            return restClient.post()
                    .uri(CONFIRM_URI)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Idempotency-Key", idempotencyKey)
                    .body(request)
                    .retrieve()
                    .body(TossConfirmApiResponse.class);
        } catch (RestClientResponseException e) {
            log.warn("토스 결제 승인 API 실패: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new TossApiException("토스 결제 승인 API 호출에 실패했습니다.");
        }
    }

    // 결제 조회 — 웹훅으로 받은 내용을 그대로 믿지 않고 paymentKey로 토스에 다시 물어봐서 검증할 때 쓴다.
    public TossConfirmApiResponse getPayment(String paymentKey) {
        try {
            return restClient.get()
                    .uri(LOOKUP_URI, paymentKey)
                    .retrieve()
                    .body(TossConfirmApiResponse.class);
        } catch (RestClientResponseException e) {
            log.warn("토스 결제 조회 API 실패: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new TossApiException("토스 결제 조회 API 호출에 실패했습니다.");
        }
    }

    // 결제 취소 — 대여 취소/거절 시 실제로 카드사·간편결제사 환불을 요청한다.
    public TossConfirmApiResponse cancel(String paymentKey, String cancelReason, String idempotencyKey) {
        try {
            return restClient.post()
                    .uri(CANCEL_URI, paymentKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Idempotency-Key", idempotencyKey)
                    .body(new TossCancelApiRequest(cancelReason))
                    .retrieve()
                    .body(TossConfirmApiResponse.class);
        } catch (RestClientResponseException e) {
            log.warn("토스 결제 취소 API 실패: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new TossApiException("토스 결제 취소 API 호출에 실패했습니다.");
        }
    }

    private String basicAuthHeader( String secretKey ) {
        String credentials = secretKey + ":";
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }
}
