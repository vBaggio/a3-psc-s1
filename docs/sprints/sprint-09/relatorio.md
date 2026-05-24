# Relatório Semanal de Desenvolvimento - Sprint 9

**Responsável Principal:** Vinícius Baggio  
**Período Avaliado:** 21/05/2026 a 24/05/2026  
**Status do Ciclo:** Concluído — Pós-sprint estendida  

---

## 1. Resumo Executivo das Avaliações

A Sprint 9 implementou controle de acesso por papel efetivo em cada projeto. O modelo anterior não possuía nenhuma restrição de perfil na UI nem nos controllers — qualquer usuário autenticado podia visualizar e modificar qualquer dado. A solução adotada calcula o papel do usuário dinamicamente por projeto (`ADMIN > GERENTE_EFETIVO > COLABORADOR > SEM_ACESSO`), em vez de usar apenas o perfil global, o que permite que um GERENTE tenha papel diferente em projetos distintos dependendo de ser ou não o `gerente_id`.

A **Fase 0** reuniu dois extras aplicados antes do plano principal: o botão Cancelar da `GestaoProjetoPanel` passou a fechar a janela (comportamento esperado pelo usuário), e as migrations V3/V4 foram refatoradas para incluir cargos pré-cadastrados (`Sysadmin`, `Coordenador de Projetos`, `Desenvolvedor`, `Líder Técnico`) com associação automática aos usuários padrão do sistema. O admin recebeu UUID fixo, eliminando o `gen_random_uuid()` que dificultava referências em migrations subsequentes.

A **Fase 1** criou a infraestrutura base: o enum `RoleNoProjeto` (uso interno, nunca exibido na UI) e a migration V7 com índice de performance em `equipe_membro(usuario_id)` — necessário para a query de visibilidade que cruza membros por usuário.

A **Fase 2** adicionou três queries: `listarPorMembroOuGerente` (projetos visíveis para não-ADMIN), `existeProjetoAtivoComGerente` (guard de remoção de usuário) e `contarPorPerfil` (guard de remoção do último ADMINISTRADOR). A query de visibilidade exigiu adaptação do JPQL em relação ao plano, pois `Projeto.equipe` é `@ManyToOne` (não uma coleção), tornando o `LEFT JOIN FETCH p.equipe eq` e o `LEFT JOIN eq.membros m` a forma correta de navegar a associação.

A **Fase 3** adicionou 6 guards nos controllers com parâmetro `caller`. `UsuarioController` ganhou proteção contra escalação de privilégio (não-ADMIN alterando perfil de outro usuário) e contra remoção do último ADMINISTRADOR ou de gerentes com projetos ativos. `ProjetoController` ganhou `listarProjetosVisiveis` e guards em 4 métodos de mutação. `TarefaController` ganhou self-assignment forçado para não-ADMIN/não-gerente-efetivo em `criarTarefa` e guard de papel efetivo em `reatribuirResponsavel`. `RelatorioController` ganhou `listarProjetosParaRelatorio` para filtrar o combo de relatórios por perfil.

A **Fase 4** propagou o `Usuario` até todas as Views via lambda capture em `HomePanel` (substituindo os `XxxPanel::new` que não repassavam o usuário). `CargoPanel`, `UsuarioPanel` e `EquipePanel` receberam construtor `(Usuario)` e desabilitam todos os botões de ação para não-ADMIN. `RelatorioPanel` e `ProjetoPanel` receberam construtor `(Usuario)` com filtro de dados por perfil. `GestaoProjetoPanel` recebeu a implementação mais complexa: `calcularRole(Projeto)`, `aplicarConstraintesDeRole()`, `isCellEditable` restrito por role e coluna de responsável, e `montarComboResponsavel()` com self-only para COLABORADOR.

---

## 2. Diário Histórico de Execuções e Decisões Técnicas

