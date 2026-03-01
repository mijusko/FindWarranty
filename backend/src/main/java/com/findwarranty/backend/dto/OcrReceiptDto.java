package com.findwarranty.backend.dto;

/**
 * Extracted receipt data from OCR, for pre-filling the form.
 * User can edit any field before saving.
 */
public class OcrReceiptDto {
    private String storeName;
    private String productName;
    private String purchaseDate; // ISO date string YYYY-MM-DD for input[type=date]
    private String price;

    public OcrReceiptDto() {}

    public OcrReceiptDto(String storeName, String productName, String purchaseDate, String price) {
        this.storeName = storeName;
        this.productName = productName;
        this.purchaseDate = purchaseDate;
        this.price = price;
    }

    public String getStoreName() { return storeName; }
    public void setStoreName(String storeName) { this.storeName = storeName; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getPurchaseDate() { return purchaseDate; }
    public void setPurchaseDate(String purchaseDate) { this.purchaseDate = purchaseDate; }

    public String getPrice() { return price; }
    public void setPrice(String price) { this.price = price; }
}
