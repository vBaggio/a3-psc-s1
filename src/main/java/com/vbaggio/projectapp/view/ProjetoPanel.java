package com.vbaggio.projectapp.view;

import com.vbaggio.projectapp.controller.ProjetoController;
import com.vbaggio.projectapp.controller.UsuarioController;
import com.vbaggio.projectapp.model.entity.Projeto;
import com.vbaggio.projectapp.model.entity.Usuario;
import com.vbaggio.projectapp.model.enums.Perfil;
import com.vbaggio.projectapp.model.enums.StatusProjeto;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;

public class ProjetoPanel extends JPanel {

    private final ProjetoController  ctrl        = new ProjetoController();
    private final UsuarioController  usuarioCtrl = new UsuarioController();

    private final DefaultTableModel modelo = new DefaultTableModel(
            new String[]{"ID", "Nome", "Status", "Início", "Previsão", "Gerente"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable tabela = new JTable(modelo);

    public ProjetoPanel() {
        setLayout(new BorderLayout(0, 4));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        add(criarToolbar(), BorderLayout.NORTH);
        add(new JScrollPane(tabela), BorderLayout.CENTER);

        ocultarColuna(0);
        carregar();
    }

    private JPanel criarToolbar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        JButton btnNovo     = new JButton("Novo");
        JButton btnStatus   = new JButton("Alterar Status");
        JButton btnEncerrar = new JButton("Encerrar");
        bar.add(btnNovo); bar.add(btnStatus); bar.add(btnEncerrar);

        btnNovo.addActionListener(e -> abrirFormulario());
        btnStatus.addActionListener(e -> alterarStatus());
        btnEncerrar.addActionListener(e -> encerrar());
        return bar;
    }

    private void abrirFormulario() {
        List<Usuario> gerentes = usuarioCtrl.listarPorPerfil(Perfil.GERENTE);
        if (gerentes.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Não há usuários com perfil GERENTE cadastrados.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JTextField campNome     = new JTextField(24);
        JTextArea  campDesc     = new JTextArea(3, 24);
        JTextField campInicio   = new JTextField("yyyy-MM-dd", 10);
        JTextField campPrevisao = new JTextField("yyyy-MM-dd", 10);

        String[] nomesGerentes = gerentes.stream().map(Usuario::getNome).toArray(String[]::new);
        JComboBox<String> comboGerente = new JComboBox<>(nomesGerentes);

        JPanel form = new JPanel(new GridLayout(0, 2, 6, 4));
        form.add(new JLabel("Nome:"));                 form.add(campNome);
        form.add(new JLabel("Descrição:"));            form.add(new JScrollPane(campDesc));
        form.add(new JLabel("Início (yyyy-MM-dd):"));  form.add(campInicio);
        form.add(new JLabel("Previsão (yyyy-MM-dd):")); form.add(campPrevisao);
        form.add(new JLabel("Gerente:"));              form.add(comboGerente);

        int op = JOptionPane.showConfirmDialog(this, form, "Novo Projeto",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (op != JOptionPane.OK_OPTION) return;

        try {
            LocalDate inicio   = parsarData(campInicio.getText().trim());
            LocalDate previsao = parsarData(campPrevisao.getText().trim());
            UUID gerenteId = gerentes.get(comboGerente.getSelectedIndex()).getId();
            ctrl.criarProjeto(campNome.getText().trim(), campDesc.getText().trim(),
                    inicio, previsao, gerenteId);
            carregar();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void alterarStatus() {
        int linha = tabela.getSelectedRow();
        if (linha < 0) { JOptionPane.showMessageDialog(this, "Selecione um projeto."); return; }
        UUID id = UUID.fromString(modelo.getValueAt(linha, 0).toString());

        StatusProjeto[] opcoes = StatusProjeto.values();
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

        String dataStr = JOptionPane.showInputDialog(this,
                "Data de encerramento (yyyy-MM-dd):", LocalDate.now().toString());
        if (dataStr == null || dataStr.isBlank()) return;

        try {
            ctrl.encerrarProjeto(id, LocalDate.parse(dataStr.trim()));
            carregar();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void carregar() {
        modelo.setRowCount(0);
        for (Projeto p : ctrl.listarProjetos()) {
            modelo.addRow(new Object[]{
                    p.getId().toString(),
                    p.getNome(),
                    p.getStatus(),
                    p.getDataInicio()   != null ? p.getDataInicio().toString()   : "",
                    p.getDataPrevisao() != null ? p.getDataPrevisao().toString() : "",
                    p.getGerente()      != null ? p.getGerente().getNome()       : ""
            });
        }
    }

    private LocalDate parsarData(String texto) {
        if (texto == null || texto.isBlank() || texto.equals("yyyy-MM-dd")) return null;
        try { return LocalDate.parse(texto); }
        catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Data inválida '" + texto + "'. Use o formato yyyy-MM-dd.");
        }
    }

    private void ocultarColuna(int col) {
        tabela.getColumnModel().getColumn(col).setMinWidth(0);
        tabela.getColumnModel().getColumn(col).setMaxWidth(0);
        tabela.getColumnModel().getColumn(col).setWidth(0);
    }
}
