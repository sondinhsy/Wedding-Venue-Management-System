package com.weddingapp.util;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Utility class for formatting currency in Vietnamese Dong (VND)
 */
public final class CurrencyFormatter {
    private static final DecimalFormat VND_FORMATTER;
    private static final DecimalFormat VND_COMPACT_FORMATTER;
    
    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.getDefault());
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        
        VND_FORMATTER = new DecimalFormat("#,###", symbols);
        VND_COMPACT_FORMATTER = new DecimalFormat("#,##0.#", symbols);
    }
    
    private CurrencyFormatter() {
    }
    
    /**
     * Format amount as Vietnamese Dong with full format
     * @param amount the amount to format
     * @return formatted string like "1.500.000 đ"
     */
    public static String formatVND(double amount) {
        return VND_FORMATTER.format(amount) + " đ";
    }
    
    /**
     * Format amount as Vietnamese Dong with compact format for large numbers
     * @param amount the amount to format
     * @return formatted string like "1,5 triệu đ" for 1.500.000
     */
    public static String formatVNDCompact(double amount) {
        if (amount >= 1_000_000) {
            double millions = amount / 1_000_000;
            return String.format("%.1f triệu đ", millions);
        } else if (amount >= 1_000) {
            double thousands = amount / 1_000;
            return String.format("%.1f nghìn đ", thousands);
        }
        return formatVND(amount);
    }
    
    /**
     * Parse VND string to double (removes "đ" and dots)
     * @param vndString string like "1.500.000 đ"
     * @return double value
     */
    public static double parseVND(String vndString) {
        if (vndString == null || vndString.trim().isEmpty()) {
            return 0.0;
        }
        String cleaned = vndString.replace("đ", "").replace(".", "").replace(",", ".").trim();
        try {
            return Double.parseDouble(cleaned);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}

