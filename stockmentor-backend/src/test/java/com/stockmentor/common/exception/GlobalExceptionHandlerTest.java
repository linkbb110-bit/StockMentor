package com.stockmentor.common.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.stockmentor.common.api.ApiResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotBlank;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

class GlobalExceptionHandlerTest {
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ExceptionTestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void errorCodesMapToTheirSpecifiedHttpStatuses() {
        assertThat(ErrorCode.VALIDATION_FAILED.httpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ErrorCode.RESOURCE_NOT_FOUND.httpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(ErrorCode.CONFLICT.httpStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(ErrorCode.INTERNAL_ERROR.httpStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void businessExceptionPreservesTheSelectedErrorCode() {
        BusinessException exception = new BusinessException(ErrorCode.CONFLICT);

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CONFLICT);
        assertThat(exception).hasMessage("资源冲突");
    }

    @Test
    void businessExceptionCanUseAnExplicitSafeMessage() {
        BusinessException exception = new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "课程不存在");

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
        assertThat(exception).hasMessage("课程不存在");
    }

    @Test
    void businessExceptionProducesItsHttpStatusAndApiResponse() throws Exception {
        mockMvc.perform(get("/business"))
                .andExpect(status().isConflict())
                .andExpect(content().json("""
                        {"code":"CONFLICT","message":"资源冲突","data":null}
                        """));
    }

    @Test
    void unsafeBusinessExceptionMessageFallsBackToTheErrorCodeMessage() throws Exception {
        mockMvc.perform(get("/unsafe-business"))
                .andExpect(status().isNotFound())
                .andExpect(content().json("""
                        {"code":"RESOURCE_NOT_FOUND","message":"资源不存在","data":null}
                        """));
    }

    @Test
    void databaseBusinessExceptionMessageFallsBackToTheErrorCodeMessage() throws Exception {
        mockMvc.perform(get("/unsafe-database-business"))
                .andExpect(status().isNotFound())
                .andExpect(content().json("""
                        {"code":"RESOURCE_NOT_FOUND","message":"资源不存在","data":null}
                        """));
    }

    @Test
    void invalidRequestBodyProducesValidationFailedApiResponse() throws Exception {
        mockMvc.perform(post("/validated")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("""
                        {"code":"VALIDATION_FAILED","message":"请求参数不合法","data":null}
                        """));
    }

    @Test
    void constraintViolationProducesValidationFailedApiResponse() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        Set<ConstraintViolation<ConstraintTarget>> violations = validator.validate(new ConstraintTarget(""));

        ResponseEntity<ApiResponse<Void>> response = new GlobalExceptionHandler()
                .handleConstraintViolationException(new ConstraintViolationException(violations));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo(ApiResponse.failure("VALIDATION_FAILED", "请求参数不合法"));
    }

    @Test
    void unreadableJsonProducesValidationFailedApiResponse() throws Exception {
        mockMvc.perform(post("/validated")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":"))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("""
                        {"code":"VALIDATION_FAILED","message":"请求参数不合法","data":null}
                        """));
    }

    @Test
    void unexpectedExceptionReturnsInternalErrorWithoutLeakingItsMessage() throws Exception {
        mockMvc.perform(get("/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().json("""
                        {"code":"INTERNAL_ERROR","message":"服务器内部错误","data":null}
                        """));
    }

    private record ValidationRequest(@NotBlank String name) {
    }

    private record ConstraintTarget(@NotBlank String value) {
    }

    @RestController
    private static class ExceptionTestController {
        @GetMapping("/business")
        void business() {
            throw new BusinessException(ErrorCode.CONFLICT);
        }

        @GetMapping("/unsafe-business")
        void unsafeBusiness() {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND,
                    "java.sql.SQLException: access denied\n\tat com.mysql.cj.jdbc.ClientPreparedStatement.execute");
        }

        @GetMapping("/unsafe-database-business")
        void unsafeDatabaseBusiness() {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND,
                    "Duplicate entry 'alice@example.com' for key 'sys_user.email'");
        }

        @PostMapping("/validated")
        void validated(@Valid @RequestBody ValidationRequest request) {
        }

        @GetMapping("/unexpected")
        void unexpected() {
            throw new IllegalStateException("database credentials must not be disclosed");
        }
    }
}
