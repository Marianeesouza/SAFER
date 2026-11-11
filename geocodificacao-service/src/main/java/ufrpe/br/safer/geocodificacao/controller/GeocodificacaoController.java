package ufrpe.br.safer.geocodificacao.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ufrpe.br.safer.geocodificacao.model.Coordinates;
import ufrpe.br.safer.geocodificacao.model.Location;
import ufrpe.br.safer.geocodificacao.service.GeocodificacaoService;

@RestController
@RequestMapping("/geocoding")
@Tag(name = "Geocoding", description = "Conversion between addresses and coordinates")
public class GeocodificacaoController {

	private final GeocodificacaoService service;

	public GeocodificacaoController(GeocodificacaoService service) {
		this.service = service;
	}

	@GetMapping("/coordinates")
	@Operation(summary = "Gets the coordinates of an address")
	@ApiResponse(responseCode = "200", description = "Address found")
	@ApiResponse(responseCode = "400", description = "Address missing, blank or too long")
	@ApiResponse(responseCode = "404", description = "Address not found")
	@ApiResponse(responseCode = "503", description = "External provider unavailable and query not cached")
	public Location coordinates(
			@Parameter(description = "Free-text address", example = "UFRPE, Recife")
			@RequestParam @NotBlank @Size(max = 300) String address) {
		return service.obterCoordenadas(address)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Address not found"));
	}

	@GetMapping("/address")
	@Operation(summary = "Gets the address of a coordinate pair")
	@ApiResponse(responseCode = "200", description = "Address found")
	@ApiResponse(responseCode = "400", description = "Coordinates missing or outside the valid range")
	@ApiResponse(responseCode = "404", description = "No address for the coordinates")
	@ApiResponse(responseCode = "503", description = "External provider unavailable and query not cached")
	public Location address(
			@Parameter(description = "Latitude in decimal degrees", example = "-8.017611")
			@RequestParam @NotNull @DecimalMin("-90") @DecimalMax("90") Double latitude,
			@Parameter(description = "Longitude in decimal degrees", example = "-34.949472")
			@RequestParam @NotNull @DecimalMin("-180") @DecimalMax("180") Double longitude) {
		return service.obterEndereco(new Coordinates(latitude, longitude))
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
						"No address found for the coordinates"));
	}

}
