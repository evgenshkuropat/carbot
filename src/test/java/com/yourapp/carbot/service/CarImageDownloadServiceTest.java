package com.yourapp.carbot.service;

import com.yourapp.carbot.entity.CarEntity;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CarImageDownloadServiceTest {

    private final CarImageDownloadService service = new CarImageDownloadService();

    @Test
    void acceptsOnlyUsableHttpImageUrls() {
        assertThat(service.normalizeImageUrl(" https://example.test/car.jpg ")).isEqualTo("https://example.test/car.jpg");
        assertThat(service.normalizeImageUrl("file:///etc/passwd")).isNull();
        assertThat(service.normalizeImageUrl("https://example.test/image.svg")).isNull();
        assertThat(service.normalizeImageUrl("not-a-url")).isNull();
    }

    @Test
    void downloadsTipCarsImagesBeforeSendingThemToTelegram() {
        CarEntity tipCars = new CarEntity();
        tipCars.setSource("TIPCARS");

        CarEntity bazos = new CarEntity();
        bazos.setSource("BAZOS");

        assertThat(service.requiresDownload(tipCars)).isTrue();
        assertThat(service.requiresDownload(bazos)).isFalse();
    }
}
