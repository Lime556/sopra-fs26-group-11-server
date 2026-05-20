package ch.uzh.ifi.hase.soprafs26.service;

import java.lang.reflect.Field;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.uzh.ifi.hase.soprafs26.constant.TimeOfDayMood;
import ch.uzh.ifi.hase.soprafs26.constant.WeatherCategory;

class AmbienceServiceTest {

    @Test
    void weatherCodeMapping_mapsExpectedOpenMeteoCodes() {
        assertEquals(WeatherCategory.SUNNY, AmbienceService.mapWeatherCode(0));
        assertEquals(WeatherCategory.CLOUDY, AmbienceService.mapWeatherCode(3));
        assertEquals(WeatherCategory.FOGGY, AmbienceService.mapWeatherCode(45));
        assertEquals(WeatherCategory.RAINY, AmbienceService.mapWeatherCode(61));
        assertEquals(WeatherCategory.LIGHTNING, AmbienceService.mapWeatherCode(95));
        assertEquals(WeatherCategory.SNOWING, AmbienceService.mapWeatherCode(71));
        assertEquals(WeatherCategory.UNKNOWN, AmbienceService.mapWeatherCode(999));
    }

    @Test
    void timeOfDayMapping_usesLocalHourThenIsDayFallback() {
        assertEquals(TimeOfDayMood.SUNRISE, AmbienceService.mapTimeOfDay("2026-05-17T06:00", null));
        assertEquals(TimeOfDayMood.DAY, AmbienceService.mapTimeOfDay("2026-05-17T13:00", null));
        assertEquals(TimeOfDayMood.SUNSET, AmbienceService.mapTimeOfDay("2026-05-17T18:00", null));
        assertEquals(TimeOfDayMood.NIGHT, AmbienceService.mapTimeOfDay("2026-05-17T23:00", null));
        assertEquals(TimeOfDayMood.DAY, AmbienceService.mapTimeOfDay("not-a-time", 1));
        assertEquals(TimeOfDayMood.NIGHT, AmbienceService.mapTimeOfDay("not-a-time", 0));
        assertEquals(TimeOfDayMood.UNKNOWN, AmbienceService.mapTimeOfDay("not-a-time", null));
    }

    @Test
    void externalFailure_returnsUnknownAmbience() {
        RestTemplate restTemplate = Mockito.mock(RestTemplate.class);
        Mockito.when(restTemplate.getForObject(Mockito.anyString(), Mockito.eq(AmbienceService.OpenMeteoResponse.class)))
            .thenThrow(new RestClientException("Open-Meteo unavailable"));

        var ambience = new AmbienceService(restTemplate).getCurrentAmbience();

        assertEquals(WeatherCategory.UNKNOWN, ambience.getWeather());
        assertEquals(TimeOfDayMood.UNKNOWN, ambience.getTimeOfDay());
        assertEquals("Weather ambience unavailable", ambience.getDescription());
    }

    @Test
    void openMeteoResponse_mapsCloudyDay() {
        RestTemplate restTemplate = Mockito.mock(RestTemplate.class);
        AmbienceService.OpenMeteoCurrent current = new AmbienceService.OpenMeteoCurrent();
        current.setWeather_code(3);
        current.setIs_day(1);
        current.setTime("2026-05-17T14:45");
        AmbienceService.OpenMeteoResponse response = new AmbienceService.OpenMeteoResponse();
        response.setCurrent(current);
        Mockito.when(restTemplate.getForObject(
            "https://api.open-meteo.com/v1/forecast?latitude=47.3769&longitude=8.5417&current=weather_code,is_day&timezone=auto",
            AmbienceService.OpenMeteoResponse.class
        )).thenReturn(response);

        var ambience = new AmbienceService(restTemplate).getCurrentAmbience();

        assertEquals(WeatherCategory.CLOUDY, ambience.getWeather());
        assertEquals(TimeOfDayMood.DAY, ambience.getTimeOfDay());
        assertEquals("Cloudy day", ambience.getDescription());
    }

    @Test
    void successfulFetch_reusesCachedAmbience() {
        RestTemplate restTemplate = Mockito.mock(RestTemplate.class);
        AmbienceService.OpenMeteoCurrent current = new AmbienceService.OpenMeteoCurrent();
        current.setWeather_code(0);
        current.setIs_day(1);
        current.setTime("2026-05-17T13:00");
        AmbienceService.OpenMeteoResponse response = new AmbienceService.OpenMeteoResponse();
        response.setCurrent(current);
        Mockito.when(restTemplate.getForObject(Mockito.anyString(), Mockito.eq(AmbienceService.OpenMeteoResponse.class)))
            .thenReturn(response);

        AmbienceService ambienceService = new AmbienceService(restTemplate);

        var firstAmbience = ambienceService.getCurrentAmbience();
        var secondAmbience = ambienceService.getCurrentAmbience();

        assertEquals(WeatherCategory.SUNNY, firstAmbience.getWeather());
        assertEquals(TimeOfDayMood.DAY, secondAmbience.getTimeOfDay());
        assertEquals("Sunny day", secondAmbience.getDescription());
        Mockito.verify(restTemplate, Mockito.times(1))
            .getForObject(Mockito.anyString(), Mockito.eq(AmbienceService.OpenMeteoResponse.class));
    }

