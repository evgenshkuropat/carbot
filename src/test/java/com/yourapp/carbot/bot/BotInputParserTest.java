package com.yourapp.carbot.bot;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BotInputParserTest {

    private final BotInputParser parser = new BotInputParser();

    @Test
    void extractsCommandWithoutArguments() {
        assertThat(parser.command(" /SELL test ")).isEqualTo("/sell");
    }

    @Test
    void mapsLegacyFilterButton() {
        assertThat(parser.legacyEditField("filter_edit:max_price")).isEqualTo("max_price");
    }
}
