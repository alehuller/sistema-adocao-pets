package br.com.alevh.sistema_adocao_pets.service;

import br.com.alevh.sistema_adocao_pets.controller.AdocaoController;
import br.com.alevh.sistema_adocao_pets.controller.UsuarioController;
import br.com.alevh.sistema_adocao_pets.data.dto.security.LoginDTO;
import br.com.alevh.sistema_adocao_pets.data.dto.security.TokenDTO;
import br.com.alevh.sistema_adocao_pets.data.dto.security.RegistroDTO;
import br.com.alevh.sistema_adocao_pets.data.dto.v1.AdocaoDTO;
import br.com.alevh.sistema_adocao_pets.data.dto.v1.UsuarioDTO;
import br.com.alevh.sistema_adocao_pets.data.dto.v1.UsuarioUpdateDTO;
import br.com.alevh.sistema_adocao_pets.exceptions.ResourceNotFoundException;
import br.com.alevh.sistema_adocao_pets.mapper.DozerMapper;
import br.com.alevh.sistema_adocao_pets.model.Adocao;
import br.com.alevh.sistema_adocao_pets.model.LoginIdentityView;
import br.com.alevh.sistema_adocao_pets.model.Usuario;
import br.com.alevh.sistema_adocao_pets.repository.AdocaoRepository;
import br.com.alevh.sistema_adocao_pets.repository.UsuarioRepository;
import br.com.alevh.sistema_adocao_pets.security.Roles;
import br.com.alevh.sistema_adocao_pets.service.auth.TokenService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import br.com.alevh.sistema_adocao_pets.util.validations.UsuarioValidacao;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedModel;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Set;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

    private final AdocaoRepository adocaoRepository;

    private final PasswordEncoder passwordEncoder;

    private final PagedResourcesAssembler<UsuarioDTO> assembler;

    private final AuthenticationManager authenticationManager;

    private final TokenService tokenService;

    private final PagedResourcesAssembler<AdocaoDTO> adocaoDtoAssembler;

    private final Validator validator;

    private final UsuarioValidacao usuarioValidacao;

    public PagedModel<EntityModel<UsuarioDTO>> findAll(Pageable pageable) {

        Page<Usuario> usuarioPage = usuarioRepository.findAll(pageable);

        Page<UsuarioDTO> usuarioDtosPage = usuarioPage.map(u -> DozerMapper.parseObject(u, UsuarioDTO.class));
        usuarioDtosPage
                .map(u -> u.add(linkTo(methodOn(UsuarioController.class).acharUsuarioPorId(u.getKey())).withSelfRel()));

        Link link = linkTo(methodOn(UsuarioController.class).listarUsuarios(pageable.getPageNumber(),
                pageable.getPageSize(), "asc")).withSelfRel();
        return assembler.toModel(usuarioDtosPage, link);
    }

    public UsuarioDTO findByNomeUsuario(String nomeUsuario) {

        Usuario entity = usuarioRepository.findByNomeUsuario(nomeUsuario)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));

        UsuarioDTO dto = DozerMapper.parseObject(entity, UsuarioDTO.class);
        dto.add(linkTo(methodOn(UsuarioController.class).acharUsuarioPorNomeUsuario(nomeUsuario)).withSelfRel());
        return dto;
    }

    public UsuarioDTO findById(Long id) {

        Usuario entity = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));

        UsuarioDTO dto = DozerMapper.parseObject(entity, UsuarioDTO.class);
        dto.add(linkTo(methodOn(UsuarioController.class).acharUsuarioPorId(id)).withSelfRel());
        return dto;
    }

    public PagedModel<EntityModel<AdocaoDTO>> findAllAdocoesByNomeUsuario(String nomeUsuario, Pageable pageable) {

        Page<Adocao> adocaoPage = adocaoRepository.findAdocoesByNomeUsuario(nomeUsuario, pageable);

        Page<AdocaoDTO> adocaoDtoPage = adocaoPage.map(a -> DozerMapper.parseObject(a, AdocaoDTO.class));

        adocaoDtoPage = adocaoDtoPage.map(
                dto -> dto.add(linkTo(methodOn(AdocaoController.class).acharAdocaoPorId(dto.getKey())).withSelfRel()));

        Link selfLink = linkTo(methodOn(UsuarioController.class)
                .listarAdocoesPorNomeUsuario(nomeUsuario, pageable.getPageNumber(), pageable.getPageSize(), "asc"))
                .withSelfRel();

        return adocaoDtoAssembler.toModel(adocaoDtoPage, selfLink);
    }
    
    public UsuarioDTO create(RegistroDTO registroDTO) {

        usuarioValidacao.validate(registroDTO);

        Usuario entity = DozerMapper.parseObject(registroDTO, Usuario.class);
        entity.setCpf(registroDTO.getCpf().getCpf());
        entity.setSenha(passwordEncoder.encode(registroDTO.getPassword()));
        entity.setEmail(registroDTO.getEmail().toLowerCase());
        entity.setRole(Roles.USER);
        UsuarioDTO dto = DozerMapper.parseObject(usuarioRepository.save(entity), UsuarioDTO.class);
        dto.add(
                linkTo(
                        methodOn(UsuarioController.class).acharUsuarioPorId(dto.getKey())).withSelfRel());
        return dto;
    }

    public TokenDTO logar(LoginDTO data) {

        String identifier = data.identifier();

        // Se for um e-mail (tem '@'), transforma para lowercase
        if (identifier.contains("@")) {
            identifier = identifier.toLowerCase();
        }

        // credenciais do spring security
        var usernamePassword = new UsernamePasswordAuthenticationToken(identifier, data.password());

        // autentica de forma milagrosa as credenciais
        var auth = this.authenticationManager.authenticate(usernamePassword);

        var token = tokenService.generateToken((LoginIdentityView) auth.getPrincipal());

        return new TokenDTO(token);
    }

    public UsuarioDTO update(UsuarioUpdateDTO usuarioUpdate, String nomeUsuario) {

        Usuario entity = usuarioRepository.findByNomeUsuario(nomeUsuario)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));

        entity.setNome(usuarioUpdate.getNome());
        entity.setNomeUsuario(usuarioUpdate.getNomeUsuario());
        entity.setFotoPerfil(usuarioUpdate.getFotoPerfil());
        entity.setEmail(usuarioUpdate.getEmail().toLowerCase());
        entity.setSenha(passwordEncoder.encode(usuarioUpdate.getSenha()));
        entity.setCell(usuarioUpdate.getCell());

        usuarioValidacao.validateUpdate(entity);

        UsuarioDTO dto = DozerMapper.parseObject(usuarioRepository.save(entity), UsuarioDTO.class);
        dto.add(linkTo(methodOn(UsuarioController.class).acharUsuarioPorId(dto.getKey())).withSelfRel());
        return dto;
    }

    public UsuarioDTO partialUpdate(String nomeUsuario, Map<String, Object> updates) {
        Usuario usuario = usuarioRepository.findByNomeUsuario(nomeUsuario)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));

        usuarioValidacao.validatePartialUpdate(nomeUsuario, updates);

        ObjectMapper mapper = new ObjectMapper();
        updates.forEach((campo, valor) -> {
            Field field = ReflectionUtils.findField(Usuario.class, campo);
            if (field != null) {
                field.setAccessible(true);

                if (campo.equalsIgnoreCase("email") && valor instanceof String) {
                    valor = ((String) valor).toLowerCase();
                }

                ReflectionUtils.setField(field, usuario, mapper.convertValue(valor, field.getType()));
            }
        });

        UsuarioDTO usuarioDTO = DozerMapper.parseObject(usuario, UsuarioDTO.class);

        Set<ConstraintViolation<UsuarioDTO>> violations = validator.validate(usuarioDTO);

        if (!violations.isEmpty()) {
            StringBuilder errors = new StringBuilder();
            for (ConstraintViolation<UsuarioDTO> violation : violations) {
                errors.append(violation.getMessage());
            }
            throw new ConstraintViolationException("Erro de validação: " + errors.toString(), violations);
        }

        usuarioRepository.save(usuario);
        return DozerMapper.parseObject(usuario, UsuarioDTO.class);
    }

    @Transactional
    public void delete(String nomeUsuario) {
        usuarioRepository.deleteByNomeUsuario(nomeUsuario);
    }
}
