# Relatório Semanal de Desenvolvimento - Sprint 7

**Responsável Principal:** Vinícius Baggio  
**Período Avaliado:** 03/05/2026 a 07/05/2026  
**Status do Ciclo:** Concluído  

---

## 1. Resumo Executivo das Avaliações

A Sprint 7 foi dividida em quatro fases com objetivos distintos. A **Fase 1** entregou um conjunto de melhorias transversais de qualidade de UI: carregamento assíncrono via `SwingWorker` em todos os painéis, double-click para edição, desabilitação contextual de botões, formatação legível de enums e datas, empty state em tabelas, ordenação por coluna e atalhos de teclado. A **Fase 2** surgiu de uma análise UX que identificou problemas estruturais no módulo de projetos e tarefas — redundância de botões, ausência de coesão entre `Projeto` e `Tarefa`, e `TarefaPanel` como módulo autônomo de baixo valor — e resultou no redesign completo do módulo.

O ponto central do redesign foi a criação do `GestaoProjetoPanel`: uma janela que unifica o formulário de dados do projeto com a gestão das suas tarefas em um único contexto, eliminando quatro telas de diálogo separadas e dois botões redundantes. Paralelamente, `TarefaPanel` foi removido como módulo autônomo, a grade do `HomePanel` foi reorganizada para `3 + 2` e o botão `Sair` foi relocado do cabeçalho para o rodapé ao lado do nome do usuário logado.

Commits que conflitavam com as novas decisões de design foram removidos do histórico via **rebase local** antes do início da implementação, mantendo o histórico coeso e sem revelar decisões de design descartadas nos relatórios finais.

A **Fase 3** tornou `GestaoProjetoPanel` atômico: projeto e tarefas passaram a ser persistidos juntos em um único `Salvar`, com staging em memória via três coleções (`tarefasNovas`, `tarefasEditadas`, `tarefasExcluidas`) e aviso ao fechar janela com alterações pendentes.

A **Fase 4** aplicou correções cirúrgicas de qualidade identificadas após a entrega: remoção das restrições de transição de status e de exclusão por status de tarefa, combos de status listando todos os valores, `stopCellEditing` automático antes de salvar, fechamento automático da janela de gestão após salvar. Adicionalmente, a migration `V4` criou dados padrão de seed — três usuários (um por perfil) e uma equipe com todos como membros — documentados no README.

---

## 2. Diário Histórico de Execuções e Decisões Técnicas

