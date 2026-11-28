package model.storage;

import java.nio.file.Path;

public class StoredFile {
    private final String webPath;       // DB FILE_PATH에 저장
    private final String originalName;  // DB ORIGINAL_FILE_NAME에 저장
    private final Path diskPath;
    
    public StoredFile(String webPath, String originalName, Path diskPath) {
        super();
        this.webPath = webPath;
        this.originalName = originalName;
        this.diskPath = diskPath;
    }

    public String getWebPath() {
        return webPath;
    }

    public String getOriginalName() {
        return originalName;
    }

    public Path getDiskPath() {
        return diskPath;
    }
}
