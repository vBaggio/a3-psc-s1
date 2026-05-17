# Sprint Backlog - Sprint 8

**Período Inicial/Final:** 14/05/2026 a 21/05/2026  
**Responsável do Projeto:** Vinícius Baggio  
**Objetivo da Sprint:** Corrigir o modelo de dados do vínculo equipe↔projeto de ManyToMany para ManyToOne — alinhando banco, entidades, controllers e UI com a regra de negócio de que um projeto pertence a exatamente uma equipe. A sprint também restringe a atribuição de responsável de tarefa aos membros da equipe vinculada ao projeto.

---

## Fase 1 — Correção do Modelo de Dados

O schema original modela `equipe_projeto` como tabela de junção ManyToMany, permitindo que um projeto pertença a múltiplas equipes. A entidade `Projeto` não possui referência alguma a `Equipe`. O relacionamento correto é ManyToOne: um projeto pertence a uma equipe.

| ID | Descrição da Tarefa | Status |
|----|----------------------|--------|
| **TSK-01** | Migration `V5__Vinculo_equipe_projeto.sql`: `DROP TABLE equipe_projeto` e `ALTER TABLE projeto ADD COLUMN equipe_id UUID REFERENCES equipe(id)`. A coluna é adicionada sem `NOT NULL` para compatibilidade com dados existentes — restrição aplicada em V6 se necessário. | ✅ Concluído |
| **TSK-02** | Entidade `Projeto` — adicionar `@ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "equipe_id") private Equipe equipe` com `getEquipe()` e `setEquipe(Equipe)`. | ✅ Concluído |
| **TSK-03** | Entidade `Equipe` — substituir `@ManyToMany` + `@JoinTable` por `@OneToMany(mappedBy = "equipe") private List<Projeto> projetos`. Remover método `addProjeto(Projeto)`. | ✅ Concluído |
| **TSK-04** | `EquipeRepository` — remover `adicionarProjeto(UUID, UUID)`. Simplificar `buscarPorId()` removendo o `LEFT JOIN FETCH e.projetos` (agora desnecessário na direção inversa). | ✅ Concluído |
| **TSK-05** | `ProjetoRepository` — adicionar `JOIN FETCH p.equipe` em `listarTodos()` e `buscarPorId()` para evitar N+1 ao exibir o nome da equipe na listagem e na tela de gestão. | ✅ Concluído |

---

## Fase 2 — Controller

| ID | Descrição da Tarefa | Status |
|----|----------------------|--------|
| **TSK-06** | `ProjetoController.criarProjeto()` — adicionar parâmetro `UUID equipeId`. Implementar método privado `resolverEquipe(UUID equipeId)` que valida existência e presença de ao menos 1 membro antes de retornar a entidade `Equipe`. Setar `projeto.setEquipe(equipe)` antes de persistir. | ✅ Concluído |
| **TSK-07** | `ProjetoController.atualizarProjeto()` — adicionar parâmetro `UUID equipeId`. Chamar `resolverEquipe()` e setar a equipe no projeto antes de persistir via `projetoRepo.atualizar()`. | ✅ Concluído |
| **TSK-08** | `ProjetoController` — remover `atribuirEquipe(UUID, UUID)`. O vínculo agora é gerenciado diretamente em `criarProjeto()` e `atualizarProjeto()`. | ✅ Concluído |

---

## Fase 3 — UI: Listagem e Criação de Projeto

| ID | Descrição da Tarefa | Status |
|----|----------------------|--------|
| **TSK-09** | `ProjetoPanel` — adicionar coluna `"Equipe"` no `DefaultTableModel` e popular com `p.getEquipe() != null ? p.getEquipe().getNome() : ""` no `carregar()`. Ajustar largura preferencial da coluna. | ✅ Concluído |
| **TSK-10** | `ProjetoPanel.abrirFormulario()` — adicionar `comboEquipe` (`JComboBox<OpcaoItem>`) populado com equipes que possuem ao menos 1 membro. Exibir aviso e bloquear criação se não houver equipes elegíveis. Passar `equipeId` para `ctrl.criarProjeto()`. | ✅ Concluído |

---

## Fase 4 — Refinamentos de Integridade (Council Sprint 8)

