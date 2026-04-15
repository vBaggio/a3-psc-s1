# Sprint Backlog - Sprint 4

**Período Inicial/Final:** 12/04/2026 a 18/04/2026  
**Responsável do Projeto:** Vinícius Baggio  
**Objetivo da Sprint:** Complementar a camada de domínio com a entidade Tarefa, garantir segurança no armazenamento de senhas com BCrypt e ajustar os controllers existentes para integridade referencial com a nova entidade.

---

## Tarefas Catalogadas (Backlog Items)

| ID | Descrição da Tarefa | Status |
|----|----------------------|--------|
| **TSK-01** | Criar migration `V2__Create_tarefa.sql` para a tabela `tarefa` no PostgreSQL, com FK para `projeto` e `usuario` (responsável nullable). | ✅ Concluído |
| **TSK-02** | Implementar entidade JPA `Tarefa` com mapeamentos `@ManyToOne` para `Projeto` e `Usuario`, e campos de prazo e status. | ✅ Concluído |
| **TSK-03** | Implementar enum `StatusTarefa` com os valores: `PENDENTE`, `EM_ANDAMENTO`, `CONCLUIDA`, `CANCELADA`. | ✅ Concluído |
| **TSK-04** | Implementar `TarefaRepository` com operações CRUD e filtros por projeto, por responsável e por status. | ✅ Concluído |
| **TSK-05** | Adicionar método `buscarPorEmail()` em `UsuarioRepository`. | ✅ Concluído |
| **TSK-06** | Implementar `TarefaController` com máquina de estados de status e regras de negócio (prazo, responsável, vínculo com projeto). | ✅ Concluído |
| **TSK-07** | Implementar hash BCrypt de senhas em `UsuarioController`: geração no cadastro, verificação na autenticação e re-hash na atualização de senha. | ✅ Concluído |
| **TSK-08** | Ajustar `ProjetoController`: ao cancelar um projeto, propagar cancelamento em cascata para todas as tarefas vinculadas com status não-terminal. | ✅ Concluído |
| **TSK-09** | Ajustar `UsuarioController`: bloquear remoção de usuário que possua tarefas ativas (`PENDENTE` ou `EM_ANDAMENTO`) atribuídas. | ✅ Concluído |

---

## Ferramentas Adotadas na Sprint

- **ORM / Persistência:** Hibernate 6.4.4 (JPA 3.0)
- **Controle de Schema:** Flyway 10.10.0
- **Segurança de Senhas:** jbcrypt 0.4 (BCrypt)
- **Banco de Dados:** PostgreSQL 16 (Alpine via Docker)
- **Editor de Texto:** VS Code / IntelliJ IDEA
- **Controle de Versões:** Git (commits semânticos por camada)
