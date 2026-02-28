package peerport.backend.unit.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import peerport.backend.controllers.ContentController;
import peerport.backend.dto.content.ContentWithChildrenDTO;
import peerport.backend.exceptions.FailedToParseFormDataException;
import peerport.backend.model.ContentModel;
import peerport.backend.model.CourseModel;
import peerport.backend.service.ContentService;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
@DisplayName("ContentController Unit Tests")
class ContentControllerTest {

    @InjectMocks
    private ContentController controller;

    @Mock
    private ContentService contentService;

    @Mock
    private ObjectMapper objectMapper;

    @Test
    void getAllContent_returns200() {
        ContentWithChildrenDTO dto = new ContentWithChildrenDTO();
        dto.contentId = "content-1";
        when(contentService.getStructuredContent()).thenReturn(List.of(dto));

        ResponseEntity<List<ContentWithChildrenDTO>> response = controller.getAllContent();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void getContentById_returns200() {
        ContentModel content = buildContent("ct1");
        when(contentService.getContentById("ct1")).thenReturn(content);

        ResponseEntity<?> response = controller.getContentById("ct1");

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void createContent_returns201() {
        ContentModel content = buildContent("ct1");
        when(contentService.createContent(content, "c1")).thenReturn(content);

        ResponseEntity<?> response = controller.createContent("c1", content);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    @Test
    void createContentMultipart_whenJsonInvalid_throwsFailedToParse() throws IOException {
        JacksonException jacksonException = org.mockito.Mockito.mock(JacksonException.class);
        when(objectMapper.readValue("{bad}", ContentModel.class)).thenThrow(jacksonException);

        assertThrows(FailedToParseFormDataException.class, () ->
            controller.createContent(
                "c1",
                "{bad}",
                List.of(new MockMultipartFile("files", "a.txt", "text/plain", "x".getBytes()))
            )
        );
    }

    @Test
    void deleteContent_returns204() {
        ResponseEntity<Void> response = controller.deleteContent("ct1");

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(contentService).deleteContent("ct1");
    }

    private ContentModel buildContent(String contentId) {
        CourseModel course = new CourseModel("c1", "Course", "C1", true, null, new Date(), new Date());
        return new ContentModel(contentId, "Title", "Desc", true, new Date(), new Date(), course, null, List.of(), List.of());
    }
}
