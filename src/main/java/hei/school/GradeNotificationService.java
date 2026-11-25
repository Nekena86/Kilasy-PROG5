package hei.school;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class GradeNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(GradeNotificationService.class);

    private final JavaMailSender mailSender;
    private final SmsSender smsSender;

    public GradeNotificationService(JavaMailSender mailSender, SmsSender smsSender) {
        this.mailSender = mailSender;
        this.smsSender = smsSender;
    }

    public void notifyGrade(String fullName, String email, String phone, String title,
            LatePenalty.Result penalty, String feedback) {
        String firstName = fullName.split(" ")[0];
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("notes@kilasy.test");
        message.setTo(email);
        message.setSubject("Kilasy : nouvelle note pour " + title);
        String text = "Bonjour " + firstName + ",\n\n"
                + "Ta note pour « " + title + " » est disponible : " + penalty.grade().doubleValue() + "/20.\n";
        if (penalty.daysLate() > 0) {
            text += "Note avant pénalité : " + penalty.rawGrade() + "/20. "
                    + "Pénalité de retard : " + penalty.penaltyPercent() + "% ("
                    + penalty.daysLate() + " jour(s)).\n";
        }
        if (feedback != null && !feedback.isBlank()) {
            text += "\nCommentaire de l'enseignant : " + feedback + "\n";
        }
        text += "\nL'équipe Kilasy";
        message.setText(text);
        mailSender.send(message);
        logger.info("Email de note envoyé à {}", email);

        if (phone == null || phone.isBlank()) {
            logger.warn("SMS de note non envoyé : aucun numéro enregistré pour {}", email);
            return;
        }

        String sms = "Kilasy : ta note pour « " + title + " » est disponible : "
                + penalty.grade().doubleValue() + "/20.";
        try {
            smsSender.send(phone, sms);
        } catch (RuntimeException exception) {
            logger.error("Échec de l'envoi du SMS de note à {}", phone, exception);
        }
    }
}
