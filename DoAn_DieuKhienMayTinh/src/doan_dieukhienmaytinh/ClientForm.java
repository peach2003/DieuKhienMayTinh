package doan_dieukhienmaytinh;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;

public class ClientForm extends JFrame {
    private JTextField ipField;
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

        JPanel topPanel = new JPanel(new GridLayout(1, 2));
        ipField = new JTextField("Nhập IP");
        passwordField = new JPasswordField("Nhập mật khẩu");
        connectButton = new JButton("Connect");
        topPanel.add(ipField);
        topPanel.add(passwordField);
        topPanel.add(connectButton);

        screenLabel = new JLabel();
        screenLabel.setHorizontalAlignment(SwingConstants.CENTER);

        add(topPanel, BorderLayout.NORTH);
        add(screenLabel, BorderLayout.CENTER);

        connectButton.addActionListener(e -> connectToServer());
    }

    private void connectToServer() {
        String ip = ipField.getText();
        String password = new String(passwordField.getPassword());

        try {
            socket = new Socket(ip, 5000);
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
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Lỗi kết nối: " + e.getMessage());
        }
    }

    private void receiveScreen() {
        try {
            while (true) {
                byte[] imageBytes = (byte[]) inputStream.readObject();
                ImageIcon icon = new ImageIcon(imageBytes);

                // Hiển thị hình ảnh với kích thước giao diện
                Image scaledImage = icon.getImage().getScaledInstance(screenLabel.getWidth(),
                        screenLabel.getHeight(), Image.SCALE_SMOOTH);
                screenLabel.setIcon(new ImageIcon(scaledImage));
                screenLabel.repaint();
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Mất kết nối với Server!");
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ClientForm clientForm = new ClientForm();
            clientForm.setVisible(true);
        });
    }
}
