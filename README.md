# Skycast

Live weather for any city — no sign-up, no API key, no sample data.

## The problem

Checking the weather usually means opening an ad-heavy weather site or an app
that wants your location and an account. All you actually want is: what is it
like right now, what is the week ahead, and do I need an umbrella?

## What it does

Skycast answers that from the free, keyless [Open-Meteo](https://open-meteo.com/)
API (geocoding + forecast, data CC BY 4.0 from national weather services):

- **Search any city** — geocoding suggestions, then current conditions, a 7-day
  forecast and a 24-hour temperature chart.
- **Plain-English advice** — derived from the forecast itself: umbrella when rain
  is likely, jacket when mornings get cold, water when the week gets hot.
- **Saved cities** — your recent cities stay in localStorage as quick chips.
- **°C / °F toggle** — converts the cached Celsius data, never re-fetches.
- **Honest failures** — if Open-Meteo cannot be reached, Skycast says so and
  exits (CLI) or shows an error (web) instead of printing invented weather.

The same endpoints are wrapped twice in this repo:

1. **Java CLI + client library** (`src/skycast/`) — zero shipped dependencies,
   plain `javac`, JUnit 5 tests. `java -jar build/skycast.jar "Mumbai"`,
   `--json`, `--fahrenheit`, `--days N`, and `--serve` to serve the dashboard
   on localhost.
2. **Interactive dashboard** (`docs/`) — the full app, served by GitHub Pages.
   It calls Open-Meteo directly from the browser, so it works with no backend.

## Build and test

Requires JDK 21+ and the JUnit 5 console jar in `lib/` (test dependency only,
gitignored — download `junit-platform-console-standalone-1.11.4.jar` there).

```sh
./build.sh        # compile, run tests, package build/skycast.jar
```

## Run

```sh
java -jar build/skycast.jar "Mumbai"
java -jar build/skycast.jar --json "London"
java -jar build/skycast.jar --fahrenheit "Tokyo"
java -jar build/skycast.jar --serve          # dashboard at http://localhost:8080/
java -jar build/skycast.jar --serve 9090     # …on port 9090
```

Offline replay of a saved API response (useful for demos and CI):

```sh
java -jar build/skycast.jar --from-file examples/mumbai-forecast.json --place "Mumbai, Maharashtra, India"
```

## Example output

Live Open-Meteo response for Mumbai, 5 November 2026, 07:45 IST, replayed
from `examples/mumbai-forecast.json` (the sandbox blocks the JVM's outbound
HTTPS, so the live call is demonstrated from the saved response and from the
browser dashboard, which calls the same endpoint directly):

```
Mumbai, Maharashtra, India
Now: 26.3°C (feels 28.8°C), Clear sky, humidity 66%, wind 7.4 km/h
It gets hot this week — carry water and sun protection.
Daily forecast:
  2026-11-05  Overcast  25.6°C – 35.2°C  rain 10%
Source: Open-Meteo (Asia/Kolkata)
```

(The daily condition is the day's dominant WMO code; the current condition at
07:45 was clear sky.)

## Live demo

https://judd69.github.io/skycast/ — flip Pages on (Settings → Pages → branch
`main`, folder `/docs`) after pushing, and the dashboard above runs there.

## License

MIT — see `LICENSE`.
