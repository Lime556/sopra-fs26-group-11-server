package ch.uzh.ifi.hase.soprafs26.rest.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import ch.uzh.ifi.hase.soprafs26.constant.TimeOfDayMood;
import ch.uzh.ifi.hase.soprafs26.constant.WeatherCategory;

class GameAmbienceDTOTest {

    @Test
    void constructorAndSetters_storeValues() {
        GameAmbienceDTO dto = new GameAmbienceDTO(
            WeatherCategory.SUNNY,
            TimeOfDayMood.DAY,
            "Sunny day"
        );

        assertEquals(WeatherCategory.SUNNY, dto.getWeather());
        assertEquals(TimeOfDayMood.DAY, dto.getTimeOfDay());
        assertEquals("Sunny day", dto.getDescription());

        dto.setWeather(WeatherCategory.RAINY);
        dto.setTimeOfDay(TimeOfDayMood.NIGHT);
        dto.setDescription("Rainy night over the island");

        assertEquals(WeatherCategory.RAINY, dto.getWeather());
        assertEquals(TimeOfDayMood.NIGHT, dto.getTimeOfDay());
        assertEquals("Rainy night over the island", dto.getDescription());
    }
}
