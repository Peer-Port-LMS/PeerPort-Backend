package peerport.backend.controllers;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import peerport.backend.dto.assignments.AssignmentDTO;
import peerport.backend.dto.assignments.AssignmentWithCourseDTO;
import peerport.backend.exceptions.FailedToParseFormDataException;
import peerport.backend.model.AssignmentModel;
import peerport.backend.service.AssignmentService;
import tools.jackson.databind.ObjectMapper;

/**
 * Controller for handling assignment-related endpoints
 */
@RestController
@RequestMapping("/assignments")
public class AssignmentController {
    protected static final Logger logger = LoggerFactory.getLogger(AssignmentController.class);
    
	@Autowired
	private AssignmentService assignmentService;

	@Autowired
	private ObjectMapper objectMapper;

	/**
	 * Get all assignments.
	 * 
	 * @return List of AssignmentWithCourseDTO
	 * @throws UserNotAuthenticatedException if the user is not authenticated to perform this action.
	 */
	@GetMapping
	public ResponseEntity<List<AssignmentWithCourseDTO>> getAllAssignments() {
		logger.debug("Retrieving all assignments");

		// Get all assignments
		List<AssignmentModel> assignments = assignmentService.getAllAssignments();

		// Convert to DTOs
		List<AssignmentWithCourseDTO> assignmentDTOs = assignments.stream()
				.map(AssignmentModel::toAssignmentWithCourseDTO)
				.toList();
		
		// Return the list of DTOs
		logger.debug("Successfully retrieved {} assignments", assignmentDTOs.size());
		return ResponseEntity.ok(assignmentDTOs);
	}

	/**
	 * Get an assignment by its ID.
	 * 
	 * @param assignmentId The ID of the assignment to retrieve.
	 * @return The assignment with the given ID.
	 * @throws AssignmentNotFoundException if the assignment with the given ID does not exist.
	 * @throws UserNotAuthenticatedException if the user is not authenticated to perform this action.
	 * @throws UserNotAuthorizedException if the user is not authorized to view this assignment.
	 */
	@GetMapping("/{assignmentId}")
	public ResponseEntity<AssignmentDTO> getAssignmentById(@PathVariable String assignmentId) {
		logger.debug("Retrieving assignment with ID: {}", assignmentId);

		// Get assignment by ID
		AssignmentModel assignment = assignmentService.getAssignmentById(assignmentId);

		// Convert assignment to DTO and return
		logger.debug("Successfully retrieved assignment with ID: {}", assignmentId);
		return ResponseEntity.ok(assignment.toDTO());
	}

	/**
	 * Create a new assignment for a course.
	 * 
	 * @param courseId The ID of the course to which the assignment belongs.
	 * @param assignment The assignment data to create.
	 * @return The created assignment.
	 * @throws CourseNotFoundException if the course with the given ID does not exist.
	 * @throws UserNotAuthenticatedException if the user is not authenticated to perform this action.
	 * @throws UserNotAuthorizedException if the user is not authorized to create an assignment for this course.
	 */
	@PostMapping("/{courseId}")
	@PreAuthorize("@authservice.hasAnyRole(@authservice.ADMIN, @authservice.INSTRUCTOR)")
	public ResponseEntity<AssignmentDTO> createAssignment(@PathVariable String courseId, @Validated @RequestBody AssignmentModel assignment) {
		logger.debug("Creating assignment for course ID: {}", courseId);

		// Try to create the assignment
		AssignmentModel savedAssignment = assignmentService.createAssignment(assignment, courseId);

		// Return the created assignment with 201 status
		logger.debug("Successfully created assignment with ID: {} for course ID: {}", savedAssignment.getAssignmentId(), courseId);
		return ResponseEntity.status(HttpStatus.CREATED).body(savedAssignment.toDTO());
	}

