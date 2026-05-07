# Relatório Semanal de Desenvolvimento - Sprint 7

**Responsável Principal:** Vinícius Baggio  
**Período Avaliado:** 03/05/2026 a 07/05/2026  
**Status do Ciclo:** Concluído  

---

## 1. Resumo Executivo das Avaliações

A Sprint 7 foi dividida em duas fases com objetivos distintos. A **Fase 1** entregou um conjunto de melhorias transversais de qualidade de UI: carregamento assíncrono via `SwingWorker` em todos os painéis, double-click para edição, desabilitação contextual de botões, formatação legível de enums e datas, empty state em tabelas, ordenação por coluna e atalhos de teclado. A **Fase 2** surgiu de uma análise UX que identificou problemas estruturais no módulo de projetos e tarefas — redundância de botões, ausência de coesão entre `Projeto` e `Tarefa`, e `TarefaPanel` como módulo autônomo de baixo valor — e resultou no redesign completo do módulo.

O ponto central do redesign foi a criação do `GestaoProjetoPanel`: uma janela que unifica o formulário de dados do projeto com a gestão das suas tarefas em um único contexto, eliminando quatro telas de diálogo separadas e dois botões redundantes. Paralelamente, `TarefaPanel` foi removido como módulo autônomo, a grade do `HomePanel` foi reorganizada para `3 + 2` e o botão `Sair` foi relocado do cabeçalho para o rodapé ao lado do nome do usuário logado.

Commits que conflitavam com as novas decisões de design foram removidos do histórico via **rebase local** antes do início da implementação, mantendo o histórico coeso e sem revelar decisões de design descartadas nos relatórios finais.

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

---

## 3. Registros de Desafios Enfrentados

O principal desafio técnico da Fase 1 foi a correta separação entre código de UI e código de acesso a dados nos métodos de carregamento. O padrão `SwingWorker<T, Void>` parece simples, mas exige disciplina: qualquer chamada a `usuarioCtrl`, `projetoCtrl` ou similares que apareça dentro de `done()` (que roda na EDT) constitui uma violação — mesmo que seja para popular um `JComboBox` a partir do resultado já carregado. Em `GestaoProjetoPanel.carregarProjeto()`, por exemplo, a lista de gerentes foi inicialmente colocada em `done()` e precisou ser movida para `doInBackground()` via `SwingWorker<Object[], Void>` com resultado composto.

O principal desafio do redesign foi a resolução dos conflitos de merge durante o rebase. Três commits removidos tinham interdependências com quatro commits mantidos — o merge automático tentava reconciliar versões que referenciavam campos (`projetoFixo`, `janelasTarefas`, `btnTarefas`) que não existiam mais no estado correto do arquivo. Cada conflito foi resolvido manualmente, tomando o lado "incoming" onde era necessário manter as melhorias de UI (empty state, scroll, tooltips) e o lado "ours" onde as referências ao design descartado precisavam ser removidas.

Um segundo desafio do redesign foi a implementação do `TableCellEditor` inline para status de tarefa. O override de `DefaultCellEditor` usa o campo `protected editorComponent` (typed como `Component`) que precisa ser reatribuído a um novo `JComboBox` a cada ativação da célula — sem esse padrão, o mesmo combo seria reutilizado com os itens errados para tarefas com status diferentes. A chamada a `super.stopCellEditing()` antes de iniciar o `SwingWorker` garante que o editor seja liberado antes da operação assíncrona, evitando estados inconsistentes na tabela.

---

## 4. Artefatos Entregues

| Artefato | Caminho | Operação |
|----------|---------|----------|
| Tela unificada de gestão de projeto e tarefas | `src/main/java/.../view/GestaoProjetoPanel.java` | Criado |
| Painel de projetos simplificado | `src/main/java/.../view/ProjetoPanel.java` | Modificado |
| Grade 3+2 e rodapé com usuário/Sair | `src/main/java/.../view/HomePanel.java` | Modificado |
| Utilitário de combos | `src/main/java/.../util/OpcaoItem.java` | Criado |
| Utilitário de datas | `src/main/java/.../util/DateUtils.java` | Criado |
| Utilitário de tabelas com empty state | `src/main/java/.../view/TableUtils.java` | Criado |
| Método `removerProjeto()` e cascade | `src/main/java/.../controller/ProjetoController.java` | Modificado |
| `@OneToMany` com cascade e orphanRemoval | `src/main/java/.../model/entity/Projeto.java` | Modificado |
| Painel autônomo de tarefas (removido) | `src/main/java/.../view/TarefaPanel.java` | Removido |

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
