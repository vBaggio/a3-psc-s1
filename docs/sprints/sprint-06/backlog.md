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

## Ferramentas Adotadas na Sprint

- **Interface Gráfica:** Java Swing (javax.swing)
- **Look & Feel:** FlatLaf 3.4.1 (`FlatDarkLaf`) — herdado da Sprint 5
- **Componentes:** `JTabbedPane`, `JTable` + `DefaultTableModel`, `JComboBox`, `GridBagLayout`, `GridLayout`
- **Padrão de dados:** DTOs imutáveis como contrato entre Controller e View
- **Banco de Dados:** Nenhuma migration adicionada — dados derivados das tabelas existentes
- **Build:** Apache Maven 3.x
- **Controle de Versões:** Git (branch `claude/evaluate-sprint-6-eE7nQ`)
