package ufrpe.br.safer.geocodificacao.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "cache_geocodificacao")
public class CacheGeocodificacao {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Getter
    @Column(nullable = false, unique = true)
	private String chave;

	@Column(nullable = false)
	private double latitude;

	@Column(nullable = false)
	private double longitude;

    @Getter
	@Column(nullable = false)
	private String endereco;

    @Getter
	@Column(nullable = false)
	private LocalDateTime criadoEm;

	public CacheGeocodificacao(String chave, Coordinates coordenadas, String endereco) {
		this.chave = chave;
		this.latitude = coordenadas.latitude();
		this.longitude = coordenadas.longitude();
		this.endereco = endereco;
		this.criadoEm = LocalDateTime.now();
	}

	public Coordinates getCoordenadas() {
		return new Coordinates(latitude, longitude);
	}

}
