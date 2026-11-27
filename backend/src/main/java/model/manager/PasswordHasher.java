package model.manager;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

// 가입과 로그인이 반드시 같은 방식으로 해시해야 비교가 되므로 계산을 한 곳에 둔다
final class PasswordHasher {

	private PasswordHasher() {
	}

	static String hash(String password) {
		try {
			MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
			byte[] digest = sha256.digest(password.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		} catch (NoSuchAlgorithmException ex) {
			// SHA-256은 모든 Java에 필수로 들어 있어 실제로는 발생하지 않는다
			throw new IllegalStateException(ex);
		}
	}
}