| Data de Registro | Evento Executado / Veredito Arquitetural |
|------------------|------------------------------------------|
| **03/05/2026** | **SwingWorker em todos os painéis:** Todos os métodos `carregar()` dos painéis CRUD (Cargo, Usuário, Equipe, Projeto, Tarefa) foram refatorados para executar as queries JPA em `doInBackground()` e popular o `DefaultTableModel` em `done()`. O login também foi convertido para autenticação assíncrona: o botão é desabilitado durante a tentativa, evitando cliques duplos, e o resultado é exibido na EDT. Esse padrão previne congelamento da interface em cenários com latência de rede real. |
| **03/05/2026** | **Double-click para edição:** `MouseAdapter` adicionado a todas as `JTable` para detectar `clickCount == 2` na linha selecionada e invocar o método de edição correspondente — eliminando a necessidade de mover o cursor até o botão após selecionar o registro. |
| **03/05/2026** | **Desabilitação contextual de botões:** `ListSelectionListener` adicionado a todas as tabelas para controlar o estado `enabled` dos botões de ação. Botões como `Editar` e `Encerrar` também avaliam o `StatusProjeto`/`StatusTarefa` da linha selecionada via `modelo.getValueAt()` — um projeto `CONCLUIDO` não habilita `Editar`, por exemplo. |
| **04/05/2026** | **`OpcaoItem` e `DateUtils` como utilitários reutilizáveis:** Antes desses utilitários, cada painel declarava seus próprios arrays paralelos de UUIDs e nomes para popular `JComboBox`, e duplicava a lógica de parse/format de datas. `OpcaoItem` (record com `UUID id` e `String label`) centralizou o padrão de combo; `DateUtils` centralizou a máscara `dd/MM/yyyy`, o parse com `DateTimeFormatter` e a criação de `JFormattedTextField` com `MaskFormatter`. |
| **04/05/2026** | **`TableUtils.tabelaComMensagem()` — empty state:** Tabelas vazias exibiam silêncio — sem nenhuma indicação para o usuário de que o módulo funcionava mas não havia dados. A solução usa um `JTable` estendido que sobrescreve `paintComponent()` para centralizar uma mensagem quando `getRowCount() == 0`, sem precisar de um componente extra na hierarquia. |
| **05/05/2026** | **[DECISÃO DE DESIGN] Análise UX — identificação de problemas estruturais:** `ProjetoPanel` tinha 5 botões de ação com sobreposição de responsabilidades: `Editar` (dados básicos), `Alterar Status` (só status), `Encerrar` (status especial com data) e `Tarefas` (navegar para entidade filha). `TarefaPanel` tinha `Editar`, `Alterar Status` e `Reatribuir` como 3 entradas separadas para o mesmo registro. A navegação cross-projeto de `TarefaPanel` foi avaliada como de menor valor que o gerenciamento contextual — relatórios já cobrem a visão agregada. Decisão: criar `GestaoProjetoPanel` como ponto único de entrada para gestão de projeto e suas tarefas. |
| **05/05/2026** | **[DECISÃO DE CONTROLE DE VERSÃO] Rebase local de commits conflitantes:** Três commits da Sprint 7 conflitavam diretamente com as novas decisões de design (botão `Tarefas` no toolbar de `ProjetoPanel`, confirmação antes de encerrar projeto como etapa separada, e referências a `projetoFixo` em `TarefaPanel`). Como nenhum desses commits havia sido publicado no repositório remoto, foram removidos via `git rebase` com `GIT_SEQUENCE_EDITOR` para operação não-interativa. Quatro conflitos de merge foram resolvidos manualmente, preservando as melhorias de UI da Fase 1. |
| **06/05/2026** | **`ProjetoPanel` simplificado — toolbar `Novo` / `Gerenciar` / `Excluir`:** O método `criarToolbar()` foi reescrito com três botões. `Gerenciar` abre um `GestaoProjetoPanel` em `JFrame` dedicado, com controle de janela por `Map<UUID, JFrame>` para evitar instâncias duplicadas do mesmo projeto. `Excluir` confirma antes de remover e guarda contra deleção com janela de gestão ainda aberta. Tecla `Delete` ligada via `InputMap`/`ActionMap` (mais confiável que `KeyListener` para foco de tabela). Double-click redirecionado para `abrirGestao()`. Cascade delete configurado com `@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)` na entidade `Projeto`. |
| **06/05/2026** | **`GestaoProjetoPanel` — bloco de projeto:** Formulário sempre visível (não dialog) com campos Nome, Descrição, Status (`JComboBox<StatusProjeto>` populado com status atual + `proximosStatus()`), Gerente, Início e Previsão. Botão `Salvar Projeto` chama `projetoCtrl.atualizarProjeto()` e — quando o novo status é `CONCLUIDO` e o atual não era — solicita a data de encerramento em dialog antes de chamar `projetoCtrl.encerrarProjeto()`. Após salvar, invoca o callback `onSalvar` para atualizar a listagem no `ProjetoPanel` pai. Carregamento inicial e todas as gravações em `SwingWorker`. |
| **06/05/2026** | **`GestaoProjetoPanel` — bloco de tarefas com edição inline de status:** Tabela com colunas Nome, Status, Prazo, Responsável. Coluna Status usa `DefaultCellEditor` sobrescrito: `getTableCellEditorComponent()` constrói um `JComboBox<StatusTarefa>` com o status atual + `proximosStatus()` a cada ativação; `stopCellEditing()` chama `tarefaCtrl.atualizarStatus()` em `SwingWorker` quando o valor mudou, cancelando a edição em caso de erro. Double-click (excluindo coluna de status) abre o dialog de edição completa. Tecla `Delete` remove com confirmação. |
| **07/05/2026** | **`HomePanel` — grade `3 + 2` e rodapé com usuário e `Sair`:** Card `Tarefas` e sua cor (`CORES_MODULO` de 6 → 5 elementos) removidos. Segunda linha com 2 cards (`Projetos` e `Relatórios`) centralizada naturalmente pelo `FlowLayout.CENTER` de `fileira()`. Botão `Sair` removido do `criarCabecalho()` (onde destoava visualmente dos cards) e adicionado ao `criarRodape()` ao lado de `usuario.getNome()` e `v1.0-SNAPSHOT`. |
| **07/05/2026** | **`TarefaPanel` removido:** `grep -r "TarefaPanel" src/` confirmou ausência de referências externas após as alterações em `HomePanel`. Arquivo deletado (`-321 linhas`). Tarefas passam a existir exclusivamente no contexto do projeto que as contém, com gestão via `GestaoProjetoPanel`. |
| **07/05/2026** | **[DECISÃO DE DESIGN] Análise UX — save atômico:** O botão "Salvar Projeto" posicionado entre o formulário e a tabela de tarefas criava uma quebra semântica visual e funcional: cada operação de tarefa (criar, editar, excluir, mudar status) persistia diretamente no banco, sem relação com o salvamento do projeto. Decisão: tornar a tela atômica — projeto e tarefas são persistidos juntos em um único `Salvar` no rodapé. |
| **07/05/2026** | **Spec e design do modelo de staging:** Especificação de três coleções em memória: `tarefasNovas` (`Map<UUID, DadosTarefa>` keyed por UUID temporário), `tarefasEditadas` (`Map<UUID, DadosTarefa>` keyed por UUID real), `tarefasExcluidas` (`Set<UUID>`). Record `DadosTarefa` com campos `nome`, `descricao`, `prazo`, `responsavelId`, `status` como contrato do staging. Rodapé com `[Cancelar]` e `[Salvar]` no canto inferior direito. Aviso ao fechar janela com staging não vazio. Campo `Status` adicionado ao diálogo de edição de tarefa. Edge cases mapeados: tarefa nova excluída → remove de `tarefasNovas` sem ir a `tarefasExcluidas`; tarefa existente excluída remove de `tarefasEditadas` e entra em `tarefasExcluidas`. |
| **07/05/2026** | **Implementação do save atômico — staging e layout:** `criarBlocoProjet()` simplificado (sem botão Salvar); `criarRodape()` adicionado ao `BorderLayout.SOUTH` com `[Cancelar][Salvar]` e separador `MatteBorder`. Tabela expandida de 5 para 7 colunas: `RespID` (col 5) e `Desc` (col 6) ocultas via `setMaxWidth(0)` — permitem que o helper `dadosParaStaging()` reconstituia um `DadosTarefa` completo a partir do modelo sem acessar o banco. `atualizarContagem()` extraído para atualização incremental do label durante operações de staging. |
| **07/05/2026** | **Implementação do save atômico — refatoração dos métodos de tarefa:** `novaTarefa()`, `editarTarefa()` e `excluirTarefa()` refatorados para operar exclusivamente sobre as coleções de staging, sem chamadas a `tarefaCtrl`. O editor inline de `stopCellEditing()` teve o `SwingWorker` removido — mudanças de status vão para `tarefasEditadas` via `dadosParaStaging()` sem I/O. `editarTarefa()` e o inline agora carregam o estado mais recente do staging antes de abrir o diálogo, preservando edições intermediárias. Campo `Status` adicionado ao diálogo de edição com `JComboBox<StatusTarefa>` populado com estado atual + `proximosStatus()`. |
| **07/05/2026** | **Implementação do save atômico — `salvarTudo()` e `cancelar()`:** `salvarProjeto()` removido. `salvarTudo()` copia os três staging maps para variáveis locais antes de disparar o `SwingWorker` (evitando race condition), persiste projeto, aplica novas tarefas (`criarTarefa` + `atualizarStatus` se não-PENDENTE), aplica edições (`atualizarTarefa` + `reatribuirResponsavel` + `atualizarStatus` se diferente do banco) e executa exclusões. Em `done()`: limpa staging, invoca callback e recarrega. `cancelar()` limpa as três coleções e recarrega do banco. |
| **07/05/2026** | **`ProjetoPanel` — aviso de alterações não salvas:** `abrirGestao()` alterado para `JFrame.DO_NOTHING_ON_CLOSE`; `WindowAdapter.windowClosing()` consulta `gestaoPanel.temAlteracoesPendentes()` e, se verdadeiro, exibe diálogo YES/NO antes de chamar `dispose()`. `windowClosed()` permanece responsável por limpar o mapa de janelas abertas e atualizar a listagem. |
| **07/05/2026** | **Bugfix — view row vs. model row com RowSorter:** `setAutoCreateRowSorter(true)` faz `getSelectedRow()` e `getEditingRow()` retornarem índices de view, que divergem dos índices de model quando a tabela está ordenada. Todos os três pontos de acesso ao `DefaultTableModel` com índice dinâmico foram corrigidos com `convertRowIndexToModel()`: `editarTarefa()`, `excluirTarefa()` e `stopCellEditing()`. Sem essa correção, ordenar a tabela e editar/excluir operaria sobre a linha errada silenciosamente. |

