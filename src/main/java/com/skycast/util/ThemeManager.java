package com.skycast.util;

import javafx.scene.layout.VBox;

public class ThemeManager {

    private boolean darkThemeEnabled = false;

    public void toggleTheme() {
        darkThemeEnabled = !darkThemeEnabled;
    }

    public String getThemeButtonText() {
        if (darkThemeEnabled) {
            return "Theme: Dark";
        }

        return "Theme: Default";
    }

    public void applyTheme(VBox root, String weatherMain, String iconCode) {
        if (root == null) {
            return;
        }

        root.getStyleClass().removeAll(
                "background-default",
                "background-clear-day",
                "background-clear-night",
                "background-clouds",
                "background-rain",
                "background-thunderstorm",
                "background-snow",
                "background-mist",
                "background-dark"
        );

        if (darkThemeEnabled) {
            root.getStyleClass().add("background-dark");
            return;
        }

        String weather = weatherMain == null ? "" : weatherMain.toLowerCase();
        boolean isNight = iconCode != null && iconCode.endsWith("n");

        if (weather.contains("clear") && isNight) {
            root.getStyleClass().add("background-clear-night");
        } else if (weather.contains("clear")) {
            root.getStyleClass().add("background-clear-day");
        } else if (weather.contains("cloud")) {
            root.getStyleClass().add("background-clouds");
        } else if (weather.contains("rain") || weather.contains("drizzle")) {
            root.getStyleClass().add("background-rain");
        } else if (weather.contains("thunderstorm")) {
            root.getStyleClass().add("background-thunderstorm");
        } else if (weather.contains("snow")) {
            root.getStyleClass().add("background-snow");
        } else if (weather.contains("mist")
                || weather.contains("fog")
                || weather.contains("haze")
                || weather.contains("smoke")
                || weather.contains("dust")) {
            root.getStyleClass().add("background-mist");
        } else {
            root.getStyleClass().add("background-default");
        }
    }
}