package ru.maelnor.ozonbomgebot.bot.flow.util;

public final class Validators {
    public static long parseSku(String text) {
        try {
            long v = Long.parseLong(text.trim());
            if (v <= 0) throw new IllegalArgumentException();
            return v;
        } catch (Exception e) {
            throw new IllegalArgumentException("Некорректный артикул. Введите положительное число.");
        }
    }

    public static long parsePositiveMoneyRub(String text) {
        try {
            long v = Long.parseLong(text.trim());
            if (v <= 0) throw new IllegalArgumentException();
            return v;
        } catch (Exception e) {
            throw new IllegalArgumentException("Некорректная цена. Введите целое число в рублях.");
        }
    }

    public static long parsePercent0to100(String text) {
        try {
            long v = Long.parseLong(text.trim());
            if (v <= 0 || v > 100) throw new IllegalArgumentException();
            return v;
        } catch (Exception e) {
            throw new IllegalArgumentException("Некорректный процент. Введите число от 1 до 100.");
        }
    }
}
