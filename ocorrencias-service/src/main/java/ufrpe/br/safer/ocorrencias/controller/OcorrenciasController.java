package ufrpe.br.safer.ocorrencias.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;
import ufrpe.br.safer.ocorrencias.dto.AtualizarStatusDTO;
import ufrpe.br.safer.ocorrencias.dto.CancelarOcorrenciaDTO;
import ufrpe.br.safer.ocorrencias.dto.OcorrenciaDetalhadaDTO;
import ufrpe.br.safer.ocorrencias.dto.OcorrenciaResponseDTO;
import ufrpe.br.safer.ocorrencias.dto.RegistrarOcorrenciaDTO;
import ufrpe.br.safer.ocorrencias.dto.VincularAnalistaDTO;
import ufrpe.br.safer.ocorrencias.model.StatusOcorrencia;
import ufrpe.br.safer.ocorrencias.service.OcorrenciasService;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/ocorrencias")
@RequiredArgsConstructor
@Tag(name = "Ocorrências", description = "Endpoints para registro, consulta, tramitação e cancelamento de ocorrências de fauna")
public class OcorrenciasController {

    private final OcorrenciasService ocorrenciasService;

    @PostMapping
    @Operation(summary = "Registrar nova ocorrência (CDU05)", description = "Valida localização no campus, gera protocolo e persiste a ocorrência")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Ocorrência registrada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou fora do campus")
    })
    public ResponseEntity<OcorrenciaResponseDTO> registrarOcorrencia(
            @Valid @RequestBody RegistrarOcorrenciaDTO dto,
            UriComponentsBuilder uriBuilder
    ) {
        OcorrenciaResponseDTO criada = ocorrenciasService.registrarOcorrencia(dto);
        URI uri = uriBuilder.path("/ocorrencias/{id}").buildAndExpand(criada.id()).toUri();
        return ResponseEntity.created(uri).body(criada);
    }

    @PostMapping("/{id}/vincular-analista")
    @Operation(summary = "Assumir ocorrência por um analista (CDU07 / RN03)", description = "Vincula o analista e transiciona de EM_ABERTO para EM_ATENDIMENTO com proteção contra concorrência")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Atendimento assumido com sucesso"),
            @ApiResponse(responseCode = "404", description = "Ocorrência não encontrada"),
            @ApiResponse(responseCode = "409", description = "Conflito: outro analista já assumiu a ocorrência")
    })
    public ResponseEntity<OcorrenciaResponseDTO> vincularAnalista(
            @Parameter(description = "ID numérico da ocorrência") @PathVariable Long id,
            @Valid @RequestBody VincularAnalistaDTO dto
    ) {
        return ResponseEntity.ok(ocorrenciasService.vincularAnalista(id, dto.analistaId()));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Atualizar status da ocorrência (CDU07)", description = "Realiza a transição de estado validando a máquina de estados e registra auditoria")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status atualizado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Ocorrência não encontrada"),
            @ApiResponse(responseCode = "422", description = "Transição de status inválida para o estado atual")
    })
    public ResponseEntity<OcorrenciaResponseDTO> atualizarStatus(
            @Parameter(description = "ID numérico da ocorrência") @PathVariable Long id,
            @Valid @RequestBody AtualizarStatusDTO dto
    ) {
        return ResponseEntity.ok(ocorrenciasService.atualizarStatus(id, dto));
    }

    @PatchMapping("/{id}/cancelar")
    @Operation(summary = "Cancelar ocorrência pendente (CDU08 / RN03)", description = "Realiza o cancelamento lógico com justificativa obrigatória e proteção concorrente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ocorrência cancelada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Justificativa não informada"),
            @ApiResponse(responseCode = "404", description = "Ocorrência não encontrada"),
            @ApiResponse(responseCode = "409", description = "Conflito: a ocorrência mudou de estado antes do cancelamento")
    })
    public ResponseEntity<OcorrenciaResponseDTO> cancelarOcorrencia(
            @Parameter(description = "ID numérico da ocorrência") @PathVariable Long id,
            @Valid @RequestBody CancelarOcorrenciaDTO dto
    ) {
        return ResponseEntity.ok(ocorrenciasService.cancelarOcorrencia(id, dto));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar detalhes da ocorrência por ID (CDU06)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ocorrência encontrada"),
            @ApiResponse(responseCode = "404", description = "Ocorrência não encontrada")
    })
    public ResponseEntity<OcorrenciaDetalhadaDTO> buscarPorId(
            @Parameter(description = "ID numérico da ocorrência") @PathVariable Long id
    ) {
        return ocorrenciasService.buscarDetalhesPorId(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/protocolo/{protocolo}")
    @Operation(summary = "Consultar detalhes da ocorrência por Protocolo (CDU06)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ocorrência encontrada"),
            @ApiResponse(responseCode = "404", description = "Ocorrência não encontrada")
    })
    public ResponseEntity<OcorrenciaDetalhadaDTO> buscarPorProtocolo(
            @Parameter(description = "Código de protocolo (ex: OC-20261111-XXXX)") @PathVariable String protocolo
    ) {
        return ocorrenciasService.buscarDetalhesPorProtocolo(protocolo)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/em-aberto")
    @Operation(summary = "Listar ocorrências em aberto para triagem de analistas (CDU06)")
    public ResponseEntity<List<OcorrenciaResponseDTO>> listarEmAberto() {
        return ResponseEntity.ok(ocorrenciasService.listarEmAberto());
    }

    @GetMapping("/usuario/{usuarioId}")
    @Operation(summary = "Listar ocorrências registradas por um usuário (CDU06)")
    public ResponseEntity<List<OcorrenciaResponseDTO>> listarPorUsuario(
            @Parameter(description = "ID do usuário comunicante") @PathVariable Long usuarioId
    ) {
        return ResponseEntity.ok(ocorrenciasService.listarPorUsuario(usuarioId));
    }

    @GetMapping
    @Operation(summary = "Listar ocorrências com filtro opcional por status (CDU06)")
    public ResponseEntity<List<OcorrenciaResponseDTO>> listar(
            @Parameter(description = "Filtro opcional por status da ocorrência") @RequestParam(required = false) StatusOcorrencia status
    ) {
        return ResponseEntity.ok(ocorrenciasService.listar(status));
    }
}
