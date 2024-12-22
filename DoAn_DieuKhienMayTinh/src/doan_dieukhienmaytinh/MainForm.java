package doan_dieukhienmaytinh;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import javax.imageio.ImageIO;

public class MainForm extends JFrame {
    private JTextField targetIpField;
    private JPasswordField targetPasswordField;
    private JLabel serverIpLabel;
    private JLabel serverPasswordLabel;
    private JButton connectButton;
    private ServerSocket serverSocket;
    private Socket socket;
    private ObjectOutputStream outputStream;
    private ObjectInputStream inputStream;
    private int screenWidthServer;
    private int screenHeightServer;
    private static final int PORT = 5000;
    private static final String PASSWORD = "123456";

    public MainForm() {
        setTitle("Remote Desktop Control");
        setSize(600, 300);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new GridLayout(1, 2));

        // Left Panel for Server Info
        JPanel serverPanel = new JPanel(new GridLayout(2, 1));
        serverIpLabel = new JLabel("Server IP: Initializing...");
        serverPasswordLabel = new JLabel("Password: " + PASSWORD);
        serverPanel.add(serverIpLabel);
        serverPanel.add(serverPasswordLabel);

        // Right Panel for Client Input
        JPanel clientPanel = new JPanel(new GridLayout(3, 1));
        targetIpField = new JTextField("Enter Target IP");
        targetPasswordField = new JPasswordField("123456");
        connectButton = new JButton("Connect");
        clientPanel.add(targetIpField);
        clientPanel.add(targetPasswordField);
        clientPanel.add(connectButton);

        add(serverPanel);
        add(clientPanel);

        startServer();

        connectButton.addActionListener(e -> connectToTarget());
    }

    private void startServer() {
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(PORT);
                String ipAddress = InetAddress.getLocalHost().getHostAddress();
                serverIpLabel.setText("Server IP: " + ipAddress);
                while (true) {
                    socket = serverSocket.accept();
                    setupStreams();
                    authenticateClient();
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Server Error: " + e.getMessage());
            }
        }).start();
    }

    private void connectToTarget() {
        String targetIp = targetIpField.getText();
        String password = new String(targetPasswordField.getPassword());

        try {
            socket = new Socket(targetIp, PORT);
            setupStreams();
            authenticateServer(password);
            openControlForm();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Connection Error: " + e.getMessage());
        }
    }

    private void setupStreams() throws IOException {
        outputStream = new ObjectOutputStream(socket.getOutputStream());
        inputStream = new ObjectInputStream(socket.getInputStream());
    }

    private void authenticateClient() throws IOException, ClassNotFoundException {
        outputStream.writeObject(PASSWORD);
        String response = (String) inputStream.readObject();
        if (!response.equals("Client authenticated")) {
            throw new SecurityException("Authentication Failed");
        }
    }

    private void authenticateServer(String password) throws IOException, ClassNotFoundException {
        outputStream.writeObject(password);
        String[] screenSize = ((String) inputStream.readObject()).split(",");
        screenWidthServer = Integer.parseInt(screenSize[0]);
        screenHeightServer = Integer.parseInt(screenSize[1]);
        JOptionPane.showMessageDialog(this, "Connected Successfully!");
    }

    private void openControlForm() {
        new ControlForm(socket, outputStream, inputStream, screenWidthServer, screenHeightServer).setVisible(true);
        this.dispose();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MainForm mainForm = new MainForm();
            mainForm.setVisible(true);
        });
    }
}

class ControlForm extends JFrame {
    private JLabel screenLabel;
    private JButton disconnectButton;
    private boolean isControlsVisible = false;
    private ObjectOutputStream outputStream;
    private ObjectInputStream inputStream;
    private Socket socket;

    public ControlForm(Socket socket, ObjectOutputStream outputStream, ObjectInputStream inputStream, int screenWidth, int screenHeight) {
        this.socket = socket;
        this.outputStream = outputStream;
        this.inputStream = inputStream;

        setTitle("Remote Desktop Viewer");
        setSize(1000, 700);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        screenLabel = new JLabel();
        screenLabel.setHorizontalAlignment(SwingConstants.CENTER);
        add(screenLabel, BorderLayout.CENTER);

        JPanel controlsPanel = new JPanel(new BorderLayout());
        JButton toggleControlsButton = new JButton("\u25BC"); // Arrow down
        disconnectButton = new JButton("Disconnect");
        disconnectButton.setVisible(false);

        controlsPanel.add(toggleControlsButton, BorderLayout.NORTH);
        controlsPanel.add(disconnectButton, BorderLayout.CENTER);
        add(controlsPanel, BorderLayout.SOUTH);

        toggleControlsButton.addActionListener(e -> {
            isControlsVisible = !isControlsVisible;
            disconnectButton.setVisible(isControlsVisible);
            toggleControlsButton.setText(isControlsVisible ? "\u25B2" : "\u25BC");
        });

        disconnectButton.addActionListener(e -> disconnect());
        new Thread(this::receiveScreen).start();
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
            JOptionPane.showMessageDialog(this, "Connection Lost!");
            disconnect();
        }
    }

    private void disconnect() {
        try {
            if (socket != null) socket.close();
            JOptionPane.showMessageDialog(this, "Disconnected Successfully!");
            System.exit(0);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error Disconnecting: " + e.getMessage());
        }
    }
}
