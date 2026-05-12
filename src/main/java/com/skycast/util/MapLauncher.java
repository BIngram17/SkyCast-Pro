package com.skycast.util;

import com.skycast.api.WeatherService.WeatherData;

import java.awt.Desktop;
import java.net.URI;

public class MapLauncher {

    public void openWeatherMap(WeatherData weatherData, String layer) throws Exception {
        if (weatherData == null) {
            throw new IllegalArgumentException("Search for a city before opening a map.");
        }

        String layerName = switch (layer) {
            case "wind" -> "wind";
            case "satellite" -> "satellite";
            default -> "radar";
        };

        String mapUrl = String.format(
                "https://www.windy.com/?%s,%.4f,%.4f,9",
                layerName,
                weatherData.latitude(),
                weatherData.longitude()
        );

        if (!Desktop.isDesktopSupported()) {
            throw new UnsupportedOperationException("Map opening is not supported on this system.");
        }

        Desktop.getDesktop().browse(new URI(mapUrl));
    }
}
