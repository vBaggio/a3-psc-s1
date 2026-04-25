package com.vbaggio.projectapp.view;

import com.vbaggio.projectapp.controller.UsuarioController;
import com.vbaggio.projectapp.model.entity.Usuario;

import javax.swing.*;
import java.awt.*;

public class LoginPanel extends JPanel {

    private final MainFrame         frame;
    private final UsuarioController usuarioCtrl = new UsuarioController();

    private final JTextField     campLogin = new JTextField(20);
    private final JPasswordField campSenha = new JPasswordField(20);
    private final JButton        btnEntrar = new JButton("Entrar");
    private final JLabel         lblErro   = new JLabel(" ");

    public LoginPanel(MainFrame frame) {
        this.frame = frame;
        setLayout(new GridBagLayout());

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder("Login"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 8, 6, 8);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0;
        form.add(new JLabel("Login:"), gbc);
        gbc.gridx = 1;
        form.add(campLogin, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        form.add(new JLabel("Senha:"), gbc);
        gbc.gridx = 1;
        form.add(campSenha, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        form.add(btnEntrar, gbc);

        lblErro.setForeground(Color.RED);
        gbc.gridy = 3;
        form.add(lblErro, gbc);

        add(form);

        btnEntrar.addActionListener(e -> tentarLogin());
        campSenha.addActionListener(e -> tentarLogin());
    }

    private void tentarLogin() {
        String login = campLogin.getText().trim();
        String senha = new String(campSenha.getPassword());
        try {
            Usuario usuario = usuarioCtrl.autenticar(login, senha);
            frame.mostrarHome(usuario);
        } catch (Exception ex) {
            lblErro.setText(ex.getMessage());
            campSenha.setText("");
        }
    }
}
