# Sprint Backlog - Sprint 9

**Período Inicial/Final:** 21/05/2026 a 24/05/2026  
**Responsável do Projeto:** Vinícius Baggio  
**Objetivo da Sprint:** Implementar controle de acesso por papel efetivo em cada projeto — calculado dinamicamente como `ADMIN > gerente_id do projeto > membro da equipe > sem acesso` — com guards nos controllers e restrições visuais nas Views por perfil. A sprint também corrigiu o botão Cancelar da tela de Gestão de Projeto e introduziu cargos pré-cadastrados nas migrations.

---

## Fase 0 — Extras de Melhoria (Pré-Sprint)

Itens identificados antes da execução do plano principal, implementados como parte da branch.

| ID | Descrição da Tarefa | Status |
|----|----------------------|--------|
| **TSK-00A** | `GestaoProjetoPanel.cancelar()` — substituir comportamento de "descartar e recarregar" por `SwingUtilities.getWindowAncestor(this).dispose()`, fechando efetivamente a janela. O `windowClosed` do `ProjetoPanel` cuida da limpeza do mapa e do reload da lista. | ✅ Concluído |
| **TSK-00B** | Migration `V3` (refatorada) — inserir 4 cargos pré-cadastrados com UUIDs fixos: `Sysadmin`, `Coordenador de Projetos`, `Desenvolvedor`, `Líder Técnico`. Admin passa a ter UUID fixo (`00000000-0000-0000-0000-000000000001`) e `cargo_id = Sysadmin`. | ✅ Concluído |
| **TSK-00C** | Migration `V4` (refatorada) — associar `Gerente Padrão` ao cargo `Coordenador de Projetos` e `Usuário Padrão` ao cargo `Desenvolvedor`. Simplificar referência ao admin usando UUID fixo (eliminar `SELECT id FROM usuario WHERE login = 'admin'`). | ✅ Concluído |

---

## Fase 1 — Infraestrutura: Enum e Migration

| ID | Descrição da Tarefa | Status |
|----|----------------------|--------|
| **TSK-01** | Criar `RoleNoProjeto.java` com valores `ADMIN`, `GERENTE_EFETIVO`, `COLABORADOR`, `SEM_ACESSO`. Enum de uso interno — calcula o papel efetivo do usuário em um projeto específico, independentemente do perfil global. | ✅ Concluído |
| **TSK-02** | Migration `V7__add_index_equipe_membro_usuario.sql` — `CREATE INDEX IF NOT EXISTS idx_equipe_membro_usuario ON equipe_membro(usuario_id)`. Índice necessário para a query `listarPorMembroOuGerente` que filtra projetos visíveis por usuário. | ✅ Concluído |

---

## Fase 2 — Repositories

| ID | Descrição da Tarefa | Status |
|----|----------------------|--------|
| **TSK-03** | `ProjetoRepository.listarPorMembroOuGerente(UUID usuarioId)` — JPQL com `DISTINCT`, `LEFT JOIN FETCH p.gerente`, `LEFT JOIN FETCH p.equipe eq`, `LEFT JOIN eq.membros m`, `WHERE (p.gerente IS NOT NULL AND p.gerente.id = :usuarioId) OR m.id = :usuarioId ORDER BY p.nome`. Retorna projetos onde o usuário é gerente_id ou membro da equipe. | ✅ Concluído |
| **TSK-04** | `ProjetoRepository.existeProjetoAtivoComGerente(UUID gerenteId)` — conta projetos com status `PLANEJADO` ou `EM_ANDAMENTO` gerenciados pelo usuário. Usado pelo guard de remoção de usuário. | ✅ Concluído |
| **TSK-05** | `UsuarioRepository.contarPorPerfil(Perfil perfil)` — `SELECT COUNT(u) FROM Usuario u WHERE u.perfil = :perfil`. Usado pelo guard de remoção do último ADMINISTRADOR. | ✅ Concluído |

---

## Fase 3 — Controllers (Guards de Autorização)

