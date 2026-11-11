package ufrpe.br.safer.geocodificacao.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import io.swagger.v3.oas.annotations.media.Schema;

/** Map settings the front-end needs to render tiles. Values come from {@code safer.map.*}. */
@ConfigurationProperties(prefix = "safer.map")
public record MapConfig(
		@Schema(description = "Tile URL template", example = "https://tile.openstreetmap.org/{z}/{x}/{y}.png")
		String urlTemplate,
		@Schema(description = "Attribution text the map must display", example = "© OpenStreetMap contributors")
		String attribution,
		@Schema(description = "Maximum zoom level", example = "19")
		int maxZoom) {
}
