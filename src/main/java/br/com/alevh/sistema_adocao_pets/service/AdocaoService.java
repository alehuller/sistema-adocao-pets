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
import org.springframework.stereotype.Service;
import org.springframework.util.ReflectionUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import br.com.alevh.sistema_adocao_pets.controller.AdocaoController;
import br.com.alevh.sistema_adocao_pets.data.dto.v1.AdocaoDTO;
import br.com.alevh.sistema_adocao_pets.data.dto.v1.AnimalDTO;
import br.com.alevh.sistema_adocao_pets.exceptions.RequiredObjectIsNullException;
import br.com.alevh.sistema_adocao_pets.exceptions.ResourceNotFoundException;
import br.com.alevh.sistema_adocao_pets.mapper.DozerMapper;
import br.com.alevh.sistema_adocao_pets.model.Adocao;
import br.com.alevh.sistema_adocao_pets.model.Animal;
import br.com.alevh.sistema_adocao_pets.model.Ong;
import br.com.alevh.sistema_adocao_pets.model.Usuario;
import br.com.alevh.sistema_adocao_pets.repository.AdocaoRepository;
import br.com.alevh.sistema_adocao_pets.repository.AnimalRepository;
import br.com.alevh.sistema_adocao_pets.repository.OngRepository;
import br.com.alevh.sistema_adocao_pets.repository.UsuarioRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdocaoService {

    private final AdocaoRepository adocaoRepository;

    private final AnimalRepository animalRepository;

    private final UsuarioRepository usuarioRepository;

    private final AnimalService animalService;

    private final OngRepository ongRepository;

    private final PagedResourcesAssembler<AdocaoDTO> assembler;

    private final Validator validator;

    public PagedModel<EntityModel<AdocaoDTO>> findAll(Pageable pageable) {

        Page<Adocao> adocaoPage = adocaoRepository.findAll(pageable);

        Page<AdocaoDTO> adocaoDtosPage = adocaoPage.map(a -> DozerMapper.parseObject(a, AdocaoDTO.class));
        adocaoDtosPage
                .map(a -> a.add(linkTo(methodOn(AdocaoController.class).acharAdocaoPorId(a.getKey())).withSelfRel()));

        Link link = linkTo(methodOn(AdocaoController.class).listarAdocoes(pageable.getPageNumber(),
                pageable.getPageSize(), "asc")).withSelfRel();
        return assembler.toModel(adocaoDtosPage, link);
    }

    public AdocaoDTO findById(Long id) {

        Adocao entity = adocaoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Adoção não encontrada."));

        AdocaoDTO dto = DozerMapper.parseObject(entity, AdocaoDTO.class);
        dto.add(linkTo(methodOn(AdocaoController.class).acharAdocaoPorId(id)).withSelfRel());
        return dto;
    }

    public AdocaoDTO create(AdocaoDTO adocao) {
        if (adocao == null)
            throw new RequiredObjectIsNullException();

        Animal animal = animalRepository.findById(adocao.getIdAnimal())
                .orElseThrow(() -> new ResourceNotFoundException("Animal não encontrado"));

        Usuario usuario = usuarioRepository.findById(adocao.getIdUsuario())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        Adocao entity = DozerMapper.parseObject(adocao, Adocao.class);
        entity.setAnimal(animal);
        entity.setUsuario(usuario);
        AdocaoDTO dto = DozerMapper.parseObject(adocaoRepository.save(entity), AdocaoDTO.class);
        dto.add(linkTo(methodOn(AdocaoController.class).acharAdocaoPorId(dto.getKey())).withSelfRel());
        return dto;
    }

    public AdocaoDTO update(AdocaoDTO adocao, Long id) {

        if (adocao == null)
            throw new RequiredObjectIsNullException();

        Adocao entity = adocaoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Adoção não encontrada."));

        Usuario usuario = usuarioRepository.findById(adocao.getIdUsuario())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrada."));

        AnimalDTO animalDTO = animalService.findById(adocao.getIdAnimal());
        Animal animal = DozerMapper.parseObject(animalDTO, Animal.class);

        Ong ong = ongRepository.findById(animal.getOng().getIdOng())
                .orElseThrow(() -> new ResourceNotFoundException("Ong não encontrada."));

        entity.setDataAdocao(adocao.getDataAdocao());
        entity.setStatus(adocao.getStatus());
        entity.setUsuario(usuario);
        animal.setOng(ong);
        entity.setAnimal(animal);

        AdocaoDTO dto = DozerMapper.parseObject(adocaoRepository.save(entity), AdocaoDTO.class);
        dto.add(linkTo(methodOn(AdocaoController.class).acharAdocaoPorId(dto.getKey())).withSelfRel());
        return dto;
    }

    public AdocaoDTO partialUpdate(Long id, Map<String, Object> updates) {
        Adocao adocao = adocaoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Adoção não encontrada."));

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        updates.forEach((campo, valor) -> {
            Field field = ReflectionUtils.findField(Adocao.class, campo);
            if (field != null) {
                field.setAccessible(true);
                ReflectionUtils.setField(field, adocao, mapper.convertValue(valor, field.getType()));
            }
        });

        // Mapeia a entidade para o DTO
        AdocaoDTO adocaoDTO = DozerMapper.parseObject(adocao, AdocaoDTO.class);

        // Faz a validação do DTO
        Set<ConstraintViolation<AdocaoDTO>> violations = validator.validate(adocaoDTO);

        // Se houver erros de validação, lança uma exceção
        if (!violations.isEmpty()) {
            StringBuilder errors = new StringBuilder();
            for (ConstraintViolation<AdocaoDTO> violation : violations) {
                errors.append(violation.getMessage());
            }
            throw new ConstraintViolationException("Erro de validação: " + errors.toString(), violations);
        }

        adocaoRepository.save(adocao);
        return DozerMapper.parseObject(adocao, AdocaoDTO.class);
    }

    public void delete(Long id) {
        adocaoRepository.deleteById(id);
    }
}