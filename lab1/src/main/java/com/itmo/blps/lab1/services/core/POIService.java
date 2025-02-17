package com.itmo.blps.lab1.services.core;

import com.itmo.blps.lab1.entities.*;
import com.itmo.blps.lab1.repositories.POIRepository;
import com.itmo.blps.lab1.repositories.AdvertisementPOIRepository;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;

@Service
public class POIService {

    @Autowired
    private POIRepository poiRepository;

    @Autowired
    private AdvertisementPOIRepository advertisementPOIRepository;

    @Value("${poi.search-radius-meters:1000}")
    private Double defaultSearchRadius;

    public void updateAdvertisementPOIs(Advertisement advertisement) {
        // Find all POIs near the advertisement
        for (POIType poiType: POIType.values()) {
            List < POI > nearbyPOIs = poiRepository.findNearbyPOIsByType(
                advertisement.getPosition().getLatitude(),
                advertisement.getPosition().getLongitude(),
                defaultSearchRadius,
                poiType.name()
            );

            // Create AdvertisementPOI entries for each nearby POI
            for (POI poi: nearbyPOIs) {
                AdvertisementPOI adPoi = new AdvertisementPOI();
                adPoi.setAdvertisement(advertisement);
                adPoi.setPoi(poi);
                // Calculate distance
                double distance = calculateDistance(
                    advertisement.getPosition().getLatitude(),
                    advertisement.getPosition().getLongitude(),
                    poi.getLatitude(),
                    poi.getLongitude()
                );
                adPoi.setDistanceInMeters(distance);
                advertisementPOIRepository.save(adPoi);
            }
        }
    }

    private double calculateVincentyDistance(double lat1, double lon1, double lat2, double lon2) {
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
                Math.pow(cosU1 * sinU2 - sinU1 * cosU2 * cosLambda, 2)
            );
            if (sinSigma == 0) return 0; // coincident points
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
