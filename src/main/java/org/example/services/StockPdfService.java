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
import org.example.models.MouvementStock;

import java.io.File;
import java.time.format.DateTimeFormatter;

public class StockPdfService {

    private static final DeviceRgb BLUE_DARK  = new DeviceRgb(33,  66,  147);
    private static final DeviceRgb BLUE_LIGHT = new DeviceRgb(232, 240, 251);
    private static final DeviceRgb GRAY_TEXT  = new DeviceRgb(107, 114, 128);
    private static final DeviceRgb GREEN      = new DeviceRgb(22,  163, 74);
    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // ── BON D'ENTREE ───────────────────────────────────────────────────────────
    public File genererPdfEntree(MouvementStock m, File destination) throws Exception {
        try (PdfWriter writer = new PdfWriter(destination);
             PdfDocument pdf  = new PdfDocument(writer);
             Document doc     = new Document(pdf)) {

            doc.setMargins(40, 40, 40, 40);

            // En-tête
            ajouterEnTete(doc, "BON D'ENTREE STOCK", m.getNumeroBon());

            // Informations générales
            Table infoTable = new Table(
                    UnitValue.createPercentArray(new float[]{1.2f, 1.8f, 1.2f, 1.8f}))
                    .useAllAvailableWidth()
                    .setMarginBottom(20);

            ajouterPaireInfo(infoTable, "Date",
                    m.getDateCreation() != null ? m.getDateCreation().format(FMT) : "—");
            ajouterPaireInfo(infoTable, "Fournisseur",
                    m.getNomFournisseur() != null ? m.getNomFournisseur() : "—");

            doc.add(infoTable);

            // Table des matériels
            Table table = new Table(
                    UnitValue.createPercentArray(new float[]{3f, 1f, 1.8f, 1.8f}))
                    .useAllAvailableWidth()
                    .setMarginBottom(16);

            // En-têtes colonnes
            for (String h : new String[]{"Matériel / Référence", "Quantité",
                    "Prix unitaire (TND)", "Montant (TND)"}) {
                table.addHeaderCell(new Cell()
                        .setBackgroundColor(BLUE_DARK)
                        .setBorder(Border.NO_BORDER)
                        .add(new Paragraph(h)
                                .setFontColor(ColorConstants.WHITE)
                                .setBold()
                                .setFontSize(10)
                                .setTextAlignment(TextAlignment.CENTER)));
            }

            // Ligne matériel
            String nomRef = m.getNomMateriel() != null ? m.getNomMateriel() : "—";
            if (m.getReferenceMateriel() != null && !m.getReferenceMateriel().isBlank())
                nomRef += "\n" + m.getReferenceMateriel();

            table.addCell(cellule(nomRef, TextAlignment.LEFT, false));
            table.addCell(cellule(String.valueOf(m.getQuantite()),
                    TextAlignment.CENTER, true));
            table.addCell(cellule(String.format("%.3f", m.getPrixUnitaire()),
                    TextAlignment.CENTER, false));
            table.addCell(new Cell()
                    .add(new Paragraph(String.format("%.3f", m.getMontantTotal()))
                            .setFontColor(GREEN).setBold().setFontSize(11)
                            .setTextAlignment(TextAlignment.CENTER))
                    .setPadding(6));

            doc.add(table);

            // Total en évidence
            Table totalTable = new Table(
                    UnitValue.createPercentArray(new float[]{3f, 1.8f}))
                    .useAllAvailableWidth()
                    .setMarginBottom(20);
            totalTable.addCell(new Cell()
                    .setBorder(Border.NO_BORDER)
                    .add(new Paragraph("")));
            totalTable.addCell(new Cell()
                    .setBackgroundColor(BLUE_LIGHT)
                    .setBorder(Border.NO_BORDER)
                    .add(new Paragraph(
                            "TOTAL : " + String.format("%.3f TND", m.getMontantTotal()))
                            .setBold().setFontSize(12).setFontColor(BLUE_DARK)
                            .setTextAlignment(TextAlignment.RIGHT)));
            doc.add(totalTable);

            // Observation
            if (m.getObservation() != null && !m.getObservation().isBlank()) {
                doc.add(new Paragraph("Observation : " + m.getObservation())
                        .setFontSize(10).setFontColor(GRAY_TEXT).setMarginTop(8));
            }

            // Zone signatures
            ajouterSignatures(doc);
            ajouterPiedDePage(doc);
        }
        return destination;
    }