| ID | Descrição da Tarefa | Status |
|----|----------------------|--------|
| **TSK-06** | `UsuarioController.atualizarUsuario()` — adicionar parâmetro `Usuario caller`. Guards: (1) não-ADMIN não pode editar dados de outro usuário; (2) não-ADMIN não pode alterar o campo `perfil`. | ✅ Concluído |
| **TSK-07** | `UsuarioController.removerUsuario()` — adicionar parâmetro `Usuario caller`. Guards: (1) ADMIN-only; (2) não pode remover o único ADMINISTRADOR do sistema; (3) não pode remover gerente com projetos ativos; (4) não pode remover usuário com tarefas ativas (pré-existente, preservado). | ✅ Concluído |
| **TSK-08** | `ProjetoController.listarProjetosVisiveis(Usuario usuario)` — ADMIN retorna `listarTodos()`; demais retornam `listarPorMembroOuGerente(usuario.getId())`. Método público principal para listagem filtrada. | ✅ Concluído |
| **TSK-09** | `ProjetoController.removerProjeto()` — adicionar parâmetro `caller`. Guard: ADMIN-only. | ✅ Concluído |
| **TSK-10** | `ProjetoController.atualizarProjeto()` — adicionar parâmetro `caller`. Guards: (1) ADMIN ou gerente efetivo podem editar; (2) apenas ADMIN pode trocar o `gerente_id`. Helper privado `isGerenteEfetivo(Projeto, Usuario)` centraliza a verificação. | ✅ Concluído |
| **TSK-11** | `ProjetoController.atualizarStatus()` e `encerrarProjeto()` — adicionar parâmetro `caller`. Guard: ADMIN ou gerente efetivo. | ✅ Concluído |
| **TSK-12** | `TarefaController.criarTarefa()` — adicionar parâmetro `caller`. Guard: não-ADMIN/não-gerente-efetivo tem `responsavelId` forçado para `caller.getId()` (self-assignment). Gerente efetivo: responsável deve ser membro da equipe. | ✅ Concluído |
| **TSK-13** | `TarefaController.reatribuirResponsavel()` — adicionar parâmetro `caller`. Guards: (1) ADMIN ou gerente efetivo; (2) novo responsável deve ser membro da equipe. Lazy-load tratado via `projetoRepo.buscarPorId(tarefa.getProjeto().getId())`. | ✅ Concluído |
| **TSK-14** | `RelatorioController.listarProjetosParaRelatorio(Usuario usuario)` — ADMIN retorna todos; demais retornam `listarPorGerente(usuario.getId())`. Usado pelo `RelatorioPanel` para filtrar o combo de projetos. | ✅ Concluído |

---

## Fase 4 — Views (Propagação de Usuario e Constraints)

| ID | Descrição da Tarefa | Status |
|----|----------------------|--------|
| **TSK-15** | `HomePanel.criarGrade()` — substituir `XxxPanel::new` por lambdas `() -> new XxxPanel(usuario)`. Filtrar cards por perfil: COLABORADOR vê apenas `Projetos`; GERENTE e ADMIN veem todos os 5 cards. | ✅ Concluído |
| **TSK-16** | `CargoPanel` — adicionar `private final Usuario usuario`, construtor `CargoPanel(Usuario)`, desabilitar botões `btnNovo`, `btnEditar`, `btnExcluir` para não-ADMIN. | ✅ Concluído |
| **TSK-17** | `UsuarioPanel` — mesmo padrão de `CargoPanel` (campo `usuarioLogado` para evitar shadowing). Atualizar chamadas `ctrl.atualizarUsuario(...)` e `ctrl.removerUsuario(...)` para incluir `usuarioLogado` como último argumento. | ✅ Concluído |
| **TSK-18** | `EquipePanel` — mesmo padrão de `CargoPanel`. Promover 5 botões de variáveis locais a campos de instância; desabilitar todos para não-ADMIN. | ✅ Concluído |
| **TSK-19** | `RelatorioPanel` — adicionar `private final Usuario usuario`, construtor `RelatorioPanel(Usuario)`. Atualizar `popularComboProjetos()` para chamar `ctrl.listarProjetosParaRelatorio(usuario)` em vez do método anterior. | ✅ Concluído |
| **TSK-20** | `ProjetoPanel` — adicionar `private final Usuario usuario`, construtor `ProjetoPanel(Usuario)`. Atualizar `carregar()` para `ctrl.listarProjetosVisiveis(usuario)`. Exibir mensagem informativa para não-ADMIN com lista vazia. Desabilitar `btnNovo` para COLABORADOR, `btnExcluir` para não-ADMIN (incluindo guard no `ListSelectionListener`). Atualizar `abrirGestao()` com `new GestaoProjetoPanel(id, usuario, ...)` e `excluir()` com `ctrl.removerProjeto(id, usuario)`. | ✅ Concluído |
| **TSK-21** | `GestaoProjetoPanel` — adicionar `private final Usuario usuario` e `private RoleNoProjeto roleNoProjeto`. Construtor atualizado para `(UUID, Usuario, Runnable)`. Métodos privados `calcularRole(Projeto, List<Usuario>)` e `aplicarConstraintesDeRole()` calculam e aplicam constraints de campos e combos. Chamar ambos no `done()` de `carregarProjeto()`. `isCellEditable` atualizado para checar `roleNoProjeto` e, para COLABORADOR, verificar `usuario.getId() == respId` (coluna 5). `montarComboResponsavel()` retorna combo desabilitado com self-only para não-ADMIN/não-gerente. `btnEditar` habilitado para ADMIN/GERENTE_EFETIVO em qualquer tarefa e para COLABORADOR apenas nas próprias tarefas (via `podeEditarTarefa(viewRow)`); `btnExcluir` restrito a ADMIN/GERENTE_EFETIVO. Todas as 5 chamadas de controller em `salvarTudo()` atualizadas com `, usuario`. | ✅ Concluído |

