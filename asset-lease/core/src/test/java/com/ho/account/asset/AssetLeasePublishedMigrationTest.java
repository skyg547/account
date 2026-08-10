package com.ho.account.asset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.InputStream;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;

class AssetLeasePublishedMigrationTest {

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
                "6c6918c25fc1a5dc07bbfa9056a34273639bd57d72d6dd6a771cf2ea8909c127",
                actual);
    }
}
