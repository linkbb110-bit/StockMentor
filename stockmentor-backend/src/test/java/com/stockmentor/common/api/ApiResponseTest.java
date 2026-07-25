package com.stockmentor.common.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ApiResponseTest {

    @Test
    void successShouldContainSuccessCodeAndData() {
        ApiResponse<String> response = ApiResponse.success("ready");

        assertThat(response.code()).isEqualTo("SUCCESS");
        assertThat(response.message()).isEqualTo("操作成功");
        assertThat(response.data()).isEqualTo("ready");
    }

    @Test
    void successWithCustomMessageShouldContainProvidedMessageAndData() {
        ApiResponse<Integer> response = ApiResponse.success("已保存", 42);

        assertThat(response.code()).isEqualTo("SUCCESS");
        assertThat(response.message()).isEqualTo("已保存");
        assertThat(response.data()).isEqualTo(42);
    }

    @Test
    void failureShouldContainProvidedErrorAndNullData() {
        ApiResponse<Void> response =
            ApiResponse.failure("VALIDATION_FAILED", "请求参数不合法");

        assertThat(response.code()).isEqualTo("VALIDATION_FAILED");
        assertThat(response.message()).isEqualTo("请求参数不合法");
        assertThat(response.data()).isNull();
    }
}
