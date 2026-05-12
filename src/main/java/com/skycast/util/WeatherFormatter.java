package com.skycast.util;

public class WeatherFormatter {

    private boolean useFahrenheit = true;

    public boolean isUsingFahrenheit() {
        return useFahrenheit;
    }

    public void toggleUnit() {
        useFahrenheit = !useFahrenheit;
    }

    public String getToggleButtonText() {
        if (useFahrenheit) {
            return "Show °C";
        }

        return "Show °F";
    }

    public String formatTemperature(double fahrenheitValue) {
        if (useFahrenheit) {
            return String.format("%.0f°F", fahrenheitValue);
        }

        return String.format("%.0f°C", fahrenheitToCelsius(fahrenheitValue));
    }

    public String formatTemperatureRange(double highFahrenheit, double lowFahrenheit) {
        if (useFahrenheit) {
            return String.format("%.0f° / %.0f°", highFahrenheit, lowFahrenheit);
        }

        return String.format(
                "%.0f° / %.0f°",
                fahrenheitToCelsius(highFahrenheit),
                fahrenheitToCelsius(lowFahrenheit)
        );
    }

    public String formatWindSpeed(double milesPerHour) {
        if (useFahrenheit) {
            return String.format("%.1f mph", milesPerHour);
        }

        double kilometersPerHour = milesPerHour * 1.60934;
        return String.format("%.1f km/h", kilometersPerHour);
    }

    private double fahrenheitToCelsius(double fahrenheit) {
        return (fahrenheit - 32) * 5 / 9;
    }
}