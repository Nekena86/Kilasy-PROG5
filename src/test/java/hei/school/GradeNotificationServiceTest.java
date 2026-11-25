package hei.school;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.MapPropertySource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

class GradeNotificationServiceTest {

    @Test
    void sendsGradeByEmailAndSms() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        SmsSender smsSender = mock(SmsSender.class);
        GradeNotificationService service = new GradeNotificationService(mailSender, smsSender);

        service.notifyGrade("Alice Smith", "alice@example.test", "+261340000001", "Tri à bulles",
                penalty(), "Bon travail.");

        var mail = org.mockito.ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(mail.capture());
        assertTrue(mail.getValue().getText().contains("Ta note pour « Tri à bulles » est disponible : 16.0/20."));
        assertTrue(mail.getValue().getText().contains("Commentaire de l'enseignant : Bon travail."));
        verify(smsSender).send("+261340000001",
                "Kilasy : ta note pour « Tri à bulles » est disponible : 16.0/20.");
    }

    @Test
    void smsFailureDoesNotPreventEmailNotification() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        SmsSender smsSender = mock(SmsSender.class);
        doThrow(new IllegalStateException("SMS indisponible"))
                .when(smsSender).send("+261340000001", "Kilasy : ta note pour « Tri à bulles » est disponible : 16.0/20.");
        GradeNotificationService service = new GradeNotificationService(mailSender, smsSender);

        assertDoesNotThrow(() -> service.notifyGrade("Alice Smith", "alice@example.test", "+261340000001",
                "Tri à bulles", penalty(), null));

        verify(mailSender).send(org.mockito.ArgumentMatchers.any(SimpleMailMessage.class));
    }

    @Test
    void consoleSmsSenderWritesSimulatedMessage() {
        PrintStream originalOut = System.out;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            System.setOut(new PrintStream(output, true, StandardCharsets.UTF_8));
            new ConsoleSmsSender().send("+261340000001", "Note disponible");
        } finally {
            System.setOut(originalOut);
        }

        assertTrue(output.toString(StandardCharsets.UTF_8).contains(
                "SMS simulé à +261340000001 : Note disponible"));
    }

    @Test
    void configuredSmsProviderReplacesConsoleDefault() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().getPropertySources().addFirst(
                    new MapPropertySource("test", Map.of("kilasy.sms.provider", "replacement")));
            context.register(SmsConfiguration.class, ReplacementSmsConfiguration.class);
            context.refresh();

            assertSame(ReplacementSmsConfiguration.SENDER, context.getBean(SmsSender.class));
        }
    }

    @Test
    void consoleSmsProviderIsTheDefault() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(SmsConfiguration.class)) {
            assertTrue(context.getBean(SmsSender.class) instanceof ConsoleSmsSender);
        }
    }

    private LatePenalty.Result penalty() {
        return LatePenalty.calculate(BigDecimal.valueOf(16), BigDecimal.valueOf(20),
                LocalDate.of(2026, 11, 25), LocalDate.of(2026, 11, 25));
    }

    @Configuration
    static class ReplacementSmsConfiguration {
        private static final SmsSender SENDER = (phoneNumber, message) -> {
        };

        @Bean
        @ConditionalOnProperty(prefix = "kilasy.sms", name = "provider", havingValue = "replacement")
        SmsSender replacementSmsSender() {
            return SENDER;
        }
    }
}
