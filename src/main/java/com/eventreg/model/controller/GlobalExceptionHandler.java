package com.eventreg.model.controller;

import com.eventreg.exception.ParticipantNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.net.URI;
import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler
{
  @ExceptionHandler(ParticipantNotFoundException.class)
  public ResponseEntity<ProblemDetail> handleParticipantNotFound(ParticipantNotFoundException ex, WebRequest webRequest)
  {
    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.NOT_FOUND,
            ex.getMessage()
    );

    problemDetail.setTitle("Participant Not Found");
    problemDetail.setType((URI.create("https://api.eventreg.com/errors/participant-not-found")));




    return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .header("X-Error-Code", "PARTICIPANT_404")
            .header("X-Timestamp", Instant.now().toString())
            .contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .body(problemDetail);
  }

}
