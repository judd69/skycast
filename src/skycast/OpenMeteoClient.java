package skycast;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Client for the free, keyless Open-Meteo APIs:
 * geocoding (city name to coordinates) and forecast (current + daily + hourly).
 * Network code is isolated here so the parsing logic stays unit-testable
 * without any network access (see parseGeocodeJson / parseForecastJson).
 */
public final class OpenMeteoClient {
    public static final String GEOCODING_URL = "https://geocoding-api.open-meteo.com/v1/search";
    public static final String FORECAST_URL = "https://api.open-meteo.com/v1/forecast";

    private final HttpClient http;

    public OpenMeteoClient() {
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public List<Forecast.Place> geocode(String query) throws IOException, InterruptedException {
        String url = GEOCODING_URL + "?name=" + URLEncoder.encode(query, StandardCharsets.UTF_8)
                + "&count=5&language=en&format=json";
        String body = get(url);
        return parseGeocodeJson(body);
    }

    public Forecast forecast(Forecast.Place place, int days) throws IOException, InterruptedException {
        String url = FORECAST_URL
                + "?latitude=" + place.latitude()
                + "&longitude=" + place.longitude()
                + "&current=temperature_2m,apparent_temperature,relative_humidity_2m,weather_code,wind_speed_10m"
                + "&hourly=temperature_2m"
                + "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max"
                + "&timezone=auto&forecast_days=" + days;
        String body = get(url);
        return parseForecastJson(body, place);
    }

    private String get(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("User-Agent", "Skycast/1.0 (https://github.com/judd69/skycast)")
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Open-Meteo returned HTTP " + response.statusCode() + " for " + url);
        }
        return response.body();
    }

    /** Pure parsing: geocoding response to places. */
    public static List<Forecast.Place> parseGeocodeJson(String json) {
        Map<String, Object> root = Json.obj(Json.parse(json));
        List<Forecast.Place> places = new ArrayList<>();
        Object results = root.get("results");
        if (results == null) {
            return places;
        }
        for (Object item : Json.arr(results)) {
            Map<String, Object> r = Json.obj(item);
            String admin = r.get("admin1") instanceof String s ? s : "";
            String country = r.get("country") instanceof String s ? s : "";
            places.add(new Forecast.Place(
                    Json.str(Json.field(r, "name")),
                    admin, country,
                    Json.num(Json.field(r, "latitude")),
                    Json.num(Json.field(r, "longitude"))));
        }
        return places;
    }

    /** Pure parsing: forecast response to a Forecast for the given place. */
    public static Forecast parseForecastJson(String json, Forecast.Place place) {
        Map<String, Object> root = Json.obj(Json.parse(json));
        String timezone = root.get("timezone") instanceof String s ? s : "UTC";

        Map<String, Object> cur = Json.obj(Json.field(root, "current"));
        Forecast.Current current = new Forecast.Current(
                Json.str(Json.field(cur, "time")),
                Json.num(Json.field(cur, "temperature_2m")),
                cur.get("apparent_temperature") instanceof Double d ? d : Json.num(Json.field(cur, "temperature_2m")),
                cur.get("relative_humidity_2m") instanceof Double d ? d : 0,
                (int) Json.num(Json.field(cur, "weather_code")),
                Json.num(Json.field(cur, "wind_speed_10m")));

        Map<String, Object> daily = Json.obj(Json.field(root, "daily"));
        List<Object> dates = Json.arr(Json.field(daily, "time"));
        List<Object> maxs = Json.arr(Json.field(daily, "temperature_2m_max"));
        List<Object> mins = Json.arr(Json.field(daily, "temperature_2m_min"));
        List<Object> codes = Json.arr(Json.field(daily, "weather_code"));
        Object precipRaw = daily.get("precipitation_probability_max");
        List<Object> precips = precipRaw instanceof List<?> ? Json.arr(precipRaw) : List.of();
        List<Forecast.Day> days = new ArrayList<>();
        for (int i = 0; i < dates.size(); i++) {
            double precip = (i < precips.size() && precips.get(i) instanceof Double d) ? d : 0;
            days.add(new Forecast.Day(
                    Json.str(dates.get(i)),
                    Json.num(maxs.get(i)), Json.num(mins.get(i)),
                    precip, (int) Json.num(codes.get(i))));
        }

        Map<String, Object> hourly = Json.obj(Json.field(root, "hourly"));
        List<String> hours = new ArrayList<>();
        List<Double> temps = new ArrayList<>();
        for (Object t : Json.arr(Json.field(hourly, "time"))) {
            hours.add(Json.str(t));
        }
        for (Object t : Json.arr(Json.field(hourly, "temperature_2m"))) {
            temps.add(Json.num(t));
        }
        return new Forecast(place, current, days, new Forecast.Hourly(hours, temps), timezone);
    }
}