	/**
	 * Create a new assignment for a course with file attachments.
	 * 
	 * @param courseId The ID of the course to which the assignment belongs.
	 * @param assignmentJson JSON string of assignment data.
	 * @param files List of files to attach to the assignment.
	 * @return The created assignment.
	 * @throws IOException If there was an error processing files.
	 * @throws FileSizeLimitExceededException if any file exceeds size limit.
	 * @throws CourseNotFoundException if the course with the given ID does not exist.
	 * @throws UserNotAuthenticatedException if the user is not authenticated to perform this action.
	 * @throws UserNotAuthorizedException if the user is not authorized to create an assignment for this course.
	 */
	@PostMapping(value = "/{courseId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@PreAuthorize("@authservice.hasAnyRole(@authservice.ADMIN, @authservice.INSTRUCTOR)")
	public ResponseEntity<AssignmentDTO> createAssignmentWithFiles(
			@PathVariable String courseId,
			@RequestPart("assignment") String assignmentJson,
			@RequestPart(value = "files", required = false) List<MultipartFile> files
	) throws IOException {
		logger.debug("Creating assignment for course ID: {} with multipart/form-data", courseId);

		// Parse JSON to AssignmentModel
		AssignmentModel assignment;
		try {
			assignment = objectMapper.readValue(assignmentJson, AssignmentModel.class);
		} catch (Exception e) {
			throw new FailedToParseFormDataException("Failed to parse assignment JSON: " + e.getMessage());
		}
		
		// Validate assignment
		assignmentService.validateAssignment(assignment);
		
		// Create the assignment with files
		AssignmentModel savedAssignment = assignmentService.createAssignment(assignment, courseId, files);
		
		// Return the created assignment with 201 status
		logger.debug("Successfully created assignment with ID: {} for course ID: {}", savedAssignment.getAssignmentId(), courseId);
		return ResponseEntity.status(HttpStatus.CREATED).body(savedAssignment.toDTO());
	}

	/**
	 * Update an assignment by its ID.
	 * 
	 * @param assignmentId The ID of the assignment to update.
	 * @param assignment The assignment data to update.
	 * @return The updated assignment.
	 * @throws AssignmentNotFoundException if the assignment with the given ID does not exist.
	 * @throws UserNotAuthenticatedException if the user is not authenticated to perform this action.
	 * @throws UserNotAuthorizedException if the user is not authorized to update this assignment.
	 */
	@PutMapping("/{assignmentId}")
	@PreAuthorize("@authservice.hasAnyRole(@authservice.ADMIN, @authservice.INSTRUCTOR)")
	public ResponseEntity<AssignmentDTO> updateAssignment(@PathVariable String assignmentId, @RequestBody AssignmentModel assignment) {
		logger.debug("Updating assignment with ID: {}", assignmentId);

		// Update the assignment
		AssignmentModel updatedAssignment = assignmentService.updateAssignment(assignmentId, assignment);

		// Return the updated assignment as DTO
		logger.debug("Successfully updated assignment with ID: {}", assignmentId);
		return ResponseEntity.ok(updatedAssignment.toDTO());
	}

	/**
	 * Update an assignment by its ID with file changes.
	 * 
	 * @param assignmentId The ID of the assignment to update.
	 * @param assignmentJson JSON string of assignment data.
	 * @param files List of files to attach to the assignment.
	 * @param removeFileIds List of file IDs to remove.
	 * @param replaceAll Whether to replace all existing files.
	 * @return The updated assignment.
	 * @throws IOException If there was an error processing files.
	 * @throws FileSizeLimitExceededException if any file exceeds size limit.
	 * @throws AssignmentNotFoundException if the assignment with the given ID does not exist.
	 * @throws UserNotAuthenticatedException if the user is not authenticated to perform this action.
	 * @throws UserNotAuthorizedException if the user is not authorized to update this assignment.
	 */
	@PutMapping(value = "/{assignmentId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@PreAuthorize("@authservice.hasAnyRole(@authservice.ADMIN, @authservice.INSTRUCTOR)")
	public ResponseEntity<AssignmentDTO> updateAssignmentWithFiles(
			@PathVariable String assignmentId,
			@RequestPart("assignment") String assignmentJson,
			@RequestPart(value = "files", required = false) List<MultipartFile> files,
			@RequestParam(value = "removeFileIds", required = false) List<String> removeFileIds,
			@RequestParam(value = "replaceAll", required = false, defaultValue = "false") Boolean replaceAll
	) throws IOException {
		logger.debug("Updating assignment with ID: {} with multipart/form-data", assignmentId);

		// Parse JSON to AssignmentModel
		AssignmentModel assignment;
		try {
			assignment = objectMapper.readValue(assignmentJson, AssignmentModel.class);
		} catch (Exception e) {
			throw new FailedToParseFormDataException("Failed to parse assignment JSON: " + e.getMessage());
		}
		
		// Validate assignment
		assignmentService.validateAssignment(assignment);
		
		// Update the assignment with file changes
		AssignmentModel updatedAssignment = assignmentService.updateAssignment(assignmentId, assignment, files, removeFileIds, replaceAll);
		
		// Return the updated assignment as DTO
		logger.debug("Successfully updated assignment with ID: {}", assignmentId);
		return ResponseEntity.ok(updatedAssignment.toDTO());
	}

	/**
	 * Patch an assignment by its ID.
	 * 
	 * @param assignmentId The ID of the assignment to patch.
	 * @param assignment The assignment data to patch.
	 * @return The patched assignment.
	 * @throws AssignmentNotFoundException if the assignment with the given ID does not exist.
	 * @throws UserNotAuthenticatedException if the user is not authenticated to perform this action.
	 * @throws UserNotAuthorizedException if the user is not authorized to patch this assignment.
	 */
	@PatchMapping("/{assignmentId}")
	@PreAuthorize("@authservice.hasAnyRole(@authservice.ADMIN, @authservice.INSTRUCTOR)")
	public ResponseEntity<AssignmentDTO> patchAssignment(@PathVariable String assignmentId, @RequestBody AssignmentModel assignment) {
		logger.debug("Patching assignment with ID: {}", assignmentId);

		// Patch the assignment
		AssignmentModel patchedAssignment = assignmentService.patchAssignment(assignmentId, assignment);

		// Return the patched assignment as DTO
		logger.debug("Successfully patched assignment with ID: {}", assignmentId);
		return ResponseEntity.ok(patchedAssignment.toDTO());
	}

	/**
	 * Patch an assignment by its ID with file changes.
	 * 
	 * @param assignmentId The ID of the assignment to patch.
	 * @param assignmentJson JSON string of assignment fields to update.
	 * @param files List of files to attach to the assignment.
	 * @param removeFileIds List of file IDs to remove.
	 * @param replaceAll Whether to replace all existing files.
	 * @return The patched assignment.
	 * @throws IOException If there was an error processing files.
	 * @throws FileSizeLimitExceededException if any file exceeds size limit.
	 * @throws AssignmentNotFoundException if the assignment with the given ID does not exist.
	 * @throws UserNotAuthenticatedException if the user is not authenticated to perform this action.
	 * @throws UserNotAuthorizedException if the user is not authorized to patch this assignment.
	 */
	@PatchMapping(value = "/{assignmentId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@PreAuthorize("@authservice.hasAnyRole(@authservice.ADMIN, @authservice.INSTRUCTOR)")
	public ResponseEntity<AssignmentDTO> patchAssignmentWithFiles(
			@PathVariable String assignmentId,
			@RequestPart("assignment") String assignmentJson,
			@RequestPart(value = "files", required = false) List<MultipartFile> files,
			@RequestParam(value = "removeFileIds", required = false) List<String> removeFileIds,
			@RequestParam(value = "replaceAll", required = false, defaultValue = "false") Boolean replaceAll
	) throws IOException {
		logger.debug("Patching assignment with ID: {} with multipart/form-data", assignmentId);

		// Parse JSON to AssignmentModel
		AssignmentModel assignment;
		try {
			assignment = objectMapper.readValue(assignmentJson, AssignmentModel.class);
		} catch (Exception e) {
			throw new FailedToParseFormDataException("Failed to parse assignment JSON: " + e.getMessage());
		}
		
		// Note: For PATCH, we don't validate since fields are optional
		
		// Patch the assignment with file changes
		AssignmentModel patchedAssignment = assignmentService.patchAssignment(assignmentId, assignment, files, removeFileIds, replaceAll);
		
		// Return the patched assignment as DTO
		logger.debug("Successfully patched assignment with ID: {}", assignmentId);
		return ResponseEntity.ok(patchedAssignment.toDTO());
	}

	/**
	 * Delete an assignment by its ID.
	 * @param assignmentId The ID of the assignment to delete.
	 * @return A ResponseEntity with no content status if deletion is successful.
	 * @throws AssignmentNotFoundException if the assignment with the given ID does not exist.
	 * @throws UserNotAuthenticatedException if the user is not authenticated to perform this action.
	 * @throws UserNotAuthorizedException if the user is not authorized to delete this assignment.
	 */
	@DeleteMapping("/{assignmentId}")
	@PreAuthorize("@authservice.hasAnyRole(@authservice.ADMIN, @authservice.INSTRUCTOR)")
	public ResponseEntity<Void> deleteAssignment(@PathVariable String assignmentId) {
		logger.debug("Deleting assignment with ID: {}", assignmentId);
		
		// Delete the assignment
		assignmentService.deleteAssignment(assignmentId);
		
		// Return no content status
		logger.debug("Successfully deleted assignment with ID: {}", assignmentId);
		return ResponseEntity.noContent().build();
	}
}
