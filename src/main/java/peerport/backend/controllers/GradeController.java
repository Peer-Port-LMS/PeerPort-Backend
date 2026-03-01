package peerport.backend.controllers;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import peerport.backend.dto.GradeDTO;
import peerport.backend.model.GradeModel;
import peerport.backend.service.GradeService;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PutMapping;



@RestController
@RequestMapping("/grades")
public class GradeController {
    protected static final Logger logger = LoggerFactory.getLogger(GradeController.class);

    @Autowired
    private GradeService gradeService;


    @GetMapping
    public ResponseEntity<List<GradeDTO>> getAllGrades() {
        logger.debug("Retrieving all grades from the database");

        // Get all the grades related to the user's submissions
        List<GradeModel> grades = gradeService.getAllGrades();

        // Convert to DTOs
        List<GradeDTO> gradeDTOs = new ArrayList<>();
        for (GradeModel model : grades) {
            gradeDTOs.add(model.toDTO());
        }

        // Return the DTOs
        logger.debug("Successfully retrieved {} grades from the database", gradeDTOs.size());
        return ResponseEntity.ok(gradeDTOs);
    }

    @GetMapping("/{gradeId}")
    public ResponseEntity<GradeDTO> getGradeById(@PathVariable String gradeId) {
        logger.debug("Attempting to retrieve grade with ID: {}", gradeId);

        // Get the grade from the database
        GradeModel grade = gradeService.getGradeById(gradeId);

        // Convert to DTO
        GradeDTO gradeDTO = grade.toDTO();

        // Return the DTO
        logger.debug("Successfully retrieved grade with ID: {}", gradeId);
        return ResponseEntity.ok(gradeDTO);
    }

    @PostMapping("/{submissionId}")
    public ResponseEntity<GradeDTO> createGrade(@RequestBody GradeModel grade, @PathVariable String submissionId) {
        logger.debug("Attempting to create a new grade with data: {}", grade);

        // Get the submission to link the grade to (for simplicity, we will just get the first submission)
        GradeModel createdGrade = gradeService.createGrade(grade, submissionId);

        // Convert the created grade back to DTO
        GradeDTO createdGradeDTO = createdGrade.toDTO();

        // Return the created grade DTO
        logger.debug("Successfully created a new grade with ID: {}", createdGradeDTO.gradeId);
        return ResponseEntity.ok(createdGradeDTO);
    }

    @PutMapping("/{gradeId}")
    public ResponseEntity<GradeDTO> updateGrade(@PathVariable String gradeId, @RequestBody GradeModel grade) {
        logger.debug("Attempting to update grade with ID: {} with data: {}", gradeId, grade);

        // Update the grade in the database
        GradeModel updatedGrade = gradeService.updateGrade(gradeId, grade);

        // Convert the updated grade back to DTO
        GradeDTO updatedGradeDTO = updatedGrade.toDTO();

        // Return the updated grade DTO
        logger.debug("Successfully updated grade with ID: {}", gradeId);
        return ResponseEntity.ok(updatedGradeDTO);
    }

    @DeleteMapping("/{gradeId}")
    public ResponseEntity<Void> deleteGrade(@PathVariable String gradeId) {
        logger.debug("Attempting to delete grade with ID: {}", gradeId);

        // Delete the grade from the database
        gradeService.deleteGrade(gradeId);

        // Return a success response
        logger.debug("Successfully deleted grade with ID: {}", gradeId);
        return ResponseEntity.noContent().build();
    }
}