---

## Fase 5 — Correções Pós-Sprint (24/05/2026)

Bugs identificados em validação manual com perfil COLABORADOR após o merge.

| ID | Descrição da Tarefa | Status |
|----|----------------------|--------|
| **FIX-01** | `GestaoProjetoPanel.carregarProjeto()` — `LazyInitializationException` em `Equipe.membros` ao abrir a tela como COLABORADOR. `calcularRole()` acessava `projeto.getEquipe().getMembros()` no `done()` do SwingWorker (sessão JPA fechada). Correção: carregar membros via `equipeCtrl.listarMembros()` no `doInBackground()` e passar como parâmetro a `calcularRole(Projeto, List<Usuario>)`. | ✅ Concluído |
| **FIX-02** | `GestaoProjetoPanel.salvarTudo()` — `IllegalStateException: Apenas ADMINISTRADOR ou o gerente do projeto podem editar` ao salvar tarefas como COLABORADOR. `doInBackground()` sempre chamava `atualizarProjeto()` independente do role. Correção: envolver a atualização do projeto em `if (podeEditarProjeto)`. | ✅ Concluído |
| **FIX-03** | `GestaoProjetoPanel.aplicarConstraintesDeRole()` — campos Nome e Descrição exibiam `setEditable(false)` (fundo branco, aparência habilitada) enquanto os demais campos usavam `setEnabled(false)` (aparência desabilitada). Padronizado para `setEnabled`. | ✅ Concluído |
| **FIX-04** | `GestaoProjetoPanel` — tecla Delete excluía tarefas para COLABORADOR mesmo com o botão Excluir desabilitado. O `ActionMap` não verificava role. Correção: guard de role adicionado no action. Double-click também abria edição de qualquer tarefa para COLABORADOR — corrigido com `podeEditarTarefa(viewRow)` no `MouseListener`. | ✅ Concluído |

---

## Ferramentas e Componentes Adotados na Sprint

- **Interface Gráfica:** Java Swing (javax.swing)
- **Look & Feel:** FlatLaf 3.4.1 (`FlatDarkLaf`) — herdado das sprints anteriores
- **Persistência:** JPA / Hibernate — JPQL com `DISTINCT`, `LEFT JOIN FETCH`, `COUNT`
- **Versionamento de Banco:** Flyway — migration V7 + refatoração de V3 e V4
- **Padrão assíncrono:** `SwingWorker<T, Void>` — constraints aplicadas no `done()` para evitar acesso à UI fora da EDT
- **Build:** Apache Maven 3.x
- **Padrão de autorização:** Papel efetivo por projeto (`RoleNoProjeto`), não por perfil global
