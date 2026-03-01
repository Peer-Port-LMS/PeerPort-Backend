package peerport.backend.unit.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import peerport.backend.database.GradesRepository;
import peerport.backend.exceptions.grades.GradeNotFoundException;
import peerport.backend.model.AssignmentSubmissionModel;
import peerport.backend.model.GradeModel;
import peerport.backend.service.AssignmentSubmissionService;
import peerport.backend.service.GradeService;

@ExtendWith(MockitoExtension.class)
@DisplayName("GradeService Unit Tests")
class GradeServiceTest {

    @InjectMocks
    private GradeService gradeService;

    @Mock
    private GradesRepository gradeRepository;

    @Mock
    private AssignmentSubmissionService assignmentSubmissionService;

    @Test
    @DisplayName("getAllGrades returns all repository grades")
    void getAllGrades_returnsAll() {
        List<GradeModel> expected = List.of(new GradeModel(), new GradeModel());
        when(gradeRepository.findAll()).thenReturn(expected);

        List<GradeModel> result = gradeService.getAllGrades();

        assertEquals(2, result.size());
        assertSame(expected, result);
        verify(gradeRepository).findAll();
    }

    @Test
    @DisplayName("getGradeById returns grade when found")
    void getGradeById_found_returnsGrade() {
        GradeModel grade = new GradeModel("g1", 90, 100, "Great work");
        when(gradeRepository.findById("g1")).thenReturn(Optional.of(grade));

        GradeModel result = gradeService.getGradeById("g1");

        assertSame(grade, result);
    }

    @Test
    @DisplayName("getGradeById throws when grade missing")
    void getGradeById_missing_throws() {
        when(gradeRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(GradeNotFoundException.class, () -> gradeService.getGradeById("missing"));
    }

    @Test
    @DisplayName("createGrade links submission and saves grade")
    void createGrade_linksSubmissionAndSaves() {
        GradeModel grade = new GradeModel("g1", 88, 100, "Nice attempt");
        AssignmentSubmissionModel submission = new AssignmentSubmissionModel();

        when(assignmentSubmissionService.getSubmissionById("s1")).thenReturn(submission);
        when(gradeRepository.save(grade)).thenReturn(grade);

        GradeModel result = gradeService.createGrade(grade, "s1");

        assertSame(grade, result);
        assertSame(grade, ReflectionTestUtils.getField(submission, "grade"));
        verify(assignmentSubmissionService).getSubmissionById("s1");
        verify(gradeRepository).save(grade);
    }

    @Test
    @DisplayName("updateGrade updates mutable fields and saves")
    void updateGrade_updatesFieldsAndSaves() {
        GradeModel existing = new GradeModel("g1", 60, 100, "Initial");
        GradeModel updates = new GradeModel("ignored", 95, 100, "Improved");

        when(gradeRepository.findById("g1")).thenReturn(Optional.of(existing));
        when(gradeRepository.save(existing)).thenReturn(existing);

        GradeModel result = gradeService.updateGrade("g1", updates);

        assertEquals(95, result.getGradeObtained());
        assertEquals(100, result.getMaxGrade());
        assertEquals("Improved", result.getFeedback());
        verify(gradeRepository).save(existing);
    }

    @Test
    @DisplayName("deleteGrade removes grade when it exists")
    void deleteGrade_exists_deletesAndReturnsTrue() {
        GradeModel existing = new GradeModel("g1", 90, 100, "Great work");
        when(gradeRepository.findById("g1")).thenReturn(Optional.of(existing));

        boolean result = gradeService.deleteGrade("g1");

        assertTrue(result);
        verify(gradeRepository).delete(existing);
    }

    @Test
    @DisplayName("deleteGrade throws when grade missing")
    void deleteGrade_missing_throws() {
        when(gradeRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(GradeNotFoundException.class, () -> gradeService.deleteGrade("missing"));
    }
}
