package doan_dieukhienmaytinh;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
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
        setSize(1000, 700);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel topPanel = new JPanel(new GridLayout(1, 3));
        serverIpField = new JTextField("Nhập IP Server");
        passwordField = new JPasswordField("123456");
        connectButton = new JButton("Connect");
        JButton sendFileButton = new JButton("Gửi File");

        topPanel.add(serverIpField);
        topPanel.add(passwordField);
        topPanel.add(connectButton);
        topPanel.add(sendFileButton);


        screenLabel = new JLabel();
        screenLabel.setHorizontalAlignment(SwingConstants.CENTER);

        add(topPanel, BorderLayout.NORTH);
        add(screenLabel, BorderLayout.CENTER);

        connectButton.addActionListener(e -> connectToServer());
        sendFileButton.addActionListener(e -> sendFile());

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

                // Lấy kích thước khung hiển thị
                int width = screenLabel.getWidth();
                int height = screenLabel.getHeight();

                // Điều chỉnh kích thước ảnh theo khung hiển thị
                Image scaledImage = icon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
                screenLabel.setIcon(new ImageIcon(scaledImage));
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
                    outputStream.writeObject("mouse," + e.getX() + "," + e.getY());
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
                    outputStream.writeObject("key," + e.getKeyCode());
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    private void sendFile() {
        JFileChooser fileChooser = new JFileChooser();
        int returnValue = fileChooser.showOpenDialog(this);

        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();

            try {
                // Gửi lệnh gửi file
                outputStream.writeObject("sendfile");

                // Gửi tên file
                outputStream.writeObject(file.getName());

                // Gửi kích thước file
                long fileSize = file.length();
                outputStream.writeLong(fileSize);

                // Gửi nội dung file
                byte[] buffer = new byte[1024];
                try (FileInputStream fileInputStream = new FileInputStream(file)) {
                    int bytesRead;
                    while ((bytesRead = fileInputStream.read(buffer)) > 0) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                }
                outputStream.flush();
                JOptionPane.showMessageDialog(this, "File đã được gửi!");
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Lỗi khi gửi file: " + e.getMessage());
            }
        }
    }
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ClientForm clientForm = new ClientForm();
            clientForm.setVisible(true);
        });
    }
}