| **09/05/2026** | **[DECISÃO DE DESIGN] Remoção de restrições de transição de status:** A máquina de estados de `StatusTarefa` (`proximosStatus()`) foi identificada como restrição desnecessária para o contexto do sistema — o gerente deve poder atualizar o status para qualquer valor sem restrição de fluxo. `proximosStatus()` foi removido do enum, `validarTransicaoStatus()` e sua chamada foram removidos de `TarefaController.atualizarStatus()`. Os três pontos de `GestaoProjetoPanel` que populavam combos via `proximosStatus()` foram substituídos por `StatusTarefa.values()`. |
| **09/05/2026** | **Remoção da restrição de exclusão por status:** `TarefaController.removerTarefa()` verificava se o status era `EM_ANDAMENTO` ou `CONCLUIDA` antes de deletar, lançando `IllegalStateException`. A guarda equivalente existia na view (`GestaoProjetoPanel.excluirTarefa()`). Ambas foram removidas — tarefas podem ser excluídas independentemente do status. |
| **09/05/2026** | **`stopCellEditing` automático em `salvarTudo()`:** Identificado cenário onde o usuário alterava o status via combo inline e clicava em Salvar sem tirar o foco da célula — a edição permanecia "suspensa" no editor e não era capturada pelo staging. Corrigido com `if (tabelaTarefas.isEditing()) tabelaTarefas.getCellEditor().stopCellEditing()` como primeira instrução de `salvarTudo()`. |
| **09/05/2026** | **Fechamento automático da janela após salvar:** `ProjetoPanel.abrirGestao()` reordenado para criar o `JFrame` (`janelaFinal`) antes do `GestaoProjetoPanel`, permitindo que o lambda do callback `onSalvar` capture `janelaFinal` e chame `dispose()` após `this.carregar()`. Anteriormente o callback era `this::carregar` e a janela permanecia aberta após salvar. |
| **09/05/2026** | **Migration V4 — seed de usuários e equipe padrão:** `V4__Seed_usuarios_e_equipe.sql` inseriu usuários `gerente` (perfil `GERENTE`) e `usuario` (perfil `COLABORADOR`) com UUIDs fixos e senha BCrypt de `123`. Criada equipe "Equipe Padrão" com UUID fixo; membros inseridos via UUID fixo (gerente, usuario) e subconsulta por login (admin, sem UUID fixo no V3). README recebeu seção "Usuários e Equipe Padrão" com tabela de credenciais antes de "Como Executar Localmente". |

