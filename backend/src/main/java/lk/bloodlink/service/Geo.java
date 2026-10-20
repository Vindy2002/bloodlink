package lk.bloodlink.service;

import java.util.Random;

public final class Geo {
    private Geo() {}

    /** Haversine great-circle distance in km. */
    public static double km(double lat1, double lon1, double lat2, double lon2) {
        double r = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1), dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * r * Math.asin(Math.sqrt(a));
    }

    /** {minLat, maxLat, minLng, maxLng} around a point. */
    public static double[] box(double lat, double lng, double km) {
        double dLat = km / 111.0;
        double dLng = km / (111.0 * Math.max(0.01, Math.cos(Math.toRadians(lat))));
        return new double[]{lat - dLat, lat + dLat, lng - dLng, lng + dLng};
    }

    public static double round1(double v) { return Math.round(v * 10) / 10.0; }

    /** Stable pseudo-random blur so repeated calls can't be averaged to find the real point. */
    public static double blur(double v, long seed, double spreadDeg) {
        return v + (new Random(seed).nextDouble() * 2 - 1) * spreadDeg;
    }

    public static String maskName(String full) {
        if (full == null || full.isBlank()) return "Donor";
        String[] p = full.trim().split("\\s+");
        return p.length == 1 ? p[0] : p[0] + " " + p[p.length - 1].charAt(0) + ".";
    }
}
