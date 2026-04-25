# Relatório Semanal de Desenvolvimento - Sprint 5

**Responsável Principal:** Vinícius Baggio  
**Período Avaliado:** 19/04/2026 a 25/04/2026  
**Status do Ciclo:** Concluído  

---

## 1. Resumo Executivo das Avaliações

A Sprint 5 entregou a camada View completa do sistema, concluindo o ciclo MVC iniciado na Sprint 2. O ponto central foi a integração dos cinco controllers já existentes com uma interface gráfica desktop construída em Java Swing, adotando FlatLaf como Look & Feel para prover uma aparência moderna ao sistema. A navegação foi implementada com `MainFrame` (CardLayout: login ↔ home), menu bar por domínio (`Cadastros | Operações`) e janelas independentes por módulo — cada tela carrega seus dados apenas quando aberta, eliminando queries desnecessárias no startup. Foram também realizados ajustes de qualidade no backend: correção de N+1 queries via `LEFT JOIN FETCH` nos repositories, e adição de migration de seed com usuário administrador padrão para primeiro acesso.

## 2. Diário Histórico de Execuções e Decisões Técnicas

| Data de Registro | Evento Executado / Veredito Arquitetural |
|------------------|------------------------------------------|
| **19/04/2026** | **[DECISÃO TÉCNICA] FlatLaf 3.4.1 como Look & Feel:** A dependência `com.formdev:flatlaf:3.4.1` foi adicionada ao `pom.xml`. O tema `FlatDarkLaf` foi escolhido por oferecer contraste superior para uso prolongado e por ser o padrão mais common em aplicações desktop modernas. A inicialização é feita em `MainFrame.iniciar()` antes da criação de qualquer componente Swing, garantindo que o L&F esteja aplicado globalmente. |
| **19/04/2026** | **`MainFrame` + `Application.java` — substituição do smoke test:** O `CardLayout` com dois cartões (`login` e `app`) elimina a necessidade de múltiplas janelas, mantendo o estado da aplicação centralizado no frame. O smoke test que existia em `Application.main()` foi completamente substituído por `SwingUtilities.invokeLater(() -> new MainFrame().setVisible(true))`, respeitando a thread de despacho de eventos do Swing (EDT). |
| **19/04/2026** | **`LoginPanel` — autenticação integrada ao controller:** O painel de login delega diretamente a `UsuarioController.autenticar(login, senha)`. Em caso de falha, a mensagem de negócio lançada pelo controller é exibida inline (campo `lblErro` em vermelho), sem uso de janelas modais. A tecla Enter no campo de senha aciona o login, além do botão. |
| **20/04/2026** | **`DashboardPanel` — estrutura de navegação pós-login:** O header exibe o nome e perfil do usuário logado, reforçando o contexto de sessão. O botão "Sair" invoca `MainFrame.mostrarLogin()`, que reconstrói o `LoginPanel` e descarta a instância do `DashboardPanel` da memória, evitando que dados de sessão anteriores sejam visíveis após um novo login. |
| **20/04/2026** | **Padrão unificado de painéis de entidade:** Todos os cinco painéis (`CargoPanel`, `UsuarioPanel`, `ProjetoPanel`, `EquipePanel`, `TarefaPanel`) seguem o mesmo esqueleto: `BorderLayout` com toolbar ao norte, `JScrollPane` + `JTable` no centro, `DefaultTableModel` com `isCellEditable` retornando `false` (tabela somente-leitura), e coluna de ID oculta para uso interno sem exposição na UI. Erros de negócio capturados como `Exception` são exibidos via `JOptionPane.ERROR_MESSAGE`. |
| **21/04/2026** | **`CargoPanel` e `UsuarioPanel` — CRUD completo:** O `UsuarioPanel` gerencia o combo de cargos dinamicamente, carregando a lista de `CargoController.listarCargos()` a cada abertura de formulário, garantindo que novos cargos recém-cadastrados estejam disponíveis para seleção. Edição de usuário mantém a senha atual quando o campo de nova senha é deixado em branco. |
| **22/04/2026** | **`ProjetoPanel` — separação entre criação e transição de status:** A decisão de não oferecer edição de campos de projeto (nome, datas, gerente) após criação foi intencional: os controllers não expõem um método `atualizarProjeto()` genérico, e introduzir um seria escopo de uma sprint futura. O painel concentra as operações que o controller suporta — criação, `atualizarStatus()` e `encerrarProjeto()`. |
| **23/04/2026** | **`EquipePanel` — split pane com gestão de membros:** A escolha do `JSplitPane` horizontal permite ao usuário visualizar simultaneamente a lista de equipes e os membros da equipe selecionada, sem necessidade de diálogos adicionais. A lista de candidatos a membro exclui automaticamente quem já pertence à equipe, filtrando no lado Java sem nova query. |
| **24/04/2026** | **`TarefaPanel` — filtro por projeto com combo dinâmico:** O painel lista tarefas filtradas pelo projeto selecionado no combo superior, recarregando a cada seleção. O botão `↻` força a atualização do combo de projetos (útil quando um novo projeto é criado na aba Projetos sem reiniciar o painel). A criação de tarefa é bloqueada com aviso se nenhum projeto estiver selecionado. |
| **25/04/2026** | **Migration `V3__Seed_admin.sql` — usuário administrador padrão:** O banco subia vazio, impossibilitando o primeiro acesso ao sistema. Foi adicionada uma migration de seed com usuário `admin` / senha `123` (hash BCrypt pré-gerado) e perfil `ADMINISTRADOR`, aplicada automaticamente pelo Flyway na inicialização. |
| **25/04/2026** | **[REFACTOR] Navegação por janelas independentes — eliminação do `JTabbedPane`:** A arquitetura inicial carregava todos os cinco painéis na criação do dashboard, gerando 5 SELECTs desnecessários no login. A solução adotada foi substituir o `JTabbedPane` por um `HomePanel` com cards por módulo e uma `JMenuBar` com menus `Cadastros` e `Operações`. Cada módulo abre em `JFrame` próprio (singleton — segunda abertura traz ao foco), e os dados são carregados apenas quando a janela é instanciada. O `DashboardPanel` foi removido. |
| **25/04/2026** | **`LEFT JOIN FETCH` para eliminar N+1 queries:** Os métodos `listarTodos()` / `listarPorPerfil()` de `UsuarioRepository`, `listarTodos()` / `listarPorStatus()` de `ProjetoRepository` e `listarPorProjeto()` de `TarefaRepository` executavam queries individuais para cada associação lazy (`u.getCargo()`, `p.getGerente()`, `t.getResponsavel()`). A correção foi adicionar `LEFT JOIN FETCH` nas queries JPQL, garantindo que cada listagem resulte em exatamente 1 SELECT independente do volume de dados. |

