# Relatório Semanal de Desenvolvimento - Sprint 6

**Responsável Principal:** Vinícius Baggio  
**Período Avaliado:** 26/04/2026 a 02/05/2026  
**Status do Ciclo:** Concluído  

---

## 1. Resumo Executivo das Avaliações

A Sprint 6 adicionou a camada de relatórios de desempenho ao sistema, completando o requisito funcional de métricas e indicadores previsto no escopo da A3. O ponto central foi a criação do `RelatorioController` como controlador de leitura puro — sem nenhuma operação de escrita — e de um `RelatorioPanel` com três visões analíticas distintas integradas ao menu existente. **Nenhuma migration foi necessária**: todas as métricas são derivadas em memória a partir dos dados já persistidos nas entidades `Projeto`, `Tarefa` e `Usuario`.

Após a entrega inicial, foi realizada uma **revisão arquitetural aprofundada** que identificou 10 pontos de melhoria. Todos foram corrigidos ainda dentro da sprint, resultando em 8 commits de qualidade no branch de avaliação antes do merge final em `master`. As correções cobriram: bug lógico em `isAtrasado()`, conversão dos DTOs para **Java records**, eliminação de **N+1 queries**, prevenção de **EDT violations** via `SwingWorker`, eliminação de vazamento de entidade JPA para a View e melhoria de UX com refresh automático do combo de projetos.

## 2. Diário Histórico de Execuções e Decisões Técnicas

| Data de Registro | Evento Executado / Veredito Arquitetural |
|------------------|------------------------------------------|
| **26/04/2026** | **[DECISÃO TÉCNICA] DTOs como contrato entre camadas:** Em vez de expor entidades JPA diretamente para a View (o que forçaria a View a depender de `EntityManager` ainda aberto), foram criadas as classes `ResumoProjeto` e `CargaUsuario` no pacote `dto`. Ambas são imutáveis (apenas getters, sem setters), inicializadas via construtor com todos os campos obrigatórios. Essa decisão impede que a View acesse lazily associações Hibernate fora de uma sessão ativa. |
| **26/04/2026** | **`RelatorioController` — isolamento da lógica de agregação:** O controller foi implementado sem estado (stateless), instanciando os repositories no construtor e descartando-os após cada operação — seguindo o padrão já adotado em toda a camada de controle do projeto. Os três métodos públicos (`resumoGlobal`, `desempenhoPorProjeto`, `cargaDeTrabalho`) operam exclusivamente em memória após buscar as listas dos repositories, sem queries adicionais. |
| **27/04/2026** | **`resumoGlobal()` — `EnumMap` como estrutura de contagem:** A escolha por `EnumMap<StatusProjeto, Long>` garante que todos os valores de status apareçam na resposta mesmo quando a contagem é zero, eliminando a necessidade de tratamento de `null` na View. O mapa é inicializado com zero para cada enum antes da iteração. |
| **27/04/2026** | **`desempenhoPorProjeto()` — cálculo de percentual excluindo canceladas:** O método `percentualConclusao()` usa como denominador `total - canceladas` (não `total`), pois tarefas canceladas não representam trabalho entregável e inflariam artificialmente o denominador. Se o denominador resultante for zero, retorna 0% em vez de lançar `ArithmeticException`. |
| **28/04/2026** | **`cargaDeTrabalho()` — exclusão de usuários sem tarefas:** O método agrupa por responsável mas só inclui no resultado quem tem ao menos uma tarefa atribuída. Isso mantém o relatório focado em membros com carga efetiva, sem poluir a tabela com linhas de zeros. |
| **29/04/2026** | **`RelatorioPanel` — aba "Resumo Global" com cards visuais:** A aba usa `GridLayout(1, 4)` com quatro cards coloridos (um por status), cada um exibindo o número em fonte 36pt bold e o rótulo em 14pt. As cores foram escolhidas para refletir semanticamente o estado: azul (planejado), verde (em andamento), cinza (concluído), vermelho (cancelado). |
| **30/04/2026** | **`RelatorioPanel` — aba "Desempenho por Projeto" com JComboBox:** O combo é populado ao entrar na aba (via `ChangeListener`) e carrega os dados ao clicar em "Carregar". O item selecionado é um `ProjetoOpcao` (record DTO) que mantém o `UUID` para uso na chamada ao controller, exibindo apenas o nome como `toString()`. |
| **01/05/2026** | **`RelatorioPanel` — aba "Carga de Trabalho" com ordenação:** A `JTable` usa `setAutoCreateRowSorter(true)`, permitindo ao usuário ordenar por qualquer coluna sem código adicional. |
| **01/05/2026** | **Integração ao `MainFrame`:** O item "Relatórios" foi adicionado ao menu `Operações` separado dos itens de CRUD por um `addSeparator()`. A janela abre como `JFrame` singleton com dimensão `860×540`. |
| **02/05/2026** | **[REVISÃO ARQUITETURAL] Bug `isAtrasado()` para projetos CANCELADO:** O método original ignorava o `status` do projeto, marcando projetos cancelados como atrasados quando a `dataFim` superava a `dataPrevisao`. Correção: adicionada guarda `status == StatusProjeto.CANCELADO → return false`. Projetos CONCLUÍDO que terminaram após a previsão continuam sendo marcados como atrasados (informação histórica válida). |
| **02/05/2026** | **[REVISÃO ARQUITETURAL] Conversão de DTOs para Java records:** `ResumoProjeto` e `CargaUsuario` foram convertidos de classes com construtores e getters boilerplate para **Java records** nativos. Benefícios: imutabilidade garantida pelo compilador, acessores sem prefixo `get` (convenção de records), eliminação de ~60 linhas de código. O `CargaUsuario` ganhou o campo `tarefasCanceladas` que estava ausente, fechando a inconsistência de soma `pendentes + emAndamento + concluidas ≠ total`. |
| **02/05/2026** | **[REVISÃO ARQUITETURAL] DTO `ProjetoOpcao` — eliminação de entity leakage:** O método `listarProjetos()` retornava `List<Projeto>` (entidade JPA) para a View — risco de `LazyInitializationException` em qualquer acesso a associação lazy após o `EntityManager` fechado. Substituído por `listarProjetosParaCombo()` retornando `List<ProjetoOpcao>` (record leve com apenas `UUID id` e `String nome`). O `JComboBox` usa `ProjetoOpcao.toString()` → `nome` para exibição. |
| **02/05/2026** | **[REVISÃO ARQUITETURAL] Eliminação de N+1 em `cargaDeTrabalho()`:** A versão original iterava usuários e para cada um disparava uma query de tarefas via `listarPorResponsavel()` → O(N) round-trips. Refatorado para `TarefaRepository.listarComResponsavel()` (única query com `LEFT JOIN FETCH t.responsavel`) + agrupamento em memória por `t.getResponsavel().getId()` usando `LinkedHashMap` para preservar ordem de inserção. |
| **02/05/2026** | **[REVISÃO ARQUITETURAL] `resumoGlobal()` com JPQL GROUP BY:** A versão original carregava todas as entidades `Projeto` em memória apenas para contar por status. Substituído por `ProjetoRepository.contarPorStatus()` com query JPQL `SELECT p.status, COUNT(p) FROM Projeto p GROUP BY p.status` — retorna no máximo 4 linhas independente do volume de projetos. |
| **02/05/2026** | **[REVISÃO ARQUITETURAL] EDT violations corrigidas via SwingWorker:** As três abas do `RelatorioPanel` executavam queries JPA diretamente na Event Dispatch Thread, travando a UI durante o carregamento. Cada operação foi encapsulada em `SwingWorker<T, Void>`: `doInBackground()` executa a query fora da EDT, `done()` atualiza os componentes Swing de volta na EDT. Os botões de ação são desabilitados durante o carregamento e reabilitados em `done()`. |
| **02/05/2026** | **[REVISÃO ARQUITETURAL] Auto-refresh do combo e null guard:** `cbProjeto` foi promovido a campo de instância. `ChangeListener` adicionado ao `JTabbedPane` no construtor — ao entrar na aba índice 1, `popularComboProjetos()` é chamado automaticamente via SwingWorker, garantindo que projetos criados em outra janela apareçam sem reiniciar o módulo. Null guard (`if (cbProjeto == null) return`) adicionado como salvaguarda para chamadas antes da inicialização do componente. |

