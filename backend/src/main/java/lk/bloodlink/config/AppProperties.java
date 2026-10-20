package lk.bloodlink.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.List;

@ConfigurationProperties(prefix = "app")
public record AppProperties(Jwt jwt, int donationGapMonths, List<String> corsOrigins,
                            boolean seedDemoData, Mail mail, Sms sms) {
    public record Jwt(String secret, long expirationHours) {}
    public record Mail(String from) {}
    public record Sms(boolean enabled, String url, String token, String sender) {}
}