| Data de Registro | Evento Executado / Veredito Arquitetural |
|------------------|------------------------------------------|
| **21/05/2026** | **Extra — botão Cancelar fecha janela:** `GestaoProjetoPanel.cancelar()` substituído por `SwingUtilities.getWindowAncestor(this).dispose()`. O `windowClosed` do `ProjetoPanel` já tratava limpeza do mapa (`janelasGestao.remove(fId)`) e reload da lista — nenhuma mudança necessária em `ProjetoPanel`. |
| **21/05/2026** | **Extra — cargos pré-cadastrados (V3/V4 refatoradas):** V3 inseriu 4 cargos com UUIDs fixos e deu ao admin UUID fixo `00000000-0000-0000-0000-000000000001`. V4 associou os cargos ao gerente (`Coordenador de Projetos`) e ao colaborador (`Desenvolvedor`). Decisão de usar UUIDs fixos: elimina `gen_random_uuid()` em seeds, tornando as migrations determinísticas e sem dependência de `SELECT` para referenciar o admin em V4. |
| **21/05/2026** | **`RoleNoProjeto` enum:** 4 valores (`ADMIN`, `GERENTE_EFETIVO`, `COLABORADOR`, `SEM_ACESSO`). Decisão de não implementar `toString()`: o enum é exclusivamente interno (nunca exibido na UI), diferentemente de `Perfil` e `StatusProjeto` que são renderizados em tabelas e combos. |
| **21/05/2026** | **Migration V7 — índice `equipe_membro(usuario_id)`:** Sem este índice, a query `listarPorMembroOuGerente` exigiria seq scan na tabela de membros a cada listagem de projetos para não-ADMIN. |
| **21/05/2026** | **JPQL adaptado em `listarPorMembroOuGerente`:** O plano original tratava `p.equipe` como coleção (`LEFT JOIN FETCH p.equipe e1 / LEFT JOIN p.equipe e2`). A entidade real possui `@ManyToOne Equipe equipe`. Adaptado para `LEFT JOIN FETCH p.equipe eq` (eager load da equipe para evitar N+1) + `LEFT JOIN eq.membros m` (join plain para o predicado WHERE). Adicionado `p.gerente IS NOT NULL AND` antes de `p.gerente.id = :usuarioId` para null-safety — projetos sem gerente não devem causar erro de navegação nula no JPQL. |
| **21/05/2026** | **`UsuarioController` — privilege escalation (achado crítico do council):** Guard em `atualizarUsuario` impede que não-ADMIN altere o campo `perfil` de outro usuário, fechando a escalação de privilégio identificada antes da implementação. Guard em `removerUsuario` bloqueia remoção do último ADMINISTRADOR e de gerentes com projetos ativos. |
| **21/05/2026** | **`ProjetoController` — helper `isGerenteEfetivo(Projeto, Usuario)`:** Método privado centraliza a verificação `projeto.getGerente() != null && gerente.getId().equals(caller.getId())`, reutilizado nos 3 guards de mutação (`atualizarProjeto`, `atualizarStatus`, `encerrarProjeto`). |
| **21/05/2026** | **`TarefaController.criarTarefa` — self-assignment:** Para COLABORADOR e GERENTE que é membro mas não gerente do projeto, `responsavelId` é sobrescrito com `caller.getId()` antes da criação. Gerente efetivo pode atribuir a qualquer membro da equipe. ADMIN sem restrição. |
| **21/05/2026** | **`TarefaController.reatribuirResponsavel` — lazy-load:** `tarefa.getProjeto()` é `FetchType.LAZY`. Chamá-lo com EntityManager fechado lança `LazyInitializationException`. Solução: `projetoRepo.buscarPorId(tarefa.getProjeto().getId())` — o `.getId()` no proxy é seguro (campo identificador), e a segunda query carrega o projeto com todos os dados necessários para o guard. |
| **21/05/2026** | **`HomePanel` — lambda capture:** `XxxPanel::new` não passava `usuario`. Substituídos por `() -> new XxxPanel(usuario)` que captura a variável do escopo envolvente. A interface `Supplier<JPanel>` já estava no `card()` — a mudança foi cirúrgica. |
| **21/05/2026** | **`EquipePanel` — botões locais promovidos a campos:** Os métodos `criarPainelEquipes()` e `criarPainelMembros()` declaravam 5 botões como variáveis locais. Para aplicar o guard de perfil no construtor, todos foram promovidos a campos de instância e desabilitados após a criação. |
| **21/05/2026** | **`ProjetoPanel` — guard no `ListSelectionListener`:** O listener que reabilitava `btnExcluir` ao selecionar uma linha foi atualizado para checar `usuario.getPerfil() == Perfil.ADMINISTRADOR` antes de reabilitar. Sem esse patch, selecionar uma linha contornaria o guard inicial do construtor. |
| **21/05/2026** | **`GestaoProjetoPanel` — constraints no `done()` do SwingWorker:** `aplicarConstraintesDeRole()` é chamado após popular todos os campos do formulário, garantindo que a desabilitação não seja sobrescrita pela população do form. Chamar no construtor (antes do carregamento) seria ineficaz pois `roleNoProjeto` ainda seria `null`. |
| **21/05/2026** | **`GestaoProjetoPanel.isCellEditable` — coluna de responsável:** O guard para COLABORADOR lê a coluna 5 (`RespID`, coluna oculta) para comparar com `usuario.getId()`. A coluna está oculta na UI (`ocultarColuna(5)`) mas presente no modelo — acessível via `getValueAt(r, 5)`. |
| **22/05/2026** | **Merge para master via fast-forward:** 15 commits integrados. BUILD SUCCESS verificado no master após o merge. |
| **24/05/2026** | **FIX-01 — `LazyInitializationException` em `Equipe.membros`:** `calcularRole()` acessava `projeto.getEquipe().getMembros()` no `done()` do SwingWorker, após o fechamento da sessão JPA. Corrigido carregando os membros via `equipeCtrl.listarMembros()` dentro do `doInBackground()` e passando o resultado como parâmetro a `calcularRole(Projeto, List<Usuario>)`. |
| **24/05/2026** | **FIX-02 — `IllegalStateException` ao salvar como COLABORADOR:** `salvarTudo()` sempre chamava `atualizarProjeto()`, que possui guard de autorização no controller. COLABORADOR não tem permissão para editar dados do projeto, apenas tarefas. Corrigido com `if (podeEditarProjeto)` envolvendo o bloco de atualização do projeto. |
| **24/05/2026** | **FIX-03 — aparência inconsistente de campos read-only:** Nome e Descrição usavam `setEditable(false)` (fundo branco, cursor ativo) enquanto os demais campos usavam `setEnabled(false)` (aparência desabilitada). Padronizado para `setEnabled`. |
| **24/05/2026** | **FIX-04 — tecla Delete e double-click sem guard de role:** O `ActionMap` da tecla Delete executava `excluirTarefa()` sem verificar o role, contornando o `btnExcluir` desabilitado. O `MouseListener` de double-click abria edição de qualquer tarefa mesmo para COLABORADOR. Ambos corrigidos: Delete com guard de role; double-click com `podeEditarTarefa(viewRow)`, que retorna `true` para ADMIN/GERENTE ou quando a tarefa pertence ao próprio COLABORADOR. Aproveitou-se a mesma lógica para corrigir o `ListSelectionListener` do `btnEditar`, que agora habilita o botão para COLABORADOR quando a tarefa selecionada é sua. |
| **24/05/2026** | **FIX-05 — `reatribuirResponsavel` chamado incondicionalmente:** COLABORADOR alterando apenas o status da própria tarefa recebia `IllegalStateException` de permissão ao salvar. `salvarTudo()` chamava `reatribuirResponsavel()` para toda tarefa em `tarefasEditadas` sem verificar se o responsável havia mudado. Correção: guard `Objects.equals(respAtual, d.responsavelId())` — o método só é chamado quando o responsável efetivamente muda. |
| **24/05/2026** | **FIX-06 — persistência parcial em caso de erro no save:** Antes do fix, cada operação de `salvarTudo()` commitava em sua própria transação. Uma falha a meio do processo (ex.: permissão negada após `atualizarTarefa` já ter persistido) deixava dados inconsistentes no banco. Solução: injeção de `EntityManager` opcional nos métodos de escrita de `TarefaRepository`, `ProjetoRepository`, `TarefaController` e `ProjetoController`. O método sem parâmetro cria o próprio EM (comportamento original preservado); com EM externo, delega transação ao chamador. `salvarTudo()` agora cria um único EM, envolve toda a operação em `begin/commit/rollback`, garantindo atomicidade. |
| **24/05/2026** | **FIX-07 — exclusão de tarefa não persistia com EM compartilhado:** Após o FIX-06, delete de tarefa via ADMIN não era gravado no banco. Causa raiz: `ProjetoRepository.atualizar(Projeto, EntityManager)` usava `em.merge(projeto)`, que ativava o `cascade = CascadeType.ALL, orphanRemoval = true` da collection `Projeto.tarefas`. O Hibernate inicializava a collection durante o merge e, ao encontrar a tarefa excluída ainda presente nela, conflitava com o `em.remove(tarefa)` posterior na mesma transação. Correção: com EM externo, substitui `em.merge()` por `em.find(Projeto)` + atribuição direta dos campos escalares (`nome`, `descricao`, `dataInicio`, `dataPrevisao`, `dataFim`, `status`, `gerente`, `equipe`) via `em.getReference()` para as associações. A collection `tarefas` nunca é tocada, eliminando o conflito com o delete. |
| **24/05/2026** | **FIX-08 — GERENTE visualizava cadastros (Cargos, Usuários, Equipes):** `HomePanel` usava `perfil != COLABORADOR` para os três cards de cadastro, exibindo-os para GERENTE. A spec prevê que GERENTE acesse apenas Projetos (com role calculado por projeto) e Relatórios. Correção: condição alterada para `perfil == ADMINISTRADOR` nos três cards de cadastro; card de Relatórios mantém `perfil != COLABORADOR`. |
| **24/05/2026** | **FIX-09 — `Tarefa não encontrada` ao criar tarefa com status != PENDENTE:** `salvarTudo` chamava `criarTarefa` e em seguida `atualizarStatus` para a nova tarefa na mesma transação. `atualizarStatus` invocava `buscarTarefaOuFalhar` com EM próprio, que não enxerga entidades ainda não flushed no EM compartilhado. Correção: `buscarTarefaOuFalhar` recebe `EntityManager` opcional; com EM externo usa `em.find()`, que consulta o persistence context local da transação corrente. Todos os métodos com EM externo no controller passam o EM para o helper. |

