package hei.school;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SmsConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "kilasy.sms", name = "provider", havingValue = "console", matchIfMissing = true)
    public SmsSender consoleSmsSender() {
        return new ConsoleSmsSender();
    }
}
