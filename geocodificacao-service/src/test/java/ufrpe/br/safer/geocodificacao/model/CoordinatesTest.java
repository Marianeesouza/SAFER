package ufrpe.br.safer.geocodificacao.model;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class CoordinatesTest {

	@Test
	void aceitaCoordenadasValidas() {
		assertDoesNotThrow(() -> new Coordinates(-8.017611, -34.949472));
		assertDoesNotThrow(() -> new Coordinates(90, 180));
	}

	@Test
	void rejeitaLatitudeForaDaFaixa() {
		assertThrows(IllegalArgumentException.class, () -> new Coordinates(90.1, 0));
	}

	@Test
	void rejeitaLongitudeForaDaFaixaOuNaN() {
		assertThrows(IllegalArgumentException.class, () -> new Coordinates(0, -180.1));
		assertThrows(IllegalArgumentException.class, () -> new Coordinates(0, Double.NaN));
	}

}
