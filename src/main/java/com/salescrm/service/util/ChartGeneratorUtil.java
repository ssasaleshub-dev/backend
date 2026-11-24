package com.salescrm.service.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Native Java Graphics2D chart generator for executive email digests.
 * Produces crisp, high-DPI 2x Retina PNG images with simple, human design.
 * Renders directly on pure background without card frames or artificial box borders.
 */
public final class ChartGeneratorUtil {

    private static final Logger LOG = LoggerFactory.getLogger(ChartGeneratorUtil.class);

    private ChartGeneratorUtil() {}

    /**
     * Render a clean 7-day activity bar chart directly on the background.
     *
     * @param dailyCounts 7 integers representing counts for Mon-Sun
     * @param labels      7 labels (e.g. ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"])
     * @param totalCount  Total inquiries for the week
     * @return PNG image bytes
     */
    public static byte[] generateWeeklyBarChart(int[] dailyCounts, String[] labels, long totalCount) {
        int width = 1000;
        int height = 380;

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();

        try {
            // High-fidelity rendering hints
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            // 1. Pure White Background directly matching email canvas (no card border/box)
            g2.setColor(Color.WHITE);
            g2.fillRect(0, 0, width, height);

            // 2. Header: Simple clean text directly on background with Arial font
            Font fontTitle = new Font("Arial", Font.BOLD, 20);
            Font fontSub = new Font("Arial", Font.PLAIN, 15);
            Font fontLabel = new Font("Arial", Font.PLAIN, 15);
            Font fontValue = new Font("Arial", Font.BOLD, 16);

            g2.setFont(fontTitle);
            g2.setColor(new Color(34, 34, 34)); // #222222
            g2.drawString("Daily Inflow", 20, 36);

            String subText = totalCount + " inquiries this week";
            g2.setFont(fontSub);
            g2.setColor(new Color(102, 102, 102)); // #666666
            FontMetrics fmSub = g2.getFontMetrics(fontSub);
            int subX = width - fmSub.stringWidth(subText) - 20;
            g2.drawString(subText, subX, 36);

            // 3. Grid Coordinates
            int plotLeft = 40;
            int plotRight = width - 40;
            int plotTop = 80;
            int plotBottom = height - 55;
            int plotHeight = plotBottom - plotTop;
            int plotWidth = plotRight - plotLeft;

            // Find maximum count for scaling
            int maxVal = 5;
            if (dailyCounts != null) {
                for (int c : dailyCounts) {
                    if (c > maxVal) maxVal = c;
                }
            }
            int ceilingVal = ((maxVal + 4) / 5) * 5;
            if (ceilingVal < 10) ceilingVal = 10;

            // Subtle horizontal hairline guide lines
            g2.setColor(new Color(241, 245, 249)); // #f1f5f9
            g2.setStroke(new BasicStroke(1.0f));

            int gridSteps = 3;
            for (int i = 0; i <= gridSteps; i++) {
                int y = plotBottom - (i * plotHeight / gridSteps);
                g2.drawLine(plotLeft, y, plotRight, y);
            }

            // 4. Bars & Labels
            int barCount = 7;
            int slotWidth = plotWidth / barCount;
            int barWidth = Math.min(64, slotWidth - 32);

            for (int i = 0; i < barCount; i++) {
                int val = (dailyCounts != null && i < dailyCounts.length) ? dailyCounts[i] : 0;
                String label = (labels != null && i < labels.length) ? labels[i] : ("Day " + (i + 1));

                int slotCenterX = plotLeft + (i * slotWidth) + (slotWidth / 2);
                int barX = slotCenterX - (barWidth / 2);

                int barH = (int) Math.round(((double) val / ceilingVal) * plotHeight);
                if (val > 0 && barH < 8) barH = 8;
                int barY = plotBottom - barH;

                if (val > 0) {
                    // Clean solid emerald bar with soft rounded cap
                    g2.setColor(new Color(5, 150, 105)); // #059669
                    g2.fill(new RoundRectangle2D.Float(barX, barY, barWidth, barH, 8, 8));

                    // Value label directly above bar
                    g2.setFont(fontValue);
                    g2.setColor(new Color(34, 34, 34)); // #222222
                    String valStr = String.valueOf(val);
                    FontMetrics fmV = g2.getFontMetrics(fontValue);
                    int valX = slotCenterX - (fmV.stringWidth(valStr) / 2);
                    g2.drawString(valStr, valX, barY - 8);
                } else {
                    // Subtle light dot for 0
                    g2.setColor(new Color(226, 232, 240));
                    g2.fillOval(slotCenterX - 4, plotBottom - 5, 8, 8);
                }

                // Day of Week Label below axis
                g2.setFont(fontLabel);
                g2.setColor(new Color(102, 102, 102)); // #666666
                FontMetrics fmL = g2.getFontMetrics(fontLabel);
                int labelX = slotCenterX - (fmL.stringWidth(label) / 2);
                g2.drawString(label, labelX, plotBottom + 28);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", baos);
            return baos.toByteArray();

        } catch (IOException e) {
            LOG.error("[ChartUtil] Failed to render bar chart: {}", e.getMessage(), e);
            return new byte[0];
        } finally {
            g2.dispose();
        }
    }
}
