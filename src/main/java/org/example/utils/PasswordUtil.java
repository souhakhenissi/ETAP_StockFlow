package org.example.utils;

import org.mindrot.jbcrypt.BCrypt;

import javax.mail.*;
import javax.mail.internet.*;
import java.io.UnsupportedEncodingException;
import java.util.Properties;
import java.util.UUID;

public class PasswordUtil {

    // ── Bcrypt ─────────────────────────────────────────────────────────────────

    public static String hash(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(12));
    }

    public static boolean verify(String plainPassword, String hashedPassword) {
        try {
            return BCrypt.checkpw(plainPassword, hashedPassword);
        } catch (Exception e) {
            return false;
        }
    }

    // ── Token de réinitialisation ───────────────────────────────────────────────

    public static String generateResetToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    // ── Envoi email (JavaMail via SMTP Gmail) ──────────────────────────────────
    // Remplissez SMTP_USER et SMTP_PASS avec les credentials ETAP
    // ou utilisez un service SMTP interne.

    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final String SMTP_PORT = "587";
    private static final String SMTP_USER = "souhakhenissi3@gmail.com";
    private static final String SMTP_PASS = "bfdmpqsjupyvictq";

    public static void envoyerEmailReset(String destinataire, String token) throws MessagingException {
        Properties props = new Properties();
        props.put("mail.smtp.auth",            "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host",            SMTP_HOST);
        props.put("mail.smtp.port",            SMTP_PORT);

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SMTP_USER, SMTP_PASS);
            }
        });

        String lien = "http://localhost:8080/reset-password?token=" + token;

        Message msg = new MimeMessage(session);
        try {
            msg.setFrom(new InternetAddress(SMTP_USER, "ETAP StockFlow"));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destinataire));
        msg.setSubject("Réinitialisation de votre mot de passe — ETAP StockFlow");
        msg.setContent(
                "<html><body style='font-family:Arial;'>"
                        + "<h2 style='color:#214293;'>ETAP StockFlow</h2>"
                        + "<p>Vous avez demandé la réinitialisation de votre mot de passe.</p>"
                        + "<p>Cliquez sur le bouton ci-dessous dans les <strong>30 minutes</strong> :</p>"
                        + "<a href='" + lien + "' style='background:#214293;color:#fff;"
                        + "padding:10px 20px;border-radius:6px;text-decoration:none;'>Réinitialiser mon mot de passe</a>"
                        + "<p style='color:#666;margin-top:20px;'>Si vous n'êtes pas à l'origine de cette demande, ignorez cet email.</p>"
                        + "</body></html>",
                "text/html; charset=utf-8"
        );

        Transport.send(msg);
    }
}

