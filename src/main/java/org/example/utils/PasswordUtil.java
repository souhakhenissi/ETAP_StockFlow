package org.example.utils;

import org.mindrot.jbcrypt.BCrypt;

import javax.mail.*;
import javax.mail.internet.*;
import java.io.UnsupportedEncodingException;
import java.util.Properties;
import java.util.UUID;

/**
 * Remplace complètement votre PasswordUtil.java existant.
 * Corrections :
 *   - Utilisation de MimeMultipart pour les emails avec/sans pièce jointe
 *   - setContent() remplacé par setDataHandler() pour le PDF
 *   - Encodage UTF-8 forcé sur le sujet
 *   - try-with-resources sur Transport
 */
public class PasswordUtil {

    // ── BCrypt ─────────────────────────────────────────────────────────────────
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

    // ── Token reset ────────────────────────────────────────────────────────────
    public static String generateResetToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    // ── Config SMTP ────────────────────────────────────────────────────────────
    // Remplacez par vos credentials SMTP
    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final String SMTP_PORT = "587";
    private static final String SMTP_USER = "souhakhenissi3@gmail.com";
    private static final String SMTP_PASS = "bfdmpqsjupyvictq"; // App password Gmail 16 chars

    private static Session creerSession() {
        Properties props = new Properties();
        props.put("mail.smtp.auth",                "true");
        props.put("mail.smtp.starttls.enable",     "true");
        props.put("mail.smtp.starttls.required",   "true");
        props.put("mail.smtp.host",                SMTP_HOST);
        props.put("mail.smtp.port",                SMTP_PORT);
        props.put("mail.smtp.ssl.protocols",       "TLSv1.2");
        props.put("mail.smtp.ssl.trust",           SMTP_HOST);
        props.put("mail.mime.charset",             "UTF-8");

        return Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SMTP_USER, SMTP_PASS);
            }
        });
    }

    // ── Email simple HTML (sans pièce jointe) ─────────────────────────────────
    public static void envoyerEmail(String destinataire,
                                    String sujet,
                                    String corpsHtml) throws MessagingException, UnsupportedEncodingException {
        envoyerEmailAvecPdf(destinataire, sujet, corpsHtml, null, null);
    }

    // ── Email HTML avec PDF optionnel en pièce jointe ─────────────────────────
    public static void envoyerEmailAvecPdf(String destinataire,
                                           String sujet,
                                           String corpsHtml,
                                           byte[] pdfBytes,
                                           String nomFichierPdf)
            throws MessagingException, UnsupportedEncodingException {

        Session session = creerSession();

        MimeMessage msg = new MimeMessage(session);
        msg.setFrom(new InternetAddress(SMTP_USER, "ETAP StockFlow", "UTF-8"));
        msg.setRecipients(Message.RecipientType.TO,
                InternetAddress.parse(destinataire));

        // Encoder le sujet en UTF-8 pour éviter les problèmes d'accents
        msg.setSubject(sujet, "UTF-8");

        if (pdfBytes != null && nomFichierPdf != null) {
            // Email multipart : corps HTML + pièce jointe PDF
            MimeMultipart multipart = new MimeMultipart();

            // Partie corps HTML
            MimeBodyPart corpsPart = new MimeBodyPart();
            corpsPart.setContent(corpsHtml, "text/html; charset=UTF-8");
            multipart.addBodyPart(corpsPart);

            // Partie PDF
            MimeBodyPart pdfPart = new MimeBodyPart();
            javax.activation.DataSource ds =
                    new javax.mail.util.ByteArrayDataSource(pdfBytes, "application/pdf");
            pdfPart.setDataHandler(new javax.activation.DataHandler(ds));
            pdfPart.setFileName(MimeUtility.encodeText(nomFichierPdf, "UTF-8", "B"));
            multipart.addBodyPart(pdfPart);

            msg.setContent(multipart);
        } else {
            // Email HTML simple
            msg.setContent(corpsHtml, "text/html; charset=UTF-8");
        }

        // Envoi avec try-with-resources
        try (Transport transport = session.getTransport("smtp")) {
            transport.connect(SMTP_HOST,
                    Integer.parseInt(SMTP_PORT), SMTP_USER, SMTP_PASS);
            transport.sendMessage(msg, msg.getAllRecipients());
        } catch (Exception e) {
            throw new MessagingException("Envoi SMTP echoue : " + e.getMessage(), e);
        }
    }

    // ── Email reset mot de passe ───────────────────────────────────────────────
    public static void envoyerEmailReset(String destinataire,
                                         String token) throws MessagingException, UnsupportedEncodingException {
        String lien = "http://localhost:8080/reset-password?token=" + token;
        String corps = """
            <html><body style='font-family:Arial;color:#1a1a2e;'>
              <h2 style='color:#214293;'>ETAP StockFlow</h2>
              <p>Vous avez demande la reinitialisation de votre mot de passe.</p>
              <p>Cliquez sur le bouton ci-dessous dans les <strong>30 minutes</strong> :</p>
              <a href='%s' style='display:inline-block;background:#0154a6;color:#fff;
                 padding:10px 20px;border-radius:6px;text-decoration:none;
                 font-weight:bold;'>Reinitialiser mon mot de passe</a>
              <p style='color:#666;margin-top:20px;'>
                Si vous n'etes pas a l'origine de cette demande, ignorez cet email.
              </p>
            </body></html>
            """.formatted(lien);

        envoyerEmail(destinataire, "Reinitialisation mot de passe — ETAP StockFlow", corps);
    }
}
