package vn.edu.tdtu.edocument;

import java.awt.*;
import java.io.File;
import java.util.UUID;
import javax.swing.*;
import vn.edu.tdtu.edocument.model.Document;
import vn.edu.tdtu.edocument.service.DocumentProcessor;

public class AddDocumentDialog extends JDialog {

    private JTextField txtApplicantName, txtApplicantEmail, txtApplicantPhone;
    private JTextField txtOfficerName, txtOfficerEmail, txtOfficerPhone;
    private JComboBox<String> cbDocumentType;
    private JTextField txtDigitalSignature;
    private JLabel lblFileName;
    private File selectedFile;

    private DocumentProcessor processor;
    private MainSwingUI parent;
    private Document editingDoc;

    public AddDocumentDialog(MainSwingUI parent, DocumentProcessor processor, Document editingDoc) {
        super(parent, editingDoc == null ? "Tiếp nhận hồ sơ mới" : "Chỉnh sửa hồ sơ nháp", true);
        this.parent = parent;
        this.processor = processor;
        this.editingDoc = editingDoc;

        setSize(450, 550);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout());

        JPanel formPanel = new JPanel(new GridLayout(10, 2, 5, 10));
        formPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        formPanel.add(new JLabel("Tên người nộp:"));
        txtApplicantName = new JTextField();
        formPanel.add(txtApplicantName);

        formPanel.add(new JLabel("Email người nộp:"));
        txtApplicantEmail = new JTextField();
        formPanel.add(txtApplicantEmail);

        formPanel.add(new JLabel("SĐT người nộp:"));
        txtApplicantPhone = new JTextField();
        formPanel.add(txtApplicantPhone);

        formPanel.add(new JLabel("Tên cán bộ:"));
        txtOfficerName = new JTextField("Cán bộ trực ban");
        formPanel.add(txtOfficerName);

        formPanel.add(new JLabel("Email cán bộ:"));
        txtOfficerEmail = new JTextField("officer@tdtu.edu.vn");
        formPanel.add(txtOfficerEmail);

        formPanel.add(new JLabel("SĐT cán bộ:"));
        txtOfficerPhone = new JTextField("0123456789");
        formPanel.add(txtOfficerPhone);

        formPanel.add(new JLabel("Loại hồ sơ:"));
        cbDocumentType = new JComboBox<>(new String[]{"DON_XIN_PHEP", "BAO_CAO", "HO_SO_THUE"});
        formPanel.add(cbDocumentType);

        formPanel.add(new JLabel("Chữ ký số:"));
        txtDigitalSignature = new JTextField();
        formPanel.add(txtDigitalSignature);

        formPanel.add(new JLabel("Tập tin đính kèm:"));
        JButton btnFile = new JButton("Chọn...");
        lblFileName = new JLabel("Chưa chọn");
        JPanel pFile = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        pFile.add(btnFile);
        pFile.add(lblFileName);
        formPanel.add(pFile);

        add(formPanel, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnSend = new JButton("Gửi");
        JButton btnCancel = new JButton("Hủy");
        JButton btnDraft = new JButton("Lưu nháp"); // Thêm nút 'Lưu nháp' để thực hiện YC1
        btnPanel.add(btnSend);
        btnPanel.add(btnCancel);
        btnPanel.add(btnDraft);
        add(btnPanel, BorderLayout.SOUTH);

        if (this.editingDoc != null) {
            txtApplicantName.setText(editingDoc.getApplicantName());
            txtApplicantEmail.setText(editingDoc.getApplicantEmail());
            txtApplicantPhone.setText(editingDoc.getApplicantPhone());
            txtOfficerName.setText(editingDoc.getOfficerName());
            txtOfficerEmail.setText(editingDoc.getOfficerEmail());
            txtOfficerPhone.setText(editingDoc.getOfficerPhone());
            cbDocumentType.setSelectedItem(editingDoc.getDocumentType());
            txtDigitalSignature.setText(editingDoc.getDigitalSignature());
            if (editingDoc.getFilePath() != null && !editingDoc.getFilePath().isEmpty()) {
                selectedFile = new File(editingDoc.getFilePath());
                lblFileName.setText(selectedFile.getName());
            }
        }

        btnFile.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                selectedFile = fc.getSelectedFile();
                lblFileName.setText(selectedFile.getName());
            }
        });

        btnCancel.addActionListener(e -> dispose());

        btnSend.addActionListener(e -> {
            submitAction(false);
        });
        btnDraft.addActionListener(e -> {
            submitAction(true);
        });
    }

    private void submitAction(boolean isDraft) {
        String filePath = (selectedFile != null) ? selectedFile.getAbsolutePath() : "";
        String ext = "";
        long size = 0;
        if (selectedFile != null) {
            size = selectedFile.length() / 1024;
            String name = selectedFile.getName();
            int lastDot = name.lastIndexOf('.');
            if (lastDot > 0) {
                ext = name.substring(lastDot + 1);
            }
        }

        String docId = (editingDoc != null) ? editingDoc.getId() : UUID.randomUUID().toString().substring(0, 8);
        Document.DocumentBuilder builder = new Document.DocumentBuilder(
                docId)
                .withApplicantInfo(txtApplicantName.getText().trim(), txtApplicantEmail.getText().trim(), txtApplicantPhone.getText().trim())
                .withOfficerInfo(txtOfficerName.getText().trim(), txtOfficerEmail.getText().trim(), txtOfficerPhone.getText().trim(), cbDocumentType.getSelectedItem().toString());

        if (selectedFile != null || !txtDigitalSignature.getText().trim().isEmpty()) {
            builder.withFileInfo(filePath, ext, size, txtDigitalSignature.getText().trim());
        }

        if (isDraft) {
            builder.withStatus("LUU_NHAP");
            Document draftDoc = builder.build(); 
            
            processor.saveDraft(draftDoc); 
            if (editingDoc != null) {
                parent.updateDocumentInList(draftDoc);
            } else {
                parent.addDocumentToList(draftDoc);  
            }
            JOptionPane.showMessageDialog(this, "Đã lưu nháp hồ sơ thành công!");
            dispose();
        } else {
            builder.withStatus("MOI_TAO");
            Document doc = builder.build(); 

            processor.process(doc);
            if ("DANG_XET_DUYET".equals(doc.getStatus())) {
                if (editingDoc != null) {
                    parent.updateDocumentInList(doc);
                } else {
                    parent.addDocumentToList(doc);
                }
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Hồ sơ không hợp lệ. Vui lòng kiểm tra lại log.", "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
