package br.com.alevh.sistema_adocao_pets.repository;

import br.com.alevh.sistema_adocao_pets.model.LoginIdentityView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LoginIdentityViewRepository extends JpaRepository<LoginIdentityView, Long> {
    Optional<LoginIdentityView> findByEmail(String email);
    Optional<LoginIdentityView> findByNomeUsuario(String nomeUsuario);
}
