package skycast;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Skycast CLI.
 *
 * <pre>
 *   java -jar build/skycast.jar "Mumbai"          live forecast for a city
 *   java -jar build/skycast.jar --json "Mumbai"   same, as JSON
 *   java -jar build/skycast.jar --fahrenheit "Delhi"
 *   java -jar build/skycast.jar --from-file examples/mumbai-forecast.json
 *          replay a saved Open-Meteo forecast response (offline demo / CI)
 *   java -jar build/skycast.jar --serve [port]    serve docs/ on localhost
 * </pre>
 *
 * Live data comes from Open-Meteo: no API key, no sign-up. If the API cannot
 * be reached, Skycast says so and exits 2 instead of printing invented weather.
 */
public final class Main {
    private Main() {
    }

    public static void main(String[] args) throws Exception {
        // Degree signs and dashes must survive a C-locale terminal.
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        boolean json = false;
        boolean fahrenheit = false;
        boolean serve = false;
        int servePort = 8080;
        int days = 7;
        String fromFile = null;
        String placeName = null;
        String city = null;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--help", "-h" -> {
                    System.out.println("""
                            Skycast — live weather for any city, from Open-Meteo (no API key).
                            Usage: java -jar build/skycast.jar [options] [city]
                              --json            print JSON instead of text
                              --fahrenheit      temperatures in Fahrenheit
                              --days N          forecast days, 1-7 (default 7)
                              --from-file FILE  replay a saved Open-Meteo forecast response
                              --place NAME      label for a --from-file replay (e.g. "Mumbai")
                              --serve [port]    serve the docs/ dashboard on localhost (default 8080)
                              --help            this help
                            Default city: Mumbai.""");
                    return;
                }
                case "--json" -> json = true;
                case "--fahrenheit" -> fahrenheit = true;
                case "--serve" -> {
                    serve = true;
                    if (i + 1 < args.length && args[i + 1].matches("\\d+")) {
                        servePort = Integer.parseInt(args[++i]);
                    }
                }
                case "--days" -> {
                    if (i + 1 >= args.length) {
                        System.err.println("error: --days needs a number");
                        System.exit(2);
                    }
                    days = Integer.parseInt(args[++i]);
                    if (days < 1 || days > 7) {
                        System.err.println("error: --days must be between 1 and 7");
                        System.exit(2);
                    }
                }
                case "--from-file" -> {
                    if (i + 1 >= args.length) {
                        System.err.println("error: --from-file needs a path");
                        System.exit(2);
                    }
                    fromFile = args[++i];
                }
                case "--place" -> {
                    if (i + 1 >= args.length) {
                        System.err.println("error: --place needs a name");
                        System.exit(2);
                    }
                    placeName = args[++i];
                }
                default -> {
                    if (args[i].startsWith("--")) {
                        System.err.println("error: unknown option " + args[i]);
                        System.exit(2);
                    }
                    city = args[i];
                }
            }
        }

        if (serve) {
            serve(servePort);
            return;
        }

        Forecast forecast;
        if (fromFile != null) {
            String body = Files.readString(Path.of(fromFile));
            // A forecast response carries coordinates, not a city name, so
            // --place supplies the label; otherwise the coordinates are shown.
            forecast = OpenMeteoClient.parseForecastJson(body,
                    new Forecast.Place(placeName != null ? placeName : "Saved forecast", "", "", 0, 0));
            forecast = withCoordinates(forecast, body);
        } else {
            String query = city == null ? "Mumbai" : city;
            OpenMeteoClient client = new OpenMeteoClient();
            List<Forecast.Place> places;
            try {
                places = client.geocode(query);
            } catch (IOException | InterruptedException e) {
                System.err.println("error: could not reach Open-Meteo geocoding: " + e.getMessage());
                System.exit(2);
                return;
            }
            if (places.isEmpty()) {
                System.err.println("error: no place found for '" + query + "'");
                System.exit(2);
                return;
            }
            try {
                forecast = client.forecast(places.get(0), days);
            } catch (IOException | InterruptedException e) {
                System.err.println("error: could not reach Open-Meteo forecast: " + e.getMessage());
                System.exit(2);
                return;
            }
        }

        System.out.println(json ? Formatter.json(forecast) : Formatter.text(forecast, fahrenheit));
    }

    private static Forecast withCoordinates(Forecast forecast, String body) {
        try {
            var root = Json.obj(Json.parse(body));
            double lat = Json.num(Json.field(root, "latitude"));
            double lon = Json.num(Json.field(root, "longitude"));
            String name = forecast.place.name();
            return new Forecast(new Forecast.Place(name, "", "", lat, lon),
                    forecast.current, forecast.days, forecast.hourly, forecast.timezone);
        } catch (RuntimeException e) {
            return forecast;
        }
    }

    private static void serve(int port) throws IOException {
        Path docs = Files.exists(Path.of("docs/index.html"))
                ? Path.of("docs/index.html") : Path.of("../docs/index.html");
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", exchange -> {
            byte[] body;
            int status = 200;
            String contentType = "text/html; charset=utf-8";
            if (Files.exists(docs)) {
                body = Files.readAllBytes(docs);
            } else {
                body = ("<h1>Skycast</h1><p>Run this from the repository root so docs/index.html "
                        + "can be served, or open docs/index.html directly.</p>")
                        .getBytes(StandardCharsets.UTF_8);
                status = 404;
            }
            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.sendResponseHeaders(status, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        server.start();
        System.out.println("Skycast dashboard at http://localhost:" + port + "/ (Ctrl+C to stop)");
        Object lock = new Object();
        synchronized (lock) {
            try {
                lock.wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
