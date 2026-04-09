# Relatório Semanal de Desenvolvimento - Sprint 3

**Responsável Principal:** Vinícius Baggio  
**Período Avaliado:** 05/04/2026 a 11/04/2026  
**Status do Ciclo:** Concluído  

---

## 1. Resumo Executivo das Avaliações

A Sprint 3 consolidou as camadas intermediárias da arquitetura MVC: Repository e Controller. Partindo da infraestrutura JPA estabelecida na Sprint 2 (entidades mapeadas, Flyway e `persistence.xml`), evoluí o projeto com a implementação de toda a lógica de acesso a dados via `EntityManager` e das regras de negócio de domínio nos Controllers. Um Singleton central (`JpaUtil`) foi introduzido para gerenciar o ciclo de vida do `EntityManagerFactory` e garantir que as credenciais de conexão nunca sejam hardcoded no código-fonte. A stack foi validada de ponta a ponta com um smoke test de persistência executado com sucesso.

## 2. Diário Histórico de Execuções e Decisões Técnicas

| Data de Registro | Evento Executado / Veredito Arquitetural |
|------------------|------------------------------------------|
| **08/04/2026** | **[DECISÃO TÉCNICA] Centralização de Credenciais com `db.properties`:** Identifiquei que as credenciais de banco estavam replicadas em `persistence.xml`, `pom.xml` e `JpaUtil` — violação do princípio DRY. Introduzi `db.properties` como fonte única de verdade no classpath, adicionando o arquivo ao `.gitignore` e provendo `db.properties.example` como template público para colaboradores. |
| **08/04/2026** | **`properties-maven-plugin` para o Flyway CLI:** O plugin Flyway Maven precisava das credenciais para `mvn flyway:migrate`. Adotei o `properties-maven-plugin 1.2.1` para carregar `db.properties` na fase `initialize` do Maven, expondo `${db.url}`, `${db.usuario}` e `${db.senha}` como variáveis Maven — eliminando o último hardcode do `pom.xml`. |
| **08/04/2026** | **`JpaUtil.java` — Singleton de Infraestrutura JPA:** Criado como ponto central de acesso ao `EntityManagerFactory`. Integra: (1) leitura de `db.properties` via `ClassLoader`; (2) execução do Flyway antes da criação do EMF; (3) injeção programática das propriedades de conexão em `Persistence.createEntityManagerFactory()`, tornando o `persistence.xml` responsável apenas pela configuração comportamental do Hibernate. |
| **08/04/2026** | **Repositories — Acesso a Dados via JPA:** Implementados `CargoRepository`, `UsuarioRepository`, `ProjetoRepository` e `EquipeRepository` com padrão uniforme: cada operação abre e fecha seu próprio `EntityManager`, com controle explícito de transação (`begin/commit/rollback`). Buscas retornam `Optional<T>` para forçar tratamento de ausência pelo Controller. |
| **08/04/2026** | **Controllers — Regras de Negócio de Domínio:** Implementados os 4 Controllers com validações específicas por entidade: `CargoController` (nome único, bloqueio de remoção com vínculos), `UsuarioController` (CPF com 11 dígitos, unicidade de login/e-mail, autenticação), `ProjetoController` (coerência de datas, restrição de perfil GERENTE, máquina de estados de status), `EquipeController` (proteção do último membro em equipe alocada). |
| **08/04/2026** | **Smoke Test e Validação:** Adicionado fluxo de teste ao `Application.main()` cobrindo: persistência de `Cargo` e `Usuario` vinculado, leitura de lista completa e busca por login. Resultado: usuário e cargo persistidos com sucesso no PostgreSQL. Stack validada de ponta a ponta. |

## 3. Registros de Desafios Enfrentados

O principal desafio foi a gestão de credenciais espalhadas por múltiplos artefatos (`persistence.xml`, `pom.xml`, classe Java). A solução adotada — `db.properties` + `properties-maven-plugin` + injeção programática no JPA — elimina o problema de forma elegante e sem dependências pesadas, adequada ao escopo acadêmico desktop sem Spring.

* **Resolução:** A separação entre configuração de comportamento (`persistence.xml`) e configuração de ambiente (`db.properties`) garante que nenhuma credencial seja versionada acidentalmente, e que qualquer desenvolvedor que clonar o repositório receba uma mensagem clara ao tentar rodar sem o arquivo.

## 4. Artefatos Entregues

| Artefato | Caminho |
|----------|---------|
| Configuração de Ambiente | `src/main/resources/db.properties` (local, não versionado) |
| Template de Credenciais | `src/main/resources/db.properties.example` |
| Infraestrutura JPA | `src/main/java/.../infra/JpaUtil.java` |
| Repository — Cargo | `src/main/java/.../repository/CargoRepository.java` |
| Repository — Usuario | `src/main/java/.../repository/UsuarioRepository.java` |
| Repository — Projeto | `src/main/java/.../repository/ProjetoRepository.java` |
| Repository — Equipe | `src/main/java/.../repository/EquipeRepository.java` |
| Controller — Cargo | `src/main/java/.../controller/CargoController.java` |
| Controller — Usuario | `src/main/java/.../controller/UsuarioController.java` |
| Controller — Projeto | `src/main/java/.../controller/ProjetoController.java` |
| Controller — Equipe | `src/main/java/.../controller/EquipeController.java` |
