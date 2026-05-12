package com.skycast.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.skycast.config.AppConfig;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class WeatherService {

    private static final String API_KEY = AppConfig.getOpenWeatherApiKey();
    private static final String GEOCODING_URL = "https://api.openweathermap.org/geo/1.0/direct";
    private static final String CURRENT_WEATHER_URL = "https://api.openweathermap.org/data/2.5/weather";
    private static final String FORECAST_URL = "https://api.openweathermap.org/data/2.5/forecast";
    private static final String AIR_QUALITY_URL = "https://api.openweathermap.org/data/2.5/air_pollution";

    private final HttpClient httpClient;

    public WeatherService() {
        this.httpClient = HttpClient.newHttpClient();
    }

    public List<LocationSuggestion> getLocationSuggestions(String query) throws IOException, InterruptedException {
        String cleanedQuery = query == null ? "" : query.trim();

        if (cleanedQuery.length() < 2) {
            return List.of();
        }

        String encodedQuery = URLEncoder.encode(cleanedQuery, StandardCharsets.UTF_8);

        String url = GEOCODING_URL
                + "?q=" + encodedQuery
                + "&limit=6"
                + "&appid=" + API_KEY;

        JsonArray results = sendArrayRequest(url);
        List<LocationSuggestion> suggestions = new ArrayList<>();

        for (int i = 0; i < results.size(); i++) {
            JsonObject item = results.get(i).getAsJsonObject();

            String cityName = item.has("name") ? item.get("name").getAsString() : "";
            String state = item.has("state") ? item.get("state").getAsString() : "";
            String country = item.has("country") ? item.get("country").getAsString() : "";
            double latitude = item.get("lat").getAsDouble();
            double longitude = item.get("lon").getAsDouble();

            LocationSuggestion suggestion = new LocationSuggestion(cityName, state, country, latitude, longitude);

            if (!containsSuggestion(suggestions, suggestion)) {
                suggestions.add(suggestion);
            }
        }

        return suggestions;
    }

    public LocationSuggestion resolveLocation(String query) throws IOException, InterruptedException {
        List<LocationSuggestion> suggestions = getLocationSuggestions(query);

        if (suggestions.isEmpty()) {
            throw new IOException("City not found. Try entering a city like Rio Linda, CA, US.");
        }

        return suggestions.get(0);
    }

    public WeatherData getCurrentWeather(LocationSuggestion location) throws IOException, InterruptedException {
        String url = CURRENT_WEATHER_URL
                + "?lat=" + location.latitude()
                + "&lon=" + location.longitude()
                + "&appid=" + API_KEY
                + "&units=imperial";

        JsonObject root = sendObjectRequest(url);

        JsonObject main = root.getAsJsonObject("main");
        double temperature = main.get("temp").getAsDouble();
        double feelsLike = main.get("feels_like").getAsDouble();
        int humidity = main.get("humidity").getAsInt();

        JsonObject wind = root.getAsJsonObject("wind");
        double windSpeed = wind.get("speed").getAsDouble();

        JsonObject weather = root.getAsJsonArray("weather").get(0).getAsJsonObject();
        String weatherMain = weather.get("main").getAsString();
        String description = weather.get("description").getAsString();
        String iconCode = weather.get("icon").getAsString();

        return new WeatherData(
                location.cityName(),
                location.state(),
                location.country(),
                location.latitude(),
                location.longitude(),
                temperature,
                feelsLike,
                humidity,
                windSpeed,
                weatherMain,
                capitalizeWords(description),
                iconCode
        );
    }

    public List<ForecastDay> getFiveDayForecast(LocationSuggestion location) throws IOException, InterruptedException {
        JsonObject root = getForecastRoot(location);

        int timezoneOffsetSeconds = root.getAsJsonObject("city").get("timezone").getAsInt();
        ZoneOffset cityOffset = ZoneOffset.ofTotalSeconds(timezoneOffsetSeconds);

        JsonArray forecastList = root.getAsJsonArray("list");
        Map<LocalDate, DailyForecastBuilder> dailyForecasts = new LinkedHashMap<>();

        LocalDate today = LocalDate.now(cityOffset);

        for (int i = 0; i < forecastList.size(); i++) {
            JsonObject item = forecastList.get(i).getAsJsonObject();

            long unixTime = item.get("dt").getAsLong();
            LocalDate forecastDate = Instant.ofEpochSecond(unixTime).atOffset(cityOffset).toLocalDate();

            if (!forecastDate.isAfter(today)) {
                continue;
            }

            JsonObject main = item.getAsJsonObject("main");
            double tempMin = main.get("temp_min").getAsDouble();
            double tempMax = main.get("temp_max").getAsDouble();
            int humidity = main.get("humidity").getAsInt();

            JsonObject wind = item.getAsJsonObject("wind");
            double windSpeed = wind.get("speed").getAsDouble();

            double rainChance = item.has("pop") ? item.get("pop").getAsDouble() : 0.0;

            JsonObject weather = item.getAsJsonArray("weather").get(0).getAsJsonObject();
            String description = capitalizeWords(weather.get("description").getAsString());
            String iconCode = weather.get("icon").getAsString();

            int hour = Instant.ofEpochSecond(unixTime).atOffset(cityOffset).getHour();

            dailyForecasts
                    .computeIfAbsent(forecastDate, DailyForecastBuilder::new)
                    .addForecast(tempMin, tempMax, description, iconCode, humidity, windSpeed, rainChance, hour);
        }

        List<ForecastDay> fiveDayForecast = new ArrayList<>();

        for (DailyForecastBuilder builder : dailyForecasts.values()) {
            fiveDayForecast.add(builder.build());

            if (fiveDayForecast.size() == 5) {
                break;
            }
        }

        return fiveDayForecast;
    }

    public List<HourlyForecast> getHourlyForecast(LocationSuggestion location) throws IOException, InterruptedException {
        JsonObject root = getForecastRoot(location);

        int timezoneOffsetSeconds = root.getAsJsonObject("city").get("timezone").getAsInt();
        ZoneOffset cityOffset = ZoneOffset.ofTotalSeconds(timezoneOffsetSeconds);
        JsonArray forecastList = root.getAsJsonArray("list");

        List<HourlyForecast> hourlyForecasts = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE h a", Locale.US);

        for (int i = 0; i < forecastList.size() && hourlyForecasts.size() < 8; i++) {
            JsonObject item = forecastList.get(i).getAsJsonObject();

            long unixTime = item.get("dt").getAsLong();
            String displayTime = Instant.ofEpochSecond(unixTime).atOffset(cityOffset).format(formatter);

            JsonObject main = item.getAsJsonObject("main");
            double temperature = main.get("temp").getAsDouble();
            int humidity = main.get("humidity").getAsInt();

            JsonObject wind = item.getAsJsonObject("wind");
            double windSpeed = wind.get("speed").getAsDouble();

            double rainChance = item.has("pop") ? item.get("pop").getAsDouble() : 0.0;

            JsonObject weather = item.getAsJsonArray("weather").get(0).getAsJsonObject();
            String description = capitalizeWords(weather.get("description").getAsString());
            String iconCode = weather.get("icon").getAsString();

            hourlyForecasts.add(new HourlyForecast(
                    displayTime,
                    temperature,
                    humidity,
                    windSpeed,
                    rainChance,
                    description,
                    iconCode
            ));
        }

        return hourlyForecasts;
    }

    public AirQualityData getAirQuality(LocationSuggestion location) throws IOException, InterruptedException {
        String url = AIR_QUALITY_URL
                + "?lat=" + location.latitude()
                + "&lon=" + location.longitude()
                + "&appid=" + API_KEY;

        JsonObject root = sendObjectRequest(url);
        JsonArray list = root.getAsJsonArray("list");

        if (list.isEmpty()) {
            return new AirQualityData(0, "Unavailable", 0, 0);
        }

        JsonObject first = list.get(0).getAsJsonObject();
        int aqi = first.getAsJsonObject("main").get("aqi").getAsInt();

        JsonObject components = first.getAsJsonObject("components");
        double pm25 = components.has("pm2_5") ? components.get("pm2_5").getAsDouble() : 0;
        double pm10 = components.has("pm10") ? components.get("pm10").getAsDouble() : 0;

        return new AirQualityData(aqi, getAqiLabel(aqi), pm25, pm10);
    }

    private JsonObject getForecastRoot(LocationSuggestion location) throws IOException, InterruptedException {
        String url = FORECAST_URL
                + "?lat=" + location.latitude()
                + "&lon=" + location.longitude()
                + "&appid=" + API_KEY
                + "&units=imperial";

        return sendObjectRequest(url);
    }

    private String getAqiLabel(int aqi) {
        return switch (aqi) {
            case 1 -> "Good";
            case 2 -> "Fair";
            case 3 -> "Moderate";
            case 4 -> "Poor";
            case 5 -> "Very Poor";
            default -> "Unavailable";
        };
    }

    private JsonObject sendObjectRequest(String url) throws IOException, InterruptedException {
        String responseBody = sendRawRequest(url);
        return JsonParser.parseString(responseBody).getAsJsonObject();
    }

    private JsonArray sendArrayRequest(String url) throws IOException, InterruptedException {
        String responseBody = sendRawRequest(url);
        return JsonParser.parseString(responseBody).getAsJsonArray();
    }

    private String sendRawRequest(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 401) {
            throw new IOException("Invalid API key. Please check your OpenWeatherMap API key.");
        }

        if (response.statusCode() == 404) {
            throw new IOException("City not found. Try entering a city like Rio Linda, CA, US.");
        }

        if (response.statusCode() != 200) {
            throw new IOException("Weather service error. Status code: " + response.statusCode());
        }

        return response.body();
    }

    private boolean containsSuggestion(List<LocationSuggestion> suggestions, LocationSuggestion newSuggestion) {
        for (LocationSuggestion existing : suggestions) {
            boolean sameCity = existing.cityName().equalsIgnoreCase(newSuggestion.cityName());
            boolean sameState = existing.state().equalsIgnoreCase(newSuggestion.state());
            boolean sameCountry = existing.country().equalsIgnoreCase(newSuggestion.country());

            if (sameCity && sameState && sameCountry) {
                return true;
            }
        }

        return false;
    }

    private String capitalizeWords(String text) {
        String[] words = text.split(" ");
        StringBuilder result = new StringBuilder();

        for (String word : words) {
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1))
                        .append(" ");
            }
        }

        return result.toString().trim();
    }

    private static class DailyForecastBuilder {
        private final LocalDate date;
        private double minTemp = Double.MAX_VALUE;
        private double maxTemp = -Double.MAX_VALUE;
        private String bestDescription = "";
        private String bestIconCode = "01d";
        private int closestHourToNoon = Integer.MAX_VALUE;
        private int humidityTotal = 0;
        private double windTotal = 0;
        private double maxRainChance = 0;
        private int count = 0;

        DailyForecastBuilder(LocalDate date) {
            this.date = date;
        }

        void addForecast(
                double tempMin,
                double tempMax,
                String description,
                String iconCode,
                int humidity,
                double windSpeed,
                double rainChance,
                int hour
        ) {
            minTemp = Math.min(minTemp, tempMin);
            maxTemp = Math.max(maxTemp, tempMax);
            humidityTotal += humidity;
            windTotal += windSpeed;
            maxRainChance = Math.max(maxRainChance, rainChance);
            count++;

            int distanceFromNoon = Math.abs(12 - hour);

            if (distanceFromNoon < closestHourToNoon) {
                closestHourToNoon = distanceFromNoon;
                bestDescription = description;
                bestIconCode = iconCode;
            }
        }

        ForecastDay build() {
            String dayName = date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.US);
            String monthName = date.getMonth().getDisplayName(TextStyle.SHORT, Locale.US);
            String displayDate = dayName + ", " + monthName + " " + date.getDayOfMonth();

            int averageHumidity = count == 0 ? 0 : humidityTotal / count;
            double averageWind = count == 0 ? 0 : windTotal / count;

            return new ForecastDay(
                    displayDate,
                    minTemp,
                    maxTemp,
                    bestDescription,
                    bestIconCode,
                    averageHumidity,
                    averageWind,
                    maxRainChance
            );
        }
    }

    public record LocationSuggestion(
            String cityName,
            String state,
            String country,
            double latitude,
            double longitude
    ) {
        public String displayName() {
            if (state == null || state.isBlank()) {
                return cityName + ", " + country;
            }

            return cityName + ", " + state + ", " + country;
        }

        public String serialize() {
            return safe(cityName) + "\t"
                    + safe(state) + "\t"
                    + safe(country) + "\t"
                    + latitude + "\t"
                    + longitude;
        }

        private String safe(String value) {
            return value == null ? "" : value.replace("\t", " ");
        }

        public static LocationSuggestion deserialize(String value) {
            String[] parts = value.split("\t");

            if (parts.length != 5) {
                return null;
            }

            try {
                return new LocationSuggestion(
                        parts[0],
                        parts[1],
                        parts[2],
                        Double.parseDouble(parts[3]),
                        Double.parseDouble(parts[4])
                );
            } catch (NumberFormatException ex) {
                return null;
            }
        }
    }

    public record WeatherData(
            String cityName,
            String state,
            String country,
            double latitude,
            double longitude,
            double temperature,
            double feelsLike,
            int humidity,
            double windSpeed,
            String weatherMain,
            String description,
            String iconCode
    ) {
        public String displayLocation() {
            if (state == null || state.isBlank()) {
                return cityName + ", " + country;
            }

            return cityName + ", " + state + ", " + country;
        }
    }

    public record ForecastDay(
            String date,
            double lowTemp,
            double highTemp,
            String description,
            String iconCode,
            int humidity,
            double windSpeed,
            double rainChance
    ) {
    }

    public record HourlyForecast(
            String time,
            double temperature,
            int humidity,
            double windSpeed,
            double rainChance,
            String description,
            String iconCode
    ) {
    }

    public record AirQualityData(
            int aqi,
            String label,
            double pm25,
            double pm10
    ) {
    }
}