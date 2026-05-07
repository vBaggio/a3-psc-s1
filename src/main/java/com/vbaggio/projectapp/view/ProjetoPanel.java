package com.vbaggio.projectapp.view;

import com.vbaggio.projectapp.controller.ProjetoController;
import com.vbaggio.projectapp.controller.UsuarioController;
import com.vbaggio.projectapp.model.entity.Projeto;
import com.vbaggio.projectapp.model.entity.Usuario;
import com.vbaggio.projectapp.model.enums.Perfil;
import com.vbaggio.projectapp.model.enums.StatusProjeto;
import com.vbaggio.projectapp.util.DateUtils;
import com.vbaggio.projectapp.util.OpcaoItem;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static com.vbaggio.projectapp.view.TableUtils.tabelaComMensagem;

public class ProjetoPanel extends JPanel {

    private final ProjetoController ctrl        = new ProjetoController();
    private final UsuarioController usuarioCtrl = new UsuarioController();

    private final DefaultTableModel modelo = new DefaultTableModel(
            new String[]{"ID", "Nome", "Status", "Início", "Previsão", "Gerente"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable tabela = tabelaComMensagem(modelo, "Nenhum projeto cadastrado. Clique em 'Novo' para começar.");
    private boolean scrollParaFim = false;

    public ProjetoPanel() {
        setLayout(new BorderLayout(0, 4));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        add(criarToolbar(), BorderLayout.NORTH);
        add(new JScrollPane(tabela), BorderLayout.CENTER);
        ocultarColuna(0);
        tabela.setAutoCreateRowSorter(true);
        tabela.getColumnModel().getColumn(1).setPreferredWidth(200);
        tabela.getColumnModel().getColumn(2).setPreferredWidth(100);
        tabela.getColumnModel().getColumn(3).setPreferredWidth(80);
        tabela.getColumnModel().getColumn(4).setPreferredWidth(80);
        tabela.getColumnModel().getColumn(5).setPreferredWidth(140);
        tabela.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && tabela.getSelectedRow() >= 0) abrirEdicao();
            }
        });
        carregar();
    }

    private JPanel criarToolbar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        JButton btnNovo     = new JButton("Novo");
        JButton btnEditar   = new JButton("Editar");
        JButton btnStatus   = new JButton("Alterar Status");
        JButton btnEncerrar = new JButton("Encerrar");

        btnEditar.setEnabled(false);
        btnStatus.setEnabled(false);
        btnEncerrar.setEnabled(false);

        bar.add(btnNovo); bar.add(btnEditar); bar.add(btnStatus); bar.add(btnEncerrar);

        btnNovo.setToolTipText("Criar novo projeto");
        btnEditar.setToolTipText("Editar projeto selecionado");
        btnStatus.setToolTipText("Alterar status do projeto selecionado");
        btnEncerrar.setToolTipText("Registrar data de encerramento e concluir projeto");

        btnNovo.addActionListener(e -> abrirFormulario());
        btnEditar.addActionListener(e -> abrirEdicao());
        btnStatus.addActionListener(e -> alterarStatus());
        btnEncerrar.addActionListener(e -> encerrar());

        tabela.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            int row = tabela.getSelectedRow();
            if (row < 0) {
                btnEditar.setEnabled(false);
                btnStatus.setEnabled(false);
                btnEncerrar.setEnabled(false);
                return;
            }
            StatusProjeto s = (StatusProjeto) modelo.getValueAt(row, 2);
            btnEditar.setEnabled(s != StatusProjeto.CONCLUIDO && s != StatusProjeto.CANCELADO);
            btnStatus.setEnabled(s.proximosStatus().length > 0);
            btnEncerrar.setEnabled(s == StatusProjeto.EM_ANDAMENTO);
        });

        return bar;
    }

    private void abrirFormulario() {
        List<Usuario> gerentes = usuarioCtrl.listarPorPerfil(Perfil.GERENTE);
        if (gerentes.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Não há usuários com perfil GERENTE cadastrados.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JTextField campNome = new JTextField(24);
        JTextArea campDesc  = new JTextArea(3, 24);
        campDesc.setLineWrap(true); campDesc.setWrapStyleWord(true);
        JFormattedTextField campInicio   = DateUtils.campData();
        campInicio.setText(DateUtils.format(LocalDate.now()));
        JFormattedTextField campPrevisao = DateUtils.campData();
        OpcaoItem[] opcoesGerente = gerentes.stream()
                .map(u -> new OpcaoItem(u.getId(), u.getNome()))
                .toArray(OpcaoItem[]::new);
        JComboBox<OpcaoItem> comboGerente = new JComboBox<>(opcoesGerente);

        JPanel form = montarForm(
                "Nome:", campNome,
                "Descrição:", new JScrollPane(campDesc),
                "Início (dd/MM/yyyy):", campInicio,
                "Previsão (dd/MM/yyyy):", campPrevisao,
                "Gerente:", comboGerente);

        while (true) {
            int op = JOptionPane.showConfirmDialog(this, form, "Novo Projeto",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (op != JOptionPane.OK_OPTION) return;
            try {
                UUID gerenteId = ((OpcaoItem) comboGerente.getSelectedItem()).id();
                ctrl.criarProjeto(campNome.getText().trim(), campDesc.getText().trim(),
                        DateUtils.parse(campInicio.getText()),
                        DateUtils.parse(campPrevisao.getText()),
                        gerenteId);
                scrollParaFim = true;
                carregar();
                return;
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void abrirEdicao() {
        int linha = tabela.getSelectedRow();
        if (linha < 0) { JOptionPane.showMessageDialog(this, "Selecione um projeto."); return; }
        UUID id = UUID.fromString((String) modelo.getValueAt(linha, 0));
        Projeto p = ctrl.buscarPorId(id);

        List<Usuario> gerentes = usuarioCtrl.listarPorPerfil(Perfil.GERENTE);
        if (gerentes.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Não há usuários com perfil GERENTE cadastrados.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JTextField campNome = new JTextField(p.getNome(), 24);
        JTextArea campDesc  = new JTextArea(p.getDescricao() != null ? p.getDescricao() : "", 3, 24);
        campDesc.setLineWrap(true); campDesc.setWrapStyleWord(true);
        JFormattedTextField campInicio   = DateUtils.campData();
        JFormattedTextField campPrevisao = DateUtils.campData();
        if (p.getDataInicio()   != null) campInicio.setText(DateUtils.format(p.getDataInicio()));
        if (p.getDataPrevisao() != null) campPrevisao.setText(DateUtils.format(p.getDataPrevisao()));

        OpcaoItem[] opcoesGerente = gerentes.stream()
                .map(u -> new OpcaoItem(u.getId(), u.getNome()))
                .toArray(OpcaoItem[]::new);
        JComboBox<OpcaoItem> comboGerente = new JComboBox<>(opcoesGerente);
        if (p.getGerente() != null) {
            for (int i = 0; i < opcoesGerente.length; i++) {
                if (opcoesGerente[i].id().equals(p.getGerente().getId())) {
                    comboGerente.setSelectedIndex(i); break;
                }
            }
        }

        JPanel form = montarForm(
                "Nome:", campNome,
                "Descrição:", new JScrollPane(campDesc),
                "Início (dd/MM/yyyy):", campInicio,
                "Previsão (dd/MM/yyyy):", campPrevisao,
                "Gerente:", comboGerente);

        while (true) {
            int op = JOptionPane.showConfirmDialog(this, form, "Editar Projeto",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (op != JOptionPane.OK_OPTION) return;
            try {
                UUID gerenteId = ((OpcaoItem) comboGerente.getSelectedItem()).id();
                ctrl.atualizarProjeto(id, campNome.getText().trim(), campDesc.getText().trim(),
                        DateUtils.parse(campInicio.getText()),
                        DateUtils.parse(campPrevisao.getText()),
                        gerenteId);
                carregar();
                return;
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void alterarStatus() {
        int linha = tabela.getSelectedRow();
        if (linha < 0) { JOptionPane.showMessageDialog(this, "Selecione um projeto."); return; }
        UUID id = UUID.fromString(modelo.getValueAt(linha, 0).toString());
        StatusProjeto atual = (StatusProjeto) modelo.getValueAt(linha, 2);
        StatusProjeto[] opcoes = atual.proximosStatus();
        if (opcoes.length == 0) {
            JOptionPane.showMessageDialog(this,
                    "Projeto " + atual + " não permite alteração de status.",
                    "Aviso", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        StatusProjeto escolha = (StatusProjeto) JOptionPane.showInputDialog(
                this, "Novo status:", "Alterar Status",
                JOptionPane.PLAIN_MESSAGE, null, opcoes, opcoes[0]);
        if (escolha == null) return;
        try {
            ctrl.atualizarStatus(id, escolha);
            carregar();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void encerrar() {
        int linha = tabela.getSelectedRow();
        if (linha < 0) { JOptionPane.showMessageDialog(this, "Selecione um projeto."); return; }
        UUID id = UUID.fromString(modelo.getValueAt(linha, 0).toString());

        JFormattedTextField campFim = DateUtils.campData();
        campFim.setText(DateUtils.format(LocalDate.now()));

        while (true) {
            int op = JOptionPane.showConfirmDialog(this, campFim, "Data de Encerramento (dd/MM/yyyy)",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (op != JOptionPane.OK_OPTION) return;
            try {
                LocalDate dataFim = DateUtils.parse(campFim.getText());
                if (dataFim == null) {
                    JOptionPane.showMessageDialog(this, "Informe a data de encerramento.", "Erro", JOptionPane.ERROR_MESSAGE);
                    continue;
                }
                ctrl.encerrarProjeto(id, dataFim);
                carregar();
                return;
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void carregar() {
        new SwingWorker<List<Projeto>, Void>() {
            @Override protected List<Projeto> doInBackground() { return ctrl.listarProjetos(); }
            @Override protected void done() {
                try {
                    modelo.setRowCount(0);
                    for (Projeto p : get()) {
                        modelo.addRow(new Object[]{
                                p.getId().toString(), p.getNome(), p.getStatus(),
                                DateUtils.format(p.getDataInicio()),
                                DateUtils.format(p.getDataPrevisao()),
                                p.getGerente() != null ? p.getGerente().getNome() : ""
                        });
                    }
                    if (scrollParaFim && modelo.getRowCount() > 0) {
                        int last = modelo.getRowCount() - 1;
                        tabela.setRowSelectionInterval(last, last);
                        tabela.scrollRectToVisible(tabela.getCellRect(last, 0, true));
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(ProjetoPanel.this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
                } finally {
                    scrollParaFim = false;
                }
            }
        }.execute();
    }

    private static JPanel montarForm(Object... labelsECampos) {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints lbl = new GridBagConstraints();
        lbl.anchor = GridBagConstraints.NORTHWEST;
        lbl.insets = new Insets(5, 0, 2, 8); lbl.gridx = 0;
        GridBagConstraints fld = new GridBagConstraints();
        fld.weightx = 1.0; fld.gridx = 1; fld.insets = new Insets(3, 0, 2, 0);
        for (int i = 0, row = 0; i < labelsECampos.length; i += 2, row++) {
            lbl.gridy = row; fld.gridy = row;
            boolean isArea = labelsECampos[i + 1] instanceof JScrollPane;
            fld.fill    = isArea ? GridBagConstraints.BOTH : GridBagConstraints.HORIZONTAL;
            fld.weighty = isArea ? 0.5 : 0;
            p.add(new JLabel((String) labelsECampos[i]), lbl);
            p.add((Component) labelsECampos[i + 1], fld);
        }
        return p;
    }

    private void ocultarColuna(int col) {
        tabela.getColumnModel().getColumn(col).setMinWidth(0);
        tabela.getColumnModel().getColumn(col).setMaxWidth(0);
        tabela.getColumnModel().getColumn(col).setWidth(0);
    }
}
