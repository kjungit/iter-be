package com.example.iter.auth.dto.response;

import com.example.iter.auth.domain.entity.UserAddress;

public record AddressResponse(
        Long id,
        String recipientName,
        String recipientPhone,
        String zipcode,
        String address,
        String detailAddress
) {
    public static AddressResponse from(UserAddress address) {
        return new AddressResponse(
                address.getId(),
                address.getRecipientName(),
                address.getRecipientPhone(),
                address.getZipcode(),
                address.getAddress(),
                address.getDetailAddress()
        );
    }
}
