/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package doan_dieukhienmaytinh;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import javax.imageio.ImageIO;

public class ClientForm extends JFrame {
    private JTextField serverIpField;
    private JPasswordField passwordField;
    private JButton connectButton;
    private JLabel screenLabel;
    private Socket socket;
    private ObjectOutputStream outputStream;
    private ObjectInputStream inputStream;

    public ClientForm() {
        setTitle("Remote Desktop Client");
        setSize(800, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel topPanel = new JPanel();
        topPanel.setLayout(new GridLayout(1, 3));

        serverIpField = new JTextField("Enter Server IP");
        passwordField = new JPasswordField("123456");
        connectButton = new JButton("Connect");

        topPanel.add(serverIpField);
        topPanel.add(passwordField);
        topPanel.add(connectButton);

        screenLabel = new JLabel();
        screenLabel.setHorizontalAlignment(SwingConstants.CENTER);
        add(topPanel, BorderLayout.NORTH);
        add(screenLabel, BorderLayout.CENTER);

        connectButton.addActionListener(e -> connectToServer());
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ClientForm clientForm = new ClientForm();
            clientForm.setVisible(true);
        });
    }

    private void connectToServer() {
        String serverIp = serverIpField.getText();
        String password = new String(passwordField.getPassword());

        try {
            socket = new Socket(serverIp, 5000);
            outputStream = new ObjectOutputStream(socket.getOutputStream());
            inputStream = new ObjectInputStream(socket.getInputStream());

            outputStream.writeObject(password);
            String response = (String) inputStream.readObject();

            if (!response.equals("Client authenticated successfully")) {
                JOptionPane.showMessageDialog(this, "Authentication Failed");
                socket.close();
                return;
            }

            JOptionPane.showMessageDialog(this, "Connected to Server");
            new Thread(this::receiveScreen).start();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Connection Error: " + e.getMessage());
        }
    }

    private void receiveScreen() {
        try {
            while (true) {
                byte[] imageBytes = (byte[]) inputStream.readObject();
                ImageIcon icon = new ImageIcon(imageBytes);
                screenLabel.setIcon(icon);
                screenLabel.repaint();
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Disconnected from Server");
        }
    }
}

