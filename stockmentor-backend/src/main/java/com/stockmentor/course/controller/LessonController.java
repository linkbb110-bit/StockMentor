package com.stockmentor.course.controller;

import com.stockmentor.common.api.ApiResponse;
import com.stockmentor.course.service.CourseQueryService;
import com.stockmentor.course.vo.LessonDetailResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/lessons")
public class LessonController {

    private final CourseQueryService courseQueryService;

    public LessonController(CourseQueryService courseQueryService) {
        this.courseQueryService = courseQueryService;
    }

    @GetMapping("/{lessonId}")
    public ApiResponse<LessonDetailResponse> detail(@PathVariable long lessonId) {
        return ApiResponse.success(courseQueryService.getPublishedLesson(lessonId));
    }
}
