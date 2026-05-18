package ch.uzh.ifi.hase.soprafs26.service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import ch.uzh.ifi.hase.soprafs26.constant.TimeOfDayMood;
import ch.uzh.ifi.hase.soprafs26.constant.WeatherCategory;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GameAmbienceDTO;

@Service
public class AmbienceService {

    private static final Logger log = LoggerFactory.getLogger(AmbienceService.class);
    private static final String OPEN_METEO_URL = "https://api.open-meteo.com/v1/forecast"
        + "?latitude=47.3769&longitude=8.5417&current=weather_code,is_day&timezone=auto";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(1);
    private static final Duration CACHE_TTL = Duration.ofMinutes(10);
    private static final Set<Integer> SUNNY_CODES = Set.of(0);
    private static final Set<Integer> CLOUDY_CODES = Set.of(1, 2, 3);
    private static final Set<Integer> FOGGY_CODES = Set.of(45, 48);
    private static final Set<Integer> RAINY_CODES = Set.of(51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82);
    private static final Set<Integer> SNOWING_CODES = Set.of(71, 73, 75, 77, 85, 86);
    private static final Set<Integer> LIGHTNING_CODES = Set.of(95, 96, 99);

    private final RestTemplate restTemplate;
    private GameAmbienceDTO cachedAmbience;
    private Instant cachedAt;

    public AmbienceService() {
        this(createRestTemplateWithTimeouts());
    }

    AmbienceService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public synchronized GameAmbienceDTO getCurrentAmbience() {
        if (isCacheFresh()) {
            return cachedAmbience;
        }

        try {
            OpenMeteoResponse response = restTemplate.getForObject(OPEN_METEO_URL, OpenMeteoResponse.class);
            OpenMeteoCurrent current = response == null ? null : response.getCurrent();
            WeatherCategory weather = mapWeatherCode(current == null ? null : current.getWeather_code());
            TimeOfDayMood timeOfDay = mapTimeOfDay(current == null ? null : current.getTime(), current == null ? null : current.getIs_day());
            GameAmbienceDTO ambience = new GameAmbienceDTO(weather, timeOfDay, describe(weather, timeOfDay));
            cachedAmbience = ambience;
            cachedAt = Instant.now();
            return ambience;
        } catch (RestClientException | IllegalArgumentException exception) {
            log.warn("Could not fetch or parse Open-Meteo ambience.", exception);
            if (cachedAmbience != null) {
                return cachedAmbience;
            }
            return unavailable();
        }
    }

