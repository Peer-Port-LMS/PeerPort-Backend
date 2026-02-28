package peerport.backend.unit.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Path;
import jakarta.validation.Validator;
import peerport.backend.controllers.CourseController;
import peerport.backend.exceptions.FailedToParseFormDataException;
import peerport.backend.model.CourseModel;
import peerport.backend.service.CourseService;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
@DisplayName("CourseController Unit Tests")
class CourseControllerTest {

    @InjectMocks
    private CourseController controller;

    @Mock
    private CourseService courseService;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private Validator validator;

    @Test
    void getAllCourses_returns200() {
        CourseModel course = buildCourse("c1");
        when(courseService.getAllCourses()).thenReturn(List.of(course));

        ResponseEntity<?> response = controller.getAllCourses();

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void getCourseById_returns200() {
        CourseModel course = buildCourse("c1");
        when(courseService.getCourseById("c1")).thenReturn(course);

        ResponseEntity<?> response = controller.getCourseById("c1");

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void createCourse_returns201() throws IOException {
        CourseModel request = buildCourse(null);
        CourseModel saved = buildCourse("c1");
        MockMultipartFile image = new MockMultipartFile("image", "i.png", "image/png", new byte[] {1});

        when(objectMapper.readValue("{}", CourseModel.class)).thenReturn(request);
        when(validator.validate(any(CourseModel.class), any(), any())).thenReturn(Set.of());
        when(courseService.createCourse(request, image)).thenReturn(saved);

        ResponseEntity<?> response = controller.createCourse("{}", image);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    @Test
    void createCourse_whenJsonInvalid_throwsFailedToParse() throws IOException {
        JacksonException jacksonException = org.mockito.Mockito.mock(JacksonException.class);
        when(objectMapper.readValue("{bad}", CourseModel.class)).thenThrow(jacksonException);

        assertThrows(FailedToParseFormDataException.class, () ->
            controller.createCourse("{bad}", new MockMultipartFile("image", "i.png", "image/png", new byte[] {1}))
        );
    }

    @Test
    void createCourse_whenValidationFails_throwsFailedToParse() throws IOException {
        CourseModel request = buildCourse(null);
        @SuppressWarnings("unchecked")
        ConstraintViolation<CourseModel> violation = org.mockito.Mockito.mock(ConstraintViolation.class);
        Path path = org.mockito.Mockito.mock(Path.class);
        when(path.toString()).thenReturn("name");
        when(violation.getPropertyPath()).thenReturn(path);
        when(violation.getMessage()).thenReturn("is required");

        when(objectMapper.readValue("{}", CourseModel.class)).thenReturn(request);
        when(validator.validate(any(CourseModel.class), any(), any())).thenReturn(Set.of(violation));

        assertThrows(FailedToParseFormDataException.class, () ->
            controller.createCourse("{}", new MockMultipartFile("image", "i.png", "image/png", new byte[] {1}))
        );
    }

    @Test
    void deleteCourse_returns204() {
        ResponseEntity<Void> response = controller.deleteCourse("c1");

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(courseService).deleteCourse("c1");
    }

    private CourseModel buildCourse(String id) {
        return new CourseModel(id, "Course", "C1", true, "desc", new Date(), new Date());
    }
}
