package br.com.alevh.sistema_adocao_pets.service;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Set;

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

import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.alevh.sistema_adocao_pets.controller.AdocaoController;
import br.com.alevh.sistema_adocao_pets.controller.OngController;
import br.com.alevh.sistema_adocao_pets.data.dto.common.EnderecoVO;
import br.com.alevh.sistema_adocao_pets.data.dto.common.SiteVO;
import br.com.alevh.sistema_adocao_pets.data.dto.security.LoginDTO;
import br.com.alevh.sistema_adocao_pets.data.dto.security.TokenDTO;
import br.com.alevh.sistema_adocao_pets.data.dto.v1.AdocaoDTO;
import br.com.alevh.sistema_adocao_pets.data.dto.v1.OngDTO;
import br.com.alevh.sistema_adocao_pets.data.dto.v1.OngFiltroDTO;
import br.com.alevh.sistema_adocao_pets.data.dto.v1.OngUpdateDTO;
import br.com.alevh.sistema_adocao_pets.exceptions.RequiredObjectIsNullException;
import br.com.alevh.sistema_adocao_pets.exceptions.ResourceNotFoundException;
import br.com.alevh.sistema_adocao_pets.mapper.DozerMapper;
import br.com.alevh.sistema_adocao_pets.model.Adocao;
import br.com.alevh.sistema_adocao_pets.model.LoginIdentityView;
import br.com.alevh.sistema_adocao_pets.model.Ong;
import br.com.alevh.sistema_adocao_pets.repository.AdocaoRepository;
import br.com.alevh.sistema_adocao_pets.repository.OngRepository;
import br.com.alevh.sistema_adocao_pets.security.Roles;
import br.com.alevh.sistema_adocao_pets.util.validations.OngValidacao;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OngService {

    private final OngRepository ongRepository;

    private final AdocaoRepository adocaoRepository;

    private final PasswordEncoder passwordEncoder;

    private final PagedResourcesAssembler<OngDTO> assembler;

    private final PagedResourcesAssembler<AdocaoDTO> adocaoDtoAssembler;

    private final Validator validator;

    private final AuthenticationManager authenticationManager;

    private final br.com.alevh.sistema_adocao_pets.service.auth.TokenService tokenService;

    private final OngValidacao ongValidacao;

    private final CepService cepService;

    public PagedModel<EntityModel<OngDTO>> findAll(Pageable pageable) {

        Page<Ong> ongPage = ongRepository.findAll(pageable);

        Page<OngDTO> ongDtosPage = ongPage.map(o -> DozerMapper.parseObject(o, OngDTO.class));
        ongDtosPage.map(o -> o.add(linkTo(methodOn(OngController.class).acharOngPorId(o.getKey())).withSelfRel()));

        Link link = linkTo(methodOn(OngController.class).listarOngs(pageable.getPageNumber(),
                pageable.getPageSize(), "asc")).withSelfRel();
        return assembler.toModel(ongDtosPage, link);
    }

    public OngDTO findById(Long id) {

        Ong entity = ongRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ong não encontrado."));

        OngDTO dto = DozerMapper.parseObject(entity, OngDTO.class);
        dto.add(linkTo(methodOn(OngController.class).acharOngPorId(id)).withSelfRel());
        return dto;

    }

    public OngDTO findByNomeUsuario(String nomeUsuario) {

        Ong entity = ongRepository.findByNomeUsuario(nomeUsuario)
                .orElseThrow(() -> new ResourceNotFoundException("Ong não encontrado."));

        OngDTO dto = DozerMapper.parseObject(entity, OngDTO.class);
        dto.add(linkTo(methodOn(OngController.class).acharOngPorNomeUsuario(nomeUsuario)).withSelfRel());
        return dto;

    }

    public PagedModel<EntityModel<AdocaoDTO>> findAllAdocoesByOngId(Long idOng, Pageable pageable) {

        Page<Adocao> adocaoPage = adocaoRepository.findAdocoesByOngId(idOng, pageable);

        Page<AdocaoDTO> adocaoDtoPage = adocaoPage.map(a -> DozerMapper.parseObject(a, AdocaoDTO.class));

        adocaoDtoPage = adocaoDtoPage.map(
                dto -> dto.add(linkTo(methodOn(AdocaoController.class).acharAdocaoPorId(dto.getKey())).withSelfRel()));

        Link selfLink = linkTo(methodOn(OngController.class)
                .listarAdocoesPorOngId(idOng, pageable.getPageNumber(), pageable.getPageSize(), "asc"))
                .withSelfRel();

        return adocaoDtoAssembler.toModel(adocaoDtoPage, selfLink);
    }

    public Page<OngDTO> filtrarOngs(OngFiltroDTO filtro, Pageable pageable) {
        Page<Ong> ongs = ongRepository.filtrarOngsNativo(filtro, pageable);
        return ongs.map(ong -> DozerMapper.parseObject(ong, OngDTO.class));
    }

    public OngDTO create(OngDTO ong) {

        // Passo 3: Preenche o endereço com base no CEP usando a API ViaCEP
        cepService.preencherEndereco(ong.getEndereco());
        ongValidacao.validarEnderecoPreenchido(ong.getEndereco());

        ongValidacao.validate(ong);

        ong.setSenha(passwordEncoder.encode(ong.getSenha()));
        Ong entity = DozerMapper.parseObject(ong, Ong.class);
        entity.setCnpj(ong.getCnpj().getCnpj());
        entity.setEmail(ong.getEmail().toLowerCase());
        entity.setRole(Roles.ONG);

        OngDTO dto = DozerMapper.parseObject(ongRepository.save(entity), OngDTO.class);
        dto.add(linkTo(methodOn(OngController.class).acharOngPorId(dto.getKey())).withSelfRel());

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

    public OngDTO update(OngUpdateDTO ongUpdate, String nomeUsuario) {
        if (ongUpdate == null)
            throw new RequiredObjectIsNullException();

        Ong entity = ongRepository.findByNomeUsuario(nomeUsuario)
                .orElseThrow(() -> new ResourceNotFoundException("Ong não encontrado."));

        entity.setNome(ongUpdate.getNome());
        entity.setNomeUsuario(ongUpdate.getNomeUsuario());
        entity.setFotoPerfil(ongUpdate.getFotoPerfil());
        entity.setEmail(ongUpdate.getEmail().toLowerCase());
        entity.setSenha(passwordEncoder.encode(ongUpdate.getSenha()));
        entity.setEndereco(ongUpdate.getEndereco());
        entity.setCell(ongUpdate.getCell());
        entity.setResponsavel(ongUpdate.getResponsavel());
        entity.setDescricao(ongUpdate.getDescricao());
        entity.setSite(ongUpdate.getSite());

        ongValidacao.validateUpdate(entity);

        OngDTO dto = DozerMapper.parseObject(ongRepository.save(entity), OngDTO.class);

        dto.add(linkTo(methodOn(OngController.class).acharOngPorId(dto.getKey())).withSelfRel());

        return dto;
    }

    public OngDTO partialUpdate(String nomeUsuario, Map<String, Object> updates) {
        Ong ong = ongRepository.findByNomeUsuario(nomeUsuario)
                .orElseThrow(() -> new ResourceNotFoundException("Ong não encontrada."));

        ongValidacao.validatePartialUpdate(nomeUsuario, updates);

        ObjectMapper mapper = new ObjectMapper();
        updates.forEach((campo, valor) -> {
            Field field = ReflectionUtils.findField(Ong.class, campo);
            if (field != null) {
                field.setAccessible(true);

                if (campo.equalsIgnoreCase("email") && valor instanceof String) {
                    valor = ((String) valor).toLowerCase();
                }
                if (campo.equals("endereco") && valor instanceof Map<?, ?> valorMapEndereco) {
                    EnderecoVO enderecoOriginal = ong.getEndereco();
                    EnderecoVO enderecoAtualizado = mapper.convertValue(valor, EnderecoVO.class);

                    if (enderecoOriginal == null) {
                        ong.setEndereco(enderecoAtualizado);
                    } else {
                        // merge campo a campo
                        if (enderecoAtualizado.getLogradouro() != null)
                            enderecoOriginal.setLogradouro(enderecoAtualizado.getLogradouro());
                        if (enderecoAtualizado.getNumero() != null)
                            enderecoOriginal.setNumero(enderecoAtualizado.getNumero());
                        if (enderecoAtualizado.getComplemento() != null)
                            enderecoOriginal.setComplemento(enderecoAtualizado.getComplemento());
                        if (enderecoAtualizado.getBairro() != null)
                            enderecoOriginal.setBairro(enderecoAtualizado.getBairro());
                        if (enderecoAtualizado.getCidade() != null)
                            enderecoOriginal.setCidade(enderecoAtualizado.getCidade());
                        if (enderecoAtualizado.getEstado() != null)
                            enderecoOriginal.setEstado(enderecoAtualizado.getEstado());
                        if (enderecoAtualizado.getCep() != null)
                            enderecoOriginal.setCep(enderecoAtualizado.getCep());
                    }

                } else if (campo.equals("site") && valor instanceof Map<?, ?> valorMapSite) {
                    SiteVO siteOriginal = ong.getSite();
                    SiteVO siteAtualizado = mapper.convertValue(valor, SiteVO.class);

                    if (siteOriginal == null) {
                        ong.setSite(siteAtualizado);
                    } else {
                        if (siteAtualizado.getSite() != null)
                            siteOriginal.setSite(siteAtualizado.getSite());
                        if (siteAtualizado.getInstagram() != null)
                            siteOriginal.setInstagram(siteAtualizado.getInstagram());
                        if (siteAtualizado.getFacebook() != null)
                            siteOriginal.setFacebook(siteAtualizado.getFacebook());
                        if (siteAtualizado.getTiktok() != null)
                            siteOriginal.setTiktok(siteAtualizado.getTiktok());
                        if (siteAtualizado.getYoutube() != null)
                            siteOriginal.setYoutube(siteAtualizado.getYoutube());
                        if (siteAtualizado.getWhatsapp() != null)
                            siteOriginal.setWhatsapp(siteAtualizado.getWhatsapp());
                        if (siteAtualizado.getX() != null)
                            siteOriginal.setX(siteAtualizado.getX());
                        if (siteAtualizado.getLinkedin() != null)
                            siteOriginal.setLinkedin(siteAtualizado.getLinkedin());
                    }
                } else {
                    ReflectionUtils.setField(field, ong, mapper.convertValue(valor, field.getType()));
                }

            }
        });

        OngDTO ongDTO = DozerMapper.parseObject(ong, OngDTO.class);

        Set<ConstraintViolation<OngDTO>> violations = validator.validate(ongDTO);

        if (!violations.isEmpty()) {
            StringBuilder errors = new StringBuilder();
            for (ConstraintViolation<OngDTO> violation : violations) {
                errors.append(violation.getMessage());
            }
            throw new ConstraintViolationException("Erro de validação: " + errors.toString(), violations);
        }

        ongRepository.save(ong);
        return DozerMapper.parseObject(ong, OngDTO.class);
    }

    @Transactional
    public void delete(String nomeUsuario) {
        ongRepository.deleteByNomeUsuario(nomeUsuario);
    }
}