## 3. Registros de Desafios Enfrentados

O principal cuidado na implementação inicial foi evitar `LazyInitializationException` ao acessar associações de entidades JPA fora do contexto de persistência. Como a View opera depois que o `EntityManager` da query já foi fechado (padrão de todos os repositories do projeto), qualquer acesso a `t.getProjeto().getNome()` dentro do `RelatorioController` poderia falhar. A solução foi calcular todas as métricas dentro do controller enquanto as entidades ainda estão em contexto, e só depois construir os DTOs — que trafegam sem dependência de sessão JPA para a View.

Na revisão arquitetural pós-entrega, o principal aprendizado foi a identificação de **EDT violations silenciosas**: o código funcionava corretamente em ambiente local com banco de dados na mesma máquina (latência < 1ms), mas qualquer latência real de rede ou volume de dados causaria congelamento total da UI. O encapsulamento em `SwingWorker` é mandatório para qualquer acesso a I/O em aplicações Swing, independente do ambiente de desenvolvimento.

## 4. Artefatos Entregues

| Artefato | Caminho |
|----------|---------|
| DTO de métricas de projeto (Java record) | `src/main/java/.../dto/ResumoProjeto.java` |
| DTO de carga de trabalho (Java record) | `src/main/java/.../dto/CargaUsuario.java` |
| DTO leve para combo de projetos (Java record) | `src/main/java/.../dto/ProjetoOpcao.java` |
| Controller de relatórios (stateless, read-only) | `src/main/java/.../controller/RelatorioController.java` |
| Painel de relatórios com SwingWorker | `src/main/java/.../view/RelatorioPanel.java` |
| Integração ao menu principal | `src/main/java/.../view/MainFrame.java` |
| Query GROUP BY para contagem de projetos | `src/main/java/.../repository/ProjetoRepository.java` |
| Query com JOIN FETCH para carga de trabalho | `src/main/java/.../repository/TarefaRepository.java` |

## 5. Métricas da Revisão Arquitetural

| Categoria | Issues Identificados | Issues Corrigidos |
|-----------|---------------------|-------------------|
| Bugs críticos | 2 (`isAtrasado()` + EDT) | 2 ✅ |
| Performance (N+1 / queries) | 2 | 2 ✅ |
| Segurança arquitetural (entity leakage) | 1 | 1 ✅ |
| Consistência de dados (CargaUsuario) | 1 | 1 ✅ |
| Refactor / qualidade (records, convenções) | 4 | 4 ✅ |
| **Total** | **10** | **10** ✅ |
