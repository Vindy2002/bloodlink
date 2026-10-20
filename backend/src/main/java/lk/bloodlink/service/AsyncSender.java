package lk.bloodlink.service;

import lk.bloodlink.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/** Sends SMS / email off the request thread so alerting hundreds of donors never blocks the API. */
@Component
public class AsyncSender {
    private static final Logger log = LoggerFactory.getLogger(AsyncSender.class);

    public interface SmsGateway { void send(String to, String text); }

    private final SmsGateway sms;
    private final ObjectProvider<JavaMailSender> mail;
    private final AppProperties props;

    public AsyncSender(SmsGateway sms, ObjectProvider<JavaMailSender> mail, AppProperties props) {
        this.sms = sms;
        this.mail = mail;
        this.props = props;
    }

    @Async
    public void sms(String to, String text) {
        try { sms.send(to, text); } catch (Exception e) { log.warn("SMS to {} failed: {}", to, e.getMessage()); }
    }

    @Async
    public void email(String to, String subject, String body) {
        JavaMailSender sender = mail.getIfAvailable();
        if (sender == null) {
            log.info("[EMAIL disabled] to={} subject={} body={}", to, subject, body);
            return;
        }
        try {
            SimpleMailMessage m = new SimpleMailMessage();
            m.setFrom(props.mail().from());
            m.setTo(to);
            m.setSubject(subject);
            m.setText(body);
            sender.send(m);
        } catch (Exception e) {
            log.warn("Email to {} failed: {}", to, e.getMessage());
        }
    }
}
