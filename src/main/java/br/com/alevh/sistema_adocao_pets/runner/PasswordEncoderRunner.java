package br.com.alevh.sistema_adocao_pets.runner;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import br.com.alevh.sistema_adocao_pets.repository.OngRepository;
import br.com.alevh.sistema_adocao_pets.repository.UsuarioRepository;
import br.com.alevh.sistema_adocao_pets.model.Ong;
import br.com.alevh.sistema_adocao_pets.model.Usuario;

import java.util.List;

@Component
public class PasswordEncoderRunner implements ApplicationRunner {

    private final OngRepository ongRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public PasswordEncoderRunner(UsuarioRepository usuarioRepository, OngRepository ongRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.ongRepository = ongRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        List<Usuario> usuarios = usuarioRepository.findAll();
        List<Ong> ongs = ongRepository.findAll();

        for (Usuario usuario : usuarios) {
            if (!usuario.getSenha().startsWith("{bcrypt}")) {
                String encodedPassword = passwordEncoder.encode(usuario.getSenha());
                usuario.setSenha(encodedPassword);
                usuarioRepository.save(usuario);
            }
        }

        for (Ong ong : ongs) {
            if (!ong.getSenha().startsWith("{bcrypt}")) {
                String encodedPassword = passwordEncoder.encode(ong.getSenha());
                ong.setSenha(encodedPassword);
                ongRepository.save(ong);
            }
        }
    }
}
