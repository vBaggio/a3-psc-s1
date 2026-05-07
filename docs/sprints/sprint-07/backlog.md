# Sprint Backlog - Sprint 7

**Período Inicial/Final:** 03/05/2026 a 07/05/2026  
**Responsável do Projeto:** Vinícius Baggio  
**Objetivo da Sprint:** Elevar a qualidade da interface gráfica com melhorias de UX e, a partir de uma análise estrutural, redesenhar o módulo de projetos e tarefas para eliminar redundâncias e melhorar a coesão entre entidades relacionadas.

---

## Fase 1 — Melhorias de Qualidade de UI

| ID | Descrição da Tarefa | Status |
|----|----------------------|--------|
| **TSK-01** | `SwingWorker` em todos os painéis — mover chamadas JPA para `doInBackground()` e autenticação de login para thread separada, evitando bloqueio da EDT durante carregamentos. | ✅ Concluído |
| **TSK-02** | Double-click em linha de tabela abre o formulário de edição do registro selecionado em todos os módulos. | ✅ Concluído |
| **TSK-03** | Botões contextuais (`Editar`, `Alterar Status`, `Encerrar`, `Excluir`) desabilitados quando não há seleção; `Editar`/`Encerrar` desabilitados conforme status do registro selecionado. | ✅ Concluído |
| **TSK-04** | Dialogs de `Alterar Status` exibem apenas as transições válidas definidas por `proximosStatus()` nos enums `StatusProjeto` e `StatusTarefa`. | ✅ Concluído |
| **TSK-05** | Auto-load da aba "Desempenho por Projeto" via `ChangeListener` no `JTabbedPane` — atualiza o combo de projetos ao entrar na aba sem necessidade de botão de recarregar. | ✅ Concluído |
| **TSK-06** | Ordenação por coluna (`setAutoCreateRowSorter(true)`) e larguras proporcionais definidas por `setPreferredWidth()` em todas as tabelas. | ✅ Concluído |
| **TSK-07** | Formatação legível de enums e datas nas tabelas: status exibidos com `toString()` dos enums, datas com máscara `dd/MM/yyyy` via `DateUtils.format()`. | ✅ Concluído |
| **TSK-08** | Empty state com mensagem centralizada nas tabelas sem dados (`TableUtils.tabelaComMensagem()`). | ✅ Concluído |
| **TSK-09** | Teclado `Enter` confirma login, foco retorna ao campo `login` após erro de autenticação, tooltips descritivos adicionados a todos os botões de ação. | ✅ Concluído |
| **TSK-10** | Scroll automático para o item recém-criado após inserção em tabela. | ✅ Concluído |
| **TSK-11** | Utilitários extraídos: `OpcaoItem` (record para combos com `UUID id` e `String label`) e `DateUtils` (parse/format de datas com máscara). | ✅ Concluído |
| **TSK-12** | Correções diversas de layout, tratamento de erro e `JOIN FETCH` em repositories para eliminar queries N+1 residuais em Equipe, Projeto e Tarefa. | ✅ Concluído |

---

## Fase 2 — Redesign UX do Módulo de Projetos e Tarefas

Após análise UX realizada no decorrer da sprint, foram identificados problemas estruturais que motivaram um redesign mais profundo do módulo. Commits conflitantes com as decisões anteriores foram removidos do histórico via rebase local antes do início da implementação.

