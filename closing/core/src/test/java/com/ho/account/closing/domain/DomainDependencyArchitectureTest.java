package com.ho.account.closing.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/** Guards the pure domain boundary, including source files added after this extraction. */
class DomainDependencyArchitectureTest {
    private static final Pattern FRAMEWORK_IMPORT = Pattern.compile(
            "(?m)^\\s*import\\s+(?:static\\s+)?(?:(?:jakarta|javax)\\.persistence|org\\.(?:hibernate|springframework))\\.[^;\\n]+;");
    private static final Path DOMAIN = Path.of("src/main/java/com/ho/account/closing/domain");

    @Test
    void domainSourcesDoNotImportPersistenceOrSpringFrameworks() throws IOException {
        try (var sources = Files.walk(DOMAIN)) {
            List<String> violations = sources.filter(path -> path.toString().endsWith(".java"))
                    .map(path -> path + ": " + forbiddenImport(read(path)))
                    .filter(result -> !result.endsWith(": "))
                    .toList();
            assertThat(violations).isEmpty();
        }
    }

    @Test
    void guardRejectsTheAuditedJpaImportsAndWildcards() {
        assertThat(forbiddenImport("import jakarta.persistence.Entity;\n")).isEqualTo("import jakarta.persistence.Entity");
        assertThat(forbiddenImport("import jakarta.persistence.*;\n")).isEqualTo("import jakarta.persistence.*");
        assertThat(forbiddenImport("import javax.persistence.Version;\n")).isEqualTo("import javax.persistence.Version");
        assertThat(forbiddenImport("import org.springframework.stereotype.Component;\n")).isEqualTo("import org.springframework.stereotype.Component");
        assertThat(forbiddenImport("import org.hibernate.annotations.*;\n")).isEqualTo("import org.hibernate.annotations.*");
    }

    private static String forbiddenImport(String source) {
        var matcher = FRAMEWORK_IMPORT.matcher(source);
        return matcher.find() ? matcher.group().trim().replace(";", "") : "";
    }

    private static String read(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot inspect domain source: " + path, exception);
        }
    }
}
