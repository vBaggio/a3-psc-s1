# Sprint Backlog - Sprint 6

**Período Inicial/Final:** 26/04/2026 a 02/05/2026  
**Responsável do Projeto:** Vinícius Baggio  
**Objetivo da Sprint:** Implementação da camada de relatórios e métricas de desempenho, expondo indicadores de projetos e equipes sem modificar o esquema de banco de dados.

---

## Escopo Previsto

| ID | Descrição da Tarefa | Status |
|----|----------------------|--------|
| **TSK-01** | DTO `ResumoProjeto` — transporta métricas calculadas de um projeto (contagem de tarefas por status, percentual de conclusão, situação de prazo). | ✅ Concluído |
| **TSK-02** | DTO `CargaUsuario` — transporta métricas de carga de trabalho por membro (tarefas ativas, concluídas, vencidas). | ✅ Concluído |
| **TSK-03** | `RelatorioController.resumoGlobal()` — agrega contagem de projetos por status (`Map<StatusProjeto, Long>`). | ✅ Concluído |
| **TSK-04** | `RelatorioController.desempenhoPorProjeto(UUID)` — calcula métricas de tarefas e prazo de um projeto específico, retornando `ResumoProjeto`. | ✅ Concluído |
| **TSK-05** | `RelatorioController.cargaDeTrabalho()` — itera todos os usuários com tarefas atribuídas e compila `List<CargaUsuario>`. | ✅ Concluído |
| **TSK-06** | `RelatorioPanel` — painel Swing com `JTabbedPane` de 3 abas: **Resumo Global** (cards por status), **Desempenho por Projeto** (combo + JTable de métricas), **Carga de Trabalho** (JTable com membros e cargas). | ✅ Concluído |
| **TSK-07** | Integração de `RelatorioPanel` ao `MainFrame` — novo item `Relatórios` no menu `Operações`, separado dos itens de CRUD por um separador visual. | ✅ Concluído |

---

## Escopo Pós-Review (Revisão Arquitetural)

Após revisão arquitetural multi-agente do código entregue, foram identificados e corrigidos 10 pontos de melhoria distribuídos em quatro categorias de severidade.

| ID | Categoria | Descrição | Status |
|----|-----------|-----------|--------|
| **TSK-08** | Bug crítico | Corrigir `ResumoProjeto.isAtrasado()` — projetos `CANCELADO` eram incorretamente marcados como atrasados quando `dataFim` superava `dataPrevisao`. | ✅ Concluído |
| **TSK-09** | Refactor | Converter `ResumoProjeto` de classe com boilerplate para **Java record** — elimina construtor de 10 parâmetros vulnerável a troca de posição, acessores sem prefixo `get`. | ✅ Concluído |
| **TSK-10** | Refactor + Bug | Converter `CargaUsuario` para **Java record** e adicionar campo `tarefasCanceladas` — fechava inconsistência onde `sum(pendentes + emAndamento + concluidas) != tarefas.size()` quando havia canceladas. | ✅ Concluído |
| **TSK-11** | Segurança arquitetural | Criar DTO `ProjetoOpcao(UUID id, String nome)` e substituir `listarProjetos(): List<Projeto>` por `listarProjetosParaCombo(): List<ProjetoOpcao>` — elimina vazamento de entidade JPA para a View (risco de `LazyInitializationException`). | ✅ Concluído |
| **TSK-12** | Performance (N+1) | Eliminar N+1 em `cargaDeTrabalho()`: substituir loop de queries por agrupamento em memória após uma única query com `JOIN FETCH` (`TarefaRepository.listarComResponsavel()`). | ✅ Concluído |
| **TSK-13** | Performance | Otimizar `resumoGlobal()`: substituir carregamento de todas as entidades `Projeto` por query JPQL `GROUP BY p.status` (`ProjetoRepository.contarPorStatus()`). | ✅ Concluído |
| **TSK-14** | EDT / UI freeze | Mover todas as operações JPA para `SwingWorker.doInBackground()` nas 3 abas do `RelatorioPanel` — evita travamento da EDT em cargas com volume. | ✅ Concluído |
| **TSK-15** | UX | Auto-refresh do combo de projetos ao entrar na aba "Desempenho por Projeto" via `ChangeListener` no `JTabbedPane` — garante que projetos criados em outra janela apareçam sem reiniciar o módulo. | ✅ Concluído |
| **TSK-16** | Qualidade | Remover chamada redundante a `popularComboProjetos()` em `buildDesempenhoTab()` (substituída pelo `ChangeListener`), adicionar null guard no método e corrigir convenção de nomeação `getTotalAtivas()` → `totalAtivas()`. | ✅ Concluído |

---

## Ferramentas Adotadas na Sprint

- **Interface Gráfica:** Java Swing (javax.swing)
- **Look & Feel:** FlatLaf 3.4.1 (`FlatDarkLaf`) — herdado da Sprint 5
- **Componentes:** `JTabbedPane`, `JTable` + `DefaultTableModel`, `JComboBox`, `GridBagLayout`, `GridLayout`, `SwingWorker`
- **Padrão de dados:** DTOs imutáveis (Java records) como contrato entre Controller e View
- **Banco de Dados:** Nenhuma migration adicionada — dados derivados das tabelas existentes
- **Build:** Apache Maven 3.x
- **Controle de Versões:** Git (branch `claude/evaluate-sprint-6-eE7nQ`, mergeado em `master`)
