package vn.edu.tdtu.edocument.service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import vn.edu.tdtu.edocument.model.Document;

public class DocumentProcessor {

    public void process(Document doc) {
        System.out.println("\n=======================================================");
        System.out.println("BẮT ĐẦU XỬ LÝ HỒ SƠ ID: " + doc.getId());

        if (doc.getId() == null || doc.getId().isEmpty()
                || doc.getApplicantName() == null || doc.getApplicantName().isEmpty()
                || doc.getApplicantEmail() == null || doc.getApplicantEmail().isEmpty()
                || doc.getApplicantPhone() == null || doc.getApplicantPhone().isEmpty()
                || doc.getOfficerName() == null || doc.getOfficerName().isEmpty()
                || doc.getOfficerEmail() == null || doc.getOfficerEmail().isEmpty()
                || doc.getOfficerPhone() == null || doc.getOfficerPhone().isEmpty()
                || doc.getDocumentType() == null || doc.getDocumentType().isEmpty()
                || doc.getFilePath() == null || doc.getFilePath().isEmpty()
                || doc.getFileExtension() == null || doc.getFileExtension().isEmpty()
                || doc.getDigitalSignature() == null || doc.getDigitalSignature().isEmpty()) {

            System.out.println("[LỖI TIẾP NHẬN] Thiếu trường thông tin bắt buộc. Hủy tạo hồ sơ.");
            return;
        }

        doc.setStatus("DA_TIEP_NHAN");
        sendNotifications(doc);

        System.out.println("[KIỂM DUYỆT] Đang kiểm tra dung lượng và định dạng...");
        if (doc.getFileSizeKB() > 5120) {
            System.out.println("[TỪ CHỐI] Dung lượng file " + doc.getFileSizeKB() + "KB vượt quá 5MB.");
            doc.setStatus("TU_CHOI");
            sendNotifications(doc);
            return;
        }

        if (!doc.getFileExtension().equalsIgnoreCase("txt")) {
            System.out.println("[TỪ CHỐI] Định dạng " + doc.getFileExtension() + " không được hỗ trợ ở v1.0.");
            doc.setStatus("TU_CHOI");
            sendNotifications(doc);
            return;
        }

        System.out.println("[TRÍCH XUẤT] Đang đọc nội dung tệp đính kèm...");
        try {
            String content = new String(Files.readAllBytes(Paths.get(doc.getFilePath())));
            doc.setExtractedContent(content);
        } catch (IOException e) {
            System.out.println("[LỖI] Không thể đọc nội dung file: " + e.getMessage());
            doc.setStatus("TU_CHOI");
            sendNotifications(doc);
            return;
        }

        System.out.println("[LƯU TRỮ] Đang sao chép file và xuất dữ liệu JSON...");
        saveToStorage(doc);

        System.out.println("[HOÀN TẤT] Hồ sơ hợp lệ và đã được lưu trữ thành công.");
        doc.setStatus("DANG_XET_DUYET");
        sendNotifications(doc);
    }

    public void saveDraft(Document doc) {
        System.out.println("\n=======================================================");
        System.out.println("[LƯU NHÁP] Đang lưu trữ vật lý hồ sơ nháp ID: " + doc.getId());
        doc.setStatus("LUU_NHAP");
        saveToStorage(doc);
    }

    private void saveToStorage(Document doc) {
        String storageDirPath = "server_storage";
        File storageDir = new File(storageDirPath);
        if (!storageDir.exists()) {
            storageDir.mkdir();
        }

        try {
            String targetPathString = "";
            if (doc.getFilePath() != null && !doc.getFilePath().isEmpty()) {
                Path sourcePath = Paths.get(doc.getFilePath());
                String fileName = sourcePath.getFileName().toString();
                // Tránh tình trạng tiền tố ID bị nối dài thêm khi người dùng tiếp tục chỉnh sửa nháp
                if (!fileName.startsWith(doc.getId() + "_")) {
                    fileName = doc.getId() + "_" + fileName;
                }
                Path targetPath = Paths.get(storageDirPath + File.separator + fileName);

                if (Files.exists(sourcePath)) {
                    if (!sourcePath.toAbsolutePath().normalize().equals(targetPath.toAbsolutePath().normalize())) {
                        Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                    }
                    targetPathString = targetPath.toString().replace("\\", "\\\\");
                } else {
                    targetPathString = doc.getFilePath().replace("\\", "\\\\");
                }
            }

            String json = "{\n"
                    + "  \"id\": \"" + doc.getId() + "\",\n"
                    + "  \"applicantName\": \"" + doc.getApplicantName() + "\",\n"
                    + "  \"applicantEmail\": \"" + doc.getApplicantEmail() + "\",\n"
                    + "  \"applicantPhone\": \"" + doc.getApplicantPhone() + "\",\n"
                    + "  \"officerName\": \"" + doc.getOfficerName() + "\",\n"
                    + "  \"officerEmail\": \"" + doc.getOfficerEmail() + "\",\n"
                    + "  \"officerPhone\": \"" + doc.getOfficerPhone() + "\",\n"
                    + "  \"documentType\": \"" + doc.getDocumentType() + "\",\n"
                    + "  \"filePath\": \"" + targetPathString + "\",\n"
                    + "  \"fileExtension\": \"" + doc.getFileExtension() + "\",\n"
                    + "  \"fileSizeKB\": " + doc.getFileSizeKB() + ",\n"
                    + "  \"digitalSignature\": \"" + doc.getDigitalSignature() + "\",\n"
                    + "  \"status\": \"" + doc.getStatus() + "\"\n"
                    + "}";

            File dataFile = new File(storageDirPath + File.separator + doc.getId() + "_data.json");
            FileWriter writer = new FileWriter(dataFile);
            writer.write(json);
            writer.close();

        } catch (IOException e) {
            System.out.println("[LỖI HỆ THỐNG] Lỗi khi lưu trữ vật lý: " + e.getMessage());
        }
    }

    private void sendNotifications(Document doc) {
        System.out.println("  [GỬI EMAIL] -> Người nộp (" + doc.getApplicantEmail() + "): Hồ sơ chuyển sang trạng thái " + doc.getStatus());
        System.out.println("  [GỬI SMS]   -> Người nộp (" + doc.getApplicantPhone() + "): Hồ sơ chuyển sang trạng thái " + doc.getStatus());
        System.out.println("  [GỬI EMAIL] -> Cán bộ xử lý (" + doc.getOfficerEmail() + "): Hồ sơ chuyển sang trạng thái " + doc.getStatus());
        System.out.println("  [GỬI SMS]   -> Cán bộ xử lý (" + doc.getOfficerPhone() + "): Hồ sơ chuyển sang trạng thái " + doc.getStatus());
    }
}
