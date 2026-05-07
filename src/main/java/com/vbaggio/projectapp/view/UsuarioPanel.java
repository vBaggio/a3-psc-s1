package com.vbaggio.projectapp.view;

import com.vbaggio.projectapp.controller.CargoController;
import com.vbaggio.projectapp.controller.UsuarioController;
import com.vbaggio.projectapp.model.entity.Cargo;
import com.vbaggio.projectapp.model.entity.Usuario;
import com.vbaggio.projectapp.model.enums.Perfil;
import com.vbaggio.projectapp.util.OpcaoItem;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.UUID;

import static com.vbaggio.projectapp.view.TableUtils.tabelaComMensagem;

public class UsuarioPanel extends JPanel {

    private final UsuarioController ctrl      = new UsuarioController();
    private final CargoController   cargoCtrl = new CargoController();

    private final DefaultTableModel modelo = new DefaultTableModel(
            new String[]{"ID", "Nome", "Login", "CPF", "E-mail", "Perfil", "Cargo"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable tabela = tabelaComMensagem(modelo, "Nenhum usuário cadastrado.");
    private boolean scrollParaFim = false;

    public UsuarioPanel() {
        setLayout(new BorderLayout(0, 4));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        add(criarToolbar(), BorderLayout.NORTH);
        add(new JScrollPane(tabela), BorderLayout.CENTER);

        ocultarColuna(0);
        tabela.setAutoCreateRowSorter(true);
        tabela.getColumnModel().getColumn(1).setPreferredWidth(160);
        tabela.getColumnModel().getColumn(2).setPreferredWidth(100);
        tabela.getColumnModel().getColumn(3).setPreferredWidth(95);
        tabela.getColumnModel().getColumn(4).setPreferredWidth(180);
        tabela.getColumnModel().getColumn(5).setPreferredWidth(90);
        tabela.getColumnModel().getColumn(6).setPreferredWidth(110);
        tabela.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && tabela.getSelectedRow() >= 0) {
                    int linha = tabela.getSelectedRow();
                    UUID id = UUID.fromString(modelo.getValueAt(linha, 0).toString());
                    abrirFormulario(ctrl.buscarPorId(id));
                }
            }
        });
        carregar();
    }

    private JPanel criarToolbar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        JButton btnNovo    = new JButton("Novo");
        JButton btnEditar  = new JButton("Editar");
        JButton btnExcluir = new JButton("Excluir");
        bar.add(btnNovo); bar.add(btnEditar); bar.add(btnExcluir);

        btnEditar.setEnabled(false);
        btnExcluir.setEnabled(false);

        tabela.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            boolean sel = tabela.getSelectedRow() >= 0;
            btnEditar.setEnabled(sel);
            btnExcluir.setEnabled(sel);
        });

        btnNovo.setToolTipText("Criar novo usuário");
        btnEditar.setToolTipText("Editar usuário selecionado");
        btnExcluir.setToolTipText("Excluir usuário selecionado");

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
        OpcaoItem[] opcoesCargo = new OpcaoItem[cargos.size() + 1];
        opcoesCargo[0] = new OpcaoItem(null, "(sem cargo)");
        for (int i = 0; i < cargos.size(); i++) {
            opcoesCargo[i + 1] = new OpcaoItem(cargos.get(i).getId(), cargos.get(i).getNome());
        }
        JComboBox<OpcaoItem> comboCargo = new JComboBox<>(opcoesCargo);
        if (edicao && usuario.getCargo() != null) {
            for (int i = 0; i < opcoesCargo.length; i++) {
                if (usuario.getCargo().getId().equals(opcoesCargo[i].id())) {
                    comboCargo.setSelectedIndex(i); break;
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

        UUID cargoSel;
        String senha;

        while (true) {
            int op = JOptionPane.showConfirmDialog(this, form,
                    edicao ? "Editar Usuário" : "Novo Usuário",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (op != JOptionPane.OK_OPTION) return;

            cargoSel = ((OpcaoItem) comboCargo.getSelectedItem()).id();
            senha    = new String(campSenha.getPassword());

            try {
                if (!edicao) {
                    ctrl.cadastrarUsuario(
                            campNome.getText().trim(), campCpf.getText().trim(),
                            campEmail.getText().trim(), campLogin.getText().trim(),
                            senha, (Perfil) comboPerfil.getSelectedItem(), cargoSel);
                    scrollParaFim = true;
                } else {
                    ctrl.atualizarUsuario(
                            usuario.getId(),
                            campNome.getText().trim(), campCpf.getText().trim(),
                            campEmail.getText().trim(), campLogin.getText().trim(),
                            senha.isBlank() ? null : senha,
                            (Perfil) comboPerfil.getSelectedItem(), cargoSel);
                }
                carregar();
                return;
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void excluir() {
        int linha = tabela.getSelectedRow();
        if (linha < 0) { JOptionPane.showMessageDialog(this, "Selecione um usuário."); return; }
        String nome = modelo.getValueAt(linha, 1).toString();
        int conf = JOptionPane.showConfirmDialog(this,
                "Excluir usuário '" + nome + "'?",
                "Confirmar", JOptionPane.YES_NO_OPTION);
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
        new SwingWorker<List<Usuario>, Void>() {
            @Override protected List<Usuario> doInBackground() { return ctrl.listarUsuarios(); }
            @Override protected void done() {
                try {
                    modelo.setRowCount(0);
                    for (Usuario u : get()) {
                        modelo.addRow(new Object[]{
                                u.getId().toString(), u.getNome(), u.getLogin(),
                                u.getCpf(), u.getEmail(), u.getPerfil(),
                                u.getCargo() != null ? u.getCargo().getNome() : ""
                        });
                    }
                    if (scrollParaFim && modelo.getRowCount() > 0) {
                        int last = modelo.getRowCount() - 1;
                        tabela.setRowSelectionInterval(last, last);
                        tabela.scrollRectToVisible(tabela.getCellRect(last, 0, true));
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(UsuarioPanel.this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
                } finally {
                    scrollParaFim = false;
                }
            }
        }.execute();
    }

    private void ocultarColuna(int col) {
        tabela.getColumnModel().getColumn(col).setMinWidth(0);
        tabela.getColumnModel().getColumn(col).setMaxWidth(0);
        tabela.getColumnModel().getColumn(col).setWidth(0);
    }
}
