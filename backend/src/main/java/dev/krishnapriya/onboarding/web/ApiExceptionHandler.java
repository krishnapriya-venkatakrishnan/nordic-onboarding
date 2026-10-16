package dev.krishnapriya.onboarding.web;

import dev.krishnapriya.onboarding.domain.UnknownMarketException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    /** A market that is built but not enabled looks exactly like one that doesn't exist. */
    @ExceptionHandler(UnknownMarketException.class)
    public ProblemDetail unknownMarket(UnknownMarketException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }
}