---

## 3. Registros de Desafios Enfrentados

O principal desafio foi o design do modelo de permissão. A solução mais simples seria usar apenas `usuario.getPerfil()` para verificar ADMIN/GERENTE/COLABORADOR, mas isso quebraria o caso em que um GERENTE é membro de equipe em um projeto que não gerencia — ele deveria ter papel de COLABORADOR nesse projeto. A decisão de calcular o papel efetivo por projeto (`calcularRole(Projeto)`) exigiu que `GestaoProjetoPanel` carregasse o projeto antes de aplicar constraints, e que as constraints fossem aplicadas no `done()` do `SwingWorker` (após o carregamento), não no construtor.

O segundo desafio foi a propagação do `Usuario` pelas Views. O `HomePanel` já recebia o usuário no construtor, mas usava `XxxPanel::new` (method reference sem argumentos) para instanciar os painéis filhos. A mudança para `() -> new XxxPanel(usuario)` foi simples, mas exigiu que todos os 5 painéis filhos fossem atualizados para aceitar o `Usuario` no construtor — o que criou um período de compilação quebrada intencional (Tasks 8–12) até que todos os painéis fossem atualizados.

O terceiro desafio foi o `LazyInitializationException` latente em `reatribuirResponsavel`. O método recebe um `UUID tarefaId` e precisa do `Projeto` da tarefa para verificar se o `caller` é o gerente efetivo e se o novo responsável é membro da equipe. `tarefa.getProjeto()` retorna um proxy Hibernate cujo EntityManager já foi fechado. A solução foi reutilizar o `ProjetoRepository` (já disponível como campo do controller) para recarregar o projeto pelo ID do proxy — que é sempre seguro de acessar mesmo em proxy detached.

