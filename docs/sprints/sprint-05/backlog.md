# Sprint Backlog - Sprint 5

**Período Inicial/Final:** 19/04/2026 a 25/04/2026  
**Responsável do Projeto:** Vinícius Baggio  
**Objetivo da Sprint:** Construção da camada View com Java Swing utilizando FlatLaf como Look & Feel moderno. Integração dos formulários com a camada Controller. Implementação de navegação via CardLayout com tela de login e painéis por entidade.

---

## Escopo Previsto

| ID | Descrição da Tarefa | Status |
|----|----------------------|--------|
| **TSK-01** | Configuração do FlatLaf como Look & Feel e inicialização do `MainFrame` principal com CardLayout. | ✅ Concluído |
| **TSK-02** | Tela de login (`LoginPanel`) integrada ao `UsuarioController.autenticar()`, com exibição de erro inline. | ✅ Concluído |
| **TSK-03** | Painel de listagem e formulário para a entidade `Cargo` (`CargoPanel`). | ✅ Concluído |
| **TSK-04** | Painel de listagem e formulário para a entidade `Usuario` (`UsuarioPanel`). | ✅ Concluído |
| **TSK-05** | Painel de listagem e gerenciamento de status para a entidade `Projeto` (`ProjetoPanel`). | ✅ Concluído |
| **TSK-06** | Painel de listagem de equipes com sub-painel de gestão de membros (`EquipePanel`). | ✅ Concluído |
| **TSK-07** | Painel de tarefas com filtro por projeto e suporte a reatribuição de responsável (`TarefaPanel`). | ✅ Concluído |
| **TSK-08** | Substituição do smoke test em `Application.main()` pela inicialização da UI via `SwingUtilities.invokeLater()`. | ✅ Concluído |

---

## Ferramentas Adotadas na Sprint

- **Interface Gráfica:** Java Swing (javax.swing)
- **Look & Feel:** FlatLaf 3.4.1 (`FlatDarkLaf`)
- **Gerenciamento de Telas:** CardLayout (login ↔ dashboard)
- **Navegação:** JTabbedPane com 5 abas de entidade
- **Componentes:** JTable + DefaultTableModel, JDialog modal, JOptionPane, JSplitPane
- **Banco de Dados:** PostgreSQL 16 (Alpine via Docker)
- **Build:** Apache Maven 3.x
- **Controle de Versões:** Git (commits semânticos por camada)
