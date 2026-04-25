package com.vbaggio.projectapp.view;

import com.vbaggio.projectapp.controller.CargoController;
import com.vbaggio.projectapp.controller.UsuarioController;
import com.vbaggio.projectapp.model.entity.Cargo;
import com.vbaggio.projectapp.model.entity.Usuario;
import com.vbaggio.projectapp.model.enums.Perfil;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.UUID;

public class UsuarioPanel extends JPanel {

    private final UsuarioController ctrl      = new UsuarioController();
    private final CargoController   cargoCtrl = new CargoController();

    private final DefaultTableModel modelo = new DefaultTableModel(
            new String[]{"ID", "Nome", "Login", "CPF", "E-mail", "Perfil", "Cargo"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable tabela = new JTable(modelo);

    public UsuarioPanel() {
        setLayout(new BorderLayout(0, 4));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        add(criarToolbar(), BorderLayout.NORTH);
        add(new JScrollPane(tabela), BorderLayout.CENTER);

        ocultarColuna(0);
        carregar();
    }

    private JPanel criarToolbar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        JButton btnNovo    = new JButton("Novo");
        JButton btnEditar  = new JButton("Editar");
        JButton btnExcluir = new JButton("Excluir");
        bar.add(btnNovo); bar.add(btnEditar); bar.add(btnExcluir);

        btnNovo.addActionListener(e -> abrirFormulario(null));
        btnEditar.addActionListener(e -> {
            int linha = tabela.getSelectedRow();
            if (linha < 0) { JOptionPane.showMessageDialog(this, "Selecione um usuário."); return; }
            UUID id = UUID.fromString(modelo.getValueAt(linha, 0).toString());
            abrirFormulario(ctrl.buscarPorId(id));
        });
        btnExcluir.addActionListener(e -> excluir());
        return bar;
    }

    private void abrirFormulario(Usuario usuario) {
        boolean edicao = usuario != null;

        JTextField     campNome   = new JTextField(edicao ? usuario.getNome()  : "", 20);
        JTextField     campCpf    = new JTextField(edicao ? usuario.getCpf()   : "", 11);
        JTextField     campEmail  = new JTextField(edicao ? usuario.getEmail() : "", 20);
        JTextField     campLogin  = new JTextField(edicao ? usuario.getLogin() : "", 16);
        JPasswordField campSenha  = new JPasswordField(16);
        JComboBox<Perfil> comboPerfil = new JComboBox<>(Perfil.values());
        if (edicao) comboPerfil.setSelectedItem(usuario.getPerfil());

        List<Cargo> cargos = cargoCtrl.listarCargos();
        String[] nomeCargos = new String[cargos.size() + 1];
        UUID[]   idCargos   = new UUID[cargos.size() + 1];
        nomeCargos[0] = "(sem cargo)"; idCargos[0] = null;
        for (int i = 0; i < cargos.size(); i++) {
            nomeCargos[i + 1] = cargos.get(i).getNome();
            idCargos[i + 1]   = cargos.get(i).getId();
        }
        JComboBox<String> comboCargo = new JComboBox<>(nomeCargos);
        if (edicao && usuario.getCargo() != null) {
            for (int i = 0; i < idCargos.length; i++) {
                if (usuario.getCargo().getId().equals(idCargos[i])) {
                    comboCargo.setSelectedIndex(i);
                    break;
                }
            }
        }

        JPanel form = new JPanel(new GridLayout(0, 2, 6, 4));
        form.add(new JLabel("Nome:"));         form.add(campNome);
        form.add(new JLabel("CPF (11 dígitos):")); form.add(campCpf);
        form.add(new JLabel("E-mail:"));       form.add(campEmail);
        form.add(new JLabel("Login:"));        form.add(campLogin);
        form.add(new JLabel(edicao ? "Nova senha (vazio = manter):" : "Senha:"));
        form.add(campSenha);
        form.add(new JLabel("Perfil:"));       form.add(comboPerfil);
        form.add(new JLabel("Cargo:"));        form.add(comboCargo);

        int op = JOptionPane.showConfirmDialog(this, form,
                edicao ? "Editar Usuário" : "Novo Usuário",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (op != JOptionPane.OK_OPTION) return;

        UUID cargoSel = idCargos[comboCargo.getSelectedIndex()];
        String senha  = new String(campSenha.getPassword());

        try {
            if (!edicao) {
                ctrl.cadastrarUsuario(
                        campNome.getText().trim(),
                        campCpf.getText().trim(),
                        campEmail.getText().trim(),
                        campLogin.getText().trim(),
                        senha,
                        (Perfil) comboPerfil.getSelectedItem(),
                        cargoSel);
            } else {
                ctrl.atualizarUsuario(
                        usuario.getId(),
                        campNome.getText().trim(),
                        campCpf.getText().trim(),
                        campEmail.getText().trim(),
                        campLogin.getText().trim(),
                        senha.isBlank() ? null : senha,
                        (Perfil) comboPerfil.getSelectedItem(),
                        cargoSel);
            }
            carregar();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void excluir() {
        int linha = tabela.getSelectedRow();
        if (linha < 0) { JOptionPane.showMessageDialog(this, "Selecione um usuário."); return; }
        int conf = JOptionPane.showConfirmDialog(this, "Confirmar exclusão?",
                "Excluir", JOptionPane.YES_NO_OPTION);
        if (conf != JOptionPane.YES_OPTION) return;
        UUID id = UUID.fromString(modelo.getValueAt(linha, 0).toString());
        try {
            ctrl.removerUsuario(id);
            carregar();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void carregar() {
        modelo.setRowCount(0);
        for (Usuario u : ctrl.listarUsuarios()) {
            modelo.addRow(new Object[]{
                    u.getId().toString(),
                    u.getNome(),
                    u.getLogin(),
                    u.getCpf(),
                    u.getEmail(),
                    u.getPerfil(),
                    u.getCargo() != null ? u.getCargo().getNome() : ""
            });
        }
    }

    private void ocultarColuna(int col) {
        tabela.getColumnModel().getColumn(col).setMinWidth(0);
        tabela.getColumnModel().getColumn(col).setMaxWidth(0);
        tabela.getColumnModel().getColumn(col).setWidth(0);
    }
}
