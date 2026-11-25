package model.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

public class PasswordHasherTest {

	// seed.sql의 test1234 계정 해시와 같은 값이어야 seed 계정으로 로그인된다
	private static final String SEED_HASH = "937e8d5fbb48bd4949536cd65b8d35c426b80d2f830c5c308e2cdec422ae2244";

	@Test
	public void seed_계정과_같은_해시가_나온다() {
		assertEquals(SEED_HASH, PasswordHasher.hash("test1234"));
	}

	@Test
	public void 비밀번호가_다르면_해시도_다르다() {
		assertNotEquals(SEED_HASH, PasswordHasher.hash("test9999"));
	}

	@Test
	public void 해시를_다시_해시하면_다른_값이_나온다() {
		assertNotEquals(SEED_HASH, PasswordHasher.hash(SEED_HASH));
	}
}
