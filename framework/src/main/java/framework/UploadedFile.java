package framework;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public final class UploadedFile {

    private final String fieldName;
    private final String fileName;
    private final String contentType;
    private final byte[] content;

    public UploadedFile(String fieldName, String fileName, String contentType, byte[] content) {
        this.fieldName = fieldName;
        this.fileName = fileName;
        this.contentType = contentType;
        this.content = content.clone();
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getFileName() {
        return fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public byte[] getContent() {
        return content.clone();
    }

    public long getSize() {
        return content.length;
    }

    public Path save(Path directory) throws IOException {
        if (directory == null) {
            throw new IllegalArgumentException("Upload directory must not be null");
        }
        if (fileName == null) {
            throw new IllegalArgumentException("Uploaded file has no file name");
        }
        String safeFileName = Path.of(fileName).getFileName().toString();
        if (safeFileName.isBlank() || ".".equals(safeFileName) || "..".equals(safeFileName)) {
            throw new IllegalArgumentException("Uploaded file has no valid file name");
        }
        Files.createDirectories(directory);
        Path destination = directory.resolve(safeFileName).normalize();
        if (!destination.getParent().equals(directory.normalize())) {
            throw new IllegalArgumentException("Invalid uploaded file name");
        }
        return Files.write(destination, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }
}
