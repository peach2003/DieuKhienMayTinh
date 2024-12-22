package doan_dieukhienmaytinh;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
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
    private boolean connected = false;
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
            if (connected) {
                try {
                    disconnectFromServer();
                    connectButton.setText("Connect");
                    JOptionPane.showMessageDialog(this, "Disconnected successfully.");
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(this, "Error while disconnecting: " + ex.getMessage());
                }
            } else {
                if (connectToServer()) {
                    connectButton.setText("Disconnect");
                    JOptionPane.showMessageDialog(this, "Connected successfully.");
                } else {
                    JOptionPane.showMessageDialog(this, "Failed to connect. Please check the server IP and try again.");
                }
            }
        });
    }

    public boolean connectToServer() {
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
                return false;
            }

            connected = true;
            new Thread(this::receiveScreen).start();
            setupControlListeners();
            return true;
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Lỗi kết nối: " + e.getMessage());
            return false;
        }
    }

    public void disconnectFromServer() throws IOException {
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
        connected = false;
    }

    public boolean isConnected() {
        return connected;
    }

    private void receiveScreen() {
        try {
            while (connected) {
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
            connected = false;
        }
    }

    private void setupControlListeners() {
        screenLabel.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                sendMouseEvent("mouse", e.getX(), e.getY());
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                sendMouseEvent("drag", e.getX(), e.getY());
            }
        });

        screenLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                sendEvent("click");
            }
        });

        screenLabel.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                sendEvent("key," + e.getKeyCode());
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

    public JButton getConnectButton() {
        return connectButton;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ClientForm clientForm = new ClientForm();
            clientForm.setVisible(true);
        });
    }
}
