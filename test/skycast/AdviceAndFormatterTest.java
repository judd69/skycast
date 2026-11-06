package skycast;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class AdviceAndFormatterTest {
    private static Forecast forecast(double currentTemp, int code, double wind,
                                     double todayPrecip, double weekMax, double weekMin) {
        Forecast.Place place = new Forecast.Place("Test City", "", "", 0, 0);
        Forecast.Current current = new Forecast.Current("2026-11-05T07:45",
                currentTemp, currentTemp, 50, code, wind);
        Forecast.Day day = new Forecast.Day("2026-11-05", weekMax, weekMin, todayPrecip, code);
        return new Forecast(place, current, List.of(day),
                new Forecast.Hourly(List.of(), List.of()), "UTC");
    }

    @Test
    void rainAdviceWhenPrecipitationLikely() {
        assertTrue(Advice.summarize(forecast(20, 0, 5, 80, 22, 18))
                .contains("umbrella"));
    }

    @Test
    void heatAndColdAdvice() {
        String advice = Advice.summarize(forecast(36, 0, 5, 0, 38, 3));
        assertTrue(advice.contains("water"));
        assertTrue(advice.contains("jacket"));
    }

    @Test
    void clearAdviceWhenNothingToWarn() {
        assertTrue(Advice.summarize(forecast(22, 0, 5, 0, 24, 18))
                .contains("Clear and comfortable"));
    }

    @Test
    void fahrenheitConversion() {
        assertEquals(32.0, Formatter.toF(0), 0.001);
        assertEquals("77.0°F", Formatter.temp(25, true));
        assertEquals("25.0°C", Formatter.temp(25, false));
    }

    @Test
    void textAndJsonRenderRealFields() {
        Forecast f = forecast(26.3, 0, 7.4, 10, 35.2, 25.6);
        String text = Formatter.text(f, false);
        assertTrue(text.contains("26.3°C"));
        assertTrue(text.contains("Clear sky"));
        String json = Formatter.json(f);
        assertTrue(json.contains("\"temperature_c\":26.3"));
        assertTrue(json.contains("\"condition\":\"Clear sky\""));
    }
}
