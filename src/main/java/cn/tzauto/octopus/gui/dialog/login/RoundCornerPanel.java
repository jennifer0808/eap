/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.gui.dialog.login;

import java.awt.*;
import java.awt.geom.*;
import javax.swing.*;

public class RoundCornerPanel extends JPanel{
    private static final long serialVersionUID = -2631585163797279638L;
    private int arcH = 30 ;
    private int arcW = 30 ;
    Shape shape ;
    public RoundCornerPanel() {
        setBorder(null);
        setBackground(new Color(0,78,161));
        setOpaque(true);
    }
    @Override
    public boolean contains(int x, int y) {
        shape = new RoundRectangle2D.Double(0, 0, getWidth(), getHeight() , 15 , 15) ;
        return shape.contains(x, y);
    }
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g ;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(Color.white);
        g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, arcW, arcH);
        g2.setColor(new Color(0,78,161));
        g2.fillRoundRect(1, 1, getWidth()-2, getHeight()-2, arcW, arcH);
    }
}
