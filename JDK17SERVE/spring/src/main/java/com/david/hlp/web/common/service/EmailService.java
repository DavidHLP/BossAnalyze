package com.david.hlp.web.common.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String from;

    /**
     * 发送纯文本邮件
     *
     * @param to      收件人
     * @param subject 邮件标题
     * @param content 邮件内容
     */
    public void sendSimpleMail(String to, String subject, String content) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(content);
        try {
            mailSender.send(message);
            log.info("纯文本邮件发送成功, 收件人: {}", to);
        } catch (MailException e) {
            log.error("纯文本邮件发送失败, 收件人: {}", to, e);
            throw e;
        }
    }

    /**
     * 发送 HTML 邮件
     *
     * @param to          收件人
     * @param subject     邮件标题
     * @param htmlContent HTML 内容
     */
    public void sendHtmlMail(String to, String subject, String htmlContent) {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            mailSender.send(mimeMessage);
            log.info("HTML 邮件发送成功, 收件人: {}", to);
        } catch (MessagingException | MailException e) {
            log.error("HTML 邮件发送失败, 收件人: {}", to, e);
            throw new RuntimeException(e);
        }
    }
}
