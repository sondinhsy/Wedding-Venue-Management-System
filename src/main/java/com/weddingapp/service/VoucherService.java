package com.weddingapp.service;

import java.util.HashMap;
import java.util.Map;

/**
 * Service để kiểm tra và quản lý voucher
 */
public class VoucherService {
    
    // Stub data - trong thực tế sẽ query từ database
    private static final Map<String, VoucherInfo> voucherDatabase = new HashMap<>();
    
    static {
        // ===== MÙA THẤP ĐIỂM - THÁNG 6-8 (HÈ) =====
        // Voucher 5-15% cho booking trước 3 tháng
        voucherDatabase.put("HE2024S3M", new VoucherInfo("HE2024S3M", "percent", 12.0, true, 
            "Voucher mùa hè - Đặt trước 3 tháng", "Tháng 6-8"));
        voucherDatabase.put("SOMOMM2024", new VoucherInfo("SOMOMM2024", "percent", 15.0, true, 
            "Sớm 3 tháng mùa hè", "Tháng 6-8"));
        voucherDatabase.put("HE2024VIP", new VoucherInfo("HE2024VIP", "percent", 15.0, true, 
            "Voucher VIP mùa hè - Đặt sớm", "Tháng 6-8"));
        
        // Giảm 5-10% cho các ngày thứ 2-5
        voucherDatabase.put("T2T5HE2024", new VoucherInfo("T2T5HE2024", "percent", 8.0, true, 
            "Giảm giá thứ 2-5 mùa hè", "Tháng 6-8"));
        voucherDatabase.put("WEEKDAY10", new VoucherInfo("WEEKDAY10", "percent", 10.0, true, 
            "Ngày thường mùa hè", "Tháng 6-8"));
        voucherDatabase.put("WEEKDAY5", new VoucherInfo("WEEKDAY5", "percent", 5.0, true, 
            "Ngày thường giảm 5%", "Tháng 6-8"));
        
        // ===== MÙA THẤP ĐIỂM - THÁNG 1-2 (SAU TẾT) =====
        // Voucher 100$ - 500$ cho đơn trên 50 triệu
        voucherDatabase.put("XUAN2025500K", new VoucherInfo("XUAN2025500K", "amount", 500.0, true, 
            "Voucher chào xuân 500$", "Tháng 1-2"));
        voucherDatabase.put("XUAN2025300K", new VoucherInfo("XUAN2025300K", "amount", 300.0, true, 
            "Voucher chào xuân 300$", "Tháng 1-2"));
        voucherDatabase.put("TET2025", new VoucherInfo("TET2025", "amount", 500.0, true, 
            "Voucher sau Tết", "Tháng 1-2"));
        
        // "Voucher chào xuân" giảm 10-12%
        voucherDatabase.put("XUAN2025", new VoucherInfo("XUAN2025", "percent", 10.0, true, 
            "Voucher chào xuân 10%", "Tháng 1-2"));
        voucherDatabase.put("CHAOXUAN12", new VoucherInfo("CHAOXUAN12", "percent", 12.0, true, 
            "Chào xuân giảm 12%", "Tháng 1-2"));
        
        // ===== VOUCHER CƠ BẢN (Dùng quanh năm) =====
        voucherDatabase.put("GIAM5", new VoucherInfo("GIAM5", "percent", 5.0, true, 
            "Giảm giá 5%", "Quanh năm"));
        voucherDatabase.put("GIAM10", new VoucherInfo("GIAM10", "percent", 10.0, true, 
            "Giảm giá 10%", "Quanh năm"));
        voucherDatabase.put("GIAM15", new VoucherInfo("GIAM15", "percent", 15.0, true, 
            "Giảm giá 15%", "Quanh năm"));
        voucherDatabase.put("GIAM50K", new VoucherInfo("GIAM50K", "amount", 100.0, true, 
            "Giảm 100$", "Quanh năm"));
        voucherDatabase.put("GIAM100K", new VoucherInfo("GIAM100K", "amount", 150.0, true, 
            "Giảm 150$", "Quanh năm"));
        voucherDatabase.put("GIAM200K", new VoucherInfo("GIAM200K", "amount", 200.0, true, 
            "Giảm 200$", "Quanh năm"));
        voucherDatabase.put("GIAM500K", new VoucherInfo("GIAM500K", "amount", 500.0, true, 
            "Giảm 500$ (tối đa)", "Quanh năm"));
    }
    
