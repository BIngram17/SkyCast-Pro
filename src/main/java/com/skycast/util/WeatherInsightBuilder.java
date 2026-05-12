package com.skycast.util;

import com.skycast.api.WeatherService.ForecastDay;
import com.skycast.api.WeatherService.WeatherData;

import java.util.ArrayList;
import java.util.List;

public class WeatherInsightBuilder {

    private final WeatherFormatter weatherFormatter;

    public WeatherInsightBuilder(WeatherFormatter weatherFormatter) {
        this.weatherFormatter = weatherFormatter;
    }

    public String buildWeatherSummary(WeatherData currentWeatherData) {
        if (currentWeatherData == null) {
            return "Search for a location to get a plain-English forecast.";
        }

        String location = currentWeatherData.displayLocation();
        String temperature = weatherFormatter.formatTemperature(currentWeatherData.temperature());
        String condition = currentWeatherData.description().toLowerCase();

        return String.format(
                "%s is currently %s with %s. Wind is %s and humidity is %d%%.",
                location,
                temperature,
                condition,
                weatherFormatter.formatWindSpeed(currentWeatherData.windSpeed()),
                currentWeatherData.humidity()
        );
    }

    public String buildWeatherAlerts(WeatherData currentWeatherData, List<ForecastDay> currentForecast) {
        if (currentWeatherData == null) {
            return "None yet.";
        }

        List<String> alerts = new ArrayList<>();

        if (currentWeatherData.temperature() >= 95) {
            alerts.add("Heat caution. Stay hydrated and limit strenuous outdoor activity.");
        }

        if (currentWeatherData.temperature() <= 35) {
            alerts.add("Cold weather caution. Dress warmly.");
        }

        if (currentWeatherData.windSpeed() >= 20) {
            alerts.add("Wind notice. Secure loose outdoor items.");
        }

        if (currentWeatherData.humidity() >= 85) {
            alerts.add("High humidity notice. It may feel warmer or heavier outside.");
        }

        String currentCondition = currentWeatherData.weatherMain().toLowerCase();

        if (currentCondition.contains("rain")
                || currentCondition.contains("drizzle")
                || currentCondition.contains("thunderstorm")) {
            alerts.add("Wet weather notice. Roads and sidewalks may be slick.");
        }

        if (currentForecast != null) {
            for (ForecastDay day : currentForecast) {
                if (day.rainChance() >= 0.45) {
                    alerts.add("Rain chance appears in the upcoming forecast.");
                    break;
                }
            }
        }

        if (alerts.isEmpty()) {
            return "No major weather concerns detected.";
        }

        return String.join(" ", alerts);
    }

    public String buildActivityRecommendation(WeatherData currentWeatherData) {
        if (currentWeatherData == null) {
            return "Waiting for weather data.";
        }

        int score = 10;
        List<String> tips = new ArrayList<>();

        if (currentWeatherData.temperature() >= 95) {
            score -= 3;
            tips.add("bring water");
        } else if (currentWeatherData.temperature() >= 85) {
            score -= 1;
            tips.add("wear light clothing");
        }

        if (currentWeatherData.temperature() <= 45) {
            score -= 2;
            tips.add("wear a jacket");
        }

        if (currentWeatherData.windSpeed() >= 20) {
            score -= 2;
            tips.add("expect windy conditions");
        }

        String condition = currentWeatherData.weatherMain().toLowerCase();

        if (condition.contains("rain") || condition.contains("drizzle")) {
            score -= 3;
            tips.add("carry an umbrella");
        }

        if (condition.contains("thunderstorm")) {
            score -= 5;
            tips.add("avoid outdoor activity if lightning is nearby");
        }

        if (condition.contains("clear")
                && currentWeatherData.temperature() >= 65
                && currentWeatherData.temperature() <= 85) {
            tips.add("great conditions for being outside");
        }

        score = Math.max(1, Math.min(10, score));

        if (tips.isEmpty()) {
            tips.add("normal conditions");
        }

        return "Outdoor rating " + score + "/10. " + String.join(", ", tips) + ".";
    }
}
