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

        JPanel topPanel = new JPanel(new GridLayout(1, 3));
        serverIpField = new JTextField("Nhập IP Server");
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

    private void connectToServer() {
        String serverIp = serverIpField.getText();
        String password = new String(passwordField.getPassword());

        try {
            socket = new Socket(serverIp, 5000);
            outputStream = new ObjectOutputStream(socket.getOutputStream());
            inputStream = new ObjectInputStream(socket.getInputStream());

            outputStream.writeObject(password);

            String response = (String) inputStream.readObject();
            if (!response.equals("Máy khách xác thực thành công")) {
                JOptionPane.showMessageDialog(this, "Xác thực thất bại!");
                socket.close();
                return;
            }
            JOptionPane.showMessageDialog(this, "Kết nối thành công!");
            new Thread(this::receiveScreen).start();
            setupControlListeners();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Lỗi kết nối: " + e.getMessage());
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
            JOptionPane.showMessageDialog(this, "Mất kết nối với Server!");
        }
    }

    private void setupControlListeners() {
        screenLabel.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                try {
                    int x = e.getX();
                    int y = e.getY();
                    outputStream.writeObject("mouse," + x + "," + y);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });

        screenLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    outputStream.writeObject("click");
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });

        screenLabel.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                try {
                    int keyCode = e.getKeyCode();
                    outputStream.writeObject("key," + keyCode);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ClientForm clientForm = new ClientForm();
            clientForm.setVisible(true);
        });
    }
}
