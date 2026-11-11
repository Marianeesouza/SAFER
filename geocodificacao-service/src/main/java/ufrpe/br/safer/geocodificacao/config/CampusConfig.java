package ufrpe.br.safer.geocodificacao.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import ufrpe.br.safer.geocodificacao.model.Coordinates;

/** Campus center point and the radius (in meters) that counts as inside the campus. */
@ConfigurationProperties(prefix = "safer.campus")
public record CampusConfig(double latitude, double longitude, double radiusMeters) {

	public boolean contains(Coordinates point) {
		return new Coordinates(latitude, longitude).distanceInMetersTo(point) <= radiusMeters;
	}

}
