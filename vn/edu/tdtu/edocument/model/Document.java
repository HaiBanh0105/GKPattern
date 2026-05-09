package vn.edu.tdtu.edocument.model;

public class Document {
    private String id;
    private String applicantName;
    private String applicantEmail;
    private String applicantPhone;
    private String officerName;
    private String officerEmail;
    private String officerPhone;
    private String documentType;
    private String filePath;
    private String fileExtension;
    private long fileSizeKB;
    private String digitalSignature;
    private String extractedContent;
    private String status;

    // Chỉ cho phép khởi tạo thông qua Builder
    private Document(DocumentBuilder builder) {
        this.id = builder.id;
        this.applicantName = builder.applicantName;
        this.applicantEmail = builder.applicantEmail;
        this.applicantPhone = builder.applicantPhone;
        this.officerName = builder.officerName;
        this.officerEmail = builder.officerEmail;
        this.officerPhone = builder.officerPhone;
        this.documentType = builder.documentType;
        this.filePath = builder.filePath;
        this.fileExtension = builder.fileExtension;
        this.fileSizeKB = builder.fileSizeKB;
        this.digitalSignature = builder.digitalSignature;
        this.extractedContent = builder.extractedContent;
        this.status = builder.status;
    }

    // --- CÁC HÀM GETTER/SETTER CƠ BẢN ---
    public String getId() {
        return id;
    }

    public String getApplicantName() {
        return applicantName;
    }

    public String getApplicantEmail() {
        return applicantEmail;
    }

    public String getApplicantPhone() {
        return applicantPhone;
    }

    public String getOfficerName() {
        return officerName;
    }

    public String getOfficerEmail() {
        return officerEmail;
    }

    public String getOfficerPhone() {
        return officerPhone;
    }

    public String getDocumentType() {
        return documentType;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getFileExtension() {
        return fileExtension;
    }

    public long getFileSizeKB() {
        return fileSizeKB;
    }

    public String getDigitalSignature() {
        return digitalSignature;
    }

    public String getExtractedContent() {
        return extractedContent;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setExtractedContent(String extractedContent) {
        this.extractedContent = extractedContent;
    }

    // Thêm Builder Pattern vào Document - YC1
    public static class DocumentBuilder {
        private String id;
        private String applicantName = "";
        private String applicantEmail = "";
        private String applicantPhone = "";
        private String officerName = "";
        private String officerEmail = "";
        private String officerPhone = "";
        private String documentType = "";
        private String filePath = "";
        private String fileExtension = "";
        private long fileSizeKB = 0;
        private String digitalSignature = "";
        private String extractedContent = "";
        private String status = "LUU_NHAP"; 

        public DocumentBuilder(String id) {
            this.id = id;
        }

        public DocumentBuilder withApplicantInfo(String name, String email, String phone) {
            this.applicantName = name;
            this.applicantEmail = email;
            this.applicantPhone = phone;
            return this;
        }

        public DocumentBuilder withOfficerInfo(String officerName, String officerEmail, String officerPhone, String documentType) {
            this.officerName = officerName;
            this.officerEmail = officerEmail;
            this.officerPhone = officerPhone;
            this.documentType = documentType;
            return this;
        }

        public DocumentBuilder withFileInfo(String filePath, String extension, long size, String signature) {
            this.filePath = filePath;
            this.fileExtension = extension;
            this.fileSizeKB = size;
            this.digitalSignature = signature;
            return this;
        }

        public DocumentBuilder withStatus(String status) {
            this.status = status;
            return this;
        }

        public Document build() {
            return new Document(this);
        }
    }

    @Override
    public String toString() {
        return String.format("Hồ sơ [%s] - Nộp bởi: %s - Trạng thái: %s", id, applicantName, status);
    }
}