## 3. Registros de Desafios Enfrentados

O principal ponto de atenção foi a gestão do ciclo de vida do `DashboardPanel` ao fazer logout. Em Swing, componentes removidos do container mas ainda referenciados por variáveis de instância não são coletados pelo GC, mantendo os dados do usuário anterior na memória. A solução foi anular explicitamente `dashboard = null` em `MainFrame.mostrarLogin()` e chamar `cardPanel.removeAll()` antes de recriar o `LoginPanel`, garantindo que a referência ao painel anterior seja liberada.

Outro desafio foi a exibição do formulário de usuário: o `JDialog` gerado por `JOptionPane.showConfirmDialog()` tem altura fixa que pode truncar formulários com muitos campos. A solução foi usar `GridLayout(0, 2)` (número de linhas dinâmico) com `JScrollPane` nos campos de área de texto, mantendo o formulário dentro dos limites visuais sem redimensionamento manual.

## 4. Artefatos Entregues

| Artefato | Caminho |
|----------|---------|
| Dependência FlatLaf | `pom.xml` |
| Ponto de entrada da UI | `src/main/java/.../Application.java` |
| Frame principal + menu bar | `src/main/java/.../view/MainFrame.java` |
| Tela de login | `src/main/java/.../view/LoginPanel.java` |
| Tela inicial com cards de módulo | `src/main/java/.../view/HomePanel.java` |
| CRUD de Cargos | `src/main/java/.../view/CargoPanel.java` |
| CRUD de Usuários | `src/main/java/.../view/UsuarioPanel.java` |
| Gestão de Projetos | `src/main/java/.../view/ProjetoPanel.java` |
| Gestão de Equipes e Membros | `src/main/java/.../view/EquipePanel.java` |
| Gestão de Tarefas | `src/main/java/.../view/TarefaPanel.java` |
| Migration seed — admin padrão | `src/main/resources/db/migration/V3__Seed_admin.sql` |
| Correção N+1 — JOIN FETCH | `src/main/java/.../repository/UsuarioRepository.java` |
| Correção N+1 — JOIN FETCH | `src/main/java/.../repository/ProjetoRepository.java` |
| Correção N+1 — JOIN FETCH | `src/main/java/.../repository/TarefaRepository.java` |
