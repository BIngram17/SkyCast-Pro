package com.skycast;

import com.skycast.api.WeatherService;
import com.skycast.api.WeatherService.AirQualityData;
import com.skycast.api.WeatherService.ForecastDay;
import com.skycast.api.WeatherService.HourlyForecast;
import com.skycast.api.WeatherService.LocationSuggestion;
import com.skycast.api.WeatherService.WeatherData;
import com.skycast.storage.LocationStorage;
import com.skycast.util.MapLauncher;
import com.skycast.util.ThemeManager;
import com.skycast.util.WeatherFormatter;
import com.skycast.util.WeatherInsightBuilder;
import com.skycast.util.WeatherSymbolMapper;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

public class Main extends Application {

    private static final int MAX_RECENT_LOCATIONS = 6;
    private static final int MAX_FAVORITES = 6;

    private final WeatherService weatherService = new WeatherService();
    private final WeatherFormatter weatherFormatter = new WeatherFormatter();
    private final WeatherSymbolMapper weatherSymbolMapper = new WeatherSymbolMapper();
    private final WeatherInsightBuilder weatherInsightBuilder = new WeatherInsightBuilder(weatherFormatter);
    private final LocationStorage locationStorage = new LocationStorage(Main.class);
    private final MapLauncher mapLauncher = new MapLauncher();
    private final ThemeManager themeManager = new ThemeManager();

    private VBox root;
    private Label tempLabel;
    private Label conditionLabel;
    private Label detailsLabel;
    private Label statusLabel;
    private Label currentWeatherIconLabel;
    private Label summaryLabel;
    private Label alertLabel;
    private Label recommendationLabel;
    private Label airQualityLabel;
    private TextField cityInput;
    private Button searchButton;
    private Button unitToggleButton;
    private Button saveLocationButton;
    private Button themeButton;
    private VBox suggestionPanel;
    private FlowPane favoritesPane;
    private FlowPane recentPane;
    private HBox forecastRow;
    private HBox hourlyRow;

    private WeatherData currentWeatherData;
    private AirQualityData currentAirQuality;
    private List<ForecastDay> currentForecast;
    private List<HourlyForecast> currentHourlyForecast;
    private LocationSuggestion selectedLocation;
    private int suggestionRequestCounter = 0;
    private boolean programmaticTextChange = false;

    private final List<LocationSuggestion> favoriteLocations = new ArrayList<>();
    private final List<LocationSuggestion> recentLocations = new ArrayList<>();

