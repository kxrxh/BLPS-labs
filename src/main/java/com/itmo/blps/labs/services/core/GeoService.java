package com.itmo.blps.labs.services.core;

import com.itmo.blps.labs.dto.api.NominatimResponse;
import com.itmo.blps.labs.entities.Position;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class GeoService {

    private static final long REQUEST_INTERVAL_MS = 1000; // 1 second between requests
    private final AtomicLong lastRequestTime = new AtomicLong(0);
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public GeoService() {
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public Position getPositionFromAddress(String address, String city) {
        String fullAddress = city + ", " + address;

        try {
            // Rate limiting
            synchronized (this) {
                long now = System.currentTimeMillis();
                long timeSinceLastRequest = now - lastRequestTime.get();

                if (timeSinceLastRequest < REQUEST_INTERVAL_MS) {
                    long waitTime = REQUEST_INTERVAL_MS - timeSinceLastRequest;
                    Thread.sleep(waitTime);
                }
            }

            String encodedAddress = URLEncoder.encode(fullAddress, StandardCharsets.UTF_8);
            String url = "https://nominatim.openstreetmap.org/search?format=json&q=" + encodedAddress;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "my-app/1.0")
                    .header("Accept", "*/*")
                    .GET()
                    .build();

            System.out.println("Request URL: " + url);
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            // Update last request time after successful request
            synchronized (this) {
                lastRequestTime.set(System.currentTimeMillis());
            }

            NominatimResponse[] results = objectMapper.readValue(response.body(), NominatimResponse[].class);

            System.out.println("Raw Response: " + (results != null ? results.length : "null") + " items");
            if (results != null) {
                for (NominatimResponse result : results) {
                    System.out.println("  - " + result);
                }
            }

            if (results == null || results.length == 0) {
                System.out.println("No results found for: " + fullAddress);
                return getFallbackPosition(address, city);
            }

            // Find best match based on importance
            NominatimResponse bestMatch = Arrays.stream(results)
                    .max(Comparator.comparingDouble(r -> r.getImportance() != null ? r.getImportance() : 0))
                    .get();

            return Position.builder()
                    .latitude(Double.parseDouble(bestMatch.getLat()))
                    .longitude(Double.parseDouble(bestMatch.getLon()))
                    .address(address)
                    .city(city)
                    .build();

        } catch (Exception e) {
            System.out.println("Error while geocoding address: " + e.getMessage());
            e.printStackTrace();
            return getFallbackPosition(address, city);
        }
    }

    private Position getFallbackPosition(String address, String city) {
        Position fallbackPosition = Position.builder()
                .latitude(41.0)
                .longitude(19.0)
                .address(address)
                .city(city)
                .build();

        double baseLat = 41.0;
        double latOffset = (Math.abs(address.hashCode() + city.hashCode()) % 3600) / 100.0;
        fallbackPosition.setLatitude(baseLat + latOffset);

        double baseLong = 19.0;
        double longOffset = (Math.abs(city.hashCode()) % 15000) / 100.0;
        fallbackPosition.setLongitude(baseLong + longOffset);

        return fallbackPosition;
    }

    public double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        // Uses the WGS84 ellipsoid model for higher accuracy
        double a = 6378137.0;
        double b = 6356752.314245;
        double f = 1 / 298.257223563;
        double L = Math.toRadians(lon2 - lon1);
        double U1 = Math.atan((1 - f) * Math.tan(Math.toRadians(lat1)));
        double U2 = Math.atan((1 - f) * Math.tan(Math.toRadians(lat2)));
        double sinU1 = Math.sin(U1), cosU1 = Math.cos(U1);
        double sinU2 = Math.sin(U2), cosU2 = Math.cos(U2);

        double lambda = L, lambdaP;
        int iterLimit = 100;
        double sinSigma, cosSigma, sigma;
        double sinAlpha, cosSqAlpha, cos2SigmaM;

        do {
            double sinLambda = Math.sin(lambda);
            double cosLambda = Math.cos(lambda);
            sinSigma = Math.sqrt(
                    Math.pow(cosU2 * sinLambda, 2) +
                            Math.pow(cosU1 * sinU2 - sinU1 * cosU2 * cosLambda, 2));
            if (sinSigma == 0)
                return 0; // coincident points
            cosSigma = sinU1 * sinU2 + cosU1 * cosU2 * cosLambda;
            sigma = Math.atan2(sinSigma, cosSigma);
            sinAlpha = cosU1 * cosU2 * sinLambda / sinSigma;
            cosSqAlpha = 1 - sinAlpha * sinAlpha;
            cos2SigmaM = (cosSqAlpha != 0) ? (cosSigma - 2 * sinU1 * sinU2 / cosSqAlpha) : 0;
            double C = f / 16 * cosSqAlpha * (4 + f * (4 - 3 * cosSqAlpha));
            lambdaP = lambda;
            lambda = L + (1 - C) * f * sinAlpha *
                    (sigma + C * sinSigma * (cos2SigmaM + C * cosSigma *
                            (-1 + 2 * cos2SigmaM * cos2SigmaM)));
        } while (Math.abs(lambda - lambdaP) > 1e-12 && --iterLimit > 0);

        if (iterLimit == 0) {
            throw new RuntimeException("Vincenty's formula failed to converge");
        }

        double uSq = cosSqAlpha * (a * a - b * b) / (b * b);
        double A = 1 + uSq / 16384 * (4096 + uSq * (-768 + uSq * (320 - 175 * uSq)));
        double B = uSq / 1024 * (256 + uSq * (-128 + uSq * (74 - 47 * uSq)));
        double deltaSigma = B * sinSigma * (cos2SigmaM + B / 4 *
                (cosSigma * (-1 + 2 * cos2SigmaM * cos2SigmaM) -
                        B / 6 * cos2SigmaM * (-3 + 4 * sinSigma * sinSigma) *
                                (-3 + 4 * cos2SigmaM * cos2SigmaM)));
        double distance = b * A * (sigma - deltaSigma);
        return distance; // distance in meters
    }

}