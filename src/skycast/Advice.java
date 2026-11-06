package skycast;

import java.util.ArrayList;
import java.util.List;

/**
 * "What should I carry?" advice derived from the forecast itself.
 * Every sentence is driven by a threshold on real forecast values.
 */
public final class Advice {
    private Advice() {
    }

    public static String summarize(Forecast forecast) {
        List<String> tips = new ArrayList<>();
        double todayRain = forecast.days.isEmpty() ? 0 : forecast.days.get(0).precipProbability();
        boolean wetToday = todayRain >= 50 || WeatherCode.isWet(forecast.current.weatherCode());
        if (wetToday) {
            tips.add("Carry an umbrella — rain is likely today.");
        }
        if (WeatherCode.isSnow(forecast.current.weatherCode())) {
            tips.add("Wear warm, waterproof layers — snow is in the forecast.");
        }
        double max = forecast.days.stream().mapToDouble(Forecast.Day::maxC).max().orElse(0);
        double min = forecast.days.stream().mapToDouble(Forecast.Day::minC).min().orElse(0);
        if (max >= 35) {
            tips.add("It gets hot this week — carry water and sun protection.");
        }
        if (min <= 5) {
            tips.add("Mornings get cold — take a jacket.");
        }
        if (forecast.current.windKmh() >= 40) {
            tips.add("Strong wind right now — hold on to loose items.");
        }
        if (tips.isEmpty()) {
            tips.add("Clear and comfortable — no special gear needed.");
        }
        return String.join(" ", tips);
    }
}
