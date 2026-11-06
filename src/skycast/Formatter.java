package skycast;

import java.util.Locale;

/** Text and JSON rendering of a Forecast. Fahrenheit conversion lives here. */
public final class Formatter {
    private Formatter() {
    }

    public static double toF(double celsius) {
        return celsius * 9.0 / 5.0 + 32;
    }

    public static String temp(double celsius, boolean fahrenheit) {
        double value = fahrenheit ? toF(celsius) : celsius;
        return String.format(Locale.ROOT, "%.1f°%s", value, fahrenheit ? "F" : "C");
    }

    public static String text(Forecast forecast, boolean fahrenheit) {
        StringBuilder sb = new StringBuilder();
        sb.append(forecast.place.displayName()).append("\n");
        sb.append("Now: ").append(temp(forecast.current.temperatureC(), fahrenheit))
                .append(" (feels ").append(temp(forecast.current.apparentC(), fahrenheit)).append("), ")
                .append(WeatherCode.describe(forecast.current.weatherCode()))
                .append(", humidity ").append((int) forecast.current.humidity()).append("%")
                .append(", wind ").append(String.format(Locale.ROOT, "%.1f", forecast.current.windKmh()))
                .append(" km/h\n");
        sb.append(Advice.summarize(forecast)).append("\n");
        sb.append("Daily forecast:\n");
        for (Forecast.Day day : forecast.days) {
            sb.append("  ").append(day.date()).append("  ")
                    .append(WeatherCode.describe(day.weatherCode())).append("  ")
                    .append(temp(day.minC(), fahrenheit)).append(" – ")
                    .append(temp(day.maxC(), fahrenheit))
                    .append("  rain ").append((int) day.precipProbability()).append("%\n");
        }
        sb.append("Source: Open-Meteo (").append(forecast.timezone).append(")");
        return sb.toString();
    }

    public static String json(Forecast forecast) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"place\":").append(quote(forecast.place.displayName()))
                .append(",\"latitude\":").append(forecast.place.latitude())
                .append(",\"longitude\":").append(forecast.place.longitude())
                .append(",\"timezone\":").append(quote(forecast.timezone))
                .append(",\"current\":{\"time\":").append(quote(forecast.current.time()))
                .append(",\"temperature_c\":").append(forecast.current.temperatureC())
                .append(",\"apparent_c\":").append(forecast.current.apparentC())
                .append(",\"humidity\":").append(forecast.current.humidity())
                .append(",\"weather_code\":").append(forecast.current.weatherCode())
                .append(",\"condition\":").append(quote(WeatherCode.describe(forecast.current.weatherCode())))
                .append(",\"wind_kmh\":").append(forecast.current.windKmh())
                .append("},\"advice\":").append(quote(Advice.summarize(forecast)))
                .append(",\"daily\":[");
        for (int i = 0; i < forecast.days.size(); i++) {
            Forecast.Day day = forecast.days.get(i);
            if (i > 0) {
                sb.append(",");
            }
            sb.append("{\"date\":").append(quote(day.date()))
                    .append(",\"max_c\":").append(day.maxC())
                    .append(",\"min_c\":").append(day.minC())
                    .append(",\"precipitation_probability\":").append(day.precipProbability())
                    .append(",\"weather_code\":").append(day.weatherCode())
                    .append(",\"condition\":").append(quote(WeatherCode.describe(day.weatherCode())))
                    .append("}");
        }
        sb.append("]}");
        return sb.toString();
    }

    private static String quote(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
