package com.ho.account.masterdata;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
    "spring.cloud.vault.enabled=false",
    "spring.cloud.config.enabled=false",
    "eureka.client.enabled=false"
})
class MasterDataApplicationTests {

    @Test
    void contextLoads() {
    }

}