    @Override
    public void start(Stage primaryStage) {
        loadStoredLocations();

        Label titleLabel = new Label("SkyCast Pro");
        titleLabel.getStyleClass().add("title-label");

        cityInput = new TextField();
        cityInput.setPromptText("Enter city, state, country: Rio Linda, CA, US");
        cityInput.getStyleClass().add("city-input");

        searchButton = new Button("Search Weather");
        searchButton.getStyleClass().add("search-button");

        unitToggleButton = new Button(weatherFormatter.getToggleButtonText());
        unitToggleButton.getStyleClass().add("secondary-button");

        saveLocationButton = new Button("Save Location");
        saveLocationButton.getStyleClass().add("secondary-button");
        saveLocationButton.setDisable(true);

        themeButton = new Button(themeManager.getThemeButtonText());
        themeButton.getStyleClass().add("secondary-button");

        HBox searchBox = new HBox(10);
        searchBox.setAlignment(Pos.CENTER);
        searchBox.getChildren().addAll(cityInput, searchButton, unitToggleButton, saveLocationButton, themeButton);

        suggestionPanel = new VBox(4);
        suggestionPanel.getStyleClass().add("suggestion-panel");
        suggestionPanel.setMaxWidth(470);
        suggestionPanel.setVisible(false);
        suggestionPanel.setManaged(false);

        Label searchTipLabel = new Label("Tip: Start typing a location, then choose the best city match from the suggestions.");
        searchTipLabel.getStyleClass().add("search-tip-label");

        favoritesPane = new FlowPane(8, 8);
        favoritesPane.setAlignment(Pos.CENTER);
        favoritesPane.getStyleClass().add("location-panel");

        recentPane = new FlowPane(8, 8);
        recentPane.setAlignment(Pos.CENTER);
        recentPane.getStyleClass().add("location-panel");

        updateStoredLocationButtons();

        currentWeatherIconLabel = new Label("☼");
        currentWeatherIconLabel.getStyleClass().add("main-weather-icon");

        tempLabel = new Label("--°F");
        tempLabel.getStyleClass().add("temperature-label");

        conditionLabel = new Label("Search for a city to view live weather");
        conditionLabel.getStyleClass().add("condition-label");

        detailsLabel = new Label("Feels Like: --°F   Humidity: --%   Wind: -- mph");
        detailsLabel.getStyleClass().add("details-label");

        statusLabel = new Label("");
        statusLabel.getStyleClass().add("status-label");

        HBox currentWeatherTopRow = new HBox(22);
        currentWeatherTopRow.setAlignment(Pos.CENTER);
        currentWeatherTopRow.getChildren().addAll(currentWeatherIconLabel, tempLabel);

        VBox weatherCard = new VBox(12);
        weatherCard.setAlignment(Pos.CENTER);
        weatherCard.getStyleClass().add("weather-card");
        weatherCard.getChildren().addAll(currentWeatherTopRow, conditionLabel, detailsLabel, statusLabel);

        summaryLabel = new Label("Summary: Search for a location to get a plain-English forecast.");
        summaryLabel.getStyleClass().add("insight-text");
        summaryLabel.setWrapText(true);

        alertLabel = new Label("Alerts: None yet.");
        alertLabel.getStyleClass().add("insight-text");
        alertLabel.setWrapText(true);

        recommendationLabel = new Label("Activity Recommendation: Waiting for weather data.");
        recommendationLabel.getStyleClass().add("insight-text");
        recommendationLabel.setWrapText(true);

        airQualityLabel = new Label("Air Quality: Waiting for location.");
        airQualityLabel.getStyleClass().add("insight-text");
        airQualityLabel.setWrapText(true);

        VBox insightCard = new VBox(10);
        insightCard.setAlignment(Pos.CENTER_LEFT);
        insightCard.getStyleClass().add("insight-card");
        insightCard.getChildren().addAll(summaryLabel, alertLabel, recommendationLabel, airQualityLabel);

        Label forecastTitleLabel = new Label("5-Day Forecast");
        forecastTitleLabel.getStyleClass().add("forecast-title-label");

        forecastRow = new HBox(14);
        forecastRow.setAlignment(Pos.CENTER);
        forecastRow.getStyleClass().add("forecast-row");

        addEmptyForecastCards();

        Label hourlyTitleLabel = new Label("Next 24 Hours");
        hourlyTitleLabel.getStyleClass().add("forecast-title-label");

        hourlyRow = new HBox(10);
        hourlyRow.setAlignment(Pos.CENTER);
        hourlyRow.getStyleClass().add("hourly-row");

        addEmptyHourlyCards();

        HBox mapButtonRow = new HBox(10);
        mapButtonRow.setAlignment(Pos.CENTER);

        Button radarButton = createMapButton("Open Radar", "radar");
        Button windButton = createMapButton("Open Wind Map", "wind");
        Button satelliteButton = createMapButton("Open Satellite", "satellite");

        mapButtonRow.getChildren().addAll(radarButton, windButton, satelliteButton);

        root = new VBox(16);
        root.setPadding(new Insets(28));
        root.setAlignment(Pos.TOP_CENTER);
        root.getStyleClass().addAll("app-background", "background-default");
        root.getChildren().addAll(
                titleLabel,
                searchBox,
                suggestionPanel,
                searchTipLabel,
                favoritesPane,
                recentPane,
                weatherCard,
                insightCard,
                mapButtonRow,
                forecastTitleLabel,
                forecastRow,
                hourlyTitleLabel,
                hourlyRow
        );

        cityInput.textProperty().addListener((observable, oldValue, newValue) -> {
            if (programmaticTextChange) {
                return;
            }

            selectedLocation = null;
            loadLocationSuggestions(newValue);
        });

        searchButton.setOnAction(event -> searchWeather());
        cityInput.setOnAction(event -> searchWeather());
        unitToggleButton.setOnAction(event -> toggleTemperatureUnit());
        saveLocationButton.setOnAction(event -> saveCurrentLocation());
        themeButton.setOnAction(event -> toggleTheme());

        ScrollPane scrollPane = new ScrollPane(root);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.getStyleClass().add("app-scroll-pane");

        Scene scene = new Scene(scrollPane, 1180, 760);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());

