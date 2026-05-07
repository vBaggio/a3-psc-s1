# Design — Tela de Gestão de Projeto Atômica

**Data:** 2026-05-07  
**Arquivo alvo:** `GestaoProjetoPanel.java`  
**Status:** aprovado

---

## Contexto

A tela de gestão de projeto (`GestaoProjetoPanel`) apresenta dois problemas:

1. O botão "Salvar Projeto" fica no meio da tela, visualmente separando o formulário do projeto da tabela de tarefas.
2. Operações de tarefa (criar, editar, excluir, mudar status) persistem diretamente no banco de dados, sem relação com o salvamento do projeto.

## Objetivo

Tornar a tela atômica: todas as mudanças — dados do projeto e operações de tarefa — são persistidas juntas em um único Salvar.

---

## Layout

`GestaoProjetoPanel` usa `BorderLayout`:

- **NORTH** — formulário do projeto (sem botão Salvar)
- **CENTER** — bloco de tarefas com toolbar (Nova / Editar / Excluir)
- **SOUTH** — rodapé com `[Cancelar]` à esquerda e `[Salvar]` à direita

---

## Modelo de Staging

Três coleções privadas no painel rastreiam o estado pendente:

```java
private final List<DadosTarefa>      tarefasNovas     = new ArrayList<>();
private final Map<UUID, DadosTarefa> tarefasEditadas  = new LinkedHashMap<>();
private final Set<UUID>              tarefasExcluidas = new LinkedHashSet<>();
```

`DadosTarefa` é um record com os campos editáveis da tarefa:

```java
record DadosTarefa(String nome, String descricao, LocalDate prazo, UUID responsavelId, StatusTarefa status) {}
```

Tarefas novas (ainda sem UUID no banco) são identificadas por um UUID temporário gerado localmente e armazenado na coluna oculta da tabela. O prefixo `tmp-` ou a presença do UUID em `tarefasNovas` distingue novos de existentes.

---

## Fluxo de Cada Operação

| Ação | Comportamento anterior | Comportamento novo |
|---|---|---|
| Nova Tarefa | persiste no banco | adiciona `DadosTarefa` em `tarefasNovas`; insere linha na table com UUID temporário |
| Editar (botão / duplo clique) | persiste no banco | upserta em `tarefasEditadas`; atualiza linha na table |
| Status inline (combo na coluna) | persiste no banco | upserta em `tarefasEditadas`; atualiza célula |
| Excluir | persiste no banco | remove linha da table; adiciona UUID em `tarefasExcluidas` |
| Salvar | só salvava projeto | persiste projeto + aplica todo staging em um `SwingWorker` |
| Cancelar | não existia | limpa as 3 coleções e recarrega projeto + tarefas do banco |

---

## Edge Cases

| Situação | Resolução |
|---|---|
| Tarefa nova criada e depois excluída (antes de Salvar) | Remove de `tarefasNovas`; não entra em `tarefasExcluidas` |
| Tarefa nova editada (antes de Salvar) | Atualiza o `DadosTarefa` diretamente em `tarefasNovas` |
| Tarefa existente excluída que estava em `tarefasEditadas` | Remove de `tarefasEditadas`; adiciona UUID em `tarefasExcluidas` |

---

## Diálogo de Edição de Tarefa

Ganha o campo **Status** com `JComboBox<StatusTarefa>` populado com o status atual + próximos estados válidos (mesma lógica do editor inline). Isso elimina a necessidade de dois caminhos separados para alterar status.

---

## Aviso de Alterações Não Salvas

`GestaoProjetoPanel` expõe:

```java
public boolean temAlteracoesPendentes()
```

Retorna `true` se qualquer das 3 coleções não estiver vazia ou se os campos do formulário diferem do estado carregado.

A tela hospedeira (quem exibe este painel via `CardLayout` ou similar) chama esse método antes de navegar e exibe:

> "Há alterações não salvas. Deseja sair sem salvar?"  
> `[Sair sem salvar]` · `[Cancelar]`

---

## Ordem de Persistência no Salvar

O `SwingWorker` executa na ordem:

1. `projetoCtrl.atualizarProjeto(...)` — dados do formulário
2. Transição de status do projeto (se mudou)
3. Para cada tarefa em `tarefasNovas` → `tarefaCtrl.criarTarefa(...)`
4. Para cada entrada em `tarefasEditadas` → `tarefaCtrl.atualizarTarefa(...)` + `tarefaCtrl.atualizarStatus(...)` se status mudou
5. Para cada UUID em `tarefasExcluidas` → `tarefaCtrl.removerTarefa(...)`
6. Em `done()`: chama `onSalvar`, recarrega projeto e tarefas, limpa staging

Em caso de erro em qualquer etapa: exibe mensagem e mantém o staging (o usuário pode corrigir e tentar de novo).

---

## O que NÃO muda

- Estrutura dos controllers (`ProjetoController`, `TarefaController`)
- Modelo de dados (`Projeto`, `Tarefa`)
- `TableUtils`, `DateUtils`, `OpcaoItem`
- Lógica de `configurarTabela()` (editor inline continua existindo, mas agora grava em staging)
