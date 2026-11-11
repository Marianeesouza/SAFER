package ufrpe.br.safer.geocodificacao.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import ufrpe.br.safer.geocodificacao.model.Coordinates;

class CampusConfigTest {

	private final CampusConfig campus = new CampusConfig(-8.017611, -34.949472, 1200);

	@Test
	void centerIsInside() {
		assertTrue(campus.contains(new Coordinates(-8.017611, -34.949472)));
	}

	@Test
	void pointAbout1100MetersAwayIsInside() {
		// 0.01 degree of latitude is roughly 1,112 m.
		assertTrue(campus.contains(new Coordinates(-8.007611, -34.949472)));
	}

	@Test
	void pointAbout1330MetersAwayIsOutside() {
		assertFalse(campus.contains(new Coordinates(-8.005611, -34.949472)));
	}

}