        primaryStage.setTitle("SkyCast Pro");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(1000);
        primaryStage.setMinHeight(700);
        primaryStage.show();
    }

    private Button createMapButton(String text, String layer) {
        Button button = new Button(text);
        button.getStyleClass().add("secondary-button");
        button.setDisable(true);

        button.setOnAction(event -> openMap(layer));
        button.disableProperty().bind(saveLocationButton.disabledProperty());

        return button;
    }

    private void loadLocationSuggestions(String query) {
        String cleanedQuery = query == null ? "" : query.trim();

        if (cleanedQuery.length() < 2) {
            hideSuggestions();
            return;
        }

        int requestId = ++suggestionRequestCounter;

        Thread suggestionThread = new Thread(() -> {
            try {
                List<LocationSuggestion> suggestions = weatherService.getLocationSuggestions(cleanedQuery);

                Platform.runLater(() -> {
                    if (requestId != suggestionRequestCounter) {
                        return;
                    }

                    showSuggestions(suggestions);
                });

            } catch (Exception ignored) {
                Platform.runLater(this::hideSuggestions);
            }
        });

        suggestionThread.setDaemon(true);
        suggestionThread.start();
    }

    private void showSuggestions(List<LocationSuggestion> suggestions) {
        suggestionPanel.getChildren().clear();

        if (suggestions == null || suggestions.isEmpty()) {
            hideSuggestions();
            return;
        }

        for (LocationSuggestion suggestion : suggestions) {
            Button suggestionButton = new Button(suggestion.displayName());
            suggestionButton.getStyleClass().add("suggestion-button");
            suggestionButton.setMaxWidth(Double.MAX_VALUE);
            suggestionButton.setAlignment(Pos.CENTER_LEFT);

            suggestionButton.setOnAction(event -> {
                selectedLocation = suggestion;

                programmaticTextChange = true;
                cityInput.setText(suggestion.displayName());
                cityInput.positionCaret(cityInput.getText().length());
                programmaticTextChange = false;

                hideSuggestions();
                searchWeather();
            });

            suggestionPanel.getChildren().add(suggestionButton);
        }

        suggestionPanel.setVisible(true);
        suggestionPanel.setManaged(true);
    }

    private void hideSuggestions() {
        suggestionPanel.getChildren().clear();
        suggestionPanel.setVisible(false);
        suggestionPanel.setManaged(false);
    }

    private void searchWeather() {
        String city = cityInput.getText().trim();

        if (city.isEmpty()) {
            statusLabel.setText("Please enter a city name.");
            return;
        }

        hideSuggestions();
        setLoadingState(true);
        statusLabel.setText("Loading weather data...");

        Thread weatherThread = new Thread(() -> {
            try {
                LocationSuggestion locationToSearch = selectedLocation;

                if (locationToSearch == null) {
                    locationToSearch = weatherService.resolveLocation(city);
                }

                WeatherData weatherData = weatherService.getCurrentWeather(locationToSearch);
                List<ForecastDay> forecast = weatherService.getFiveDayForecast(locationToSearch);
                List<HourlyForecast> hourlyForecast = weatherService.getHourlyForecast(locationToSearch);
                AirQualityData airQuality = weatherService.getAirQuality(locationToSearch);

                LocationSuggestion finalLocationToSearch = locationToSearch;

                Platform.runLater(() -> {
                    selectedLocation = finalLocationToSearch;
                    currentWeatherData = weatherData;
                    currentForecast = forecast;
                    currentHourlyForecast = hourlyForecast;
                    currentAirQuality = airQuality;

                    programmaticTextChange = true;
                    cityInput.setText(finalLocationToSearch.displayName());
                    cityInput.positionCaret(cityInput.getText().length());
                    programmaticTextChange = false;

                    addRecentLocation(finalLocationToSearch);

                    updateCurrentWeatherDisplay();
                    updateForecastCards(currentForecast);
                    updateHourlyCards(currentHourlyForecast);
                    updateInsightCard();
                    updateBackground();

                    saveLocationButton.setDisable(false);
                    statusLabel.setText("");
                    setLoadingState(false);
                });

            } catch (Exception ex) {
                Platform.runLater(() -> {
                    statusLabel.setText(ex.getMessage());
                    setLoadingState(false);
                });
            }
        });

        weatherThread.setDaemon(true);
        weatherThread.start();
    }

    private void updateCurrentWeatherDisplay() {
        if (currentWeatherData == null) {
            return;
        }

        currentWeatherIconLabel.setText(
                weatherSymbolMapper.getWeatherSymbol(currentWeatherData.weatherMain(), currentWeatherData.iconCode())
        );

        tempLabel.setText(weatherFormatter.formatTemperature(currentWeatherData.temperature()));

        conditionLabel.setText(
                currentWeatherData.description()
                        + " in "
                        + currentWeatherData.displayLocation()
        );

        detailsLabel.setText(String.format(
                "Feels Like: %s   Humidity: %d%%   Wind: %s",
                weatherFormatter.formatTemperature(currentWeatherData.feelsLike()),
                currentWeatherData.humidity(),
                weatherFormatter.formatWindSpeed(currentWeatherData.windSpeed())
        ));
    }

    private void updateInsightCard() {
        if (currentWeatherData == null) {
            return;
        }

        summaryLabel.setText("Summary: " + weatherInsightBuilder.buildWeatherSummary(currentWeatherData));
        alertLabel.setText("Alerts: " + weatherInsightBuilder.buildWeatherAlerts(currentWeatherData, currentForecast));
        recommendationLabel.setText("Activity Recommendation: " + weatherInsightBuilder.buildActivityRecommendation(currentWeatherData));

        if (currentAirQuality == null) {
            airQualityLabel.setText("Air Quality: Unavailable.");
        } else {
            airQualityLabel.setText(String.format(
                    "Air Quality: %s, AQI %d, PM2.5 %.1f, PM10 %.1f",
                    currentAirQuality.label(),
                    currentAirQuality.aqi(),
                    currentAirQuality.pm25(),
                    currentAirQuality.pm10()
            ));
        }
    }

    private void updateForecastCards(List<ForecastDay> forecast) {
        forecastRow.getChildren().clear();

        if (forecast == null || forecast.isEmpty()) {
            Label noForecastLabel = new Label("Forecast data is not available for this location.");
            noForecastLabel.getStyleClass().add("status-label");
            forecastRow.getChildren().add(noForecastLabel);
            return;
        }

        for (ForecastDay day : forecast) {
            forecastRow.getChildren().add(createForecastCard(day));
        }
    }

    private VBox createForecastCard(ForecastDay day) {
        Label iconLabel = new Label(weatherSymbolMapper.getWeatherSymbolFromIconCode(day.iconCode()));
        iconLabel.getStyleClass().add("forecast-weather-icon");

        Label dateLabel = new Label(day.date());
        dateLabel.getStyleClass().add("forecast-date-label");

        Label tempRangeLabel = new Label(weatherFormatter.formatTemperatureRange(day.highTemp(), day.lowTemp()));
        tempRangeLabel.getStyleClass().add("forecast-temp-label");

        Label descriptionLabel = new Label(day.description());
        descriptionLabel.getStyleClass().add("forecast-description-label");
        descriptionLabel.setWrapText(true);

        Label details = new Label(String.format(
                "Hum %d%%  Wind %s  Rain %.0f%%",
                day.humidity(),
                weatherFormatter.formatWindSpeed(day.windSpeed()),
                day.rainChance() * 100
        ));
        details.getStyleClass().add("forecast-detail-label");
        details.setWrapText(true);

        VBox card = new VBox(7);
        card.setAlignment(Pos.CENTER);
        card.getStyleClass().add("forecast-card");
        card.getChildren().addAll(dateLabel, iconLabel, tempRangeLabel, descriptionLabel, details);

        return card;
    }

    private void updateHourlyCards(List<HourlyForecast> hourlyForecasts) {
        hourlyRow.getChildren().clear();

        if (hourlyForecasts == null || hourlyForecasts.isEmpty()) {
            addEmptyHourlyCards();
            return;
        }

        for (HourlyForecast hour : hourlyForecasts) {
            hourlyRow.getChildren().add(createHourlyCard(hour));
        }
    }

    private VBox createHourlyCard(HourlyForecast hour) {
        Label timeLabel = new Label(hour.time());
        timeLabel.getStyleClass().add("hourly-time-label");

        Label iconLabel = new Label(weatherSymbolMapper.getWeatherSymbolFromIconCode(hour.iconCode()));
        iconLabel.getStyleClass().add("hourly-weather-icon");

        Label tempLabel = new Label(weatherFormatter.formatTemperature(hour.temperature()));
        tempLabel.getStyleClass().add("hourly-temp-label");

        Label detailLabel = new Label(String.format(
                "Rain %.0f%%\nWind %s",
                hour.rainChance() * 100,
                weatherFormatter.formatWindSpeed(hour.windSpeed())
        ));
        detailLabel.getStyleClass().add("hourly-detail-label");

        VBox card = new VBox(5);
        card.setAlignment(Pos.CENTER);
        card.getStyleClass().add("hourly-card");
        card.getChildren().addAll(timeLabel, iconLabel, tempLabel, detailLabel);

        return card;
    }

    private void addEmptyForecastCards() {
        forecastRow.getChildren().clear();

        for (int i = 0; i < 5; i++) {
            Label dateLabel = new Label("---");
            dateLabel.getStyleClass().add("forecast-date-label");

            Label iconLabel = new Label("☼");
            iconLabel.getStyleClass().add("forecast-weather-icon");

            Label tempRangeLabel = new Label("--° / --°");
            tempRangeLabel.getStyleClass().add("forecast-temp-label");

            Label descriptionLabel = new Label("Search city");
            descriptionLabel.getStyleClass().add("forecast-description-label");

            VBox card = new VBox(8);
            card.setAlignment(Pos.CENTER);
            card.getStyleClass().add("forecast-card");
            card.getChildren().addAll(dateLabel, iconLabel, tempRangeLabel, descriptionLabel);

            forecastRow.getChildren().add(card);
        }
    }

    private void addEmptyHourlyCards() {
        hourlyRow.getChildren().clear();

        for (int i = 0; i < 8; i++) {
            Label timeLabel = new Label("---");
            timeLabel.getStyleClass().add("hourly-time-label");

            Label iconLabel = new Label("☼");
            iconLabel.getStyleClass().add("hourly-weather-icon");

            Label tempLabel = new Label("--°");
            tempLabel.getStyleClass().add("hourly-temp-label");

            VBox card = new VBox(5);
            card.setAlignment(Pos.CENTER);
            card.getStyleClass().add("hourly-card");
            card.getChildren().addAll(timeLabel, iconLabel, tempLabel);

            hourlyRow.getChildren().add(card);
        }
    }

    private void saveCurrentLocation() {
        if (selectedLocation == null) {
            statusLabel.setText("Search for a location before saving it.");
            return;
        }

        locationStorage.removeMatchingLocation(favoriteLocations, selectedLocation);
        favoriteLocations.add(0, selectedLocation);

        while (favoriteLocations.size() > MAX_FAVORITES) {
            favoriteLocations.remove(favoriteLocations.size() - 1);
        }

        locationStorage.saveFavorites(favoriteLocations);
        updateStoredLocationButtons();
        statusLabel.setText("Saved " + selectedLocation.displayName() + " to favorites.");
    }

    private void addRecentLocation(LocationSuggestion location) {
        locationStorage.removeMatchingLocation(recentLocations, location);
        recentLocations.add(0, location);

        while (recentLocations.size() > MAX_RECENT_LOCATIONS) {
            recentLocations.remove(recentLocations.size() - 1);
        }

        locationStorage.saveRecentLocations(recentLocations);
        updateStoredLocationButtons();
    }

    private void updateStoredLocationButtons() {
        favoritesPane.getChildren().clear();
        recentPane.getChildren().clear();

        if (!favoriteLocations.isEmpty()) {
            Label favoritesLabel = new Label("Favorites:");
            favoritesLabel.getStyleClass().add("location-section-label");
            favoritesPane.getChildren().add(favoritesLabel);

            for (LocationSuggestion location : favoriteLocations) {
                favoritesPane.getChildren().add(createLocationButton(location));
            }
        }

        if (!recentLocations.isEmpty()) {
            Label recentLabel = new Label("Recent:");
            recentLabel.getStyleClass().add("location-section-label");
            recentPane.getChildren().add(recentLabel);

            for (LocationSuggestion location : recentLocations) {
                recentPane.getChildren().add(createLocationButton(location));
            }
        }
    }

    private Button createLocationButton(LocationSuggestion location) {
        Button button = new Button(location.displayName());
        button.getStyleClass().add("location-chip");

        button.setOnAction(event -> {
            selectedLocation = location;

            programmaticTextChange = true;
            cityInput.setText(location.displayName());
            cityInput.positionCaret(cityInput.getText().length());
            programmaticTextChange = false;

            searchWeather();
        });

        return button;
    }

    private void loadStoredLocations() {
        favoriteLocations.clear();
        recentLocations.clear();

        favoriteLocations.addAll(locationStorage.loadFavorites());
        recentLocations.addAll(locationStorage.loadRecentLocations());
    }

    private void toggleTemperatureUnit() {
        weatherFormatter.toggleUnit();
        unitToggleButton.setText(weatherFormatter.getToggleButtonText());

        updateCurrentWeatherDisplay();
        updateForecastCards(currentForecast);
        updateHourlyCards(currentHourlyForecast);
        updateInsightCard();
    }

    private void toggleTheme() {
        themeManager.toggleTheme();
        themeButton.setText(themeManager.getThemeButtonText());
        updateBackground();
    }

    private void openMap(String layer) {
        try {
            mapLauncher.openWeatherMap(currentWeatherData, layer);
        } catch (Exception ex) {
            statusLabel.setText(ex.getMessage());
        }
    }

    private void updateBackground() {
        if (currentWeatherData == null) {
            themeManager.applyTheme(root, "", "");
            return;
        }

        themeManager.applyTheme(root, currentWeatherData.weatherMain(), currentWeatherData.iconCode());
    }

    private void setLoadingState(boolean isLoading) {
        searchButton.setDisable(isLoading);
        cityInput.setDisable(isLoading);

        if (isLoading) {
            searchButton.setText("Loading...");
        } else {
            searchButton.setText("Search Weather");
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}