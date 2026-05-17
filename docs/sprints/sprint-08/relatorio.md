# Relatório Semanal de Desenvolvimento - Sprint 8

**Responsável Principal:** Vinícius Baggio  
**Período Avaliado:** 14/05/2026 a 15/05/2026  
**Status do Ciclo:** Concluído  

---

## 1. Resumo Executivo das Avaliações

A Sprint 8 corrigiu um problema de modelagem estrutural identificado ao final da Sprint 7: o vínculo entre `Equipe` e `Projeto` estava modelado como ManyToMany (tabela `equipe_projeto`), mas a regra de negócio é que um projeto pertence a exatamente uma equipe. A sprint também introduziu a restrição de que responsáveis de tarefa devem ser membros da equipe do projeto.

A **Fase 1** aplicou a correção no banco e nas entidades: a migration V5 dropa `equipe_projeto` e adiciona `equipe_id` em `projeto`; as entidades `Projeto` e `Equipe` foram ajustadas para `@ManyToOne` e `@OneToMany(mappedBy)` respectivamente; repositories atualizados com `JOIN FETCH p.equipe` para evitar N+1.

A **Fase 2** ajustou `ProjetoController`: `criarProjeto()` e `atualizarProjeto()` passaram a receber `UUID equipeId` e um método privado `resolverEquipe()` valida existência e presença de ao menos um membro antes de vincular. O método `atribuirEquipe()` foi removido — o vínculo passou a ser gerenciado no próprio ciclo de criação/edição do projeto.

A **Fase 3** atualizou a UI de listagem e criação: `ProjetoPanel` ganhou coluna `Equipe` na tabela e `comboEquipe` no formulário de criação, com carregamento assíncrono via `SwingWorker` e bloqueio de criação quando não há equipes elegíveis.

A **Fase 4** aplicou refinamentos de integridade identificados por council de revisão antes da conclusão da UI: `LEFT JOIN FETCH p.equipe` adicionado a `listarPorGerente()` (que havia sido omitido), e a migration V6 aplicou `NOT NULL` em `gerente_id` e `equipe_id` com backfill defensivo, além de criar os quatro índices FK ausentes no schema.

A **Fase 5** completou a UI em `GestaoProjetoPanel`: `comboEquipe` adicionado ao formulário do projeto, combo de responsável de tarefa filtrado para exibir apenas membros da equipe selecionada, e validação de troca de equipe que aborta se alguma tarefa possuir responsável fora da nova equipe — exibindo mensagem com os nomes dos responsáveis afetados.

A **Fase 6** reuniu correções identificadas durante a execução: eliminação de query duplicada em `resolverEquipe()`, adição de backfill ao V6 (que falhava em bases com dados pré-existentes), remoção do bloqueio excessivo de edição em tarefas com status `CONCLUIDA` ou `CANCELADA`, e implementação do bloqueio de remoção de membro que seja responsável por tarefa em projeto da equipe.

---

## 2. Diário Histórico de Execuções e Decisões Técnicas

