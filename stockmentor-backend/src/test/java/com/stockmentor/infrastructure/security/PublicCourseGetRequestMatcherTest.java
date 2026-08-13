package com.stockmentor.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;

class PublicCourseGetRequestMatcherTest {

    private final PublicCourseGetRequestMatcher matcher =
            new PublicCourseGetRequestMatcher();

    @ParameterizedTest
    @ValueSource(strings = {
        "/api/v1/courses",
        "/api/v1/courses/7",
        "/api/v1/lessons/101"
    })
    void matchesOnlyTheApprovedPublicGetShapes(String path) {
        assertThat(matcher.matches(request("GET", path))).isTrue();
    }

    @ParameterizedTest
    @CsvSource({
        "POST,/api/v1/courses",
        "PUT,/api/v1/courses/7",
        "GET,/api/v1/courses/7/chapters",
        "GET,/api/v1/lessons/101/completion",
        "GET,/api/v1/me/courses/7/progress",
        "GET,/api/v1/users/me",
        "GET,/api/v1/system/health"
    })
    void rejectsOtherMethodsNestedPathsAndPrivateEndpoints(String method, String path) {
        assertThat(matcher.matches(request(method, path))).isFalse();
    }

    private MockHttpServletRequest request(String method, String path) {
        return new MockHttpServletRequest(method, path);
    }
}
