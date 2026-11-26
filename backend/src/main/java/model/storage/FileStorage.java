package model.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import common.error.ApiException;
import common.error.ErrorCode;

@Component
public class FileStorage {
    private static final Set<String> PROOF_EXT = Set.of("pdf", "jpg", "jpeg", "png");
    private static final Set<String> POSTER_EXT = Set.of("jpg", "jpeg", "png", "webp");
    private static final long PROOF_MAX = 10L * 1024 * 1024;
    private static final long POSTER_MAX = 5L * 1024 * 1024;

    private final Path baseDir = Paths.get(System.getProperty("user.home"), "catchwish-upload")
            .toAbsolutePath().normalize();

    public void validateProof(MultipartFile f)  { check(f, true,  PROOF_EXT,  PROOF_MAX,  "대관 증빙"); }
    public void validatePoster(MultipartFile f) { check(f, false, POSTER_EXT, POSTER_MAX, "포스터"); }  // 선택 항목

    public StoredFile saveProof(MultipartFile f)  throws IOException { validateProof(f);  return save(f, "proof"); }
    public StoredFile savePoster(MultipartFile f) throws IOException { validatePoster(f); return save(f, "poster"); }

    /** 롤백 시 보상 삭제: 실패해도 원래 예외를 가리지 않는다 */
    public void delete(StoredFile s) {
        try { Files.deleteIfExists(s.getDiskPath()); } catch (IOException ignore) { }
    }

    /** WebConfig에서 /upload/poster/** 를 연결할 때 사용 */
    public String posterLocation() throws IOException {
        Path dir = baseDir.resolve("poster");
        Files.createDirectories(dir);
        String uri = dir.toUri().toString();
        return uri.endsWith("/") ? uri : uri + "/";
    }

    /** M30 심사용 다운로드에서 쓸 예정 (baseDir 밖으로 나가는 경로는 차단) */
    public Path resolve(String webPath) {
        Path p = baseDir.resolve(webPath.replaceFirst("^/upload/", "")).normalize();
        if (!p.startsWith(baseDir)) throw new ApiException(ErrorCode.BAD_REQUEST, "잘못된 파일 경로입니다.");
        return p;
    }

    private StoredFile save(MultipartFile f, String subDir) throws IOException {
        String fileName = UUID.randomUUID() + "." + extensionOf(f.getOriginalFilename());
        Path dir = baseDir.resolve(subDir);
        Files.createDirectories(dir);
        Path target = dir.resolve(fileName);
        try (InputStream in = f.getInputStream()) { Files.copy(in, target); }
        return new StoredFile("/upload/" + subDir + "/" + fileName,
                safeOriginalName(f.getOriginalFilename()), target);
    }

    private void check(MultipartFile f, boolean required, Set<String> allowed, long max, String label) {
        if (f == null || f.isEmpty()) {
            if (required) throw new ApiException(ErrorCode.BAD_REQUEST, label + " 파일을 첨부해주세요.");
            return;
        }
        if (f.getSize() > max)
            throw new ApiException(ErrorCode.BAD_REQUEST, label + " 파일은 " + (max / 1024 / 1024) + "MB 이하여야 합니다.");
        if (!allowed.contains(extensionOf(f.getOriginalFilename())))
            throw new ApiException(ErrorCode.BAD_REQUEST, label + " 파일 형식이 올바르지 않습니다.");
    }

    private String extensionOf(String name) {
        int i = (name == null) ? -1 : name.lastIndexOf('.');
        return i < 0 ? "" : name.substring(i + 1).toLowerCase();
    }

    private String safeOriginalName(String name) {
        if (name == null || name.isBlank()) return "unnamed";
        String n = Paths.get(name).getFileName().toString();
        return n.length() > 200 ? n.substring(n.length() - 200) : n;
    }
}