| ID | Descrição da Tarefa | Status |
|----|----------------------|--------|
| **TSK-13** | Análise UX e decisão de design: `ProjetoPanel` tinha 5 botões com responsabilidades sobrepostas; `TarefaPanel` como módulo autônomo tem menos valor que gestão contextual; `Tarefa` não faz sentido semântico fora do projeto que a contém. Decisão: criar `GestaoProjetoPanel` como tela unificada. | ✅ Concluído |
| **TSK-14** | Rebase local para remover commits conflitantes com o redesign (botão `Tarefas` no toolbar, confirmação de encerramento separada), preservando os commits de qualidade de UI da Fase 1. | ✅ Concluído |
| **TSK-15** | Simplificar `ProjetoPanel`: toolbar reduzida a `Novo` / `Gerenciar` / `Excluir`; `Excluir` com confirmação e cascata (`@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)`); double-click e tecla `Delete` abrem/excluem; mapa `Map<UUID, JFrame>` para controle de janelas de gestão abertas. Adição de `removerProjeto()` no `ProjetoController`. | ✅ Concluído |
| **TSK-16** | Criar `GestaoProjetoPanel`: janela `JFrame` (820×580) com dois blocos — formulário de dados do projeto (Nome, Descrição, Status inline via `JComboBox`, Gerente, Início, Previsão) e lista de tarefas com toolbar `Nova Tarefa` / `Editar` / `Excluir`. Status do projeto com transição para `CONCLUIDO` solicita data de encerramento antes de persistir. Chamadas JPA em `SwingWorker`. | ✅ Concluído |
| **TSK-17** | Edição inline de status de tarefa via `TableCellEditor` com `JComboBox` filtrado por `proximosStatus()` — persiste via `tarefaCtrl.atualizarStatus()` em `SwingWorker` ao sair da célula. | ✅ Concluído |
| **TSK-18** | Dialog de edição de tarefa em `GestaoProjetoPanel` unifica Nome, Descrição, Prazo e Responsável em um único formulário (eliminando botão `Reatribuir` separado). | ✅ Concluído |
| **TSK-19** | Ajustar `HomePanel`: remover card `Tarefas`, reorganizar grade para layout `3 + 2` (Cargos/Usuários/Equipes | Projetos/Relatórios); mover botão `Sair` do cabeçalho para o rodapé ao lado do nome do usuário logado e versão. | ✅ Concluído |
| **TSK-20** | Remover `TarefaPanel.java` — verificação de referências via `grep` confirmou ausência de dependências externas; arquivo deletado, tarefas acessíveis exclusivamente via `GestaoProjetoPanel`. | ✅ Concluído |

---

## Fase 3 — Save Atômico no GestaoProjetoPanel

Após análise crítica da UX pós-Fase 2, identificou-se que o botão "Salvar Projeto" posicionado entre o formulário e a tabela de tarefas criava uma quebra semântica: o projeto e suas tarefas pareciam entidades independentes quando deveriam ser salvas como uma unidade. Cada operação de tarefa (criar, editar, excluir, mudar status) também persistia imediatamente no banco, sem relação com o salvamento do projeto. A decisão foi tornar a tela atômica.

| ID | Descrição da Tarefa | Status |
|----|----------------------|--------|
| **TSK-21** | Análise UX, brainstorming e especificação do modelo de save atômico: botão único `Salvar` no rodapé inferior direito, botão `Cancelar` ao lado, staging em memória via três coleções (`tarefasNovas`, `tarefasEditadas`, `tarefasExcluidas`), aviso "alterações não salvas" ao fechar janela, campo `Status` adicionado ao diálogo de edição de tarefa. | ✅ Concluído |
| **TSK-22** | Implementação do save atômico: criação do record `DadosTarefa`, refatoração de `novaTarefa()` / `editarTarefa()` / `excluirTarefa()` / editor inline de status para operar sobre staging em vez do banco; `salvarTudo()` via `SwingWorker` persiste projeto + todas as mudanças de tarefa atomicamente; `cancelar()` descarta staging e recarrega do banco; `ProjetoPanel.abrirGestao()` intercepta fechamento da janela com diálogo de confirmação quando há alterações pendentes. | ✅ Concluído |
| **TSK-23** | Bugfix: `getSelectedRow()` / `getEditingRow()` retornam índice de *view*, mas `DefaultTableModel` espera índice de *model* — divergem quando o `RowSorter` está ativo. Aplicado `convertRowIndexToModel()` em todos os pontos de acesso ao modelo (`editarTarefa`, `excluirTarefa`, `stopCellEditing`). | ✅ Concluído |

---

## Ferramentas e Componentes Adotados na Sprint

- **Interface Gráfica:** Java Swing (javax.swing)
- **Look & Feel:** FlatLaf 3.4.1 (`FlatDarkLaf`) — herdado das sprints anteriores
- **Componentes novos:** `DefaultCellEditor` com `JComboBox` para edição inline, `InputMap`/`ActionMap` para atalhos de teclado, `WindowAdapter` para ciclo de vida de janelas filhas, `Map<UUID, DadosTarefa>` + `Set<UUID>` como staging em memória
- **Padrão assíncrono:** `SwingWorker<T, Void>` para todos os acessos JPA e operações de escrita
- **Controle de versão:** Git rebase local para remoção de commits conflitantes antes do redesign
- **Build:** Apache Maven 3.x
