package com.ho.account.journalledger.adapter.in.web.period;

import com.ho.account.contracts.journal.JournalPeriodCheckpoint;
import com.ho.account.contracts.journal.JournalPeriodFenceCommand;
import com.ho.account.contracts.journal.JournalPeriodFencePort;
import com.ho.account.contracts.journal.JournalPeriodFenceResult;
import com.ho.account.contracts.journal.JournalPeriodScope;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.transaction.TransactionException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Internal-only control surface; monetary behavior is shared with the in-process contract adapter. */
@RestController
@RequestMapping("/api/internal/journal-periods")
public class JournalPeriodController {
    private final JournalPeriodFencePort periods;
    private final byte[] serviceToken;

    public JournalPeriodController(@Qualifier("jdbcJournalPeriodAdmissionAdapter") JournalPeriodFencePort periods,
                                   @Value("${journal.period-fence.service-token:}") String serviceToken) {
        this.periods = periods;
        this.serviceToken = serviceToken.getBytes(StandardCharsets.UTF_8);
    }

    @PostMapping("/checkpoint")
    public JournalPeriodCheckpoint checkpoint(HttpServletRequest request, @RequestBody JournalPeriodScope scope) {
        requireClosingService(request);
        return periods.checkpoint(scope);
    }

    @PostMapping("/fence")
    public JournalPeriodFenceResult fence(HttpServletRequest request, @RequestBody JournalPeriodFenceCommand command) {
        requireClosingService(request);
        return periods.fence(command);
    }

    @PostMapping("/release")
    public JournalPeriodFenceResult release(HttpServletRequest request, @RequestBody JournalPeriodFenceCommand command) {
        requireClosingService(request);
        return periods.release(command);
    }

    private void requireClosingService(HttpServletRequest request) {
        // A caller-supplied identity header alone grants no authority. Use a dedicated credential
        // over trusted TLS transport, never an end-user bearer token or forwarded user authority.
        if (serviceToken.length < 32) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Journal service authentication unavailable");
        }
        if (Collections.list(request.getHeaderNames()).stream().map(name -> name.toLowerCase(Locale.ROOT))
                .anyMatch(name -> name.startsWith("x-auth-") || name.equals("x-user-id"))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Closing service identity required");
        }
        if (Collections.list(request.getHeaders("Authorization")).size() != 1
                || Collections.list(request.getHeaders("X-Service-Identity")).size() != 1
                || !"closing".equals(request.getHeader("X-Service-Identity"))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Closing service identity required");
        }
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")
                || !MessageDigest.isEqual(serviceToken, authorization.substring(7).getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Closing service authentication failed");
        }
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<String> conflict() {
        return ResponseEntity.status(HttpStatus.CONFLICT).body("Journal period control conflict");
    }

    @ExceptionHandler({DataAccessException.class, TransactionException.class})
    public ResponseEntity<String> unavailable() {
        // SQL diagnostics can include command payloads. Keep the HTTP failure bounded and constant.
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("Journal period control unavailable");
    }
}
