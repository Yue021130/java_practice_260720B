package com.example.mail.support;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * 动态生成图片的工具。
 *
 * 内联图片演示需要一个真实图片文件，本类用 BufferedImage 在内存中画一张
 * 简单的 PNG，避免项目里硬编码二进制图片资源。
 */
public final class ImageFactory {

    private ImageFactory() {
    }

    /**
     * 生成一张蓝底白字的 PNG 图片字节数组。
     */
    public static byte[] createPng(String text, int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        // 蓝色渐变背景
        GradientPaint paint = new GradientPaint(0, 0, new Color(37, 99, 235), width, height, new Color(30, 64, 175));
        g.setPaint(paint);
        g.fillRect(0, 0, width, height);
        // 白色文字
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 22));
        FontMetrics fm = g.getFontMetrics();
        int x = (width - fm.stringWidth(text)) / 2;
        int y = (height - fm.getHeight()) / 2 + fm.getAscent();
        g.drawString(text, x, y);
        g.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "png", baos);
        } catch (IOException e) {
            throw new IllegalStateException("生成图片失败", e);
        }
        return baos.toByteArray();
    }
}