Itens identificados pelo council de revisão antes da conclusão da UI.

| ID | Descrição da Tarefa | Status |
|----|----------------------|--------|
| **TSK-11** | `ProjetoRepository.listarPorGerente()` — adicionar `LEFT JOIN FETCH p.equipe` à query JPQL, alinhando com os demais métodos do repositório e prevenindo `LazyInitializationException`. | ✅ Concluído |
| **TSK-12** | Migration `V6__Integridade_projeto.sql`: `ALTER TABLE projeto ALTER COLUMN gerente_id SET NOT NULL`, `ALTER TABLE projeto ALTER COLUMN equipe_id SET NOT NULL`, e criação dos índices FK ausentes: `idx_projeto_equipe_id`, `idx_projeto_gerente_id`, `idx_tarefa_projeto_id`, `idx_tarefa_responsavel_id`. | ✅ Concluído |

---

## Fase 5 — UI: Gestão do Projeto

| ID | Descrição da Tarefa | Status |
|----|----------------------|--------|
| **TSK-13** | `GestaoProjetoPanel` — declarar `comboEquipe` (`JComboBox<OpcaoItem>`) e adicioná-lo ao formulário do projeto em `criarBlocoProjet()`, abaixo de `comboGerente`. | ✅ Concluído |
| **TSK-14** | `GestaoProjetoPanel.carregarProjeto()` — expandir `SwingWorker<Object[], Void>` para carregar `List<Equipe>` em paralelo com projeto e gerentes (`Object[]{p, gerentes, equipes}`). Em `done()`: popular `comboEquipe` e pré-selecionar a equipe atual do projeto. | ✅ Concluído |
| **TSK-15** | `GestaoProjetoPanel.salvarTudo()` — extrair `equipeId` do `comboEquipe` selecionado e incluí-lo na chamada `projetoCtrl.atualizarProjeto()`. Validar que uma equipe foi selecionada antes de prosseguir. Regra de troca de equipe: se a equipe mudou, verificar que nenhuma tarefa do projeto possui responsável fora dos membros da nova equipe; se houver conflito, exibir mensagem com os nomes dos responsáveis afetados e abortar. | ✅ Concluído |
| **TSK-16** | `GestaoProjetoPanel.montarComboResponsavel()` — substituir `usuarioCtrl.listarUsuarios()` (todos) por `equipeCtrl.listarMembros(equipeId)` onde `equipeId` é a equipe atualmente selecionada no `comboEquipe`. Gerente do projeto não precisa ser membro da equipe — a restrição de membro aplica-se apenas a responsáveis de tarefa. | ✅ Concluído |

---

## Fase 6 — Correções e Ajustes Pós-Implementação

Itens identificados e resolvidos durante a execução da sprint, não previstos no planejamento inicial.

| ID | Descrição da Tarefa | Status |
|----|----------------------|--------|
| **TSK-17** | `ProjetoController` — refatorar para eliminar query duplicada de membros em `resolverEquipe()`. | ✅ Concluído |
| **TSK-18** | Migration `V6` — adicionar backfill de `equipe_id` e `gerente_id` em linhas existentes antes de aplicar `NOT NULL`, evitando falha de migração em bases com dados pré-existentes. | ✅ Concluído |
| **TSK-19** | `TarefaController` — permitir edição de campos em tarefas com status `CONCLUIDA` ou `CANCELADA` (bloqueio anterior era excessivo para o fluxo de gestão). | ✅ Concluído |
| **TSK-20** | `EquipeController` — bloquear remoção de membro que seja responsável por alguma tarefa em projeto da equipe, exibindo mensagem com as tarefas afetadas. | ✅ Concluído |

---

## Ferramentas e Componentes Adotados na Sprint

- **Interface Gráfica:** Java Swing (javax.swing)
- **Look & Feel:** FlatLaf 3.4.1 (`FlatDarkLaf`) — herdado das sprints anteriores
- **Persistência:** JPA / Hibernate — ajuste de mapeamento `@ManyToOne` / `@OneToMany`
- **Versionamento de Banco:** Flyway — migrations V5 e V6
- **Padrão assíncrono:** `SwingWorker<T, Void>` para carregamentos na EDT
- **Build:** Apache Maven 3.x
