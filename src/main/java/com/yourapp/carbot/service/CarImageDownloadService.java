package com.yourapp.carbot.service;

import com.yourapp.carbot.entity.CarEntity;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.util.Locale;
import java.util.UUID;

@Service
public class CarImageDownloadService {

    private static final long MAX_IMAGE_DOWNLOAD_BYTES = 8L * 1024 * 1024;

    public File downloadToTempFile(CarEntity car) throws Exception {
        String imageUrl = normalizeImageUrl(car == null ? null : car.getImageUrl());
        if (imageUrl == null) {
            return null;
        }

        URLConnection connection = new URL(imageUrl).openConnection();
        connection.setRequestProperty(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                        "(KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
        );
        connection.setRequestProperty("Accept", "image/avif,image/webp,image/apng,image/*,*/*;q=0.8");
        connection.setRequestProperty("Referer", resolveReferer(car));
        connection.setConnectTimeout(10_000);
        connection.setReadTimeout(15_000);

        File tempFile = File.createTempFile("car-photo-" + UUID.randomUUID(), guessExtension(imageUrl));

        try (InputStream inputStream = connection.getInputStream();
             FileOutputStream outputStream = new FileOutputStream(tempFile)) {
            copyWithLimit(inputStream, outputStream);

            if (tempFile.length() == 0) {
                Files.deleteIfExists(tempFile.toPath());
                return null;
            }

            return tempFile;
        } catch (Exception e) {
            Files.deleteIfExists(tempFile.toPath());
            throw e;
        }
    }

    public boolean requiresDownload(CarEntity car) {
        String source = car == null || car.getSource() == null ? "" : car.getSource().toUpperCase(Locale.ROOT);
        return source.contains("TIPCARS") || source.contains("TIP_CARS");
    }

    public String normalizeImageUrl(String imageUrl) {
        if (imageUrl == null) {
            return null;
        }

        String normalized = imageUrl.trim();
        if (normalized.isEmpty()) {
            return null;
        }

        try {
            URL url = new URL(normalized);
            if (!"http".equalsIgnoreCase(url.getProtocol()) && !"https".equalsIgnoreCase(url.getProtocol())) {
                return null;
            }
        } catch (Exception e) {
            return null;
        }

        return normalized.toLowerCase(Locale.ROOT).endsWith(".svg") ? null : normalized;
    }

    private String resolveReferer(CarEntity car) {
        String source = car == null || car.getSource() == null ? "" : car.getSource().toUpperCase(Locale.ROOT);
        if (source.contains("BAZOS")) return "https://auto.bazos.cz/";
        if (source.contains("SAUTO")) return "https://www.sauto.cz/";
        if (source.contains("TIPCARS") || source.contains("TIP_CARS")) return "https://www.tipcars.com/";
        if (source.contains("AUTOESA")) return "https://www.autoesa.cz/";
        return "https://www.google.com/";
    }

    private String guessExtension(String imageUrl) {
        String lower = imageUrl.toLowerCase(Locale.ROOT);
        if (lower.contains(".png")) return ".png";
        if (lower.contains(".jpeg")) return ".jpeg";
        if (lower.contains(".webp")) return ".webp";
        return ".jpg";
    }

    private void copyWithLimit(InputStream inputStream, FileOutputStream outputStream) throws Exception {
        byte[] buffer = new byte[8192];
        long total = 0;
        int read;

        while ((read = inputStream.read(buffer)) != -1) {
            total += read;
            if (total > MAX_IMAGE_DOWNLOAD_BYTES) {
                throw new IllegalStateException("Image is too large");
            }
            outputStream.write(buffer, 0, read);
        }
    }
}