    private static RestTemplate createRestTemplateWithTimeouts() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(REQUEST_TIMEOUT);
        requestFactory.setReadTimeout(REQUEST_TIMEOUT);
        return new RestTemplate(requestFactory);
    }

    private boolean isCacheFresh() {
        return cachedAmbience != null
                && cachedAt != null
                && cachedAt.plus(CACHE_TTL).isAfter(Instant.now());
    }

    public static WeatherCategory mapWeatherCode(Integer weatherCode) {
        if (weatherCode == null) {
            return WeatherCategory.UNKNOWN;
        }
        if (SUNNY_CODES.contains(weatherCode)) {
            return WeatherCategory.SUNNY;
        }
        if (CLOUDY_CODES.contains(weatherCode)) {
            return WeatherCategory.CLOUDY;
        }
        if (FOGGY_CODES.contains(weatherCode)) {
            return WeatherCategory.FOGGY;
        }
        if (RAINY_CODES.contains(weatherCode)) {
            return WeatherCategory.RAINY;
        }
        if (SNOWING_CODES.contains(weatherCode)) {
            return WeatherCategory.SNOWING;
        }
        if (LIGHTNING_CODES.contains(weatherCode)) {
            return WeatherCategory.LIGHTNING;
        }
        return WeatherCategory.UNKNOWN;
    }

    public static TimeOfDayMood mapTimeOfDay(String currentTime, Integer isDay) {
        Integer hour = parseHour(currentTime);
        if (hour != null) {
            if (hour >= 5 && hour <= 8) {
                return TimeOfDayMood.SUNRISE;
            }
            if (hour >= 9 && hour <= 16) {
                return TimeOfDayMood.DAY;
            }
            if (hour >= 17 && hour <= 20) {
                return TimeOfDayMood.SUNSET;
            }
            return TimeOfDayMood.NIGHT;
        }

        if (Integer.valueOf(1).equals(isDay)) {
            return TimeOfDayMood.DAY;
        }
        if (Integer.valueOf(0).equals(isDay)) {
            return TimeOfDayMood.NIGHT;
        }
        return TimeOfDayMood.UNKNOWN;
    }

    public static String describe(WeatherCategory weather, TimeOfDayMood timeOfDay) {
        if (weather == WeatherCategory.UNKNOWN || timeOfDay == TimeOfDayMood.UNKNOWN) {
            return "Weather ambience unavailable";
        }

        return switch (weather) {
            case SUNNY -> switch (timeOfDay) {
                case SUNRISE -> "Sunny sunrise over the island";
                case DAY -> "Sunny day";
                case SUNSET -> "Sunny sunset over the island";
                case NIGHT -> "Clear night over the island";
                default -> "Weather ambience unavailable";
            };
            case CLOUDY -> switch (timeOfDay) {
                case SUNRISE -> "Cloudy sunrise over the island";
                case DAY -> "Cloudy day";
                case SUNSET -> "Cloudy sunset over the island";
                case NIGHT -> "Cloudy night over the island";
                default -> "Weather ambience unavailable";
            };
            case RAINY -> switch (timeOfDay) {
                case SUNRISE -> "Rainy sunrise over the island";
                case DAY -> "Rainy day";
                case SUNSET -> "Rainy sunset over the island";
                case NIGHT -> "Rainy night over the island";
                default -> "Weather ambience unavailable";
            };
            case LIGHTNING -> timeOfDay == TimeOfDayMood.NIGHT
                ? "Stormy night over the island"
                : "Stormy " + moodLabel(timeOfDay) + " over the island";
            case SNOWING -> timeOfDay == TimeOfDayMood.NIGHT
                ? "Snowy night over the island"
                : "Snowy " + moodLabel(timeOfDay) + " over the island";
            case FOGGY -> "Foggy " + moodLabel(timeOfDay) + " over the island";
            default -> "Weather ambience unavailable";
        };
    }

    private static GameAmbienceDTO unavailable() {
        return new GameAmbienceDTO(
            WeatherCategory.UNKNOWN,
            TimeOfDayMood.UNKNOWN,
            "Weather ambience unavailable"
        );
    }

    private static Integer parseHour(String currentTime) {
        if (currentTime == null || currentTime.isBlank()) {
            return null;
        }

        try {
            return LocalDateTime.parse(currentTime).getHour();
        } catch (DateTimeParseException exception) {
            try {
                return OffsetDateTime.parse(currentTime).getHour();
            } catch (DateTimeParseException ignored) {
                return null;
            }
        }
    }

    private static String moodLabel(TimeOfDayMood timeOfDay) {
        return switch (timeOfDay) {
            case SUNRISE -> "sunrise";
            case DAY -> "day";
            case SUNSET -> "sunset";
            case NIGHT -> "night";
            default -> "weather";
        };
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OpenMeteoResponse {
        private OpenMeteoCurrent current;

        public OpenMeteoCurrent getCurrent() {
            return current;
        }

        public void setCurrent(OpenMeteoCurrent current) {
            this.current = current;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OpenMeteoCurrent {
        @JsonProperty("weather_code")
        private Integer weather_code;
        @JsonProperty("is_day")
        private Integer is_day;
        private String time;

        public Integer getWeather_code() {
            return weather_code;
        }

        public void setWeather_code(Integer weather_code) {
            this.weather_code = weather_code;
        }

        public Integer getIs_day() {
            return is_day;
        }

        public void setIs_day(Integer is_day) {
            this.is_day = is_day;
        }

        public String getTime() {
            return time;
        }

        public void setTime(String time) {
            this.time = time;
        }
    }
}
