# Relatório Semanal de Desenvolvimento - Sprint 6

**Responsável Principal:** Vinícius Baggio  
**Período Avaliado:** 26/04/2026 a 02/05/2026  
**Status do Ciclo:** Concluído  

---

## 1. Resumo Executivo das Avaliações

A Sprint 6 adicionou a camada de relatórios de desempenho ao sistema, completando o requisito funcional de métricas e indicadores previsto no escopo da A3. O ponto central foi a criação do `RelatorioController` como controlador de leitura puro — sem nenhuma operação de escrita — e de um `RelatorioPanel` com três visões analíticas distintas integradas ao menu existente. **Nenhuma migration foi necessária**: todas as métricas são derivadas em memória a partir dos dados já persistidos nas entidades `Projeto`, `Tarefa` e `Usuario`.

## 2. Diário Histórico de Execuções e Decisões Técnicas

| Data de Registro | Evento Executado / Veredito Arquitetural |
|------------------|------------------------------------------|
| **26/04/2026** | **[DECISÃO TÉCNICA] DTOs como contrato entre camadas:** Em vez de expor entidades JPA diretamente para a View (o que forçaria a View a depender de `EntityManager` ainda aberto), foram criadas as classes `ResumoProjeto` e `CargaUsuario` no pacote `dto`. Ambas são imutáveis (apenas getters, sem setters), inicializadas via construtor com todos os campos obrigatórios. Essa decisão impede que a View acesse lazily associações Hibernate fora de uma sessão ativa. |
| **26/04/2026** | **`RelatorioController` — isolamento da lógica de agregação:** O controller foi implementado sem estado (stateless), instanciando os repositories no construtor e descartando-os após cada operação — seguindo o padrão já adotado em toda a camada de controle do projeto. Os três métodos públicos (`resumoGlobal`, `desempenhoPorProjeto`, `cargaDeTrabalho`) operam exclusivamente em memória após buscar as listas dos repositories, sem queries adicionais. |
| **27/04/2026** | **`resumoGlobal()` — `EnumMap` como estrutura de contagem:** A escolha por `EnumMap<StatusProjeto, Long>` garante que todos os valores de status apareçam na resposta mesmo quando a contagem é zero, eliminando a necessidade de tratamento de `null` na View. O mapa é inicializado com zero para cada enum antes da iteração. |
| **27/04/2026** | **`desempenhoPorProjeto()` — cálculo de percentual excluindo canceladas:** O método `ResumoProjeto.getPercentualConclusao()` usa como denominador `total - canceladas` (não `total`), pois tarefas canceladas não representam trabalho entregável e inflariam artificialmente o denominador. Se o denominador resultante for zero, retorna 0% em vez de lançar `ArithmeticException`. |
| **28/04/2026** | **`cargaDeTrabalho()` — exclusão de usuários sem tarefas:** O método itera todos os usuários mas só inclui no resultado quem tem ao menos uma tarefa atribuída (`if (tarefas.isEmpty()) continue`). Isso mantém o relatório focado em membros com carga efetiva, sem poluir a tabela com linhas de zeros. |
| **29/04/2026** | **`RelatorioPanel` — aba "Resumo Global" com cards visuais:** A aba usa `GridLayout(1, 4)` com quatro cards coloridos (um por status), cada um exibindo o número em fonte 36pt bold e o rótulo em 14pt. As cores foram escolhidas para refletir semanticamente o estado: azul (planejado), verde (em andamento), cinza (concluído), vermelho (cancelado). O botão "Atualizar" recarrega os dados sem recriar o painel. |
| **30/04/2026** | **`RelatorioPanel` — aba "Desempenho por Projeto" com JComboBox:** O combo é populado na construção do painel e ao clicar em "Carregar" — não há auto-refresh para evitar queries desnecessárias. O item selecionado é um `ProjetoItem` (classe interna) que mantém o `UUID` do projeto para uso na chamada ao controller, exibindo apenas o nome como `toString()`. |
| **01/05/2026** | **`RelatorioPanel` — aba "Carga de Trabalho" com ordenação:** A `JTable` usa `setAutoCreateRowSorter(true)`, permitindo ao usuário ordenar por qualquer coluna (ex: "Total Ativas" decrescente) sem código adicional. |
| **01/05/2026** | **Integração ao `MainFrame`:** O item "Relatórios" foi adicionado ao menu `Operações` separado dos itens de CRUD (`Projetos`, `Tarefas`) por um `addSeparator()`, sinalizando visualmente que é uma funcionalidade de leitura e não de escrita. A janela abre como `JFrame` singleton (comportamento padrão do `abrirTela()`), com dimensão `860×540`. |

## 3. Registros de Desafios Enfrentados

O principal cuidado foi evitar `LazyInitializationException` ao acessar associações de entidades JPA fora do contexto de persistência. Como a View opera depois que o `EntityManager` da query já foi fechado (padrão de todos os repositories do projeto), qualquer acesso a `t.getProjeto().getNome()` dentro do `RelatorioController` poderia falhar. A solução foi calcular todas as métricas dentro do controller enquanto as entidades ainda estão em contexto, e só depois construir os DTOs — que trafegam sem dependência de sessão JPA para a View.

## 4. Artefatos Entregues

| Artefato | Caminho |
|----------|---------|
| DTO de métricas de projeto | `src/main/java/.../dto/ResumoProjeto.java` |
| DTO de carga de trabalho | `src/main/java/.../dto/CargaUsuario.java` |
| Controller de relatórios | `src/main/java/.../controller/RelatorioController.java` |
| Painel de relatórios (View) | `src/main/java/.../view/RelatorioPanel.java` |
| Integração ao menu principal | `src/main/java/.../view/MainFrame.java` |
