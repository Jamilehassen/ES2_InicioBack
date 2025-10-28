package com.example.apiparticipantes.controller;

import com.example.apiparticipantes.dto.ParticipanteResponseDto;
import com.example.apiparticipantes.dto.ParticipanteUpdateRequestDto;
import com.example.apiparticipantes.model.*;
import com.example.apiparticipantes.repository.*; // Importa todos os repositórios necessários
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID; // Importar UUID
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/participantes")
@CrossOrigin(origins = "*")
public class ParticipanteController {

    private final ParticipanteRepository participanteRepository;
    // --- Adicionar injeção dos repositórios de endereço ---
    private final EnderecoRepository enderecoRepository;
    private final BairroRepository bairroRepository;
    private final CidadeRepository cidadeRepository;
    private final UnidadeFederacaoRepository unidadeFederacaoRepository;
    private final LogradouroRepository logradouroRepository;
    private final TipoLogradouroRepository tipoLogradouroRepository;


    // --- Atualizar o construtor ---
    public ParticipanteController(ParticipanteRepository participanteRepository, EnderecoRepository enderecoRepository, BairroRepository bairroRepository, CidadeRepository cidadeRepository, UnidadeFederacaoRepository unidadeFederacaoRepository, LogradouroRepository logradouroRepository, TipoLogradouroRepository tipoLogradouroRepository) {
        this.participanteRepository = participanteRepository;
        this.enderecoRepository = enderecoRepository;
        this.bairroRepository = bairroRepository;
        this.cidadeRepository = cidadeRepository;
        this.unidadeFederacaoRepository = unidadeFederacaoRepository;
        this.logradouroRepository = logradouroRepository;
        this.tipoLogradouroRepository = tipoLogradouroRepository;
    }


    // ... (métodos listarTodos, visualizarParticipante) ...

    /**
     * Rota para editar informações de um participante (ADMIN ou o próprio participante).
     * Inclui a lógica para atualizar ou criar o endereço.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN') or @participanteSecurityService.isSelf(#id, authentication)")
    public ResponseEntity<ParticipanteResponseDto> editarParticipante(@PathVariable String id, @RequestBody ParticipanteUpdateRequestDto request) {
        Participante participante = participanteRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Participante não encontrado."));

        // Atualiza os dados pessoais
        participante.setNomeParticipante(request.getNomeParticipante());
        participante.setTelefoneParticipante(request.getTelefoneParticipante());

        // --- Lógica para atualizar/criar o endereço ---
        // Verifica se foram fornecidos dados de endereço na requisição
        if (request.getCep() != null && !request.getCep().isEmpty()) { // Usamos o CEP como indicador

            // Reutiliza a lógica do AuthController para encontrar/criar entidades relacionadas
            UnidadeFederacao uf = unidadeFederacaoRepository.findById(request.getSiglaUf())
                    .orElseGet(() -> unidadeFederacaoRepository.save(new UnidadeFederacao(request.getSiglaUf(), request.getSiglaUf())));

            Cidade cidade = cidadeRepository.findByNomeCidade(request.getNomeCidade())
                    .orElseGet(() -> cidadeRepository.save(new Cidade(UUID.randomUUID().toString(), request.getNomeCidade(), uf)));

            Bairro bairro = bairroRepository.findByNomeBairro(request.getNomeBairro())
                    .orElseGet(() -> bairroRepository.save(new Bairro(UUID.randomUUID().toString(), request.getNomeBairro())));

            TipoLogradouro tipoLogradouro = tipoLogradouroRepository.findByNomeTipoLogradouro(request.getNomeTipoLogradouro())
                    .orElseGet(() -> tipoLogradouroRepository.save(new TipoLogradouro(UUID.randomUUID().toString(), request.getNomeTipoLogradouro())));

            Logradouro logradouro = logradouroRepository.findByNomeLogradouro(request.getNomeLogradouro())
                    .orElseGet(() -> logradouroRepository.save(new Logradouro(UUID.randomUUID().toString(), request.getNomeLogradouro(), tipoLogradouro)));

            // Verifica se o participante já tem um endereço para atualizar, senão cria um novo
            Endereco endereco = participante.getEndereco();
            if (endereco == null) {
                endereco = new Endereco();
            }

            // Atualiza os campos do endereço
            endereco.setCep(request.getCep());
            endereco.setComplemento(request.getComplemento());
            endereco.setNumero(request.getNumero());
            endereco.setBairro(bairro);
            endereco.setCidade(cidade);
            endereco.setLogradouro(logradouro);

            // Salva o endereço (necessário se for um novo endereço)
            Endereco enderecoSalvo = enderecoRepository.save(endereco);
            // Associa o endereço (novo ou atualizado) ao participante
            participante.setEndereco(enderecoSalvo);
        }
        // Se não vieram dados de endereço no request, o endereço existente (ou a falta dele) é mantido.

        // Salva o participante com todas as alterações (dados pessoais e/ou endereço)
        Participante participanteSalvo = participanteRepository.save(participante);

        // Retorna o DTO atualizado
        return ResponseEntity.ok(new ParticipanteResponseDto(participanteSalvo));
    }

}