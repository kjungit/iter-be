package com.example.iter.common.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.annotation.Validated;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// @Valid(@RequestBody)와 @RequestParam 검증 실패가 실제로 구조화된 ErrorResponse.errors로
// 내려오는지 end-to-end로 확인한다 — 목(mock)으로 만든 예외가 아니라 진짜 Bean Validation을 태운다.
//
// @RequestParam의 @Max 같은 메서드 파라미터 검증은 원래 Spring이 @Validated 빈을 AOP 프록시로 감싸서
// (MethodValidationPostProcessor) 동작하는데, standaloneSetup은 BeanPostProcessor를 안 태우므로
// 그 프록시를 직접 만들어서 컨트롤러 대신 등록해야 실제로 ConstraintViolationException이 발생한다.
class GlobalExceptionHandlerTest {

    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(validatedProxy(new TestController()))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static Object validatedProxy(Object target) {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        MethodValidationPostProcessor processor = new MethodValidationPostProcessor();
        processor.setValidator(validator);
        processor.afterPropertiesSet();
        return processor.postProcessAfterInitialization(target, "testController");
    }

    @Test
    void RequestBody_검증_실패시_필드별_constraint와_params가_내려온다() throws Exception {
        TestRequest invalid = new TestRequest("", "this-is-way-too-long-for-the-limit");

        mockMvc.perform(post("/test/body")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors[?(@.field=='name')].constraint").value("NotBlank"))
                .andExpect(jsonPath("$.errors[?(@.field=='nickname')].constraint").value("Size"))
                .andExpect(jsonPath("$.errors[?(@.field=='nickname')].params.max").value(10));
    }

    @Test
    void RequestParam_검증_실패시_constraint와_params가_내려온다() throws Exception {
        mockMvc.perform(get("/test/param").param("size", "999"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].constraint").value("Max"))
                .andExpect(jsonPath("$.errors[0].params.value").value(100));
    }

    private record TestRequest(
            @NotBlank String name,
            @Size(max = 10) String nickname
    ) {
    }

    @RestController
    @Validated
    static class TestController {

        @PostMapping("/test/body")
        public void body(@Valid @RequestBody TestRequest request) {
        }

        @GetMapping("/test/param")
        public void param(@RequestParam @Max(100) int size) {
        }
    }
}
