# Sprint Backlog - Sprint 8

**Período Inicial/Final:** 14/05/2026 a 21/05/2026  
**Responsável do Projeto:** Vinícius Baggio  
**Objetivo da Sprint:** Corrigir o modelo de dados do vínculo equipe↔projeto de ManyToMany para ManyToOne — alinhando banco, entidades, controllers e UI com a regra de negócio de que um projeto pertence a exatamente uma equipe. A sprint também restringe a atribuição de responsável de tarefa aos membros da equipe vinculada ao projeto.

---

## Fase 1 — Correção do Modelo de Dados

O schema original modela `equipe_projeto` como tabela de junção ManyToMany, permitindo que um projeto pertença a múltiplas equipes. A entidade `Projeto` não possui referência alguma a `Equipe`. O relacionamento correto é ManyToOne: um projeto pertence a uma equipe.

| ID | Descrição da Tarefa | Status |
|----|----------------------|--------|
| **TSK-01** | Migration `V5__Vinculo_equipe_projeto.sql`: `DROP TABLE equipe_projeto` e `ALTER TABLE projeto ADD COLUMN equipe_id UUID REFERENCES equipe(id)`. A coluna é adicionada sem `NOT NULL` para compatibilidade com dados existentes — restrição aplicada em V6 se necessário. | ⬜ Pendente |
| **TSK-02** | Entidade `Projeto` — adicionar `@ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "equipe_id") private Equipe equipe` com `getEquipe()` e `setEquipe(Equipe)`. | ⬜ Pendente |
| **TSK-03** | Entidade `Equipe` — substituir `@ManyToMany` + `@JoinTable` por `@OneToMany(mappedBy = "equipe") private List<Projeto> projetos`. Remover método `addProjeto(Projeto)`. | ⬜ Pendente |
| **TSK-04** | `EquipeRepository` — remover `adicionarProjeto(UUID, UUID)`. Simplificar `buscarPorId()` removendo o `LEFT JOIN FETCH e.projetos` (agora desnecessário na direção inversa). | ⬜ Pendente |
| **TSK-05** | `ProjetoRepository` — adicionar `JOIN FETCH p.equipe` em `listarTodos()` e `buscarPorId()` para evitar N+1 ao exibir o nome da equipe na listagem e na tela de gestão. | ⬜ Pendente |

---

## Fase 2 — Controller

| ID | Descrição da Tarefa | Status |
|----|----------------------|--------|
| **TSK-06** | `ProjetoController.criarProjeto()` — adicionar parâmetro `UUID equipeId`. Implementar método privado `resolverEquipe(UUID equipeId)` que valida existência e presença de ao menos 1 membro antes de retornar a entidade `Equipe`. Setar `projeto.setEquipe(equipe)` antes de persistir. | ⬜ Pendente |
| **TSK-07** | `ProjetoController.atualizarProjeto()` — adicionar parâmetro `UUID equipeId`. Chamar `resolverEquipe()` e setar a equipe no projeto antes de persistir via `projetoRepo.atualizar()`. | ⬜ Pendente |
| **TSK-08** | `ProjetoController` — remover `atribuirEquipe(UUID, UUID)`. O vínculo agora é gerenciado diretamente em `criarProjeto()` e `atualizarProjeto()`. | ⬜ Pendente |

---

## Fase 3 — UI: Listagem e Criação de Projeto

| ID | Descrição da Tarefa | Status |
|----|----------------------|--------|
| **TSK-09** | `ProjetoPanel` — adicionar coluna `"Equipe"` no `DefaultTableModel` e popular com `p.getEquipe() != null ? p.getEquipe().getNome() : ""` no `carregar()`. Ajustar largura preferencial da coluna. | ⬜ Pendente |
| **TSK-10** | `ProjetoPanel.abrirFormulario()` — adicionar `comboEquipe` (`JComboBox<OpcaoItem>`) populado com equipes que possuem ao menos 1 membro. Exibir aviso e bloquear criação se não houver equipes elegíveis. Passar `equipeId` para `ctrl.criarProjeto()`. | ⬜ Pendente |

---

## Fase 4 — UI: Gestão do Projeto

| ID | Descrição da Tarefa | Status |
|----|----------------------|--------|
| **TSK-11** | `GestaoProjetoPanel` — declarar `comboEquipe` (`JComboBox<OpcaoItem>`) e adicioná-lo ao formulário do projeto em `criarBlocoProjet()`, abaixo de `comboGerente`. | ⬜ Pendente |
| **TSK-12** | `GestaoProjetoPanel.carregarProjeto()` — expandir `SwingWorker<Object[], Void>` para carregar `List<Equipe>` em paralelo com projeto e gerentes (`Object[]{p, gerentes, equipes}`). Em `done()`: popular `comboEquipe` e pré-selecionar a equipe atual do projeto. | ⬜ Pendente |
| **TSK-13** | `GestaoProjetoPanel.salvarTudo()` — extrair `equipeId` do `comboEquipe` selecionado e incluí-lo na chamada `projetoCtrl.atualizarProjeto()`. Validar que uma equipe foi selecionada antes de prosseguir. | ⬜ Pendente |
| **TSK-14** | `GestaoProjetoPanel.montarComboResponsavel()` — substituir `usuarioCtrl.listarUsuarios()` (todos) por `equipeCtrl.listarMembros(equipeId)` onde `equipeId` é a equipe atualmente vinculada ao projeto. Em `salvarTudo()`, validar que nenhuma tarefa pendente possui responsável fora da equipe — se houver, exibir mensagem clara antes de abortar. | ⬜ Pendente |

---

## Ferramentas e Componentes Adotados na Sprint

- **Interface Gráfica:** Java Swing (javax.swing)
- **Look & Feel:** FlatLaf 3.4.1 (`FlatDarkLaf`) — herdado das sprints anteriores
- **Persistência:** JPA / Hibernate — ajuste de mapeamento `@ManyToOne` / `@OneToMany`
- **Versionamento de Banco:** Flyway — migration V5
- **Padrão assíncrono:** `SwingWorker<T, Void>` para carregamentos na EDT
- **Build:** Apache Maven 3.x
