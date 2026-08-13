package com.stockmentor.course.controller;

import com.stockmentor.common.api.ApiResponse;
import com.stockmentor.course.service.CourseQueryService;
import com.stockmentor.course.vo.CourseDetailResponse;
import com.stockmentor.course.vo.CourseSummaryResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/courses")
public class CourseController {

    private final CourseQueryService courseQueryService;

    public CourseController(CourseQueryService courseQueryService) {
        this.courseQueryService = courseQueryService;
    }

    @GetMapping
    public ApiResponse<List<CourseSummaryResponse>> list() {
        return ApiResponse.success(courseQueryService.listPublishedCourses());
    }

    @GetMapping("/{courseId}")
    public ApiResponse<CourseDetailResponse> detail(@PathVariable long courseId) {
        return ApiResponse.success(courseQueryService.getPublishedCourse(courseId));
    }
}