---

## 4. Artefatos Entregues

| Artefato | Caminho | Operação |
|----------|---------|----------|
| Enum `RoleNoProjeto` | `src/main/java/.../model/enums/RoleNoProjeto.java` | Criado |
| Migration V3 (refatorada) — cargos + admin UUID fixo | `src/main/resources/db/migration/V3__Seed_admin.sql` | Modificado |
| Migration V4 (refatorada) — associação de cargos | `src/main/resources/db/migration/V4__Seed_usuarios_e_equipe.sql` | Modificado |
| Migration V7 — índice `equipe_membro(usuario_id)` | `src/main/resources/db/migration/V7__add_index_equipe_membro_usuario.sql` | Criado |
| `ProjetoRepository` — `listarPorMembroOuGerente`, `existeProjetoAtivoComGerente` | `src/main/java/.../repository/ProjetoRepository.java` | Modificado |
| `UsuarioRepository` — `contarPorPerfil` | `src/main/java/.../repository/UsuarioRepository.java` | Modificado |
| `UsuarioController` — guards `atualizarUsuario` + `removerUsuario` | `src/main/java/.../controller/UsuarioController.java` | Modificado |
| `ProjetoController` — `listarProjetosVisiveis` + guards em 4 métodos | `src/main/java/.../controller/ProjetoController.java` | Modificado |
| `TarefaController` — guards `criarTarefa` + `reatribuirResponsavel` | `src/main/java/.../controller/TarefaController.java` | Modificado |
| `RelatorioController` — `listarProjetosParaRelatorio` | `src/main/java/.../controller/RelatorioController.java` | Modificado |
| `HomePanel` — lambda capture + filtro de cards por perfil | `src/main/java/.../view/HomePanel.java` | Modificado |
| `CargoPanel` — construtor `(Usuario)` + readonly GERENTE | `src/main/java/.../view/CargoPanel.java` | Modificado |
| `UsuarioPanel` — construtor `(Usuario)` + readonly GERENTE + chamadas caller | `src/main/java/.../view/UsuarioPanel.java` | Modificado |
| `EquipePanel` — construtor `(Usuario)` + readonly GERENTE | `src/main/java/.../view/EquipePanel.java` | Modificado |
| `RelatorioPanel` — construtor `(Usuario)` + combo filtrado | `src/main/java/.../view/RelatorioPanel.java` | Modificado |
| `ProjetoPanel` — construtor `(Usuario)` + lista filtrada + guards UI | `src/main/java/.../view/ProjetoPanel.java` | Modificado |
| `GestaoProjetoPanel` — `RoleNoProjeto`, constraints, `isCellEditable`, combo restrito, cancelar fecha janela | `src/main/java/.../view/GestaoProjetoPanel.java` | Modificado |
| `TarefaRepository` — sobrecargas `salvar/atualizar/deletar/cancelarPorProjeto` com `EntityManager` opcional | `src/main/java/.../repository/TarefaRepository.java` | Modificado |
| `ProjetoRepository` — sobrecarga `atualizar` com `EntityManager` opcional; find+set direto para EM externo | `src/main/java/.../repository/ProjetoRepository.java` | Modificado |
| `TarefaController` — sobrecargas com `EntityManager` em `criarTarefa`, `atualizarTarefa`, `atualizarStatus`, `reatribuirResponsavel`, `removerTarefa` | `src/main/java/.../controller/TarefaController.java` | Modificado |
| `ProjetoController` — sobrecargas com `EntityManager` em `atualizarProjeto`, `atualizarStatus`, `encerrarProjeto` | `src/main/java/.../controller/ProjetoController.java` | Modificado |

---

## 5. Métricas da Sprint

| Categoria | Antes | Depois |
|-----------|-------|--------|
| Controle de acesso na UI | Nenhum (qualquer perfil vê tudo) | Por papel efetivo por projeto |
| Controle de acesso nos controllers | Nenhum | 6 métodos com guard `caller` |
| Cargos pré-cadastrados | 0 | 4 (Sysadmin, Coordenador de Projetos, Desenvolvedor, Líder Técnico) |
| Cards visíveis para COLABORADOR | 5 | 1 (apenas Projetos) |
| Cards visíveis para GERENTE | 5 | 2 (Projetos e Relatórios) |
| Escalação de privilégio (alteração de perfil) | Possível | Bloqueada — guard em `atualizarUsuario` |
| Migrations | V1–V6 | V1–V7 (V3/V4 refatoradas, V7 criada) |
| Atomicidade do save | Nenhuma (cada operação em transação própria) | Transação única por `salvarTudo()` — rollback total em caso de erro |
| Commits da sprint | — | 16 |
