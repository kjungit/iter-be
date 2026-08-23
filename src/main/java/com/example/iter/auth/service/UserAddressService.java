package com.example.iter.auth.service;

import com.example.iter.auth.domain.entity.UserAddress;
import com.example.iter.auth.domain.repository.UserAddressRepository;
import com.example.iter.auth.domain.repository.UserRepository;
import com.example.iter.auth.dto.request.AddressUpdateRequest;
import com.example.iter.auth.dto.response.AddressResponse;
import com.example.iter.common.exception.CustomException;
import com.example.iter.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserAddressService {

    private final UserAddressRepository userAddressRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public AddressResponse getDefaultAddress(Long userId) {
        return AddressResponse.from(findDefaultAddress(userId));
    }

    @Transactional
    public AddressResponse updateDefaultAddress(Long userId, AddressUpdateRequest request) {
        // 같은 사용자의 기본 배송지 최초 생성 요청을 직렬화하여
        // is_default=true인 배송지가 중복 생성되는 것을 방지한다.
        userRepository.findWithLockById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        UserAddress address = userAddressRepository.findByUserIdAndDefaultAddressTrue(userId)
                .map(existing -> {
                    existing.update(
                            request.recipientName(),
                            request.recipientPhone(),
                            request.zipcode(),
                            request.address(),
                            request.detailAddress()
                    );
                    return existing;
                })
                .orElseGet(() -> userAddressRepository.save(UserAddress.builder()
                        .userId(userId)
                        .recipientName(request.recipientName())
                        .recipientPhone(request.recipientPhone())
                        .zipcode(request.zipcode())
                        .address(request.address())
                        .detailAddress(request.detailAddress())
                        .defaultAddress(true)
                        .build()));

        log.info("기본 배송지 변경 처리: userId={}, addressId={}", userId, address.getId());

        return AddressResponse.from(address);
    }

    private UserAddress findDefaultAddress(Long userId) {
        return userAddressRepository.findByUserIdAndDefaultAddressTrue(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.ADDRESS_NOT_FOUND));
    }
}
