# Relatório Semanal de Desenvolvimento - Sprint 5

**Responsável Principal:** Vinícius Baggio  
**Período Avaliado:** 19/04/2026 a 25/04/2026  
**Status do Ciclo:** Concluído  

---

## 1. Resumo Executivo das Avaliações

A Sprint 5 entregou a camada View completa do sistema, concluindo o ciclo MVC iniciado na Sprint 2. O ponto central foi a integração dos cinco controllers já existentes com uma interface gráfica desktop construída em Java Swing, adotando FlatLaf como Look & Feel para prover uma aparência moderna ao sistema. A navegação foi estruturada em dois níveis: um `MainFrame` com `CardLayout` alternando entre a tela de login e o painel principal, e um `DashboardPanel` com `JTabbedPane` expondo um painel dedicado para cada entidade do domínio. Nenhuma alteração foi necessária na camada de backend — a integração se deu exclusivamente pelo consumo dos controllers públicos existentes.

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

## 3. Registros de Desafios Enfrentados

O principal ponto de atenção foi a gestão do ciclo de vida do `DashboardPanel` ao fazer logout. Em Swing, componentes removidos do container mas ainda referenciados por variáveis de instância não são coletados pelo GC, mantendo os dados do usuário anterior na memória. A solução foi anular explicitamente `dashboard = null` em `MainFrame.mostrarLogin()` e chamar `cardPanel.removeAll()` antes de recriar o `LoginPanel`, garantindo que a referência ao painel anterior seja liberada.

Outro desafio foi a exibição do formulário de usuário: o `JDialog` gerado por `JOptionPane.showConfirmDialog()` tem altura fixa que pode truncar formulários com muitos campos. A solução foi usar `GridLayout(0, 2)` (número de linhas dinâmico) com `JScrollPane` nos campos de área de texto, mantendo o formulário dentro dos limites visuais sem redimensionamento manual.

## 4. Artefatos Entregues

| Artefato | Caminho |
|----------|---------|
| Dependência FlatLaf | `pom.xml` |
| Ponto de entrada da UI | `src/main/java/.../Application.java` |
| Frame principal | `src/main/java/.../view/MainFrame.java` |
| Tela de login | `src/main/java/.../view/LoginPanel.java` |
| Painel principal pós-login | `src/main/java/.../view/DashboardPanel.java` |
| CRUD de Cargos | `src/main/java/.../view/CargoPanel.java` |
| CRUD de Usuários | `src/main/java/.../view/UsuarioPanel.java` |
| Gestão de Projetos | `src/main/java/.../view/ProjetoPanel.java` |
| Gestão de Equipes e Membros | `src/main/java/.../view/EquipePanel.java` |
| Gestão de Tarefas | `src/main/java/.../view/TarefaPanel.java` |
