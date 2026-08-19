package com.example.iter.device.service;

import com.example.iter.common.exception.CustomException;
import com.example.iter.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EquipmentImagePolicyTest {

    private final EquipmentImagePolicy policy = new EquipmentImagePolicy();

    @Test
    void JPEG_PNG_WEBP_시그니처가_콘텐츠_타입과_일치하면_허용한다() {
        assertThatCode(() -> policy.validateSignature("image/jpeg",
                new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}))
                .doesNotThrowAnyException();
        assertThatCode(() -> policy.validateSignature("image/png",
                new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A}))
                .doesNotThrowAnyException();
        assertThatCode(() -> policy.validateSignature("image/webp",
                new byte[]{'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P'}))
                .doesNotThrowAnyException();
    }

    @Test
    void 콘텐츠_타입_크기와_실제_시그니처를_검증한다() {
        assertInvalid(() -> policy.validateMetadata("application/pdf", 100));
        assertInvalid(() -> policy.validateMetadata("image/jpeg", 0));
        assertInvalid(() -> policy.validateMetadata(
                "image/jpeg", EquipmentImagePolicy.MAX_IMAGE_SIZE + 1));
        assertInvalid(() -> policy.validateSignature(
                "image/jpeg", new byte[]{'N', 'O', 'T'}));
    }

    private void assertInvalid(org.assertj.core.api.ThrowableAssert.ThrowingCallable callable) {
        assertThatThrownBy(callable)
                .isInstanceOf(CustomException.class)
                .extracting(exception -> ((CustomException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_IMAGE);
    }
}
