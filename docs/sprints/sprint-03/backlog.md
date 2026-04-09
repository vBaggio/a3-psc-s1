# Sprint Backlog - Sprint 3

**Período Inicial/Final:** 05/04/2026 a 11/04/2026  
**Responsável do Projeto:** Vinícius Baggio  
**Objetivo da Sprint:** Desenvolver a camada Repository para acesso a dados e mapeamento objeto-relacional com JPA/Hibernate, e a camada Controller para encapsular as regras de negócio e intermediar o fluxo entre a interface (View) e o banco de dados (Model).

---

## Tarefas Catalogadas (Backlog Items)

| ID | Descrição da Tarefa | Status |
|----|----------------------|--------|
| **TSK-01** | Centralizar credenciais de banco em `db.properties` como fonte única de verdade, removendo hardcode de `persistence.xml` e `pom.xml`. | ✅ Concluído |
| **TSK-02** | Criar `JpaUtil.java` como Singleton do `EntityManagerFactory`, com inicialização do Flyway integrada e leitura de `db.properties` via classpath. | ✅ Concluído |
| **TSK-03** | Implementar `CargoRepository` com operações CRUD e busca por nome (case-insensitive). | ✅ Concluído |
| **TSK-04** | Implementar `UsuarioRepository` com CRUD, busca por login/CPF e filtro por perfil de acesso. | ✅ Concluído |
| **TSK-05** | Implementar `ProjetoRepository` com CRUD e filtros por status e por gerente responsável. | ✅ Concluído |
| **TSK-06** | Implementar `EquipeRepository` com CRUD e gerenciamento das relações `@ManyToMany` de membros e projetos. | ✅ Concluído |
| **TSK-07** | Implementar `CargoController` com validação de nome único e bloqueio de remoção de cargo vinculado a usuários. | ✅ Concluído |
| **TSK-08** | Implementar `UsuarioController` com validação de CPF (11 dígitos), unicidade de login/e-mail e autenticação. | ✅ Concluído |
| **TSK-09** | Implementar `ProjetoController` com validação de datas, verificação de perfil GERENTE e máquina de estados de status. | ✅ Concluído |
| **TSK-10** | Implementar `EquipeController` com proteção do último membro em projeto ativo e validação de remoção de equipe alocada. | ✅ Concluído |
| **TSK-11** | Validar a stack de persistência de ponta a ponta com smoke test em `Application.main()`. | ✅ Concluído |

---

## Ferramentas Adotadas na Sprint

- **ORM / Persistência:** Hibernate 6.4.4 (JPA 3.0)
- **Controle de Schema:** Flyway 10.10.0
- **Build Maven:** `properties-maven-plugin 1.2.1` (leitura de `db.properties` no build)
- **Banco de Dados:** PostgreSQL 16 (Alpine via Docker)
- **Editor de Texto:** VS Code / IntelliJ IDEA
- **Controle de Versões:** Git (commits semânticos por camada)
