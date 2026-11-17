package ufrpe.br.safer.usuarios.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ufrpe.br.safer.usuarios.dto.AtualizarCargoDTO;
import ufrpe.br.safer.usuarios.dto.AtualizarPerfilDTO;
import ufrpe.br.safer.usuarios.dto.CadastrarUsuarioDTO;
import ufrpe.br.safer.usuarios.dto.UsuarioResponseDTO;
import ufrpe.br.safer.usuarios.service.UsuariosService;

import java.util.List;

@RestController
@RequestMapping("/usuarios")
@RequiredArgsConstructor
public class UsuariosController {

    private final UsuariosService usuariosService;

    @PostMapping
    public ResponseEntity<UsuarioResponseDTO> cadastrar(@Valid @RequestBody CadastrarUsuarioDTO dto) {
        UsuarioResponseDTO criado = usuariosService.cadastrar(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(criado);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(usuariosService.buscarPorId(id));
    }

    @GetMapping
    public ResponseEntity<List<UsuarioResponseDTO>> listarTodos() {
        return ResponseEntity.ok(usuariosService.listarTodos());
    }

    @PatchMapping("/me")
    public ResponseEntity<UsuarioResponseDTO> atualizarProprioPerfil(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @Valid @RequestBody AtualizarPerfilDTO dto
    ) {
        return ResponseEntity.ok(usuariosService.atualizarProprioPerfil(authorizationHeader, dto));
    }

    @PatchMapping("/{id}/cargo")
    public ResponseEntity<UsuarioResponseDTO> atualizarCargo(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @PathVariable Long id,
            @Valid @RequestBody AtualizarCargoDTO dto
    ) {
        return ResponseEntity.ok(usuariosService.atualizarCargo(authorizationHeader, id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @PathVariable Long id
    ) {
        usuariosService.excluir(authorizationHeader, id);
        return ResponseEntity.noContent().build();
    }
}
