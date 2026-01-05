package com.weddingapp.model;

public class MenuItem {
    private int id;
    private String title;
    private double price;
    private String category = "single"; // single | combo
    private String subCategory; // KHAI_VI, MON_CHINH, MON_PHU, DO_UONG

    public MenuItem() {
    }

    public MenuItem(int id, String title, double price, String category) {
        this.id = id;
        this.title = title;
        this.price = price;
        this.category = category;
    }

    public MenuItem(int id, String title, double price, String category, String subCategory) {
        this.id = id;
        this.title = title;
        this.price = price;
        this.category = category;
        this.subCategory = subCategory;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getSubCategory() {
        return subCategory;
    }

    public void setSubCategory(String subCategory) {
        this.subCategory = subCategory;
    }
    
    /**
     * Map tên món sang group để filter và limit
     * Returns: "appetizer" | "main" | "side" | "drink" | "dessert" | null (cho combo)
     */
    public String getGroup() {
        if ("combo".equalsIgnoreCase(category)) {
            return null; // Combo không có group
        }
        
        String titleLower = title.toLowerCase();
        
        // drink (Nước)
        if (titleLower.contains("bia") || titleLower.contains("rượu") || titleLower.contains("vodka") 
            || titleLower.contains("vang") || titleLower.contains("coca") || titleLower.contains("pepsi")
            || titleLower.contains("schweppes") || titleLower.contains("nước") || titleLower.contains("soda")
            || titleLower.contains("đồ uống") || "DO_UONG".equals(subCategory)) {
            return "drink";
        }
        
        // dessert (Tráng miệng)
        if (titleLower.contains("tráng miệng") || titleLower.contains("kem") || titleLower.contains("chè")
            || titleLower.contains("bánh") || titleLower.contains("trái cây") || titleLower.contains("fruit")) {
            return "dessert";
        }
        
        // appetizer (Khai vị)
        if (titleLower.contains("gỏi") || titleLower.contains("salad") || titleLower.contains("chả giò")
            || titleLower.contains("nem") || titleLower.contains("cuốn") || titleLower.contains("súp")
            || titleLower.contains("khai vị") || "KHAI_VI".equals(subCategory)) {
            return "appetizer";
        }
        
        // side (Món phụ)
        if (titleLower.contains("rau") || titleLower.contains("đậu") || titleLower.contains("xôi")
            || titleLower.contains("canh") || "MON_PHU".equals(subCategory)) {
            return "side";
        }
        
        // main (Món chính) - default hoặc có MON_CHINH
        if ("MON_CHINH".equals(subCategory) || titleLower.contains("bò") || titleLower.contains("gà")
            || titleLower.contains("cá") || titleLower.contains("heo") || titleLower.contains("vịt")
            || titleLower.contains("lẩu") || titleLower.contains("hải sản") || titleLower.contains("tôm")
            || titleLower.contains("nướng") || titleLower.contains("quay") || titleLower.contains("chiên")) {
            return "main";
        }
        
        // Default là main nếu không match
        return "main";
    }

    @Override
    public String toString() {
        return title;
    }
}

