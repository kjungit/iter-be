package com.example.iter.payment.service;

import com.example.iter.common.dto.response.PageResponse;
import com.example.iter.payment.domain.repository.PaymentRepository;
import com.example.iter.payment.dto.request.PaymentHistorySearchRequest;
import com.example.iter.payment.dto.response.PaymentHistoryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentHistoryService {

    private final PaymentRepository paymentRepository;

    public PageResponse<PaymentHistoryResponse> getMyPaymentHistory(
            Long userId,
            PaymentHistorySearchRequest request
    ) {
        var history = paymentRepository.findMyPaymentHistory(
                userId,
                request.status(),
                PageRequest.of(request.page(), request.size())
        );
        return PageResponse.from(history.map(row ->
                PaymentHistoryResponse.of(row.payment(), row.rental())));
    }
}