    @Test
    void expiredCacheAndExternalFailure_returnsPreviousAmbience() throws Exception {
        RestTemplate restTemplate = Mockito.mock(RestTemplate.class);
        AmbienceService.OpenMeteoCurrent current = new AmbienceService.OpenMeteoCurrent();
        current.setWeather_code(95);
        current.setIs_day(0);
        current.setTime("2026-05-17T23:00");
        AmbienceService.OpenMeteoResponse response = new AmbienceService.OpenMeteoResponse();
        response.setCurrent(current);
        Mockito.when(restTemplate.getForObject(Mockito.anyString(), Mockito.eq(AmbienceService.OpenMeteoResponse.class)))
            .thenReturn(response)
            .thenThrow(new RestClientException("Open-Meteo unavailable"));

        AmbienceService ambienceService = new AmbienceService(restTemplate);
        var firstAmbience = ambienceService.getCurrentAmbience();
        Field cachedAt = AmbienceService.class.getDeclaredField("cachedAt");
        cachedAt.setAccessible(true);
        cachedAt.set(ambienceService, Instant.now().minusSeconds(660));

        var fallbackAmbience = ambienceService.getCurrentAmbience();

        assertEquals(WeatherCategory.LIGHTNING, firstAmbience.getWeather());
        assertEquals(TimeOfDayMood.NIGHT, fallbackAmbience.getTimeOfDay());
        assertEquals("Stormy night over the island", fallbackAmbience.getDescription());
        Mockito.verify(restTemplate, Mockito.times(2))
            .getForObject(Mockito.anyString(), Mockito.eq(AmbienceService.OpenMeteoResponse.class));
    }

    @Test
    void nullOpenMeteoResponse_returnsUnknownAndCachesIt() {
        RestTemplate restTemplate = Mockito.mock(RestTemplate.class);
        Mockito.when(restTemplate.getForObject(Mockito.anyString(), Mockito.eq(AmbienceService.OpenMeteoResponse.class)))
            .thenReturn(null);

        AmbienceService ambienceService = new AmbienceService(restTemplate);

        var firstAmbience = ambienceService.getCurrentAmbience();
        var secondAmbience = ambienceService.getCurrentAmbience();

        assertEquals(WeatherCategory.UNKNOWN, firstAmbience.getWeather());
        assertEquals(TimeOfDayMood.UNKNOWN, firstAmbience.getTimeOfDay());
        assertEquals("Weather ambience unavailable", secondAmbience.getDescription());
        Mockito.verify(restTemplate, Mockito.times(1))
            .getForObject(Mockito.anyString(), Mockito.eq(AmbienceService.OpenMeteoResponse.class));
    }

    @Test
    void describe_coversWeatherMoodCombinations() {
        assertEquals("Sunny sunrise over the island",
            AmbienceService.describe(WeatherCategory.SUNNY, TimeOfDayMood.SUNRISE));
        assertEquals("Rainy sunset over the island",
            AmbienceService.describe(WeatherCategory.RAINY, TimeOfDayMood.SUNSET));
        assertEquals("Snowy day over the island",
            AmbienceService.describe(WeatherCategory.SNOWING, TimeOfDayMood.DAY));
        assertEquals("Foggy night over the island",
            AmbienceService.describe(WeatherCategory.FOGGY, TimeOfDayMood.NIGHT));
        assertEquals("Weather ambience unavailable",
            AmbienceService.describe(WeatherCategory.UNKNOWN, TimeOfDayMood.DAY));
    }

    @Test
    void openMeteoJsonShape_deserializesSnakeCaseCurrentFields() throws Exception {
        String json = """
            {
              "current": {
                "time": "2026-05-17T14:45",
                "interval": 900,
                "weather_code": 3,
                "is_day": 1
              }
            }
            """;

        AmbienceService.OpenMeteoResponse response = new ObjectMapper()
            .readValue(json, AmbienceService.OpenMeteoResponse.class);

        assertEquals(3, response.getCurrent().getWeather_code());
        assertEquals(1, response.getCurrent().getIs_day());
        assertEquals("2026-05-17T14:45", response.getCurrent().getTime());
    }
}
