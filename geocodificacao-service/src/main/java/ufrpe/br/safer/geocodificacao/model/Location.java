package ufrpe.br.safer.geocodificacao.model;

/** A point on the map and its full address. */
public record Location(Coordinates coordinates, String address) {
}
