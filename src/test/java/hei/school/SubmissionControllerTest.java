package hei.school;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;

class SubmissionControllerTest {

    @Test
    void gradeIsSavedWhenSmsDeliveryFails() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        JavaMailSender mailSender = mock(JavaMailSender.class);
        SmsSender smsSender = mock(SmsSender.class);
        GradeNotificationService notificationService = new GradeNotificationService(mailSender, smsSender);
        SubmissionController controller = new SubmissionController(jdbcTemplate, notificationService);
        when(jdbcTemplate.queryForList(anyString(), any(Object[].class)))
                .thenReturn(List.of(submissionRow()));
        org.mockito.Mockito.doThrow(new IllegalStateException("SMS indisponible"))
                .when(smsSender).send("+261340000001",
                        "Kilasy : ta note pour « Tri à bulles » est disponible : 16.0/20.");

        ResponseEntity<?> response = controller.grade(42, Map.of("points", 16));

        assertEquals(200, response.getStatusCode().value());
        verify(mailSender).send(any(org.springframework.mail.SimpleMailMessage.class));
        verify(smsSender).send("+261340000001",
                "Kilasy : ta note pour « Tri à bulles » est disponible : 16.0/20.");
        verify(jdbcTemplate).update(
                "UPDATE submission SET raw_points = ?, feedback = ?, graded_at = now() WHERE id = ?",
                16.0, null, 42L);
    }

    private Map<String, Object> submissionRow() {
        Map<String, Object> row = new HashMap<>();
        row.put("id", 42L);
        row.put("title", "Tri à bulles");
        row.put("deadline", Date.valueOf(LocalDate.of(2026, 11, 25)));
        row.put("max_points", BigDecimal.valueOf(20));
        row.put("full_name", "Alice Smith");
        row.put("email", "alice@example.test");
        row.put("phone", "+261340000001");
        row.put("submitted_at", Timestamp.valueOf(LocalDateTime.of(2026, 11, 25, 10, 0)));
        return row;
    }
}
