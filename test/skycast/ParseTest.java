package skycast;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Parsing tests use a fixed fixture shaped exactly like Open-Meteo's response. */
class ParseTest {
    static final String GEOCODE_JSON = """
            {"results": [{"id": 1275339, "name": "Mumbai", "latitude": 19.076,
            "longitude": 72.877, "country": "India", "admin1": "Maharashtra"}],
            "generationtime_ms": 0.1}""";

    static final String FORECAST_JSON = """
            {"latitude": 19.076, "longitude": 72.877, "timezone": "Asia/Kolkata",
            "current": {"time": "2026-11-05T07:45", "temperature_2m": 26.3,
            "apparent_temperature": 28.0, "relative_humidity_2m": 66,
            "weather_code": 0, "wind_speed_10m": 7.4},
            "hourly": {"time": ["2026-11-05T00:00", "2026-11-05T01:00"],
            "temperature_2m": [27.1, 26.8]},
            "daily": {"time": ["2026-11-05", "2026-11-06"],
            "weather_code": [0, 1], "temperature_2m_max": [35.2, 35.6],
            "temperature_2m_min": [25.6, 25.1],
            "precipitation_probability_max": [10, 0]}}""";

    @Test
    void parsesGeocodeResults() {
        List<Forecast.Place> places = OpenMeteoClient.parseGeocodeJson(GEOCODE_JSON);
        assertEquals(1, places.size());
        assertEquals("Mumbai, Maharashtra, India", places.get(0).displayName());
        assertEquals(19.076, places.get(0).latitude(), 0.001);
        assertEquals(72.877, places.get(0).longitude(), 0.001);
    }

    @Test
    void emptyGeocodeResults() {
        assertTrue(OpenMeteoClient.parseGeocodeJson("{}").isEmpty());
    }

    @Test
    void parsesForecastResponse() {
        Forecast.Place place = new Forecast.Place("Mumbai", "Maharashtra", "India", 19.076, 72.877);
        Forecast forecast = OpenMeteoClient.parseForecastJson(FORECAST_JSON, place);
        assertEquals(26.3, forecast.current.temperatureC(), 0.001);
        assertEquals(66, forecast.current.humidity(), 0.001);
        assertEquals(0, forecast.current.weatherCode());
        assertEquals("Asia/Kolkata", forecast.timezone);
        assertEquals(2, forecast.days.size());
        assertEquals(35.2, forecast.days.get(0).maxC(), 0.001);
        assertEquals(10, forecast.days.get(0).precipProbability(), 0.001);
        assertEquals(2, forecast.hourly.times().size());
    }
}
