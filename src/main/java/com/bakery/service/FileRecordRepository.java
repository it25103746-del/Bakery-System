package com.bakery.service;

import com.bakery.model.BakeryRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

@Repository
public class FileRecordRepository {
    private final Path dataDirectory;

    public FileRecordRepository(@Value("${app.data-dir:data}") String dataDirectory) {
        this.dataDirectory = Path.of(dataDirectory);
    }

    public List<BakeryRecord> findAll(String fileName, Supplier<? extends BakeryRecord> factory) {
        Path file = dataDirectory.resolve(fileName);
        if (!Files.exists(file)) {
            return new ArrayList<>();
        }

        try {
            return Files.readAllLines(file, StandardCharsets.UTF_8)
                    .stream()
                    .filter(line -> !line.isBlank())
                    .map(line -> toRecord(line, factory))
                    .toList();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read " + fileName, exception);
        }
    }

    public Optional<BakeryRecord> findById(String fileName, Supplier<? extends BakeryRecord> factory, String id) {
        return findAll(fileName, factory).stream()
                .filter(record -> record.getId().equals(id))
                .findFirst();
    }

    public void save(String fileName, Supplier<? extends BakeryRecord> factory, BakeryRecord record) {
        List<BakeryRecord> records = new ArrayList<>(findAll(fileName, factory));
        records.removeIf(existing -> existing.getId().equals(record.getId()));
        records.add(record);
        writeAll(fileName, records);
    }

    public void deleteById(String fileName, Supplier<? extends BakeryRecord> factory, String id) {
        List<BakeryRecord> records = new ArrayList<>(findAll(fileName, factory));
        records.removeIf(record -> record.getId().equals(id));
        writeAll(fileName, records);
    }

    private void writeAll(String fileName, List<? extends BakeryRecord> records) {
        try {
            Files.createDirectories(dataDirectory);
            List<String> lines = records.stream()
                    .map(this::toLine)
                    .toList();
            Files.write(dataDirectory.resolve(fileName), lines, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to write " + fileName, exception);
        }
    }

    private BakeryRecord toRecord(String line, Supplier<? extends BakeryRecord> factory) {
        String[] parts = line.split("\\|", -1);
        BakeryRecord record = factory.get();
        record.setId(decode(parts[0]));
        List<String> fields = new ArrayList<>();
        for (int index = 1; index < parts.length; index++) {
            fields.add(decode(parts[index]));
        }
        record.applyFileFields(fields);
        return record;
    }

    private String toLine(BakeryRecord record) {
        List<String> parts = new ArrayList<>();
        parts.add(encode(record.getId()));
        record.toFileFields().forEach(field -> parts.add(encode(field)));
        return String.join("|", parts);
    }

    private String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}
