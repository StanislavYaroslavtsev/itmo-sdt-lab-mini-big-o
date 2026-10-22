package dev.syaroslavtsev.minibigo;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Оценка сложности вида {@code O(nᵃ logᵇ n)}, например {@code O(n² log n)}.
 *
 * @param nPower   степень n
 * @param logPower степень log n
 */
public record Complexity(int nPower, int logPower) {
    private static final String SUPERSCRIPT_DIGITS = "⁰¹²³⁴⁵⁶⁷⁸⁹";

    /** {@code O(1)} */
    public static Complexity constant() {
        return new Complexity(0, 0);
    }

    /** {@code O(n)} */
    public static Complexity linear() {
        return new Complexity(1, 0);
    }

    /** {@code O(log n)} */
    public static Complexity logarithmic() {
        return new Complexity(0, 1);
    }

    /** Произведение оценок (для вложенных циклов): степени складываются. */
    public Complexity times(Complexity other) {
        return new Complexity(nPower + other.nPower, logPower + other.logPower);
    }

    /** Бо́льшая из двух оценок (для циклов подряд). Сначала сравниваем степень n, потом степень log n. */
    public Complexity max(Complexity other) {
        if (nPower != other.nPower) {
            return nPower > other.nPower ? this : other;
        }
        return logPower >= other.logPower ? this : other;
    }

    /** Строка для подсказки: {@code O(1)}, {@code O(n²)}, {@code O(n log² n)}. */
    @Override
    public @NotNull String toString() {
        List<String> parts = new ArrayList<>();
        if (nPower > 0) {
            parts.add("n" + superscript(nPower));
        }
        if (logPower > 0) {
            parts.add("log" + superscript(logPower) + " n");
        }
        return parts.isEmpty() ? "O(1)" : "O(" + String.join(" ", parts) + ")";
    }

    /** Степень надстрочными цифрами, первая степень не пишется. */
    private static String superscript(int power) {
        if (power == 1) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (char digit : Integer.toString(power).toCharArray()) {
            sb.append(SUPERSCRIPT_DIGITS.charAt(digit - '0'));
        }
        return sb.toString();
    }
}
