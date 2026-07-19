package org.example.utils;

import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Circle;

public final class CircularImageUtil {

    private CircularImageUtil() {}

    /**
     * Affiche une image dans un ImageView recadrée en carré (centrée, sans déformation)
     * puis découpée en cercle.
     *
     * @param imageView la vue cible
     * @param image     l'image source
     * @param diameter  diamètre final en pixels (fitWidth = fitHeight = diameter)
     */
    public static void appliquer(ImageView imageView, Image image, double diameter) {
        double sourceW = image.getWidth();
        double sourceH = image.getHeight();
        double side = Math.min(sourceW, sourceH);
        double x = (sourceW - side) / 2.0;
        double y = (sourceH - side) / 2.0;

        imageView.setImage(image);
        imageView.setPreserveRatio(false);
        imageView.setViewport(new Rectangle2D(x, y, side, side));
        imageView.setFitWidth(diameter);
        imageView.setFitHeight(diameter);
        imageView.setClip(new Circle(diameter / 2.0, diameter / 2.0, diameter / 2.0));
        imageView.setVisible(true);
        imageView.setManaged(true);
    }
}