---

---

## 3. Registros de Desafios Enfrentados — Fase 3 (Save Atômico)

O principal desafio técnico da Fase 1 foi a correta separação entre código de UI e código de acesso a dados nos métodos de carregamento. O padrão `SwingWorker<T, Void>` parece simples, mas exige disciplina: qualquer chamada a `usuarioCtrl`, `projetoCtrl` ou similares que apareça dentro de `done()` (que roda na EDT) constitui uma violação — mesmo que seja para popular um `JComboBox` a partir do resultado já carregado. Em `GestaoProjetoPanel.carregarProjeto()`, por exemplo, a lista de gerentes foi inicialmente colocada em `done()` e precisou ser movida para `doInBackground()` via `SwingWorker<Object[], Void>` com resultado composto.

O principal desafio do redesign foi a resolução dos conflitos de merge durante o rebase. Três commits removidos tinham interdependências com quatro commits mantidos — o merge automático tentava reconciliar versões que referenciavam campos (`projetoFixo`, `janelasTarefas`, `btnTarefas`) que não existiam mais no estado correto do arquivo. Cada conflito foi resolvido manualmente, tomando o lado "incoming" onde era necessário manter as melhorias de UI (empty state, scroll, tooltips) e o lado "ours" onde as referências ao design descartado precisavam ser removidas.

