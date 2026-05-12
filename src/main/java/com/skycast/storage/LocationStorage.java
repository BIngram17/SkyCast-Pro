package com.skycast.storage;

import com.skycast.api.WeatherService.LocationSuggestion;

import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

public class LocationStorage {

    private static final String FAVORITES_KEY = "favorites";
    private static final String RECENT_KEY = "recent";

    private final Preferences preferences;

    public LocationStorage(Class<?> preferencesOwner) {
        this.preferences = Preferences.userNodeForPackage(preferencesOwner);
    }

    public List<LocationSuggestion> loadFavorites() {
        return loadLocationList(FAVORITES_KEY);
    }

    public List<LocationSuggestion> loadRecentLocations() {
        return loadLocationList(RECENT_KEY);
    }

    public void saveFavorites(List<LocationSuggestion> favorites) {
        saveLocationList(FAVORITES_KEY, favorites);
    }

    public void saveRecentLocations(List<LocationSuggestion> recentLocations) {
        saveLocationList(RECENT_KEY, recentLocations);
    }

    public void removeMatchingLocation(List<LocationSuggestion> locations, LocationSuggestion locationToRemove) {
        if (locationToRemove == null) {
            return;
        }

        locations.removeIf(location ->
                safeEquals(location.cityName(), locationToRemove.cityName())
                        && safeEquals(location.state(), locationToRemove.state())
                        && safeEquals(location.country(), locationToRemove.country())
        );
    }

    private List<LocationSuggestion> loadLocationList(String key) {
        List<LocationSuggestion> locations = new ArrayList<>();
        String storedValue = preferences.get(key, "");

        if (storedValue.isBlank()) {
            return locations;
        }

        String[] entries = storedValue.split("\\|\\|");

        for (String entry : entries) {
            LocationSuggestion location = LocationSuggestion.deserialize(entry);

            if (location != null) {
                locations.add(location);
            }
        }

        return locations;
    }

    private void saveLocationList(String key, List<LocationSuggestion> locations) {
        List<String> values = new ArrayList<>();

        for (LocationSuggestion location : locations) {
            values.add(location.serialize());
        }

        preferences.put(key, String.join("||", values));
    }

    private boolean safeEquals(String first, String second) {
        String safeFirst = first == null ? "" : first;
        String safeSecond = second == null ? "" : second;

        return safeFirst.equalsIgnoreCase(safeSecond);
    }
}
