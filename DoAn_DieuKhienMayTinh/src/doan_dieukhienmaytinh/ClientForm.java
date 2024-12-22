package doan_dieukhienmaytinh;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;

public class ClientForm extends JFrame {
    private JTextField serverIpField;
    private JPasswordField passwordField;
    private JButton connectButton;
    private JLabel screenLabel;
    private Socket socket;
    private ObjectOutputStream outputStream;
    private ObjectInputStream inputStream;
    private int screenWidthServer;
    private int screenHeightServer;

    public ClientForm() {
        setTitle("Remote Desktop Client");
        setSize(1000, 700);
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

        connectButton.addActionListener(e -> {
            if ("Connect".equals(connectButton.getText())) {
                connectToServer();
            } else {
                disconnectFromServer();
            }
        });
    }

    private void connectToServer() {
        String serverIp = serverIpField.getText();
        String password = new String(passwordField.getPassword());

        try {
            socket = new Socket(serverIp, 5000);
            outputStream = new ObjectOutputStream(socket.getOutputStream());
            inputStream = new ObjectInputStream(socket.getInputStream());

            String[] screenSize = ((String) inputStream.readObject()).split(",");
            screenWidthServer = Integer.parseInt(screenSize[0]);
            screenHeightServer = Integer.parseInt(screenSize[1]);

            outputStream.writeObject(password);

            String response = (String) inputStream.readObject();
            if (!response.equals("Máy khách xác thực thành công")) {
                JOptionPane.showMessageDialog(this, "Xác thực thất bại!");
                socket.close();
                return;
            }

            JOptionPane.showMessageDialog(this, "Kết nối thành công!");
            connectButton.setText("Disconnect");
            new Thread(this::receiveScreen).start();
            setupControlListeners();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Lỗi kết nối: " + e.getMessage());
        }
    }

    private void disconnectFromServer() {
        try {
            if (socket != null && !socket.isClosed()) {
                outputStream.writeObject("disconnect");
                socket.close();
            }
            JOptionPane.showMessageDialog(this, "Đã ngắt kết nối.");
            connectButton.setText("Connect");
            screenLabel.setIcon(null);
            dispose(); // Close the client form
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Lỗi khi ngắt kết nối: " + e.getMessage());
        }
    }

    private void receiveScreen() {
        try {
            while (true) {
                byte[] imageBytes = (byte[]) inputStream.readObject();
                ImageIcon icon = new ImageIcon(imageBytes);

                int width = screenLabel.getWidth();
                int height = screenLabel.getHeight();

                Image scaledImage = icon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
                screenLabel.setIcon(new ImageIcon(scaledImage));
                screenLabel.repaint();
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Mất kết nối với Server!");
            disconnectFromServer();
        }
    }

    private void setupControlListeners() {
        screenLabel.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                if (socket != null && !socket.isClosed()) {
                    sendMouseEvent("mouse", e.getX(), e.getY());
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (socket != null && !socket.isClosed()) {
                    sendMouseEvent("drag", e.getX(), e.getY());
                }
            }
        });

        screenLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (socket != null && !socket.isClosed()) {
                    sendEvent("click");
                }
            }
        });

        screenLabel.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (socket != null && !socket.isClosed()) {
                    sendEvent("key," + e.getKeyCode());
                }
            }
        });

        screenLabel.setFocusable(true);
        screenLabel.requestFocusInWindow();
    }

    private void sendMouseEvent(String type, int x, int y) {
        try {
            int adjustedX = (x * screenWidthServer) / screenLabel.getWidth();
            int adjustedY = (y * screenHeightServer) / screenLabel.getHeight();
            outputStream.writeObject(type + "," + adjustedX + "," + adjustedY);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void sendEvent(String event) {
        try {
            outputStream.writeObject(event);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ClientForm clientForm = new ClientForm();
            clientForm.setVisible(true);
        });
    }
}