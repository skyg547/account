package com.ho.account.journalledger;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Guards the source boundary independently of Spring context startup. */
class DomainFrameworkBoundaryTest {
    @Test
    void domainSourcesDoNotDependOnJpaOrSpring() throws IOException {
        Path domain = Path.of("src/main/java/com/ho/account/journalledger/domain");
        try (var paths = Files.walk(domain)) {
            List<String> violations = paths.filter(path -> path.toString().endsWith(".java"))
                    .flatMap(path -> {
                        try {
                            return Files.readAllLines(path).stream()
                                    .filter(line -> line.contains("jakarta.persistence.")
                                            || line.contains("javax.persistence.")
                                            || line.contains("org.springframework."))
                                    .map(line -> path + ": " + line.trim());
                        } catch (IOException e) {
                            throw new IllegalStateException("Cannot inspect domain source " + path, e);
                        }
                    })
                    .toList();
            // The audited revision has Spring Data repositories and JPA entity imports here.
            assertThat(violations).isEmpty();
        }
    }
}