Um segundo desafio do redesign foi a implementação do `TableCellEditor` inline para status de tarefa. O override de `DefaultCellEditor` usa o campo `protected editorComponent` (typed como `Component`) que precisa ser reatribuído a um novo `JComboBox` a cada ativação da célula — sem esse padrão, o mesmo combo seria reutilizado com os itens errados para tarefas com status diferentes. A chamada a `super.stopCellEditing()` antes de iniciar o `SwingWorker` garante que o editor seja liberado antes da operação assíncrona, evitando estados inconsistentes na tabela.

O principal desafio da Fase 3 foi o design do helper `dadosParaStaging()`. Para que o diálogo de edição e o editor inline mostrem sempre o estado mais recente — e não o estado do banco —, o método precisa consultar staging primeiro (`tarefasNovas` → `tarefasEditadas`) e só cair no modelo da tabela como fallback. Para que esse fallback funcione sem acesso ao banco, dois campos ocultos foram adicionados à tabela (`RespID` e `Desc`), tornando o modelo auto-suficiente. A alternativa de um novo hit ao banco no `stopCellEditing()` foi descartada por bloquear a EDT.

A conversão `view → model row` com `RowSorter` ativo é um bug silencioso típico de Swing: o código aparenta funcionar corretamente porque, sem ordenação ativa, os índices coincidem. O bug só se manifesta após o usuário clicar em um cabeçalho de coluna para ordenar e, em seguida, editar ou excluir uma tarefa — momento em que `getSelectedRow()` retorna o índice visual, divergindo do índice real no `DefaultTableModel`. A correção foi aplicada em todos os três pontos de acesso dinâmico ao modelo.

---

## 4. Artefatos Entregues

| Artefato | Caminho | Operação |
|----------|---------|----------|
| Tela unificada de gestão de projeto e tarefas (atômica) | `src/main/java/.../view/GestaoProjetoPanel.java` | Criado / Refatorado |
| Record de staging de tarefa | `src/main/java/.../view/DadosTarefa.java` | Criado |
| Painel de projetos simplificado + aviso ao fechar | `src/main/java/.../view/ProjetoPanel.java` | Modificado |
| Grade 3+2 e rodapé com usuário/Sair | `src/main/java/.../view/HomePanel.java` | Modificado |
| Utilitário de combos | `src/main/java/.../util/OpcaoItem.java` | Criado |
| Utilitário de datas | `src/main/java/.../util/DateUtils.java` | Criado |
| Utilitário de tabelas com empty state | `src/main/java/.../view/TableUtils.java` | Criado |
| Método `removerProjeto()` e cascade | `src/main/java/.../controller/ProjetoController.java` | Modificado |
| `@OneToMany` com cascade e orphanRemoval | `src/main/java/.../model/entity/Projeto.java` | Modificado |
| Painel autônomo de tarefas (removido) | `src/main/java/.../view/TarefaPanel.java` | Removido |
| Enum de status sem restrição de transição | `src/main/java/.../model/enums/StatusTarefa.java` | Modificado |
| Controller de tarefas sem validação de transição/exclusão | `src/main/java/.../controller/TarefaController.java` | Modificado |
| Tela de gestão com combos completos e stopCellEditing | `src/main/java/.../view/GestaoProjetoPanel.java` | Modificado |
| Painel de projetos com fechamento automático pós-save | `src/main/java/.../view/ProjetoPanel.java` | Modificado |
| Seed de usuários e equipe padrão | `src/main/resources/db/migration/V4__Seed_usuarios_e_equipe.sql` | Criado |

---

## 5. Métricas do Redesign

| Categoria | Antes | Depois |
|-----------|-------|--------|
| Botões de ação em `ProjetoPanel` | 4 (Editar, Alterar Status, Encerrar, Tarefas) | 3 (Novo, Gerenciar, Excluir) |
| Dialogs para gerenciar um projeto | 3 separados | 1 tela unificada |
| Módulos no `HomePanel` | 6 (grade 3+3) | 5 (grade 3+2) |
| Ponto de acesso às tarefas | 2 (TarefaPanel + ProjetoPanel) | 1 (via GestaoProjetoPanel) |
| Linhas de código removidas (`TarefaPanel`) | — | −321 linhas |
| Commits removidos via rebase | — | 3 commits conflitantes |
