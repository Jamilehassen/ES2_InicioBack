package com.example.apiparticipantes.controller;

import com.example.apiparticipantes.dto.EventoRequest;
import com.example.apiparticipantes.dto.EventoResponseDto;
import com.example.apiparticipantes.model.*;
import com.example.apiparticipantes.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/eventos")
@CrossOrigin(origins = "*")
public class EventoController {

    private final EventoRepository eventoRepository;
    private final EnderecoRepository enderecoRepository;
    private final BairroRepository bairroRepository;
    private final CidadeRepository cidadeRepository;
    private final UnidadeFederacaoRepository unidadeFederacaoRepository;
    private final LogradouroRepository logradouroRepository;
    private final TipoLogradouroRepository tipoLogradouroRepository;
    private final ParticipanteRepository participanteRepository;

    public EventoController(EventoRepository eventoRepository, EnderecoRepository enderecoRepository, BairroRepository bairroRepository, CidadeRepository cidadeRepository, UnidadeFederacaoRepository unidadeFederacaoRepository, LogradouroRepository logradouroRepository, TipoLogradouroRepository tipoLogradouroRepository, ParticipanteRepository participanteRepository) {
        this.eventoRepository = eventoRepository;
        this.enderecoRepository = enderecoRepository;
        this.bairroRepository = bairroRepository;
        this.cidadeRepository = cidadeRepository;
        this.unidadeFederacaoRepository = unidadeFederacaoRepository;
        this.logradouroRepository = logradouroRepository;
        this.tipoLogradouroRepository = tipoLogradouroRepository;
        this.participanteRepository = participanteRepository;
    }


    @GetMapping
    public List<EventoResponseDto> listarEventos() {
        return eventoRepository.findAll().stream()
                .map(EventoResponseDto::new)
                .collect(Collectors.toList());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<EventoResponseDto> criarEvento(@RequestBody EventoRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String adminEmail = authentication.getName();
        Participante admin = participanteRepository.findByEmailParticipante(adminEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Admin não encontrado."));

        UnidadeFederacao uf = unidadeFederacaoRepository.findById(request.getSiglaUf()).orElseGet(() -> unidadeFederacaoRepository.save(new UnidadeFederacao(request.getSiglaUf(), request.getSiglaUf())));
        Cidade cidade = cidadeRepository.findByNomeCidade(request.getNomeCidade()).orElseGet(() -> cidadeRepository.save(new Cidade(UUID.randomUUID().toString(), request.getNomeCidade(), uf)));
        Bairro bairro = bairroRepository.findByNomeBairro(request.getNomeBairro()).orElseGet(() -> bairroRepository.save(new Bairro(UUID.randomUUID().toString(), request.getNomeBairro())));
        TipoLogradouro tipoLogradouro = tipoLogradouroRepository.findByNomeTipoLogradouro(request.getNomeTipoLogradouro()).orElseGet(() -> tipoLogradouroRepository.save(new TipoLogradouro(UUID.randomUUID().toString(), request.getNomeTipoLogradouro())));
        Logradouro logradouro = logradouroRepository.findByNomeLogradouro(request.getNomeLogradouro()).orElseGet(() -> logradouroRepository.save(new Logradouro(UUID.randomUUID().toString(), request.getNomeLogradouro(), tipoLogradouro)));

        Endereco endereco = new Endereco();
        endereco.setCep(request.getCep());
        endereco.setComplemento(request.getComplemento());
        endereco.setNumero(request.getNumero());
        endereco.setBairro(bairro);
        endereco.setCidade(cidade);
        endereco.setLogradouro(logradouro);
        Endereco enderecoSalvo = enderecoRepository.save(endereco);

        Evento novoEvento = new Evento();
        novoEvento.setNome(request.getNome());
        novoEvento.setDataInicio(request.getDataInicio());
        novoEvento.setDataFim(request.getDataFim());
        novoEvento.setHoraInicio(request.getHoraInicio());
        novoEvento.setHoraFim(request.getHoraFim());
        novoEvento.setDescricao(request.getDescricao());
        novoEvento.setEndereco(enderecoSalvo);
        novoEvento.setCriador(admin);

        Evento eventoSalvo = eventoRepository.save(novoEvento);

        // Converte a entidade salva para o DTO de resposta
        EventoResponseDto respostaDto = new EventoResponseDto(eventoSalvo);

        // Retorna o DTO
        return ResponseEntity.status(HttpStatus.CREATED).body(respostaDto);
    }
}