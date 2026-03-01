package gui;

import java.awt.Frame;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class RobotsProgram
{
    public static void main(String[] args) {
      try {
        // Nimbus - тема оформления
        UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
//        UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
//        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
//        UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
      } catch (Exception e) {
        e.printStackTrace();
      }
      // создание окна произойдет в правильном потоке
      SwingUtilities.invokeLater(() -> {
        MainApplicationFrame frame = new MainApplicationFrame(); //Создается главное окно
        frame.pack();
        frame.setVisible(true);
        frame.setExtendedState(Frame.MAXIMIZED_BOTH); // разворачивается на весь экран
      });
    }}
