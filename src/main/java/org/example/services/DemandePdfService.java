package org.example.services;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import org.example.models.Demande;
import org.example.models.LigneDemande;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.time.format.DateTimeFormatter;

public class DemandePdfService {

    private static final DeviceRgb BLUE_DARK  = new DeviceRgb(33,  66,  147);
    private static final DeviceRgb BLUE_LIGHT = new DeviceRgb(232, 240, 251);
    private static final DeviceRgb GRAY_TEXT  = new DeviceRgb(107, 114, 128);
    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /** Génère le PDF et retourne les bytes (pour l'email) */
    public byte[] genererPdfBytes(Demande d) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ecrirePdf(d, new PdfWriter(baos));
        return baos.toByteArray();
    }

    /** Génère le PDF dans un fichier (pour le téléchargement) */
    public File genererPdfFichier(Demande d, File destination) throws Exception {
        ecrirePdf(d, new PdfWriter(destination));
        return destination;
    }

    private void ecrirePdf(Demande d, PdfWriter writer) throws Exception {
        try (PdfDocument pdf = new PdfDocument(writer);
             Document doc = new Document(pdf)) {

            doc.setMargins(40, 40, 40, 40);

            // ── En-tête ─────────────────────────────────────────────────────
            Table header = new Table(
                    UnitValue.createPercentArray(new float[]{1f, 2f, 1f}))
                    .useAllAvailableWidth().setMarginBottom(4);

            header.addCell(new Cell().setBorder(Border.NO_BORDER)
                    .add(new Paragraph("ETAP")
                            .setBold().setFontColor(BLUE_DARK).setFontSize(18)));
            header.addCell(new Cell().setBorder(Border.NO_BORDER)
                    .add(new Paragraph("DEMANDE DE MATERIEL")
                            .setBold().setFontColor(BLUE_DARK).setFontSize(15)
                            .setTextAlignment(TextAlignment.CENTER)));
            header.addCell(new Cell().setBorder(Border.NO_BORDER)
                    .add(new Paragraph("N° " + d.getNumeroDemande())
                            .setFontColor(GRAY_TEXT).setFontSize(10)
                            .setTextAlignment(TextAlignment.RIGHT)));
            doc.add(header);

            doc.add(new Paragraph(
                    "Entreprise Tunisienne d'Activités Pétrolières — Département Réseau")
                    .setFontSize(9).setFontColor(GRAY_TEXT).setMarginBottom(4));

            doc.add(new LineSeparator(
                    new com.itextpdf.kernel.pdf.canvas.draw.SolidLine(2f))
                    .setStrokeColor(BLUE_DARK).setMarginBottom(16));

            // ── Infos demande ────────────────────────────────────────────────
            Table info = new Table(
                    UnitValue.createPercentArray(new float[]{1.2f, 1.8f, 1.2f, 1.8f}))
                    .useAllAvailableWidth().setMarginBottom(20);

            ajouterPaire(info, "Demandeur",
                    d.getNomIntervenant() != null ? d.getNomIntervenant() : "—");
            ajouterPaire(info, "Site",
                    d.getSiteIntervenant() != null ? d.getSiteIntervenant() : "—");
            ajouterPaire(info, "Date",
                    d.getDateCreation() != null ? d.getDateCreation().format(FMT) : "—");
            ajouterPaire(info, "Priorité", d.getPrioriteLabel());
            ajouterPaire(info, "Statut", d.getStatutLabel());
            ajouterPaire(info, "Justification",
                    d.getJustification() != null ? d.getJustification() : "—");
            doc.add(info);

            // ── Table articles ───────────────────────────────────────────────
            doc.add(new Paragraph("Articles demandés")
                    .setBold().setFontColor(BLUE_DARK).setFontSize(12)
                    .setMarginBottom(8));

            Table articles = new Table(
                    UnitValue.createPercentArray(new float[]{4f, 1f}))
                    .useAllAvailableWidth().setMarginBottom(20);

            for (String h : new String[]{"Matériel", "Quantité"}) {
                articles.addHeaderCell(new Cell()
                        .setBackgroundColor(BLUE_DARK)
                        .setBorder(Border.NO_BORDER)
                        .add(new Paragraph(h)
                                .setFontColor(ColorConstants.WHITE)
                                .setBold().setFontSize(10)
                                .setTextAlignment(TextAlignment.CENTER)));
            }

            for (LigneDemande l : d.getLignes()) {
                articles.addCell(new Cell()
                        .add(new Paragraph(l.getNomMateriel()).setFontSize(10))
                        .setPadding(6));
                articles.addCell(new Cell()
                        .add(new Paragraph(String.valueOf(l.getQuantite()))
                                .setFontSize(10).setTextAlignment(TextAlignment.CENTER))
                        .setPadding(6));
            }
            doc.add(articles);

            // Motif rejet si présent
            if (d.getMotifRejet() != null && !d.getMotifRejet().isBlank()) {
                doc.add(new Paragraph("Motif de rejet : " + d.getMotifRejet())
                        .setFontSize(10).setFontColor(new DeviceRgb(220, 38, 38)));
            }

            // ── Signatures ───────────────────────────────────────────────────
            doc.add(new Paragraph("\n\n"));
            Table sig = new Table(
                    UnitValue.createPercentArray(new float[]{1f, 1f}))
                    .useAllAvailableWidth().setMarginTop(20);

            sig.addCell(new Cell().setBorder(Border.NO_BORDER)
                    .add(new Paragraph("Signature du demandeur")
                            .setBold().setFontSize(10).setFontColor(BLUE_DARK))
                    .add(new Paragraph("\n\n\n___________________________")
                            .setFontSize(10).setFontColor(GRAY_TEXT)));

            sig.addCell(new Cell().setBorder(Border.NO_BORDER)
                    .add(new Paragraph("Visa de l'administrateur")
                            .setBold().setFontSize(10).setFontColor(BLUE_DARK)
                            .setTextAlignment(TextAlignment.RIGHT))
                    .add(new Paragraph("\n\n\n___________________________")
                            .setFontSize(10).setFontColor(GRAY_TEXT)
                            .setTextAlignment(TextAlignment.RIGHT)));
            doc.add(sig);

            // ── Pied de page ─────────────────────────────────────────────────
            doc.add(new Paragraph("\n"));
            doc.add(new LineSeparator(
                    new com.itextpdf.kernel.pdf.canvas.draw.SolidLine(0.5f))
                    .setStrokeColor(GRAY_TEXT).setMarginBottom(6));
            doc.add(new Paragraph(
                    "ETAP — Entreprise Tunisienne d'Activités Pétrolières\n"
                            + "Document généré automatiquement par ETAP StockFlow")
                    .setFontSize(8).setFontColor(GRAY_TEXT)
                    .setTextAlignment(TextAlignment.CENTER));
        }
    }

    private void ajouterPaire(Table table, String label, String valeur) {
        table.addCell(new Cell().setBorder(Border.NO_BORDER)
                .setBackgroundColor(BLUE_LIGHT).setPadding(6)
                .add(new Paragraph(label).setBold().setFontSize(10).setFontColor(BLUE_DARK)));
        table.addCell(new Cell().setBorder(Border.NO_BORDER).setPadding(6)
                .add(new Paragraph(valeur).setFontSize(10)));
    }
}
