package com.vbaggio.projectapp.view;

import com.vbaggio.projectapp.controller.RelatorioController;
import com.vbaggio.projectapp.dto.CargaUsuario;
import com.vbaggio.projectapp.dto.ProjetoOpcao;
import com.vbaggio.projectapp.dto.ResumoProjeto;
import com.vbaggio.projectapp.model.enums.StatusProjeto;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.Map;

public class RelatorioPanel extends JPanel {

    private final RelatorioController ctrl = new RelatorioController();

    public RelatorioPanel() {
        setLayout(new BorderLayout());
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Resumo Global",       buildResumoGlobalTab());
        tabs.addTab("Desempenho por Projeto", buildDesempenhoTab());
        tabs.addTab("Carga de Trabalho",   buildCargaTab());
        add(tabs, BorderLayout.CENTER);
    }

    // ------------------------------------------------------------------
    // Aba 1 — Resumo Global
    // ------------------------------------------------------------------

    private JPanel buildResumoGlobalTab() {
        JPanel painel = new JPanel(new BorderLayout(8, 8));
        painel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JButton btnAtualizar = new JButton("Atualizar");
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        toolbar.add(btnAtualizar);
        painel.add(toolbar, BorderLayout.NORTH);

        JPanel cards = new JPanel(new GridLayout(1, 4, 12, 0));
        painel.add(cards, BorderLayout.CENTER);

        btnAtualizar.addActionListener(e -> carregarResumoGlobal(cards));
        carregarResumoGlobal(cards);

        return painel;
    }

    private void carregarResumoGlobal(JPanel cards) {
        try {
            Map<StatusProjeto, Long> resumo = ctrl.resumoGlobal();
            cards.removeAll();
            cards.add(criarCard("Planejados",   String.valueOf(resumo.get(StatusProjeto.PLANEJADO)),    new Color(70, 130, 180)));
            cards.add(criarCard("Em Andamento", String.valueOf(resumo.get(StatusProjeto.EM_ANDAMENTO)), new Color(60, 160, 80)));
            cards.add(criarCard("Concluídos",   String.valueOf(resumo.get(StatusProjeto.CONCLUIDO)),    new Color(100, 100, 100)));
            cards.add(criarCard("Cancelados",   String.valueOf(resumo.get(StatusProjeto.CANCELADO)),    new Color(190, 60, 60)));
            cards.revalidate();
            cards.repaint();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JPanel criarCard(String titulo, String valor, Color cor) {
        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(cor.darker());
        card.setBorder(BorderFactory.createLineBorder(cor, 2, true));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0;

        JLabel lblValor = new JLabel(valor, SwingConstants.CENTER);
        lblValor.setFont(lblValor.getFont().deriveFont(Font.BOLD, 36f));
        lblValor.setForeground(Color.WHITE);

        JLabel lblTitulo = new JLabel(titulo, SwingConstants.CENTER);
        lblTitulo.setFont(lblTitulo.getFont().deriveFont(14f));
        lblTitulo.setForeground(Color.WHITE);

        card.add(lblValor, gbc);
        gbc.gridy = 1;
        card.add(lblTitulo, gbc);
        return card;
    }

    // ------------------------------------------------------------------
    // Aba 2 — Desempenho por Projeto
    // ------------------------------------------------------------------

    private JPanel buildDesempenhoTab() {
        JPanel painel = new JPanel(new BorderLayout(8, 8));
        painel.setBorder(BorderFactory.createEmptyBorder(12, 16, 16, 16));

        JComboBox<ProjetoOpcao> cbProjeto = new JComboBox<>();
        JButton btnCarregar = new JButton("Carregar");
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        toolbar.add(new JLabel("Projeto:"));
        toolbar.add(cbProjeto);
        toolbar.add(btnCarregar);
        painel.add(toolbar, BorderLayout.NORTH);

        String[] colunas = {"Métrica", "Valor"};
        DefaultTableModel model = new DefaultTableModel(colunas, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable tabela = new JTable(model);
        tabela.getColumnModel().getColumn(0).setPreferredWidth(220);
        tabela.getColumnModel().getColumn(1).setPreferredWidth(180);
        painel.add(new JScrollPane(tabela), BorderLayout.CENTER);

        Runnable popularComboProjetos = () -> {
            cbProjeto.removeAllItems();
            try {
                for (ProjetoOpcao p : ctrl.listarProjetosParaCombo()) {
                    cbProjeto.addItem(p);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(painel, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            }
        };

        btnCarregar.addActionListener(e -> {
            ProjetoOpcao selecionado = (ProjetoOpcao) cbProjeto.getSelectedItem();
            if (selecionado == null) return;
            try {
                ResumoProjeto r = ctrl.desempenhoPorProjeto(selecionado.id());
                model.setRowCount(0);
                model.addRow(new Object[]{"Projeto",            r.nome()});
                model.addRow(new Object[]{"Status",             r.status()});
                model.addRow(new Object[]{"Data prevista",      r.dataPrevisao() != null ? r.dataPrevisao().toString() : "—"});
                model.addRow(new Object[]{"Data de conclusão",  r.dataFim()      != null ? r.dataFim().toString()      : "—"});
                model.addRow(new Object[]{"Situação de prazo",  r.isAtrasado() ? "Atrasado" : "No prazo"});
                model.addRow(new Object[]{"Total de tarefas",   r.totalTarefas()});
                model.addRow(new Object[]{"Concluídas",         r.tarefasConcluidas()});
                model.addRow(new Object[]{"Em andamento",       r.tarefasEmAndamento()});
                model.addRow(new Object[]{"Pendentes",          r.tarefasPendentes()});
                model.addRow(new Object[]{"Canceladas",         r.tarefasCanceladas()});
                model.addRow(new Object[]{"Vencidas (abertas)", r.tarefasVencidas()});
                model.addRow(new Object[]{"% de conclusão",     r.percentualConclusao() + "%"});
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(painel, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            }
        });

        popularComboProjetos.run();
        return painel;
    }

    // ------------------------------------------------------------------
    // Aba 3 — Carga de Trabalho
    // ------------------------------------------------------------------

    private JPanel buildCargaTab() {
        JPanel painel = new JPanel(new BorderLayout(8, 8));
        painel.setBorder(BorderFactory.createEmptyBorder(12, 16, 16, 16));

        JButton btnAtualizar = new JButton("Atualizar");
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        toolbar.add(btnAtualizar);
        painel.add(toolbar, BorderLayout.NORTH);

        String[] colunas = {"Membro", "Perfil", "Pendentes", "Em Andamento", "Concluídas", "Canceladas", "Vencidas", "Total Ativas"};
        DefaultTableModel model = new DefaultTableModel(colunas, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable tabela = new JTable(model);
        tabela.setAutoCreateRowSorter(true);
        painel.add(new JScrollPane(tabela), BorderLayout.CENTER);

        Runnable carregar = () -> {
            try {
                List<CargaUsuario> carga = ctrl.cargaDeTrabalho();
                model.setRowCount(0);
                for (CargaUsuario c : carga) {
                    model.addRow(new Object[]{
                            c.nome(),
                            c.perfil(),
                            c.tarefasPendentes(),
                            c.tarefasEmAndamento(),
                            c.tarefasConcluidas(),
                            c.tarefasCanceladas(),
                            c.tarefasVencidas(),
                            c.getTotalAtivas()
                    });
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(painel, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            }
        };

        btnAtualizar.addActionListener(e -> carregar.run());
        carregar.run();

        return painel;
    }

}
