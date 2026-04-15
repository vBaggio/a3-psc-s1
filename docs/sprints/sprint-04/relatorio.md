# Relatório Semanal de Desenvolvimento - Sprint 4

**Responsável Principal:** Vinícius Baggio  
**Período Avaliado:** 12/04/2026 a 18/04/2026  
**Status do Ciclo:** Concluído  

---

## 1. Resumo Executivo das Avaliações

A Sprint 4 complementou a camada de domínio do sistema com a entidade `Tarefa`, adicionando uma dimensão de rastreabilidade de trabalho aos projetos já gerenciados. Três frentes foram trabalhadas em paralelo: (1) a criação completa do ciclo de vida da `Tarefa` — migration, entidade JPA, enum de status, repository e controller com máquina de estados; (2) a introdução de segurança no armazenamento de senhas via BCrypt, substituindo o armazenamento em texto plano que existia implicitamente; (3) o ajuste dos controllers existentes (`ProjetoController` e `UsuarioController`) para garantir integridade referencial com a nova entidade, propagando cancelamentos em cascata e bloqueando remoções de usuários com tarefas ativas.

## 2. Diário Histórico de Execuções e Decisões Técnicas

| Data de Registro | Evento Executado / Veredito Arquitetural |
|------------------|------------------------------------------|
| **12/04/2026** | **[DECISÃO TÉCNICA] Migration incremental com Flyway — `V2__Add_tarefa.sql`:** A tabela `tarefa` foi adicionada via migration versionada, preservando o histórico de schema. A FK para `usuario` (responsável) foi definida como `nullable` para suportar tarefas sem responsável atribuído sem violar a integridade referencial. |
| **12/04/2026** | **Entidade `Tarefa` e enum `StatusTarefa`:** A entidade foi mapeada com `@ManyToOne` para `Projeto` (obrigatório) e `Usuario` (opcional, `fetch = LAZY`). O enum `StatusTarefa` define os quatro estados possíveis: `PENDENTE`, `EM_ANDAMENTO`, `CONCLUIDA` e `CANCELADA`, armazenados como `STRING` no banco via `@Enumerated(EnumType.STRING)`. |
| **12/04/2026** | **`TarefaRepository` — CRUD e filtros contextuais:** Implementado seguindo o mesmo padrão dos repositories anteriores (controle explícito de transação, `EntityManager` por operação). Três queries JPQL especializadas foram adicionadas: `listarPorProjeto` (ordenada por prazo ASC, nulos por último), `listarPorResponsavel` e `listarPorStatus`. |
| **12/04/2026** | **Adição de `buscarPorEmail()` em `UsuarioRepository`:** Necessária para suportar a validação de unicidade de e-mail no `UsuarioController` — a operação de verificação por e-mail não existia na sprint anterior, onde apenas CPF e login eram verificados. |
| **13/04/2026** | **`TarefaController` — Máquina de estados e regras de negócio:** A lógica de transição de status foi encapsulada em método privado `validarTransicaoStatus()` usando `switch` com pattern matching. Transições permitidas: `PENDENTE → EM_ANDAMENTO | CANCELADA` e `EM_ANDAMENTO → CONCLUIDA | CANCELADA`. Tentativas de transição a partir de estados terminais lançam `IllegalStateException` diretamente no `case`. A criação de tarefa é bloqueada para projetos com status diferente de `PLANEJADO` ou `EM_ANDAMENTO`. Remoção só é permitida para tarefas `PENDENTE` ou `CANCELADA`. |
| **13/04/2026** | **BCrypt em `UsuarioController`:** Integrado `jbcrypt 0.4` para hash de senhas. No cadastro, a senha é transformada via `BCrypt.hashpw(senha, BCrypt.gensalt())` antes de persistir. Na autenticação, `BCrypt.checkpw()` compara a senha informada com o hash armazenado sem reverter o hash. Na atualização, uma nova senha em texto plano (quando informada) gera novo hash — senhas não informadas mantêm o hash existente. |
| **13/04/2026** | **`ProjetoController` — Cancelamento em cascata de tarefas:** Ao transitar um projeto para `CANCELADO`, todas as tarefas vinculadas com status não-terminal (`PENDENTE` ou `EM_ANDAMENTO`) são automaticamente canceladas em uma única operação em lote, garantindo consistência sem expor essa responsabilidade para a View. |
| **13/04/2026** | **`UsuarioController` — Bloqueio de remoção com tarefas ativas:** Antes de deletar um usuário, o controller consulta `TarefaRepository.listarPorResponsavel()` e verifica se há tarefas com `PENDENTE` ou `EM_ANDAMENTO`. Se houver, a operação é abortada com `IllegalStateException`, orientando o operador a reatribuir as tarefas primeiro. |
| **14/04/2026** | **[REFACTOR] Padronização de nomenclatura em `TarefaController`:** O campo interno foi renomeado de `tarefaRepository` para `tarefaRepo`, alinhando ao padrão dos demais controllers (`projetoRepo`, `usuarioRepo`, `equipeRepo`). Comentário de sprint removido do código fonte. |

## 3. Registros de Desafios Enfrentados

O principal desafio foi garantir que o cancelamento em cascata das tarefas no `ProjetoController` não introduzisse um ciclo de dependências circular entre controllers. A solução adotada foi manter a operação diretamente no `ProjetoController` via `TarefaRepository`, sem delegar ao `TarefaController` — o que teria forçado uma instância do controller filho dentro do pai, acoplando as camadas de forma indesejada.

* **Resolução:** Controllers dependem de Repositories, nunca de outros Controllers. A regra de cascata é executada diretamente com `TarefaRepository.atualizar()`, respeitando a separação de responsabilidades.

Outro ponto de atenção foi a validação de unicidade de e-mail no `UsuarioController`: a Sprint 3 não possuía `buscarPorEmail()` no repository, e a ausência foi identificada ao implementar o método `validarUnicidade()` na Sprint 4. A adição foi feita de forma cirúrgica sem alterar o contrato existente do repository.

## 4. Artefatos Entregues

| Artefato | Caminho |
|----------|---------|
| Migration — Tarefa | `src/main/resources/db/migration/V2__Add_tarefa.sql` |
| Entidade JPA | `src/main/java/.../model/entity/Tarefa.java` |
| Enum de Status | `src/main/java/.../model/enums/StatusTarefa.java` |
| Repository — Tarefa | `src/main/java/.../repository/TarefaRepository.java` |
| Método `buscarPorEmail()` | `src/main/java/.../repository/UsuarioRepository.java` |
| Controller — Tarefa | `src/main/java/.../controller/TarefaController.java` |
| BCrypt + bloqueio de remoção | `src/main/java/.../controller/UsuarioController.java` |
| Cancelamento em cascata | `src/main/java/.../controller/ProjetoController.java` |
