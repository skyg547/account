package com.ho.account.asset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.InputStream;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;

class AssetLeasePublishedMigrationTest {

    /**
     * 게시된 H2 V20 마이그레이션 바이트가 바뀌지 않았는지 확인한다.
     *
     * <p>기대 해시는 <strong>LF 줄바꿈 기준</strong>이다. {@code .gitattributes}가
     * {@code *.sql text eol=lf}로 SQL을 고정하므로 Windows와 Linux 어디서 체크아웃해도
     * 동일한 바이트가 나온다. 이 고정이 없던 시절에는 {@code text=auto}가 Windows에서만
     * CRLF로 변환해, 같은 커밋인데 로컬은 통과하고 CI는 실패했다.</p>
     */
    @Test
    void publishedH2V20BytesRemainImmutable() throws Exception {
        byte[] content;
        try (InputStream input = getClass().getClassLoader()
                .getResourceAsStream("db/migration/V20__init_asset_lease.sql")) {
            assertNotNull(input);
            content = input.readAllBytes();
        }

        String actual = HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(content));
        assertEquals(
                "fcd113d78820bbc8c6497658c64c6e6da26ff783d363b9aaf6ce3592acd1d6ae",
                actual);
    }
}
