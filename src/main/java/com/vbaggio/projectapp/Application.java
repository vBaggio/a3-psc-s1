package com.vbaggio.projectapp;

import com.vbaggio.projectapp.infra.JpaUtil;
import com.vbaggio.projectapp.model.entity.Cargo;
import com.vbaggio.projectapp.model.entity.Usuario;
import com.vbaggio.projectapp.model.enums.Perfil;
import com.vbaggio.projectapp.repository.CargoRepository;
import com.vbaggio.projectapp.repository.UsuarioRepository;

import java.util.List;

/**
 * Ponto de entrada da aplicação.
 * O bloco de smoke test abaixo valida a stack completa:
 * db.properties → Flyway → Hibernate/JPA → PostgreSQL.
 *
 * TODO Sprint 4: substituir o smoke test pela inicialização da UI Swing.
 */
public class Application {

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("  Sistema de Gerenciamento de Projetos e Equipes");
        System.out.println("=================================================");

        try {
            smokeTest();
        } finally {
            JpaUtil.fechar();
            System.out.println("\n[JPA] EntityManagerFactory encerrado. Bye!");
        }
    }

    // ------------------------------------------------------------------
    // Smoke test — valida persistência de ponta a ponta
    // ------------------------------------------------------------------

    private static void smokeTest() {
        CargoRepository  cargoRepo  = new CargoRepository();
        UsuarioRepository usuarioRepo = new UsuarioRepository();

        System.out.println("\n--- [SMOKE TEST] Iniciando ---\n");

        // 1. Criar e persistir um Cargo
        Cargo cargo = new Cargo();
        cargo.setNome("Desenvolvedor Backend");
        cargoRepo.salvar(cargo);
        System.out.println("[OK] Cargo salvo: " + cargo.getNome() + " | id: " + cargo.getId());

        // 2. Criar e persistir um Usuário vinculado ao cargo
        Usuario usuario = new Usuario();
        usuario.setNome("João da Silva");
        usuario.setCpf("12345678901");
        usuario.setEmail("joao@empresa.com");
        usuario.setLogin("joao.silva");
        usuario.setSenha("senha123");
        usuario.setPerfil(Perfil.COLABORADOR);
        usuario.setCargo(cargo);
        usuarioRepo.salvar(usuario);
        System.out.println("[OK] Usuário salvo: " + usuario.getNome() + " | id: " + usuario.getId());

        // 3. Listar todos os cargos e usuários para confirmar leitura
        List<Cargo> cargos = cargoRepo.listarTodos();
        System.out.println("\n[OK] Cargos no banco (" + cargos.size() + "):");
        cargos.forEach(c -> System.out.println("     - " + c.getNome() + " (" + c.getId() + ")"));

        List<Usuario> usuarios = usuarioRepo.listarTodos();
        System.out.println("\n[OK] Usuários no banco (" + usuarios.size() + "):");
        usuarios.forEach(u -> System.out.printf(
                "     - %s | login: %s | perfil: %s%n",
                u.getNome(), u.getLogin(), u.getPerfil()
        ));

        // 4. Busca por login (fluxo de autenticação)
        usuarioRepo.buscarPorLogin("joao.silva").ifPresentOrElse(
                u -> System.out.println("\n[OK] buscarPorLogin OK → " + u.getNome()),
                ()  -> System.out.println("\n[ERRO] Usuário não encontrado pelo login!")
        );

        System.out.println("\n--- [SMOKE TEST] Concluído com sucesso ---");
    }
}
