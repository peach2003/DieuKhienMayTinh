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
        JPanel clientPanel = new JPanel();
        clientPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        targetIpField = new JTextField("Enter Target IP");
        targetIpField.setPreferredSize(new Dimension(150, 25));
        targetPasswordField = new JPasswordField("123456");
        targetPasswordField.setPreferredSize(new Dimension(150, 25));
        connectButton = new JButton("Connect");
        connectButton.setPreferredSize(new Dimension(100, 30));

        gbc.gridx = 0;
        gbc.gridy = 0;
        clientPanel.add(new JLabel("Target IP:"), gbc);
        gbc.gridx = 1;
        clientPanel.add(targetIpField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        clientPanel.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1;
        clientPanel.add(targetPasswordField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        clientPanel.add(connectButton, gbc);

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
                    logMessage("Server is listening on port " + PORT);
                    socket = serverSocket.accept();
                    logMessage("Client connected: " + socket.getInetAddress());
                    setupStreams();
                    authenticateClient();
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Server Error: " + e.getMessage());
            }
        }).start();
    }

    private void connectToTarget() {
        String targetIp = targetIpField.getText().trim();
        String password = new String(targetPasswordField.getPassword());

        if (targetIp.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter valid IP and password!");
            return;
        }

        new Thread(() -> {
            try {
                logMessage("Connecting to " + targetIp + " on port " + PORT);
                socket = new Socket(targetIp, PORT);
                setupStreams();
                authenticateServer(password);
                openControlForm();
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Connection Error: Unable to connect to " + targetIp);
                logMessage("Connection failed: " + e.getMessage());
            }
        }).start();
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

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        outputStream.writeObject(screenSize.width + "," + screenSize.height);
    }

    private void authenticateServer(String password) throws IOException, ClassNotFoundException {
        outputStream.writeObject(password);
        String screenSizeData = (String) inputStream.readObject();
        String[] screenSize = screenSizeData.split(",");

        if (screenSize.length < 2) {
            throw new IOException("Invalid screen size data received: " + screenSizeData);
        }

        screenWidthServer = Integer.parseInt(screenSize[0]);
        screenHeightServer = Integer.parseInt(screenSize[1]);
        JOptionPane.showMessageDialog(this, "Connected Successfully!");
    }

    private void openControlForm() {
        SwingUtilities.invokeLater(() -> {
            new ControlForm(socket, outputStream, inputStream, screenWidthServer, screenHeightServer).setVisible(true);
            this.dispose();
        });
    }

    private void logMessage(String message) {
        System.out.println(message);
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
        disconnectButton.setPreferredSize(new Dimension(100, 30));
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
                if (imageBytes == null || imageBytes.length == 0) {
                    throw new IOException("Received empty screen data");
                }
                ImageIcon icon = new ImageIcon(imageBytes);
                int width = screenLabel.getWidth();
                int height = screenLabel.getHeight();
                Image scaledImage = icon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
                screenLabel.setIcon(new ImageIcon(scaledImage));
                screenLabel.repaint();
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Connection Lost: " + e.getMessage());
            disconnect();
        }
    }

    private void disconnect() {
        try {
            if (socket != null) socket.close();
            System.out.println("Disconnected successfully");
            JOptionPane.showMessageDialog(this, "Disconnected Successfully!");
            System.exit(0);
        } catch (IOException e) {
            System.err.println("Error while disconnecting: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Error Disconnecting: " + e.getMessage());
        }
    }
}
