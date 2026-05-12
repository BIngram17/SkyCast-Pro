package com.skycast.util;

public class WeatherSymbolMapper {

    public String getWeatherSymbol(String weatherMain, String iconCode) {
        String weather = weatherMain == null ? "" : weatherMain.toLowerCase();
        boolean isNight = iconCode != null && iconCode.endsWith("n");

        if (weather.contains("thunderstorm")) {
            return "⚡";
        }

        if (weather.contains("drizzle")) {
            return "☂";
        }

        if (weather.contains("rain")) {
            return "☔";
        }

        if (weather.contains("snow")) {
            return "❄";
        }

        if (weather.contains("mist")
                || weather.contains("fog")
                || weather.contains("haze")
                || weather.contains("smoke")
                || weather.contains("dust")) {
            return "≋";
        }

        if (weather.contains("cloud")) {
            return "☁";
        }

        if (weather.contains("clear")) {
            return isNight ? "☾" : "☼";
        }

        return "☼";
    }

    public String getWeatherSymbolFromIconCode(String iconCode) {
        if (iconCode == null || iconCode.length() < 2) {
            return "☼";
        }

        return switch (iconCode.substring(0, 2)) {
            case "01" -> iconCode.endsWith("n") ? "☾" : "☼";
            case "02", "03", "04" -> "☁";
            case "09" -> "☔";
            case "10" -> "☂";
            case "11" -> "⚡";
            case "13" -> "❄";
            case "50" -> "≋";
            default -> "☼";
        };
    }
}