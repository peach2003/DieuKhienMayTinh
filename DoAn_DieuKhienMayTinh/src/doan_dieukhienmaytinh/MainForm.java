package doan_dieukhienmaytinh;

import javax.swing.*;
import java.awt.*;

public class MainForm extends JFrame {

    public MainForm() {
        setTitle("Remote Desktop Main Menu");
        setSize(800, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Tạo thanh menu
        JMenuBar menuBar = new JMenuBar();
        JMenu menu = new JMenu("Options");
        JMenuItem openServer = new JMenuItem("Open Server Form");
        JMenuItem openClient = new JMenuItem("Open Client Form");
        JMenuItem exit = new JMenuItem("Exit");

        menu.add(openServer);
        menu.add(openClient);
        menu.addSeparator();
        menu.add(exit);
        menuBar.add(menu);
        setJMenuBar(menuBar);

        // Sự kiện cho menu item Open Server
        openServer.addActionListener(e -> {
            // Mở ServerForm trong một cửa sổ JFrame riêng
            SwingUtilities.invokeLater(() -> {
                ServerForm serverForm = new ServerForm();
                serverForm.setVisible(true);
            });
        });

        // Sự kiện cho menu item Open Client
        openClient.addActionListener(e -> {
            // Mở ClientForm trong một cửa sổ JFrame riêng
            SwingUtilities.invokeLater(() -> {
                ClientForm clientForm = new ClientForm();
                clientForm.setVisible(true);
            });
        });

        // Sự kiện cho menu item Exit
        exit.addActionListener(e -> System.exit(0));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MainForm mainForm = new MainForm();
            mainForm.setVisible(true);
        });
    }
}