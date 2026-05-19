package ch.uzh.ifi.hase.soprafs26;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

class ApplicationTest {

    @Test
    void helloWorld_returnsRunningMessage() {
        Application application = new Application();

        assertEquals("The application is running.", application.helloWorld());
    }

    @Test
    void corsConfigurer_canRegisterMappings() {
        Application application = new Application();

        assertDoesNotThrow(() -> application.corsConfigurer().addCorsMappings(new CorsRegistry()));
    }
}
