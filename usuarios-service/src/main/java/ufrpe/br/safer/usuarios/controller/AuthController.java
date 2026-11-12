package ufrpe.br.safer.usuarios.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ufrpe.br.safer.usuarios.dto.LoginDTO;
import ufrpe.br.safer.usuarios.dto.TokenResponseDTO;
import ufrpe.br.safer.usuarios.dto.UsuarioResponseDTO;
import ufrpe.br.safer.usuarios.service.UsuariosService;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UsuariosService usuariosService;

    @PostMapping("/login")
    public ResponseEntity<TokenResponseDTO> login(@Valid @RequestBody LoginDTO dto) {
        return ResponseEntity.ok(usuariosService.autenticar(dto));
    }

    @GetMapping("/me")
    public ResponseEntity<UsuarioResponseDTO> me(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader
    ) {
        return ResponseEntity.ok(usuariosService.obterUsuarioViaToken(authorizationHeader));
    }
}
