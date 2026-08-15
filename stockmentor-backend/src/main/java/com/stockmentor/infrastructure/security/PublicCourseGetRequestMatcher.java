package com.stockmentor.infrastructure.security;

import jakarta.servlet.http.HttpServletRequest;
import java.util.regex.Pattern;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;

@Component
public class PublicCourseGetRequestMatcher implements RequestMatcher {

    private static final Pattern COURSE_PATH =
            Pattern.compile("^/api/v1/courses(?:/[^/]+)?$");
    private static final Pattern LESSON_PATH =
            Pattern.compile("^/api/v1/lessons/[^/]+$");
    private static final Pattern LESSON_QUIZ_PATH =
            Pattern.compile("^/api/v1/lessons/[^/]+/quiz$");

    @Override
    public boolean matches(HttpServletRequest request) {
        if (!HttpMethod.GET.matches(request.getMethod())) {
            return false;
        }
        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (!contextPath.isEmpty() && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }
        return COURSE_PATH.matcher(path).matches()
                || LESSON_PATH.matcher(path).matches()
                || LESSON_QUIZ_PATH.matcher(path).matches();
    }
}
