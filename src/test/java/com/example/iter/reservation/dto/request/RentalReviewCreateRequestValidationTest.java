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

class RentalReviewCreateRequestValidationTest {

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
    void 평점과_내용이_모두_유효하면_통과한다() {
        var request = new RentalReviewCreateRequest(5, "정말 좋은 거래였습니다.");

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void 평점이_1보다_작으면_유효하지_않다() {
        var request = new RentalReviewCreateRequest(0, "내용");

        Set<ConstraintViolation<RentalReviewCreateRequest>> violations = validator.validate(request);

        assertThat(violations)
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("rating");
    }

    @Test
    void 평점이_5보다_크면_유효하지_않다() {
        var request = new RentalReviewCreateRequest(6, "내용");

        Set<ConstraintViolation<RentalReviewCreateRequest>> violations = validator.validate(request);

        assertThat(violations)
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("rating");
    }

    @Test
    void 평점이_없으면_유효하지_않다() {
        var request = new RentalReviewCreateRequest(null, "내용");

        Set<ConstraintViolation<RentalReviewCreateRequest>> violations = validator.validate(request);

        assertThat(violations)
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("rating");
    }

    @Test
    void 내용이_비어있으면_유효하지_않다() {
        var request = new RentalReviewCreateRequest(5, "   ");

        Set<ConstraintViolation<RentalReviewCreateRequest>> violations = validator.validate(request);

        assertThat(violations)
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("content");
    }

    @Test
    void 내용이_1000자를_초과하면_유효하지_않다() {
        var request = new RentalReviewCreateRequest(5, "가".repeat(1001));

        Set<ConstraintViolation<RentalReviewCreateRequest>> violations = validator.validate(request);

        assertThat(violations)
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("content");
    }
}
