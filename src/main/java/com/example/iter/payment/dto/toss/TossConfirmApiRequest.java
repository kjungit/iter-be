package com.example.iter.payment.dto.toss;

public record TossConfirmApiRequest (String paymentKey, String orderId, long amount){
}