    public static class VoucherInfo {
        private final String code;
        private final String type; // "percent" hoặc "amount"
        private final Double value;
        private final boolean valid;
        private final String description; // Mô tả voucher
        private final String season; // Mùa áp dụng
        
        public VoucherInfo(String code, String type, Double value, boolean valid) {
            this(code, type, value, valid, "", "");
        }
        
        public VoucherInfo(String code, String type, Double value, boolean valid, String description, String season) {
            this.code = code;
            this.type = type;
            this.value = value;
            this.valid = valid;
            this.description = description != null ? description : "";
            this.season = season != null ? season : "";
        }
        
        public String getCode() {
            return code;
        }
        
        public String getType() {
            return type;
        }
        
        public Double getValue() {
            return value;
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public String getDescription() {
            return description;
        }
        
        public String getSeason() {
            return season;
        }
        
        /**
         * Lấy mô tả hiển thị cho voucher
         */
        public String getDisplayText() {
            StringBuilder sb = new StringBuilder(code);
            if (!description.isEmpty()) {
                sb.append(" - ").append(description);
            }
            if ("percent".equalsIgnoreCase(type)) {
                sb.append(" (Giảm ").append(String.format("%.0f", value)).append("%)");
            } else {
                sb.append(" (Giảm $").append(String.format("%.0f", value)).append(")");
            }
            if (!season.isEmpty()) {
                sb.append(" - ").append(season);
            }
            return sb.toString();
        }
    }
    
    /**
     * Lấy tất cả voucher hiện có
     */
    public static java.util.List<VoucherInfo> getAllVouchers() {
        return new java.util.ArrayList<>(voucherDatabase.values());
    }
    
    /**
     * Lấy voucher theo mùa
     */
    public static java.util.List<VoucherInfo> getVouchersBySeason(String season) {
        return voucherDatabase.values().stream()
                .filter(v -> v.getSeason().equalsIgnoreCase(season))
                .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Kiểm tra voucher theo code
     * @param code Mã voucher
     * @return VoucherInfo nếu hợp lệ, null nếu không hợp lệ
     */
    public static VoucherInfo checkVoucher(String code) {
        if (code == null || code.trim().isEmpty()) {
            return null;
        }
        
        String upperCode = code.toUpperCase().trim();
        VoucherInfo voucher = voucherDatabase.get(upperCode);
        
        if (voucher != null && voucher.isValid()) {
            return voucher;
        }
        
        return null;
    }
    
    /**
     * Tính tổng tiền sau khi áp dụng voucher
     * @param originalTotal Tổng tiền gốc
     * @param voucherType Loại voucher ("percent" hoặc "amount")
     * @param voucherValue Giá trị voucher
     * @return Tổng tiền sau khi giảm giá
     */
    public static double applyDiscount(double originalTotal, String voucherType, Double voucherValue) {
        if (voucherType == null || voucherValue == null) {
            return originalTotal;
        }
        
        if ("percent".equalsIgnoreCase(voucherType)) {
            // Giảm theo phần trăm
            double discount = originalTotal * (voucherValue / 100.0);
            return Math.max(0, originalTotal - discount);
        } else if ("amount".equalsIgnoreCase(voucherType)) {
            // Giảm số tiền cố định
            return Math.max(0, originalTotal - voucherValue);
        }
        
        return originalTotal;
    }
}

