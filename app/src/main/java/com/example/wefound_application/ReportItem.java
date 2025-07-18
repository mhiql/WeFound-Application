package com.example.wefound_application;

public class ReportItem {
    private String imageUrl;
    private String documentId;
    private String type;

    public ReportItem(String imageUrl, String documentId, String type) {
        this.imageUrl = imageUrl;
        this.documentId = documentId;
        this.type = type;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getDocumentId() {
        return documentId;
    }

    public String getType() {
        return type;
    }
}
