package com.uokaradeniz.backend.utils;

import java.nio.file.*;
import java.io.IOException;

public class UsefulUtils {
    public static void cleanFolder(String folderPath) {
        try {
            Path directory = Paths.get(folderPath);

            if (!Files.exists(directory)) {
                System.out.println("Klasör bulunamadı: " + folderPath);
                return;
            }

            Files.walk(directory)
                    .filter(path -> !path.equals(directory))
                    .sorted((a, b) -> b.compareTo(a))
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                            System.out.println("Silindi: " + path);
                        } catch (IOException e) {
                            System.err.println("Silme hatası: " + path + " - " + e.getMessage());
                        }
                    });

            System.out.println("Klasör temizlendi: " + folderPath);

        } catch (IOException e) {
            System.err.println("Hata oluştu: " + e.getMessage());
        }
    }
}