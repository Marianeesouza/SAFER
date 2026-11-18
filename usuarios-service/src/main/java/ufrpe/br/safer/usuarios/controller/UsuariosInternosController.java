package ufrpe.br.safer.usuarios.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ufrpe.br.safer.usuarios.dto.DestinatarioNotificacaoDTO;
import ufrpe.br.safer.usuarios.service.UsuariosService;

import java.util.List;

@RestController
@RequestMapping("/internal/usuarios")
@RequiredArgsConstructor
public class UsuariosInternosController {

    private final UsuariosService usuariosService;

    @GetMapping("/destinatarios-novas-ocorrencias")
    public ResponseEntity<List<DestinatarioNotificacaoDTO>> destinatariosDeNovasOcorrencias() {
        return ResponseEntity.ok(usuariosService.listarDestinatariosDeNovasOcorrencias());
    }
}
