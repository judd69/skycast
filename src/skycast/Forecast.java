package skycast;

import java.util.List;

/** The forecast data Skycast works with, in plain Java types. */
public final class Forecast {
    public record Place(String name, String admin, String country, double latitude, double longitude) {
        public String displayName() {
            StringBuilder sb = new StringBuilder(name);
            if (admin != null && !admin.isEmpty()) {
                sb.append(", ").append(admin);
            }
            if (country != null && !country.isEmpty()) {
                sb.append(", ").append(country);
            }
            return sb.toString();
        }
    }

    public record Current(String time, double temperatureC, double apparentC,
                          double humidity, int weatherCode, double windKmh) {
    }

    public record Day(String date, double maxC, double minC, double precipProbability, int weatherCode) {
    }

    public record Hourly(List<String> times, List<Double> temperaturesC) {
    }

    public final Place place;
    public final Current current;
    public final List<Day> days;
    public final Hourly hourly;
    public final String timezone;

    public Forecast(Place place, Current current, List<Day> days, Hourly hourly, String timezone) {
        this.place = place;
        this.current = current;
        this.days = List.copyOf(days);
        this.hourly = hourly;
        this.timezone = timezone;
    }
}
