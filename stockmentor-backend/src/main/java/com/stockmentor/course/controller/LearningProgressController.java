package com.stockmentor.course.controller;

import com.stockmentor.common.api.ApiResponse;
import com.stockmentor.course.service.LearningProgressService;
import com.stockmentor.course.vo.CourseProgressResponse;
import com.stockmentor.course.vo.LessonCompletionResponse;
import com.stockmentor.infrastructure.security.AuthenticatedUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me")
public class LearningProgressController {

    private final LearningProgressService learningProgressService;

    public LearningProgressController(
            LearningProgressService learningProgressService
    ) {
        this.learningProgressService = learningProgressService;
    }

    @PutMapping("/lessons/{lessonId}/completion")
    public ApiResponse<LessonCompletionResponse> completeLesson(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable long lessonId
    ) {
        return ApiResponse.success(
                learningProgressService.completeLesson(
                        currentUser.userId(),
                        lessonId
                )
        );
    }

    @GetMapping("/courses/{courseId}/progress")
    public ApiResponse<CourseProgressResponse> getCourseProgress(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable long courseId
    ) {
        return ApiResponse.success(
                learningProgressService.getCourseProgress(
                        currentUser.userId(),
                        courseId
                )
        );
    }
}
