package com.example.myapplication.util;

import com.example.myapplication.BuildConfig;

import java.util.Properties;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class EmailSender {

    private static final String SENDER_EMAIL = BuildConfig.SENDER_EMAIL;
    private static final String SENDER_APP_PASSWORD = BuildConfig.SENDER_APP_PASSWORD;

    public interface Callback {
        void onSuccess();
        void onFailure(Exception e);
    }

    public static void sendVerificationCode(String toEmail, String username, String code, Callback callback) {
        new Thread(() -> {
            try {
                Properties props = new Properties();
                props.put("mail.smtp.auth", "true");
                props.put("mail.smtp.starttls.enable", "true");
                props.put("mail.smtp.host", "smtp.gmail.com");
                props.put("mail.smtp.port", "587");
                props.put("mail.smtp.ssl.trust", "smtp.gmail.com");

                Session session = Session.getInstance(props, new javax.mail.Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(SENDER_EMAIL, SENDER_APP_PASSWORD);
                    }
                });

                Message message = new MimeMessage(session);
                message.setFrom(new InternetAddress(SENDER_EMAIL));
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
                message.setSubject("Slagalica – verifikacija naloga");
                message.setText(
                    "Zdravo " + username + ",\n\n" +
                    "Tvoj verifikacioni kod je:\n\n" +
                    "    " + code + "\n\n" +
                    "Upiši ovaj kod u aplikaciju da bi potvrdio nalog.\n\n" +
                    "Slagalica tim"
                );

                Transport.send(message);
                callback.onSuccess();
            } catch (MessagingException e) {
                callback.onFailure(e);
            }
        }).start();
    }
}