    // ── BON DE SORTIE ──────────────────────────────────────────────────────────
    public File genererPdfSortie(MouvementStock m, File destination) throws Exception {
        try (PdfWriter writer = new PdfWriter(destination);
             PdfDocument pdf  = new PdfDocument(writer);
             Document doc     = new Document(pdf)) {

            doc.setMargins(40, 40, 40, 40);

            ajouterEnTete(doc, "BON DE SORTIE STOCK", m.getNumeroBon());

            // Informations générales
            Table infoTable = new Table(
                    UnitValue.createPercentArray(new float[]{1.2f, 1.8f, 1.2f, 1.8f}))
                    .useAllAvailableWidth()
                    .setMarginBottom(20);

            ajouterPaireInfo(infoTable, "Date",
                    m.getDateCreation() != null ? m.getDateCreation().format(FMT) : "—");
            ajouterPaireInfo(infoTable, "N° Demande",
                    m.getNumeroDemande() != null ? m.getNumeroDemande() : "—");
            ajouterPaireInfo(infoTable, "Site ETAP",
                    m.getSiteEtap() != null ? m.getSiteEtap() : "—");
            // Cellule vide pour équilibrer la grille 2x2
            infoTable.addCell(new Cell().setBorder(Border.NO_BORDER).add(new Paragraph("")));
            infoTable.addCell(new Cell().setBorder(Border.NO_BORDER).add(new Paragraph("")));

            doc.add(infoTable);

            // Table matériel
            Table table = new Table(
                    UnitValue.createPercentArray(new float[]{4f, 1f}))
                    .useAllAvailableWidth()
                    .setMarginBottom(16);

            for (String h : new String[]{"Matériel / Référence", "Quantité"}) {
                table.addHeaderCell(new Cell()
                        .setBackgroundColor(BLUE_DARK)
                        .setBorder(Border.NO_BORDER)
                        .add(new Paragraph(h)
                                .setFontColor(ColorConstants.WHITE)
                                .setBold().setFontSize(10)
                                .setTextAlignment(TextAlignment.CENTER)));
            }

            String nomRef = m.getNomMateriel() != null ? m.getNomMateriel() : "—";
            if (m.getReferenceMateriel() != null && !m.getReferenceMateriel().isBlank())
                nomRef += "\n" + m.getReferenceMateriel();

            table.addCell(cellule(nomRef, TextAlignment.LEFT, false));
            table.addCell(cellule(String.valueOf(m.getQuantite()),
                    TextAlignment.CENTER, true));

            doc.add(table);

            if (m.getObservation() != null && !m.getObservation().isBlank()) {
                doc.add(new Paragraph("Observation : " + m.getObservation())
                        .setFontSize(10).setFontColor(GRAY_TEXT).setMarginTop(8));
            }

            ajouterSignatures(doc);
            ajouterPiedDePage(doc);
        }
        return destination;
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private void ajouterEnTete(Document doc, String titre, String numeroBon) {
        // Ligne logo + titre
        Table header = new Table(
                UnitValue.createPercentArray(new float[]{1f, 2f, 1f}))
                .useAllAvailableWidth()
                .setMarginBottom(4);

        header.addCell(new Cell()
                .setBorder(Border.NO_BORDER)
                .add(new Paragraph("ETAP")
                        .setBold().setFontColor(BLUE_DARK).setFontSize(18)));

        header.addCell(new Cell()
                .setBorder(Border.NO_BORDER)
                .add(new Paragraph(titre)
                        .setBold().setFontColor(BLUE_DARK).setFontSize(16)
                        .setTextAlignment(TextAlignment.CENTER)));

        header.addCell(new Cell()
                .setBorder(Border.NO_BORDER)
                .add(new Paragraph("N° " + numeroBon)
                        .setFontColor(GRAY_TEXT).setFontSize(10)
                        .setTextAlignment(TextAlignment.RIGHT)));

        doc.add(header);

        doc.add(new Paragraph("Entreprise Tunisienne d'Activités Pétrolières — Département Réseau")
                .setFontSize(9).setFontColor(GRAY_TEXT).setMarginBottom(4));

        // Ligne séparatrice bleue
        doc.add(new com.itextpdf.layout.element.LineSeparator(
                new com.itextpdf.kernel.pdf.canvas.draw.SolidLine(2f))
                .setStrokeColor(BLUE_DARK)
                .setMarginBottom(16));
    }

    private void ajouterPaireInfo(Table table, String label, String valeur) {
        table.addCell(new Cell()
                .setBorder(Border.NO_BORDER)
                .setBackgroundColor(BLUE_LIGHT)
                .setPadding(6)
                .add(new Paragraph(label)
                        .setBold().setFontSize(10).setFontColor(BLUE_DARK)));
        table.addCell(new Cell()
                .setBorder(Border.NO_BORDER)
                .setPadding(6)
                .add(new Paragraph(valeur)
                        .setFontSize(10)));
    }

    private Cell cellule(String texte, TextAlignment align, boolean bold) {
        Paragraph p = new Paragraph(texte).setFontSize(10);
        if (bold) p.setBold();
        return new Cell()
                .add(p.setTextAlignment(align))
                .setPadding(6);
    }

    private void ajouterSignatures(Document doc) {
        doc.add(new Paragraph("\n\n"));

        Table sigTable = new Table(
                UnitValue.createPercentArray(new float[]{1f, 1f}))
                .useAllAvailableWidth()
                .setMarginTop(20);

        sigTable.addCell(new Cell()
                .setBorder(Border.NO_BORDER)
                .add(new Paragraph("Signature de l'émetteur")
                        .setBold().setFontSize(10).setFontColor(BLUE_DARK))
                .add(new Paragraph("\n\n\n___________________________")
                        .setFontSize(10).setFontColor(GRAY_TEXT)));

        sigTable.addCell(new Cell()
                .setBorder(Border.NO_BORDER)
                .setTextAlignment(TextAlignment.RIGHT)
                .add(new Paragraph("Signature du récepteur")
                        .setBold().setFontSize(10).setFontColor(BLUE_DARK)
                        .setTextAlignment(TextAlignment.RIGHT))
                .add(new Paragraph("\n\n\n___________________________")
                        .setFontSize(10).setFontColor(GRAY_TEXT)
                        .setTextAlignment(TextAlignment.RIGHT)));

        doc.add(sigTable);
    }

    private void ajouterPiedDePage(Document doc) {
        doc.add(new Paragraph("\n"));
        doc.add(new com.itextpdf.layout.element.LineSeparator(
                new com.itextpdf.kernel.pdf.canvas.draw.SolidLine(0.5f))
                .setStrokeColor(GRAY_TEXT)
                .setMarginBottom(6));
        doc.add(new Paragraph(
                "ETAP — Entreprise Tunisienne d'Activités Pétrolières\n"
                        + "Ce document est généré automatiquement par ETAP StockFlow")
                .setFontSize(8).setFontColor(GRAY_TEXT)
                .setTextAlignment(TextAlignment.CENTER));
    }
}
