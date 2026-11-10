package ufrpe.br.safer.geocodificacao.model;

public record Coordenadas(double latitude, double longitude) {

	public Coordenadas {
		if (!(latitude >= -90 && latitude <= 90)) {
			throw new IllegalArgumentException("Latitude deve estar entre -90 e 90");
		}
		if (!(longitude >= -180 && longitude <= 180)) {
			throw new IllegalArgumentException("Longitude deve estar entre -180 e 180");
		}
	}

}
