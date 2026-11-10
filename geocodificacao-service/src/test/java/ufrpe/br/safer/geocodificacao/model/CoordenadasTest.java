package ufrpe.br.safer.geocodificacao.model;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class CoordenadasTest {

	@Test
	void aceitaCoordenadasValidas() {
		assertDoesNotThrow(() -> new Coordenadas(-8.017611, -34.949472));
		assertDoesNotThrow(() -> new Coordenadas(90, 180));
	}

	@Test
	void rejeitaLatitudeForaDaFaixa() {
		assertThrows(IllegalArgumentException.class, () -> new Coordenadas(90.1, 0));
	}

	@Test
	void rejeitaLongitudeForaDaFaixaOuNaN() {
		assertThrows(IllegalArgumentException.class, () -> new Coordenadas(0, -180.1));
		assertThrows(IllegalArgumentException.class, () -> new Coordenadas(0, Double.NaN));
	}

}
