package br.com.alevh.sistema_adocao_pets.util.validations;

import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import br.com.alevh.sistema_adocao_pets.data.dto.v1.AdministradorDTO;
import br.com.alevh.sistema_adocao_pets.exceptions.RequiredObjectIsNullException;
import br.com.alevh.sistema_adocao_pets.model.Administrador;
import br.com.alevh.sistema_adocao_pets.repository.AdministradorRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AdministradorValidacao {

    private final AdministradorRepository administradorRepository;

    public boolean existsAdministradorWithEmail(String email) {
        return administradorRepository.findByEmail(email).isPresent();
    }

    public boolean existsAdministradorWithNomeUsuario(String nomeUsuario) {
        return administradorRepository.findByNomeUsuario(nomeUsuario).isPresent();
    }

    public boolean existsAdministradorWithCell(String cell) {
        return administradorRepository.findByCell(cell).isPresent();
    }

    public void validate(AdministradorDTO admin) {

        if (admin == null) {
            throw new RequiredObjectIsNullException("Não há dados");
        }
        if (existsAdministradorWithEmail(admin.getEmail().toLowerCase())) {
            throw new IllegalStateException("E-mail já está em uso");
        }
        if (existsAdministradorWithNomeUsuario(admin.getNomeUsuario())) {
            throw new IllegalStateException("Nome de Usuário já está em uso");
        }
        if (existsAdministradorWithCell(admin.getCell())) {
            throw new IllegalStateException("Cell já está em uso");
        }
    }

    public void validateUpdate(Administrador entity) {
        if (existsAdministradorWithEmail(entity.getEmail().toLowerCase())) {
            throw new IllegalStateException("E-mail já está em uso");
        }
        if (existsAdministradorWithNomeUsuario(entity.getNomeUsuario())) {
            throw new IllegalStateException("Nome de Usuário já está em uso");
        }
        if (existsAdministradorWithCell(entity.getCell())) {
            throw new IllegalStateException("Cell já está em uso");
        }
    }

    public void validatePartialUpdate(String nomeUsuario, Map<String, Object> updates) {
        if (updates.containsKey("email")) {
            String email = updates.get("email").toString().toLowerCase();
            Optional<Administrador> usuarioExistente = administradorRepository.findByEmail(email);
            if (usuarioExistente.isPresent() && !usuarioExistente.get().getNomeUsuario().equals(nomeUsuario)) {
                throw new IllegalStateException("E-mail já está em uso por outro administrador");
            }
        }

        if (updates.containsKey("nomeUsuario")) {
            String nomeUsuario2 = updates.get("nomeUsuario").toString();
            Optional<Administrador> usuarioExistente = administradorRepository.findByNomeUsuario(nomeUsuario2);
            if (usuarioExistente.isPresent() && !usuarioExistente.get().getNomeUsuario().equals(nomeUsuario)) {
                throw new IllegalStateException("Nome de usuário já está em uso por outro administrador");
            }
        }

        if (updates.containsKey("cell")) {
            String cell = updates.get("cell").toString();
            Optional<Administrador> usuarioExistente = administradorRepository.findByCell(cell);
            if (usuarioExistente.isPresent() && !usuarioExistente.get().getNomeUsuario().equals(nomeUsuario)) {
                throw new IllegalStateException("Celular já está em uso por outro administrador");
            }
        }
    }
}
