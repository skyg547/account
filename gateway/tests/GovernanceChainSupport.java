import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Driver;
import java.util.ArrayList;
import java.util.Properties;
import java.util.zip.ZipFile;

/** Uses only dependencies already packaged in the real Auth JAR; never downloads a JDBC driver. */
class GovernanceChainSupport {
    public static void main(String[] args) throws Exception {
        Path directory = Path.of(args[2]);
        var libraries = new ArrayList<java.net.URL>();
        try (var jar = new ZipFile(args[1])) {
            var entries = jar.entries();
            while (entries.hasMoreElements()) {
                var entry = entries.nextElement();
                String name = entry.getName();
                if (name.matches("BOOT-INF/lib/(spring-security-crypto|spring-jcl|h2)-[^/]+\\.jar")) {
                    Path target = directory.resolve(Path.of(name).getFileName());
                    if (!Files.exists(target)) {
                        try (var input = jar.getInputStream(entry)) { Files.copy(input, target); }
                    }
                    libraries.add(target.toUri().toURL());
                }
            }
        }
        try (var loader = new URLClassLoader(libraries.toArray(java.net.URL[]::new))) {
            if (args[0].equals("hash")) {
                Class<?> bcrypt = loader.loadClass("org.springframework.security.crypto.bcrypt.BCrypt");
                String salt = (String) bcrypt.getMethod("gensalt").invoke(null);
                // The parent captures stdout in memory and never prints this random synthetic credential.
                System.out.print(bcrypt.getMethod("hashpw", String.class, String.class)
                        .invoke(null, System.getenv("CHAIN_PASSWORD"), salt));
                return;
            }
            Driver driver = (Driver) loader.loadClass("org.h2.Driver").getConstructor().newInstance();
            var credentials = new Properties();
            credentials.setProperty("user", "chain");
            credentials.setProperty("password", System.getenv("CHAIN_DB_PASSWORD"));
            try (var connection = driver.connect(System.getenv("CHAIN_JDBC_URL"), credentials)) {
                try (var query = connection.createStatement();
                        var result = query.executeQuery("SELECT COUNT(*) FROM master_approval")) {
                    if (!result.next() || result.getLong(1) != 1) {
                        throw new IllegalStateException("Denied request created an approval record");
                    }
                }
                try (var query = connection.prepareStatement(
                        "SELECT role_version FROM auth_users WHERE username = ?")) {
                    query.setString(1, "chain-target");
                    try (var result = query.executeQuery()) {
                        if (!result.next() || result.getLong(1) != 2 || result.next()) {
                            throw new IllegalStateException("Persisted target role version mismatch");
                        }
                    }
                }
                try (var query = connection.prepareStatement(
                        "SELECT role_code FROM auth_role_assignments WHERE username = ? AND approved = TRUE")) {
                    query.setString(1, "chain-target");
                    try (var result = query.executeQuery()) {
                        if (!result.next() || !"ROLE_AUDITOR".equals(result.getString(1)) || result.next()) {
                            throw new IllegalStateException("Persisted role assignment mismatch");
                        }
                    }
                }
                try (var query = connection.prepareStatement(
                        "SELECT request_user, approver_user, status FROM master_approval WHERE id = ?")) {
                    query.setLong(1, Long.parseLong(System.getenv("CHAIN_APPROVAL_ID")));
                    try (var result = query.executeQuery()) {
                        if (!result.next() || !"chain-maker".equals(result.getString(1))
                                || !"chain-checker".equals(result.getString(2))
                                || !"APPROVED".equals(result.getString(3)) || result.next()) {
                            throw new IllegalStateException("Persisted approval identity/status mismatch");
                        }
                    }
                }
            }
            System.out.println("persisted approval and role effect verified");
        }
    }
}
