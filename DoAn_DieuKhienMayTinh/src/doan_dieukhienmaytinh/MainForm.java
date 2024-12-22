package doan_dieukhienmaytinh;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

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

        // Panel chính để hiển thị các form
        JPanel mainPanel = new JPanel(new BorderLayout());
        add(mainPanel, BorderLayout.CENTER);

        // Sự kiện cho menu item Open Server
        openServer.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                mainPanel.removeAll();
                ServerForm serverForm = new ServerForm();
                mainPanel.add(serverForm.getContentPane(), BorderLayout.CENTER);
                mainPanel.revalidate();
                mainPanel.repaint();
            }
        });

        // Sự kiện cho menu item Open Client
        openClient.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                mainPanel.removeAll();
                ClientForm clientForm = new ClientForm();
                setupClientConnectButton(clientForm);
                mainPanel.add(clientForm.getContentPane(), BorderLayout.CENTER);
                mainPanel.revalidate();
                mainPanel.repaint();
            }
        });

        // Sự kiện cho menu item Exit
        exit.addActionListener(e -> System.exit(0));
    }

    private void setupClientConnectButton(ClientForm clientForm) {
        JButton connectButton = clientForm.getConnectButton();
        connectButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (clientForm.isConnected()) {
                    try {
                        clientForm.disconnectFromServer();
                        connectButton.setText("Connect");
                        JOptionPane.showMessageDialog(clientForm, "Disconnected successfully.");
                    } catch (IOException ex) {
                        JOptionPane.showMessageDialog(clientForm, "Error while disconnecting: " + ex.getMessage());
                    }
                } else {
                    boolean connected = clientForm.connectToServer();
                    if (connected) {
                        connectButton.setText("Disconnect");
                        JOptionPane.showMessageDialog(clientForm, "Connected successfully.");
                    } else {
                        JOptionPane.showMessageDialog(clientForm, "Failed to connect. Please check the server IP and try again.");
                    }
                }
            }
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MainForm mainForm = new MainForm();
            mainForm.setVisible(true);
        });
    }
}
