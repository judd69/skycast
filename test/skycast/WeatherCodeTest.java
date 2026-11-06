package skycast;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class WeatherCodeTest {
    @Test
    void labelsKnownCodes() {
        assertEquals("Clear sky", WeatherCode.describe(0));
        assertEquals("Rain", WeatherCode.describe(63));
        assertEquals("Thunderstorm with hail", WeatherCode.describe(99));
        assertEquals("Unknown conditions", WeatherCode.describe(1234));
    }

    @Test
    void wetAndSnowClassification() {
        assertTrue(WeatherCode.isWet(63));
        assertTrue(WeatherCode.isWet(95));
        assertFalse(WeatherCode.isWet(0));
        assertTrue(WeatherCode.isSnow(73));
        assertFalse(WeatherCode.isSnow(63));
    }
}
