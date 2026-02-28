package peerport.backend.unit.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
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

import peerport.backend.controllers.AnnouncementController;
import peerport.backend.exceptions.FailedToParseFormDataException;
import peerport.backend.model.AnnouncementModel;
import peerport.backend.service.AnnouncementService;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
@DisplayName("AnnouncementController Unit Tests")
class AnnouncementControllerTest {

    @InjectMocks
    private AnnouncementController controller;

    @Mock
    private AnnouncementService announcementService;

    @Mock
    private ObjectMapper objectMapper;

    @Test
    void getAllAnnouncements_returns200() {
        AnnouncementModel announcement = new AnnouncementModel("a1", "title", "content", null);
        when(announcementService.getAllAnnouncements()).thenReturn(List.of(announcement));

        ResponseEntity<?> response = controller.getAllAnnouncements();

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void getAnnouncementById_returns200() {
        AnnouncementModel announcement = new AnnouncementModel("a1", "title", "content", null);
        when(announcementService.getAnnouncementById("a1")).thenReturn(announcement);

        ResponseEntity<?> response = controller.getAnnouncementById("a1");

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void createAnnouncement_returns201() {
        AnnouncementModel request = new AnnouncementModel(null, "title", "content", null);
        AnnouncementModel saved = new AnnouncementModel("a1", "title", "content", null);
        when(announcementService.createAnnouncement("c1", request)).thenReturn(saved);

        ResponseEntity<?> response = controller.createAnnouncement("c1", request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    @Test
    void createAnnouncementMultipart_whenInvalidJson_throwsFailedToParse() throws IOException {
        JacksonException jacksonException = org.mockito.Mockito.mock(JacksonException.class);
        when(objectMapper.readValue("{bad}", AnnouncementModel.class)).thenThrow(jacksonException);

        assertThrows(FailedToParseFormDataException.class, () ->
            controller.createAnnouncement("c1", "{bad}", List.of(new MockMultipartFile("files", "a.txt", "text/plain", "x".getBytes())))
        );
    }

    @Test
    void deleteAnnouncement_returns204() {
        ResponseEntity<Void> response = controller.deleteAnnouncement("a1");

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(announcementService).deleteAnnouncement("a1");
    }
}
