package br.com.alevh.sistema_adocao_pets.util.validations;

import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import br.com.alevh.sistema_adocao_pets.data.dto.v1.OngDTO;
import br.com.alevh.sistema_adocao_pets.exceptions.RequiredObjectIsNullException;
import br.com.alevh.sistema_adocao_pets.model.Ong;
import br.com.alevh.sistema_adocao_pets.repository.OngRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OngValidacao {

    private final OngRepository ongRepository;

    public void validate(OngDTO ong) {
        if (ong == null)
            throw new RequiredObjectIsNullException("JSON vazio");

        // se encontrar a ong no bd retorna badrequest
        if (existsOngWithEmail(ong.getEmail().toLowerCase())) {
            throw new IllegalStateException("E-mail já está em uso");
        }
        if (existsOngWithCnpj(ong.getCnpj().getCnpj())) {
            throw new IllegalStateException("CNPJ já está em uso");
        }
        if (existsOngWithCell(ong.getCell())) {
            throw new IllegalStateException("Celular já está em uso");
        }
        if (existsOngWithNomeUsuario(ong.getNomeUsuario())) {
            throw new IllegalStateException("Nome Usuário já está em uso");
        }
    }

    public void validateUpdate(Ong entity) {
        if (existsOngWithEmail(entity.getEmail().toLowerCase())) {
            throw new IllegalStateException("E-mail já está em uso");
        }
        if (existsOngWithCnpj(entity.getCnpj())) {
            throw new IllegalStateException("CNPJ já está em uso");
        }
        if (existsOngWithCell(entity.getCell())) {
            throw new IllegalStateException("Celular já está em uso");
        }
        if (existsOngWithNomeUsuario(entity.getNomeUsuario())) {
            throw new IllegalStateException("Nome Usuário já está em uso");
        }
    }

    public void validatePartialUpdate(Long id, Map<String, Object> updates) {
        if (updates.containsKey("email")) {
            String email = updates.get("email").toString().toLowerCase();
            Optional<Ong> ongExistente = ongRepository.findByEmail(email);
            if (ongExistente.isPresent() && !ongExistente.get().getIdOng().equals(id)) {
                throw new IllegalStateException("E-mail já está em uso por outra ong");
            }
        }

        if (updates.containsKey("cnpj")) {
            String cnpj = updates.get("cnpj").toString();
            Optional<Ong> ongExistente = ongRepository.findByCnpj(cnpj);
            if (ongExistente.isPresent() && !ongExistente.get().getIdOng().equals(id)) {
                throw new IllegalStateException("CNPJ já está em uso por outra ong");
            }
        }

        if (updates.containsKey("cell")) {
            String cell = updates.get("cell").toString();
            Optional<Ong> ongExistente = ongRepository.findByCell(cell);
            if (ongExistente.isPresent() && !ongExistente.get().getIdOng().equals(id)) {
                throw new IllegalStateException("Celular já está em uso por outra ong");
            }
        }

        if (updates.containsKey("nomeUsuario")) {
            String nomeUsuario = updates.get("nomeUsuario").toString();
            Optional<Ong> ongExistente = ongRepository.findByNomeUsuario(nomeUsuario);
            if (ongExistente.isPresent() && !ongExistente.get().getIdOng().equals(id)) {
                throw new IllegalStateException("Nome de usuário já está em uso por outra ong");
            }
        }
    }
    
    public boolean existsOngWithEmail(String email) {
        return ongRepository.findByEmail(email).isPresent();
    }

    public boolean existsOngWithCnpj(String cnpj) {
        return ongRepository.findByCnpj(cnpj).isPresent();
    }

    public boolean existsOngWithNomeUsuario(String nomeUsuario) {
        return ongRepository.findByNomeUsuario(nomeUsuario).isPresent();
    }

    public boolean existsOngWithCell(String cell) {
        return ongRepository.findByCell(cell).isPresent();
    }
}
