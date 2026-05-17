package ch.uzh.ifi.hase.soprafs26.rest.dto;

import ch.uzh.ifi.hase.soprafs26.constant.TimeOfDayMood;
import ch.uzh.ifi.hase.soprafs26.constant.WeatherCategory;

public class GameAmbienceDTO {
    private WeatherCategory weather;
    private TimeOfDayMood timeOfDay;
    private String description;

    public GameAmbienceDTO() {
    }

    public GameAmbienceDTO(WeatherCategory weather, TimeOfDayMood timeOfDay, String description) {
        this.weather = weather;
        this.timeOfDay = timeOfDay;
        this.description = description;
    }

    public WeatherCategory getWeather() {
        return weather;
    }

    public void setWeather(WeatherCategory weather) {
        this.weather = weather;
    }

    public TimeOfDayMood getTimeOfDay() {
        return timeOfDay;
    }

    public void setTimeOfDay(TimeOfDayMood timeOfDay) {
        this.timeOfDay = timeOfDay;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