| Data de Registro | Evento Executado / Veredito Arquitetural |
|------------------|------------------------------------------|
| **14/05/2026** | **Migration V5 — correção do modelo ManyToMany:** `V5__Vinculo_equipe_projeto.sql` executa `ALTER TABLE projeto ADD COLUMN equipe_id UUID REFERENCES equipe(id)` seguido de `DROP TABLE equipe_projeto`. A coluna foi adicionada sem `NOT NULL` para não bloquear bases existentes — a restrição foi planejada para uma segunda migration. |
| **14/05/2026** | **Entidade `Projeto` — `@ManyToOne`:** Adicionado campo `@ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "equipe_id") private Equipe equipe` com getters/setters correspondentes. O fetch `LAZY` foi escolhido para não impactar queries que não precisam da equipe. |
| **14/05/2026** | **Entidade `Equipe` — `@OneToMany`:** Substituído `@ManyToMany` + `@JoinTable` por `@OneToMany(mappedBy = "equipe") private List<Projeto> projetos`. Método `addProjeto(Projeto)` removido — a bidirecionalidade passa a ser mantida apenas pelo lado dono (`Projeto.equipe`). |
| **14/05/2026** | **`ProjetoRepository` — `JOIN FETCH p.equipe`:** Adicionado `JOIN FETCH p.equipe` em `listarTodos()`, `buscarPorId()` e `listarPorStatus()` para evitar `LazyInitializationException` ao acessar `p.getEquipe().getNome()` fora de contexto JPA. |
| **14/05/2026** | **`ProjetoController` — `resolverEquipe()` e remoção de `atribuirEquipe()`:** `criarProjeto()` e `atualizarProjeto()` passaram a receber `UUID equipeId`. Método privado `resolverEquipe(UUID)` centraliza a busca e a validação (equipe existe e possui ao menos 1 membro). `atribuirEquipe(UUID, UUID)` foi removido — era o único ponto de vínculo no modelo antigo e perdeu sentido com o novo fluxo. |
| **14/05/2026** | **`EquipeController` — mensagens de erro atualizadas:** Referências ao modelo ManyToMany nas mensagens de exceção foram atualizadas para refletir o novo modelo — sem impacto funcional, mas necessário para coerência nos logs e na UI. |
| **14/05/2026** | **`ProjetoPanel` — coluna Equipe e `comboEquipe` na criação:** `DefaultTableModel` recebeu a coluna `Equipe` com valor de `p.getEquipe().getNome()`. Formulário de criação ganhou `JComboBox<OpcaoItem>` populado com equipes que possuem ao menos 1 membro. Se não houver equipes elegíveis, o botão `Novo` exibe aviso e aborta. |
| **14/05/2026** | **[DECISÃO TÉCNICA] `SwingWorker` para carregamento de equipes em `ProjetoPanel`:** O carregamento inicial das equipes elegíveis estava ocorrendo na EDT durante a construção do painel. Identificado que qualquer acesso JPA na EDT é uma violação do padrão adotado na Sprint 7. Movido para `doInBackground()` de um `SwingWorker` dedicado. |
| **14/05/2026** | **`ProjetoRepository.listarPorGerente()` — `LEFT JOIN FETCH` ausente (council):** Identificado por council de revisão que `listarPorGerente()` não possuía `JOIN FETCH p.equipe`, divergindo dos demais métodos e causando `LazyInitializationException` na tela de relatórios ao acessar `p.getEquipe()`. Corrigido antes da conclusão da UI. |
| **14/05/2026** | **Migration V6 — `NOT NULL` e índices FK:** `V6__Integridade_projeto.sql` aplica `NOT NULL` em `gerente_id` e `equipe_id`, e cria os índices `idx_projeto_equipe_id`, `idx_projeto_gerente_id`, `idx_tarefa_projeto_id` e `idx_tarefa_responsavel_id` — quatro índices em colunas FK que estavam ausentes desde a criação do schema. |
| **14/05/2026** | **`GestaoProjetoPanel` — `comboEquipe` e filtro de responsável:** `comboEquipe` adicionado ao bloco de dados do projeto; `montarComboResponsavel()` passou a chamar `equipeCtrl.listarMembros(equipeId)` em vez de `usuarioCtrl.listarUsuarios()` — garantindo que o combo de responsável liste apenas membros da equipe vinculada ao projeto. Gerente não precisa ser membro da equipe — a restrição aplica-se apenas a responsáveis de tarefa. |
| **14/05/2026** | **`GestaoProjetoPanel.salvarTudo()` — validação de troca de equipe:** Quando a equipe do projeto é alterada, `salvarTudo()` verifica se alguma tarefa possui responsável fora dos membros da nova equipe antes de persistir. Se houver conflito, exibe mensagem listando os responsáveis afetados e aborta a operação sem gravar. |
| **14/05/2026** | **Refactor — eliminação de query duplicada em `resolverEquipe()`:** A validação de ao menos 1 membro realizava duas queries separadas (busca da equipe + contagem de membros). Refatorado para uma única query que retorna a equipe com membros via `JOIN FETCH`, eliminando o hit extra ao banco. |
| **14/05/2026** | **Bugfix — V6 falhava em bases com dados pré-existentes:** A migration V6 tentava aplicar `NOT NULL` em `equipe_id` e `gerente_id` sem garantir que todas as linhas possuíam valor. Em bases criadas antes da Sprint 8, `equipe_id` era nulo em todos os projetos. Adicionado bloco de backfill antes das constraints: projetos sem `equipe_id` recebem a primeira equipe disponível; projetos sem `gerente_id` recebem o primeiro gerente disponível (defensivo). |
| **14/05/2026** | **Bugfix — edição de tarefa bloqueada para status `CONCLUIDA`/`CANCELADA`:** `TarefaController.atualizarTarefa()` lançava `IllegalStateException` para tarefas com esses status, impedindo que o gerente corrigisse dados de tarefas já finalizadas. A restrição foi identificada como excessiva para o fluxo de gestão e removida. |
| **15/05/2026** | **`EquipeController` — bloqueio de remoção de membro responsável:** Adicionada validação em `removerMembro(UUID equipeId, UUID usuarioId)`: antes de remover, verifica se o usuário é responsável por alguma tarefa em projeto da equipe. Se for, lança `IllegalStateException` com mensagem listando as tarefas afetadas. Impede que a remoção gere dados inconsistentes (tarefas com responsável fora da equipe do projeto). |
| **17/05/2026** | **Bugfix — `reatribuirResponsavel` lançava `IllegalStateException` ao editar tarefa finalizada:** `salvarTudo()` em `GestaoProjetoPanel` chama `reatribuirResponsavel()` para toda tarefa no staging de edições, mesmo quando só o status foi alterado. O método ainda possuía um guard que bloqueava CONCLUIDA/CANCELADA — inconsistente com a permissão de edição introduzida no TSK-19, que havia removido a restrição apenas de `atualizarTarefa()`. Guard removido de `reatribuirResponsavel()`. |

