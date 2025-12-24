package com.weddingapp.util;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Utility class for formatting currency in US Dollars (USD)
 */
public final class CurrencyFormatter {
    private static final DecimalFormat USD_FORMATTER;
    // Trước đây dùng tỉ giá VND -> USD. Bây giờ toàn bộ hệ thống lưu trực tiếp theo USD,
    // nên đặt tỉ lệ = 1 để không đổi đơn vị nữa.
    private static final double VND_TO_USD_RATE = 1.0;
    
    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setGroupingSeparator(',');
        symbols.setDecimalSeparator('.');
        
        USD_FORMATTER = new DecimalFormat("#,##0.00", symbols);
    }
    
    private CurrencyFormatter() {
    }
    
    // Trước đây hàm này chuyển VND sang USD. Hiện tại chúng ta coi giá trị truyền vào
    // đã là USD, nên chỉ trả về nguyên giá trị.
    private static double vndToUsd(double vndAmount) {
        return vndAmount / VND_TO_USD_RATE;
    }
    
    /**
     * Format amount as US Dollars with full format
     * @param vndAmount the amount in USD (giá trị lưu trong DB)
     * @return formatted string like "$1,500.00"
     */
    public static String formatVND(double vndAmount) {
        double usdAmount = vndToUsd(vndAmount);
        return "$" + USD_FORMATTER.format(usdAmount);
    }
    
    /**
     * Format amount as US Dollars with compact format for large numbers
     * @param vndAmount the amount in USD (giá trị lưu trong DB)
     * @return formatted string like "$1.5K" for 1,500
     */
    public static String formatVNDCompact(double vndAmount) {
        double usdAmount = vndToUsd(vndAmount);
        if (usdAmount >= 1_000_000) {
            double millions = usdAmount / 1_000_000;
            return String.format("$%.2fM", millions);
        } else if (usdAmount >= 1_000) {
            double thousands = usdAmount / 1_000;
            return String.format("$%.2fK", thousands);
        }
        return formatVND(vndAmount);
    }
    
    /**
     * Parse USD string to double (removes "$" and commas)
     * @param usdString string like "$150.00"
     * @return double value in VND
     */
    public static double parseVND(String usdString) {
        if (usdString == null || usdString.trim().isEmpty()) {
            return 0.0;
        }
        String cleaned = usdString.replace("$", "").replace(",", "").trim();
        try {
            double usdAmount = Double.parseDouble(cleaned);
            return usdAmount * VND_TO_USD_RATE;
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}

