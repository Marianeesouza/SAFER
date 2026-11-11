package ufrpe.br.safer.geocodificacao.model;

public record Coordinates(double latitude, double longitude) {

	private static final double EARTH_RADIUS_METERS = 6_371_000;

	public Coordinates {
		if (!(latitude >= -90 && latitude <= 90)) {
			throw new IllegalArgumentException("Latitude must be between -90 and 90");
		}
		if (!(longitude >= -180 && longitude <= 180)) {
			throw new IllegalArgumentException("Longitude must be between -180 and 180");
		}
	}

	/** Great-circle distance to another point, in meters (haversine formula). */
	public double distanceInMetersTo(Coordinates other) {
		double dLat = Math.toRadians(other.latitude - latitude);
		double dLon = Math.toRadians(other.longitude - longitude);
		double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
				+ Math.cos(Math.toRadians(latitude)) * Math.cos(Math.toRadians(other.latitude))
				* Math.sin(dLon / 2) * Math.sin(dLon / 2);
		return EARTH_RADIUS_METERS * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
	}

}
