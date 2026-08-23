package com.example.iter.payment.controller.api;

import com.example.iter.payment.dto.toss.TossWebhookPayload;
import com.example.iter.payment.service.TossWebhookService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 토스페이먼츠가 결제 상태 변경 시 호출하는 웹훅 수신 엔드포인트.
// 토스는 별도 서명 헤더를 안 보내서(Tosspayments-Webhook-Transmission-* 헤더만 옴),
// 바디를 믿는 대신 TossWebhookService가 paymentKey로 토스에 재조회해서 검증한다.
@RestController
@RequestMapping("/api/v1/webhooks/toss")
@RequiredArgsConstructor
public class TossWebhookApiController {

    private final TossWebhookService tossWebhookService;

    @PostMapping
    public ResponseEntity<Void> receive(@RequestBody TossWebhookPayload payload) {
        tossWebhookService.handle(payload);
        return ResponseEntity.ok().build();
    }
}
