package com.example.iter.auth.service;

import com.example.iter.auth.domain.entity.User;
import com.example.iter.auth.domain.entity.UserAddress;
import com.example.iter.auth.domain.repository.UserAddressRepository;
import com.example.iter.auth.domain.repository.UserRepository;
import com.example.iter.auth.dto.request.AddressUpdateRequest;
import com.example.iter.auth.dto.response.AddressResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@ActiveProfiles("test")
@SpringBootTest
class UserAddressConcurrencyTest {

    @Autowired
    private UserAddressService userAddressService;

    @Autowired
    private UserAddressRepository userAddressRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    @AfterEach
    void cleanUp() {
        userAddressRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void concurrentFirstUpdatesCreateExactlyOneDefaultAddress() throws Exception {
        User user = userRepository.saveAndFlush(User.builder()
                .email("address-concurrent@example.com")
                .password("encoded-password")
                .name("동시성테스트")
                .nickname("테스터")
                .phone("010-0000-0000")
                .build());

        AddressUpdateRequest firstRequest = request(
                "첫번째수령인",
                "010-1111-1111",
                "11111",
                "서울특별시 강남구 첫번째로 1",
                "101호"
        );
        AddressUpdateRequest secondRequest = request(
                "두번째수령인",
                "010-2222-2222",
                "22222",
                "서울특별시 강남구 두번째로 2",
                "202호"
        );

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try {
            List<Future<AddressResponse>> futures = List.of(
                    executor.submit(() -> updateAfterSignal(user.getId(), firstRequest, ready, start)),
                    executor.submit(() -> updateAfterSignal(user.getId(), secondRequest, ready, start))
            );

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            List<AddressResponse> responses = futures.stream()
                    .map(this::getWithinTimeout)
                    .toList();

            assertThat(responses)
                    .extracting(AddressResponse::id)
                    .doesNotContainNull();
            assertThat(responses.stream().map(AddressResponse::id).distinct().count())
                    .isEqualTo(1);
            assertThat(userAddressRepository.countByUserIdAndDefaultAddressTrue(user.getId()))
                    .isEqualTo(1);

            UserAddress saved = userAddressRepository.findByUserIdAndDefaultAddressTrue(user.getId())
                    .orElseThrow();
            assertThat(List.of(saved))
                    .extracting(
                            UserAddress::getRecipientName,
                            UserAddress::getRecipientPhone,
                            UserAddress::getZipcode,
                            UserAddress::getAddress,
                            UserAddress::getDetailAddress
                    )
                    .containsAnyOf(
                            tuple(
                                    firstRequest.recipientName(),
                                    firstRequest.recipientPhone(),
                                    firstRequest.zipcode(),
                                    firstRequest.address(),
                                    firstRequest.detailAddress()
                            ),
                            tuple(
                                    secondRequest.recipientName(),
                                    secondRequest.recipientPhone(),
                                    secondRequest.zipcode(),
                                    secondRequest.address(),
                                    secondRequest.detailAddress()
                            )
                    );
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }

    private AddressResponse updateAfterSignal(
            Long userId,
            AddressUpdateRequest request,
            CountDownLatch ready,
            CountDownLatch start
    ) throws InterruptedException {
        ready.countDown();
        start.await();
        return userAddressService.updateDefaultAddress(userId, request);
    }

    private AddressResponse getWithinTimeout(Future<AddressResponse> future) {
        try {
            return future.get(10, TimeUnit.SECONDS);
        } catch (Exception exception) {
            throw new AssertionError("동시 배송지 수정 요청이 제한 시간 안에 완료되지 않았습니다.", exception);
        }
    }

    private AddressUpdateRequest request(
            String recipientName,
            String recipientPhone,
            String zipcode,
            String address,
            String detailAddress
    ) {
        return new AddressUpdateRequest(
                recipientName,
                recipientPhone,
                zipcode,
                address,
                detailAddress
        );
    }
}
