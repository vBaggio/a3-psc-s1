package com.vbaggio.projectapp.view;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

class TableUtils {

    static JTable tabelaComMensagem(DefaultTableModel modelo, String mensagem) {
        return new JTable(modelo) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (modelo.getRowCount() == 0) {
                    Graphics2D g2 = (Graphics2D) g;
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(UIManager.getColor("Label.disabledForeground") != null
                            ? UIManager.getColor("Label.disabledForeground")
                            : new Color(128, 128, 128));
                    g2.setFont(getFont().deriveFont(13f));
                    FontMetrics fm = g2.getFontMetrics();
                    int x = (getWidth() - fm.stringWidth(mensagem)) / 2;
                    int y = getHeight() / 2;
                    g2.drawString(mensagem, x, y);
                }
            }
        };
    }
}
