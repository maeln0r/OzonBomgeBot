package ru.maelnor.ozonbomgebot.bot.flow.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ValidatorsTest {

    // --- parseSku ---

    @Test
    void parseSku_validNumber_returnsLong() {
        long v = Validators.parseSku("291003311");

        assertThat(v).isEqualTo(291003311L);
    }

    @Test
    void parseSku_trimmedNumber_returnsLong() {
        long v = Validators.parseSku("  123  ");

        assertThat(v).isEqualTo(123L);
    }

    @Test
    void parseSku_zero_throwsWithRussianMessage() {
        assertThatThrownBy(() -> Validators.parseSku("0"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Некорректный артикул. Введите положительное число.");
    }

    @Test
    void parseSku_notNumber_throwsWithRussianMessage() {
        assertThatThrownBy(() -> Validators.parseSku("abc"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Некорректный артикул. Введите положительное число.");
    }

    // --- parsePositiveMoneyRub ---

    @Test
    void parsePositiveMoneyRub_valid_returnsLong() {
        long v = Validators.parsePositiveMoneyRub("4105");
        assertThat(v).isEqualTo(4105L);
    }

    @Test
    void parsePositiveMoneyRub_zero_throws() {
        assertThatThrownBy(() -> Validators.parsePositiveMoneyRub("0"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Некорректная цена. Введите целое число в рублях.");
    }

    @Test
    void parsePositiveMoneyRub_notNumber_throws() {
        assertThatThrownBy(() -> Validators.parsePositiveMoneyRub("4 105"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Некорректная цена. Введите целое число в рублях.");
    }

    // --- parsePercent0to100 ---

    @Test
    void parsePercent0to100_valid_returnsLong() {
        long v = Validators.parsePercent0to100("15");
        assertThat(v).isEqualTo(15L);
    }

    @Test
    void parsePercent0to100_gt100_throws() {
        assertThatThrownBy(() -> Validators.parsePercent0to100("120"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Некорректный процент. Введите число от 1 до 100.");
    }

    @Test
    void parsePercent0to100_zero_throws() {
        assertThatThrownBy(() -> Validators.parsePercent0to100("0"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Некорректный процент. Введите число от 1 до 100.");
    }

    @Test
    void parsePercent0to100_notNumber_throws() {
        assertThatThrownBy(() -> Validators.parsePercent0to100("ten"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Некорректный процент. Введите число от 1 до 100.");
    }
}