---

## 3. Registros de Desafios Enfrentados

O principal desafio técnico da sprint foi a migration V6. A aplicação de `NOT NULL` em colunas FK que podiam ser nulas em dados históricos exigia um passo de backfill antes das constraints — o que não é óbvio à primeira leitura do script. A primeira versão do V6 não incluía o backfill e falhava ao tentar aplicar o `NOT NULL` em bases onde a V5 havia rodado sem nenhum projeto sendo criado após a adição de `equipe_id`. A solução foi adicionar dois blocos `UPDATE` antes das constraints: um para `equipe_id` (projetos sem equipe recebem a primeira equipe disponível por nome) e um para `gerente_id` (defensivo, pois o campo já era obrigatório na UI desde o início, mas o schema não impunha a restrição).

O segundo desafio foi o `LazyInitializationException` silencioso em `listarPorGerente()`. O método era funcional desde a Sprint 3, mas a adição da coluna Equipe na tela de projetos criou um novo ponto de acesso a `p.getEquipe()` que antes não existia. A ausência de `JOIN FETCH p.equipe` só se manifestava ao navegar para a tela de relatórios como gerente — um caminho de uso raramente testado durante o desenvolvimento das telas de CRUD. O padrão estabelecido de adicionar `JOIN FETCH` a todos os métodos de listagem precisava ter sido aplicado preventivamente.

O terceiro desafio foi o design da validação de troca de equipe em `salvarTudo()`. A regra exige que, ao trocar a equipe de um projeto, nenhuma tarefa existente possua responsável fora dos membros da nova equipe. A implementação precisava distinguir entre a equipe atual do projeto (antes de salvar) e a equipe selecionada no combo (após a troca), cruzar com a lista de membros da nova equipe e, em caso de conflito, apresentar uma mensagem útil com os nomes dos responsáveis afetados — sem gravar nada. A solução foi comparar o `equipeId` atual do projeto com o `equipeId` selecionado antes de chamar o controller, e executar a validação somente se houve mudança.

---

## 4. Artefatos Entregues

| Artefato | Caminho | Operação |
|----------|---------|----------|
| Migration V5 — correção ManyToMany para ManyToOne | `src/main/resources/db/migration/V5__Vinculo_equipe_projeto.sql` | Criado |
| Migration V6 — NOT NULL, backfill e índices FK | `src/main/resources/db/migration/V6__Integridade_projeto.sql` | Criado |
| Entidade `Projeto` com `@ManyToOne Equipe` | `src/main/java/.../model/entity/Projeto.java` | Modificado |
| Entidade `Equipe` com `@OneToMany projetos` | `src/main/java/.../model/entity/Equipe.java` | Modificado |
| `ProjetoRepository` com `JOIN FETCH p.equipe` | `src/main/java/.../repository/ProjetoRepository.java` | Modificado |
| `EquipeRepository` — remoção de `adicionarProjeto()` | `src/main/java/.../repository/EquipeRepository.java` | Modificado |
| `ProjetoController` — `equipeId` em criar/atualizar, `resolverEquipe()`, remoção de `atribuirEquipe()` | `src/main/java/.../controller/ProjetoController.java` | Modificado |
| `EquipeController` — mensagens e bloqueio de remoção de membro responsável | `src/main/java/.../controller/EquipeController.java` | Modificado |
| `TarefaController` — remoção de bloqueio de edição por status | `src/main/java/.../controller/TarefaController.java` | Modificado |
| `ProjetoPanel` — coluna Equipe e `comboEquipe` na criação | `src/main/java/.../view/ProjetoPanel.java` | Modificado |
| `GestaoProjetoPanel` — `comboEquipe`, filtro de responsável e validação de troca de equipe | `src/main/java/.../view/GestaoProjetoPanel.java` | Modificado |
| `TarefaController.reatribuirResponsavel()` — remoção de guard por status | `src/main/java/.../controller/TarefaController.java` | Modificado |

---

## 5. Métricas da Correção de Modelo

| Categoria | Antes | Depois |
|-----------|-------|--------|
| Modelo de vínculo Equipe↔Projeto | ManyToMany (`equipe_projeto`) | ManyToOne (`equipe_id` em `projeto`) |
| Tabelas no schema | 7 (incluindo `equipe_projeto`) | 6 |
| Índices FK presentes | 0 (nenhuma coluna FK indexada) | 4 (`equipe_id`, `gerente_id`, `projeto_id`, `responsavel_id`) |
| Responsável de tarefa | Qualquer usuário do sistema | Apenas membros da equipe do projeto |
| Ponto de vínculo de equipe ao projeto | `atribuirEquipe()` separado | Integrado a `criarProjeto()` / `atualizarProjeto()` |
| Commits da sprint | — | 15 |
