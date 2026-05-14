\# SkyCast Pro



SkyCast Pro is a modern JavaFX weather dashboard that provides live weather data, location search suggestions, 5-day forecasts, hourly forecasts, air quality information, saved locations, recent searches, weather-based backgrounds, and external map links.



This project was built as a Java portfolio application to demonstrate API integration, JavaFX UI development, Maven project structure, local configuration management, user-focused design, and clean code organization.



\## Features



\- Live current weather by city, state, and country

\- Smart location search suggestions using OpenWeatherMap geocoding

\- Supports lowercase and partial city searches

\- Displays cleaned city, state, and country after search

\- 5-day forecast

\- Next 24-hour forecast

\- Fahrenheit and Celsius toggle

\- Wind speed conversion between mph and km/h

\- Air quality data with AQI, PM2.5, and PM10

\- Weather alerts based on temperature, wind, humidity, and rain chance

\- Outdoor activity recommendation with a 1-10 rating

\- Favorite locations

\- Recent search history

\- Dynamic weather-based backgrounds

\- Default and dark theme options

\- External map buttons for radar, wind, and satellite views

\- API key protected through local configuration

\- Scrollable UI for smaller screens



\## Tech Stack



\- Java

\- JavaFX

\- Maven

\- OpenWeatherMap API

\- Gson

\- Java Preferences API

\- VS Code



\## Project Structure



```text

skycast-pro/

├── pom.xml

├── README.md

├── .gitignore

├── local.properties.example

├── screenshots/

│   └── add-screenshots-here.txt

└── src/

&#x20;   └── main/

&#x20;       ├── java/

&#x20;       │   └── com/

&#x20;       │       └── skycast/

&#x20;       │           ├── Main.java

&#x20;       │           ├── api/

&#x20;       │           │   └── WeatherService.java

&#x20;       │           ├── config/

&#x20;       │           │   └── AppConfig.java

&#x20;       │           ├── storage/

&#x20;       │           │   └── LocationStorage.java

&#x20;       │           └── util/

&#x20;       │               ├── MapLauncher.java

&#x20;       │               ├── ThemeManager.java

&#x20;       │               ├── WeatherFormatter.java

&#x20;       │               ├── WeatherInsightBuilder.java

&#x20;       │               └── WeatherSymbolMapper.java

&#x20;       └── resources/

&#x20;           └── styles.css

