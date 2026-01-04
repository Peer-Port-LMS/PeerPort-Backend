package peerport.backend.exceptions;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import peerport.backend.dto.ErrorDTO;
import peerport.backend.exceptions.assignments.AssignmentNotFoundException;
import peerport.backend.exceptions.announcements.AnnouncementNotFoundException;
import peerport.backend.exceptions.courses.CourseNotFoundException;
import peerport.backend.exceptions.files.FileSizeLimitExceededException;
import peerport.backend.exceptions.files.InvalidFileTypeException;
import peerport.backend.exceptions.users.UserNotAuthenticatedException;
import peerport.backend.exceptions.users.UserNotAuthorizedException;
import peerport.backend.exceptions.users.UserNotFoundException;

@ControllerAdvice
public class GlobalExceptionHandler {

    // Java exceptions
    @ExceptionHandler(IOException.class)
    public ResponseEntity<ErrorDTO> handleIOException(IOException ex, WebRequest request) {
        ErrorDTO errorDTO = new ErrorDTO(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "Internal server error: " + ex.getMessage(),
            request.getDescription(false).replace("uri=", "")
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorDTO);
    }
    
    @ExceptionHandler(MalformedURLException.class)
    public ResponseEntity<ErrorDTO> handleMalformedURLException(MalformedURLException ex, WebRequest request) {
        ErrorDTO errorDTO = new ErrorDTO(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "Internal server error: " + ex.getMessage(),
            request.getDescription(false).replace("uri=", "")
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorDTO);
    }




    // Custom exceptions //
    // User exceptions
    @ExceptionHandler(UserNotAuthenticatedException.class)
    public ResponseEntity<ErrorDTO> handleUserNotAuthenticatedException(UserNotAuthenticatedException ex, WebRequest request) {
        ErrorDTO errorDTO = new ErrorDTO(
            HttpStatus.UNAUTHORIZED.value(),
            ex.getMessage(),
            request.getDescription(false).replace("uri=", "")
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorDTO);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorDTO> handleUserNotFoundException(UserNotFoundException ex, WebRequest request) {
        ErrorDTO errorDTO = new ErrorDTO(
            HttpStatus.NOT_FOUND.value(),
            ex.getMessage(),
            request.getDescription(false).replace("uri=", "")
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorDTO);
    }

    @ExceptionHandler(UserNotAuthorizedException.class)
    public ResponseEntity<ErrorDTO> handleUserNotAuthorizedException(UserNotAuthorizedException ex, WebRequest request) {
        ErrorDTO errorDTO = new ErrorDTO(
            HttpStatus.FORBIDDEN.value(),
            ex.getMessage(),
            request.getDescription(false).replace("uri=", "")
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorDTO);
    }


    // Course exceptions
    @ExceptionHandler(CourseNotFoundException.class)
    public ResponseEntity<ErrorDTO> handleCourseNotFoundException(CourseNotFoundException ex, WebRequest request) {
        ErrorDTO errorDTO = new ErrorDTO(
            HttpStatus.NOT_FOUND.value(),
            ex.getMessage(),
            request.getDescription(false).replace("uri=", "")
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorDTO);
    }


    // Announcement exceptions
    @ExceptionHandler(AnnouncementNotFoundException.class)
    public ResponseEntity<ErrorDTO> handleAnnouncementNotFoundException(AnnouncementNotFoundException ex, WebRequest request) {
        ErrorDTO errorDTO = new ErrorDTO(
            HttpStatus.NOT_FOUND.value(),
            ex.getMessage(),
            request.getDescription(false).replace("uri=", "")
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorDTO);
    }


    // Assignment exceptions
    @ExceptionHandler(AssignmentNotFoundException.class)
    public ResponseEntity<ErrorDTO> handleAssignmentNotFoundException(AssignmentNotFoundException ex, WebRequest request) {
        ErrorDTO errorDTO = new ErrorDTO(
            HttpStatus.NOT_FOUND.value(),
            ex.getMessage(),
            request.getDescription(false).replace("uri=", "")
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorDTO);
    }


    // File exceptions
    @ExceptionHandler(FileSizeLimitExceededException.class)
    public ResponseEntity<ErrorDTO> handleFileSizeLimitExceededException(FileSizeLimitExceededException ex, WebRequest request) {
        ErrorDTO errorDTO = new ErrorDTO(
            HttpStatus.CONTENT_TOO_LARGE.value(),
            ex.getMessage(),
            request.getDescription(false).replace("uri=", "")
        );
        return ResponseEntity.status(HttpStatus.CONTENT_TOO_LARGE).body(errorDTO);
    }

    @ExceptionHandler(InvalidFileTypeException.class)
    public ResponseEntity<ErrorDTO> handleInvalidFileTypeException(InvalidFileTypeException ex, WebRequest request) {
        ErrorDTO errorDTO = new ErrorDTO(
            HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(),
            ex.getMessage(),
            request.getDescription(false).replace("uri=", "")
        );
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(errorDTO);
    }

    @ExceptionHandler(FileNotFoundException.class)
    public ResponseEntity<ErrorDTO> handleFileNotFoundException(FileNotFoundException ex, WebRequest request) {
        ErrorDTO errorDTO = new ErrorDTO(
            HttpStatus.NOT_FOUND.value(),
            "File with ID: " + ex.getMessage() + " not found.",
            request.getDescription(false).replace("uri=", "")
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorDTO);
    }


    // General exception handler
    @ExceptionHandler(FailedToParseFormDataException.class)
    public ResponseEntity<ErrorDTO> handleFailedToParseFormDataException(FailedToParseFormDataException ex, WebRequest request) {
        ErrorDTO errorDTO = new ErrorDTO(
            HttpStatus.BAD_REQUEST.value(),
            ex.getMessage(),
            request.getDescription(false).replace("uri=", "")
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorDTO);
    }

}