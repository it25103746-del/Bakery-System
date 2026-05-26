package com.bakery.service;

import com.bakery.model.BakeryRecord;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class ModuleDefinition<T extends BakeryRecord> {
    private final String key;
    private final String title;
    private final String studentId;
    private final String studentName;
    private final String contribution;
    private final String createOperation;
    private final String readOperation;
    private final String updateOperation;
    private final String deleteOperation;
    private final String fileName;
    private final Supplier<T> factory;
    private final List<FieldDefinition<T>> fields;

    public ModuleDefinition(
            String key,
            String title,
            String studentId,
            String studentName,
            String contribution,
            String createOperation,
            String readOperation,
            String updateOperation,
            String deleteOperation,
            String fileName,
            Supplier<T> factory,
            List<FieldDefinition<T>> fields
    ) {
        this.key = key;
        this.title = title;
        this.studentId = studentId;
        this.studentName = studentName;
        this.contribution = contribution;
        this.createOperation = createOperation;
        this.readOperation = readOperation;
        this.updateOperation = updateOperation;
        this.deleteOperation = deleteOperation;
        this.fileName = fileName;
        this.factory = factory;
        this.fields = fields;
    }

    public String getKey() { return key; }
    public String getTitle() { return title; }
    public String getStudentId() { return studentId; }
    public String getStudentName() { return studentName; }
    public String getContribution() { return contribution; }
    public String getCreateOperation() { return createOperation; }
    public String getReadOperation() { return readOperation; }
    public String getUpdateOperation() { return updateOperation; }
    public String getDeleteOperation() { return deleteOperation; }
    public String getFileName() { return fileName; }
    public Supplier<T> getFactory() { return factory; }
    public List<FieldDefinition<T>> getFields() { return fields; }

    public record FieldDefinition<T extends BakeryRecord>(
            String name,
            String label,
            Function<T, String> getter,
            BiConsumer<T, String> setter
    ) {
        public String value(T record) {
            return getter.apply(record);
        }

        public void assign(T record, String value) {
            setter.accept(record, value);
        }
    }
}
