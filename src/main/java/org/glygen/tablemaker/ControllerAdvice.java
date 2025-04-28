package org.glygen.tablemaker;

import org.glygen.tablemaker.exception.BadRequestException;
import org.glygen.tablemaker.exception.DataNotFoundException;
import org.glygen.tablemaker.exception.DuplicateException;
import org.glygen.tablemaker.exception.UploadNotFinishedException;
import org.glygen.tablemaker.persistence.ErrorReportEntity;
import org.glygen.tablemaker.service.ErrorReportingService;
import org.glygen.tablemaker.view.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


/**
 * Base Handler Exception class. Manage response for all exception Class
 */

@RestControllerAdvice
public class ControllerAdvice {
    
    public static final Instant TIMESTAMP = LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant();
    final static Logger log = LoggerFactory.getLogger(ControllerAdvice.class);
    
    private final ErrorReportingService errorReportingService;
    
    public ControllerAdvice(ErrorReportingService errorReportingService) {
		this.errorReportingService = errorReportingService;
	}
    
    @ExceptionHandler({NoHandlerFoundException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse noHandlerFoundException(NoHandlerFoundException ex) {
        log.debug(ex.getMessage(), ex.getCause());
        return new ErrorResponse(String.valueOf(HttpStatus.NOT_FOUND.value()), "No resource found for your request. Please verify you request", TIMESTAMP);

    }
    
    @ExceptionHandler({BadCredentialsException.class})
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse badCredentialsException (BadCredentialsException ex) {
        log.debug(ex.getMessage(), ex.getCause());
        return new ErrorResponse(String.valueOf(HttpStatus.UNAUTHORIZED.value()), ex.getMessage(), TIMESTAMP);

    }

    @ExceptionHandler({DataNotFoundException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse noHandlerFoundException(Exception ex) {
        log.debug(ex.getMessage(), ex.getCause());
        return new ErrorResponse(String.valueOf(HttpStatus.NOT_FOUND.value()), ex.getMessage(), TIMESTAMP);

    }
    
    @ExceptionHandler({UploadNotFinishedException.class})
    @ResponseStatus(HttpStatus.PARTIAL_CONTENT)
    public ErrorResponse uploadNotFinishedException(Exception ex) {
        log.debug(ex.getMessage(), ex.getCause());
        return new ErrorResponse(String.valueOf(HttpStatus.PARTIAL_CONTENT.value()), ex.getMessage(), TIMESTAMP);

    }
    
    @ExceptionHandler({BadRequestException.class, DuplicateException.class, IllegalArgumentException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleBadRequestException(Exception ex) {
        return new ErrorResponse(String.valueOf(HttpStatus.BAD_REQUEST.value()), ex.getMessage(), TIMESTAMP);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public ErrorResponse notSupportedException(HttpRequestMethodNotSupportedException ex) {
        log.debug(ex.getMessage(), ex.getCause());
        return new ErrorResponse(String.valueOf(HttpStatus.METHOD_NOT_ALLOWED.value()),"Method Not Allowed. Please verify you request", TIMESTAMP);
    }

    @ExceptionHandler({Exception.class})
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleAllExceptions(Exception ex) {
    	//send an email and create a ticket with the stack trace
    	ErrorReportEntity error = new ErrorReportEntity();
    	error.setDateReported(new Date());
    	error.setMessage(ex.getMessage());
    	StringWriter stringWriter = new StringWriter();
    	PrintWriter printWriter = new PrintWriter(stringWriter);
    	ex.printStackTrace(printWriter);
    	error.setDetails(stringWriter.toString());
    	error.setTicketLabel("bug");
    	errorReportingService.reportError(error);
        log.error(ex.getMessage(), ex.getLocalizedMessage());
        return new ErrorResponse(String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value()), ex.getMessage(), TIMESTAMP);
    }
    
    @ExceptionHandler({MethodArgumentTypeMismatchException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleTypeMismatchExceptions(MethodArgumentTypeMismatchException ex) {
        Map<String, String> errors = new HashMap<>();
        errors.put(ex.getPropertyName(), ex.getMessage());
        
        return new ErrorResponse(String.valueOf(HttpStatus.BAD_REQUEST.value()), errors.toString(), TIMESTAMP);
    }
    
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ErrorResponse handleValidationExceptionHandler(MethodArgumentNotValidException ex) {

        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage()));

        return new ErrorResponse(String.valueOf(HttpStatus.BAD_REQUEST.value()), errors.toString(), TIMESTAMP);
    }
}


