package ch.uzh.ifi.hase.soprafs26.exceptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.context.request.ServletWebRequest;

class GlobalExceptionAdviceTest {

    @Test
    void handleConflict_returnsConflictResponse() {
        GlobalExceptionAdvice advice = new GlobalExceptionAdvice();

        var response = advice.handleConflict(
            new IllegalStateException("bad state"),
            new ServletWebRequest(new MockHttpServletRequest())
        );

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("This should be application specific", response.getBody());
    }

    @Test
    void handleTransactionSystemException_returnsConflictException() {
        GlobalExceptionAdvice advice = new GlobalExceptionAdvice();
        TransactionSystemException exception = new TransactionSystemException("transaction failed");
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/games/1");

        var response = advice.handleTransactionSystemException(exception, request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertSame(exception, response.getCause());
    }

    @Test
    void handleOptimisticLockingFailure_returnsRetryConflict() {
        GlobalExceptionAdvice advice = new GlobalExceptionAdvice();
        ObjectOptimisticLockingFailureException exception =
            new ObjectOptimisticLockingFailureException(GameStub.class, 1L,
                new OptimisticLockingFailureException("stale"));

        var response = advice.handleOptimisticLockingFailure(
            exception,
            new MockHttpServletRequest("POST", "/games/1/actions/end-turn")
        );

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("Game state changed. Please retry.", response.getReason());
        assertSame(exception, response.getCause());
    }

    @Test
    void handleException_returnsInternalServerError() {
        GlobalExceptionAdvice advice = new GlobalExceptionAdvice();
        HttpServerErrorException exception =
            HttpServerErrorException.InternalServerError.create(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "backend failed",
                null,
                null,
                null
            );

        var response = advice.handleException(exception);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertSame(exception, response.getCause());
    }

    private static class GameStub {
    }
}
