package vn.edu.tdtu.edocument;

import java.awt.*;
import java.io.File;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import vn.edu.tdtu.edocument.model.Document;
import vn.edu.tdtu.edocument.service.DocumentProcessor;

public class MainSwingUI extends JFrame {

    private JTextArea consoleArea;
    private JTable documentTable;
    private DefaultTableModel tableModel;
    private DocumentProcessor processor;
    private List<Document> documentList;

    public MainSwingUI() {
        processor = new DocumentProcessor();
        documentList = new ArrayList<>();

        setTitle("Hệ thống Quản lý Hồ sơ Điện tử - v1.0 (Home)");
        setSize(900, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        String[] columnNames = {"Mã hồ sơ", "Người nộp", "Loại hồ sơ", "Trạng thái", "Tập tin"};
        tableModel = new DefaultTableModel(columnNames, 0);
        documentTable = new JTable(tableModel);
        JScrollPane tableScrollPane = new JScrollPane(documentTable);
        tableScrollPane.setBorder(BorderFactory.createTitledBorder("Danh sách hồ sơ hệ thống"));

        consoleArea = new JTextArea();
        consoleArea.setEditable(false);
        consoleArea.setBackground(new Color(30, 30, 30));
        consoleArea.setForeground(Color.GREEN);
        consoleArea.setFont(new Font("Consolas", Font.PLAIN, 13));
        JScrollPane logScrollPane = new JScrollPane(consoleArea);
        logScrollPane.setBorder(BorderFactory.createTitledBorder("Hệ thống Log/Thông báo"));

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tableScrollPane, logScrollPane);
        splitPane.setDividerLocation(300);
        add(splitPane, BorderLayout.CENTER);

        JPanel toolBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnAdd = new JButton("Thêm mới hồ sơ");
        JButton btnClear = new JButton("Xóa Log");
        toolBar.add(btnAdd);
        toolBar.add(btnClear);
        add(toolBar, BorderLayout.NORTH);

        // Nút 'Tiếp tục chỉnh sửa' dành cho lưu nháp - YC1
        JButton btnEdit = new JButton("Tiếp tục chỉnh sửa");
        btnEdit.setEnabled(false);
        toolBar.add(btnEdit);

        redirectSystemStreams();
        loadExistingDocuments();
        refreshTable();

        btnAdd.addActionListener(e -> {
            AddDocumentDialog dialog = new AddDocumentDialog(this, processor, null);
            dialog.setVisible(true);
            refreshTable();
        });

        btnClear.addActionListener(e -> consoleArea.setText(""));

        btnEdit.addActionListener(e -> {
            int selectedRow = documentTable.getSelectedRow();
            if (selectedRow != -1) {
                String docId = tableModel.getValueAt(selectedRow, 0).toString();
                for (Document doc : documentList) {
                    if (doc.getId().equals(docId) && "LUU_NHAP".equals(doc.getStatus())) {
                        AddDocumentDialog dialog = new AddDocumentDialog(MainSwingUI.this, processor, doc);
                        dialog.setVisible(true);
                        refreshTable();
                        break;
                    }
                }
            }
        });

        documentTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && documentTable.getSelectedRow() != -1) {
                int selectedRow = documentTable.getSelectedRow();
                String docId = tableModel.getValueAt(selectedRow, 0).toString();

                for (Document doc : documentList) {
                    if (doc.getId().equals(docId)) {
                        System.out.println("\n--- CHI TIẾT HỒ SƠ: " + doc.getId() + " ---");
                        System.out.println("Người nộp: " + doc.getApplicantName() + " | Email: " + doc.getApplicantEmail() + " | SĐT: " + doc.getApplicantPhone());
                        System.out.println("Cán bộ tiếp nhận: " + doc.getOfficerName() + " | Email: " + doc.getOfficerEmail() + " | SĐT: " + doc.getOfficerPhone());
                        System.out.println("Loại hồ sơ: " + doc.getDocumentType());
                        System.out.println("Đường dẫn tệp: " + doc.getFilePath() + " (" + doc.getFileSizeKB() + " KB)");
                        System.out.println("Chữ ký số: " + doc.getDigitalSignature());
                        System.out.println("Trạng thái hiện tại: " + doc.getStatus());
                        System.out.println("----------------------------------------\n");

                        btnEdit.setEnabled("LUU_NHAP".equals(doc.getStatus()));
                        break;
                    }
                }
            } else if (!e.getValueIsAdjusting() && documentTable.getSelectedRow() == -1) {
                btnEdit.setEnabled(false);
            }
        });

        documentTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2 && documentTable.getSelectedRow() != -1) {
                    int selectedRow = documentTable.getSelectedRow();
                    String docId = tableModel.getValueAt(selectedRow, 0).toString();
                    for (Document doc : documentList) {
                        if (doc.getId().equals(docId)) {
                            if ("LUU_NHAP".equals(doc.getStatus())) {
                                AddDocumentDialog dialog = new AddDocumentDialog(MainSwingUI.this, processor, doc);
                                dialog.setVisible(true);
                                refreshTable();
                            }
                            break;
                        }
                    }
                }
            }
        });
    }

    private void loadExistingDocuments() {
        File storageDir = new File("server_storage");
        if (storageDir.exists() && storageDir.isDirectory()) {
            File[] files = storageDir.listFiles((dir, name) -> name.endsWith("_data.json"));
            if (files != null) {
                for (File file : files) {
                    try {
                        String content = new String(Files.readAllBytes(file.toPath()));
                        Document doc = parseJsonToDocument(content);
                        if (doc != null) {
                            documentList.add(doc);
                        }
                    } catch (Exception e) {
                        System.out.println("[LỖI LOAD] Không thể nạp hồ sơ: " + file.getName());
                    }
                }
            }
        }
    }

    private Document parseJsonToDocument(String json) {
        try {
            String id = extractValue(json, "id");
            String applicantName = extractValue(json, "applicantName");
            String applicantEmail = extractValue(json, "applicantEmail");
            String applicantPhone = extractValue(json, "applicantPhone");
            String officerName = extractValue(json, "officerName");
            String officerEmail = extractValue(json, "officerEmail");
            String officerPhone = extractValue(json, "officerPhone");
            String documentType = extractValue(json, "documentType");
            String filePath = extractValue(json, "filePath");
            String fileExtension = extractValue(json, "fileExtension");
            long fileSizeKB = Long.parseLong(extractValue(json, "fileSizeKB"));
            String digitalSignature = extractValue(json, "digitalSignature");
            String status = extractValue(json, "status");

            return new Document.DocumentBuilder(id)
                    .withApplicantInfo(applicantName, applicantEmail, applicantPhone)
                    .withOfficerInfo(officerName, officerEmail, officerPhone, documentType)
                    .withFileInfo(filePath, fileExtension, fileSizeKB, digitalSignature)
                    .withStatus(status)
                    .build();
        } catch (Exception e) {
            return null;
        }
    }

    private String extractValue(String json, String key) {
        String pattern = "\"" + key + "\": ";
        int start = json.indexOf(pattern) + pattern.length();
        if (json.charAt(start) == '\"') {
            start++;
            int end = json.indexOf("\"", start);
            return json.substring(start, end);
        } else {
            int end = json.indexOf(",", start);
            if (end == -1) {
                end = json.indexOf("\n", start);
            }
            return json.substring(start, end).trim();
        }
    }

    public void addDocumentToList(Document doc) {
        documentList.add(doc);
    }

    public void updateDocumentInList(Document updatedDoc) {
        for (int i = 0; i < documentList.size(); i++) {
            if (documentList.get(i).getId().equals(updatedDoc.getId())) {
                documentList.set(i, updatedDoc);
                break;
            }
        }
        refreshTable();
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        for (Document doc : documentList) {
            tableModel.addRow(new Object[]{
                doc.getId(), doc.getApplicantName(), doc.getDocumentType(), doc.getStatus(), doc.getFileExtension()
            });
        }
    }

    private void redirectSystemStreams() {
        OutputStream out = new OutputStream() {
            @Override
            public void write(int b) {
                updateTextArea(String.valueOf((char) b));
            }

            @Override
            public void write(byte[] b, int off, int len) {
                updateTextArea(new String(b, off, len));
            }
        };
        System.setOut(new PrintStream(out, true));
        System.setErr(new PrintStream(out, true));
    }

    private void updateTextArea(final String text) {
        SwingUtilities.invokeLater(() -> {
            consoleArea.append(text);
            consoleArea.setCaretPosition(consoleArea.getDocument().getLength());
        });
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
        }
        SwingUtilities.invokeLater(() -> new MainSwingUI().setVisible(true));
    }
}
