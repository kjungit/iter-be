package com.example.iter.reservation.dto.request;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ReturnConfirmationRequestValidationTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidatorFactory() {
        validatorFactory.close();
    }

    @Test
    void 정상_반납은_분쟁_입력_없이_유효하다() {
        var request = new ReturnConfirmationRequest(false, null, null);

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void 정상_반납의_빈_문자열은_입력하지_않은_것으로_처리한다() {
        var request = new ReturnConfirmationRequest(false, "   ", "   ");

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void 정상_반납인데_분쟁_내용이_있으면_유효하지_않다() {
        var request = new ReturnConfirmationRequest(false, "파손", "파손 설명");

        assertDisputeInputViolation(request);
    }

    @Test
    void 비정상_반납은_분쟁_사유와_설명이_모두_있으면_유효하다() {
        var request = new ReturnConfirmationRequest(true, "파손", "모서리가 파손되었습니다.");

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void 비정상_반납인데_분쟁_사유가_없으면_유효하지_않다() {
        var request = new ReturnConfirmationRequest(true, null, "모서리가 파손되었습니다.");

        assertDisputeInputViolation(request);
    }

    @Test
    void 비정상_반납인데_분쟁_설명이_공백이면_유효하지_않다() {
        var request = new ReturnConfirmationRequest(true, "파손", "   ");

        assertDisputeInputViolation(request);
    }

    @Test
    void 상품_이상_여부가_null이면_NotNull_검증에_실패한다() {
        var request = new ReturnConfirmationRequest(null, null, null);

        Set<ConstraintViolation<ReturnConfirmationRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("hasIssue");
    }

    @Test
    void 분쟁_사유는_50자까지_허용한다() {
        var request = new ReturnConfirmationRequest(true, "가".repeat(50), "설명");

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void 분쟁_사유가_50자를_초과하면_유효하지_않다() {
        var request = new ReturnConfirmationRequest(true, "가".repeat(51), "설명");

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("disputeReason");
    }

    @Test
    void 분쟁_설명은_2000자까지_허용한다() {
        var request = new ReturnConfirmationRequest(true, "파손", "가".repeat(2000));

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void 분쟁_설명이_2000자를_초과하면_유효하지_않다() {
        var request = new ReturnConfirmationRequest(true, "파손", "가".repeat(2001));

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("disputeDescription");
    }

    private void assertDisputeInputViolation(ReturnConfirmationRequest request) {
        assertThat(validator.validate(request))
                .extracting(violation -> violation.getMessage())
                .contains("상품에 이상이 있으면 분쟁 사유와 설명이 필요합니다.");
    }
}
