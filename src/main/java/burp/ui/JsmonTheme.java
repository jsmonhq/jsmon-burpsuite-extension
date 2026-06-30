package burp.ui;

import javax.swing.UIManager;
import java.awt.Color;

public class JsmonTheme {
    public final Color background;
    public final Color cardBackground;
    public final Color textPrimary;
    public final Color textSecondary;
    public final Color textValue;
    public final Color border;
    public final Color inputBackground;
    public final Color inputForeground;
    public final Color inputBorder;
    public final Color buttonPrimary;
    public final Color buttonPrimaryHover;
    public final Color buttonSecondary;
    public final Color buttonSecondaryHover;
    public final Color tableBackground;
    public final Color tableForeground;
    public final Color tableGrid;
    public final Color tableSelection;
    public final Color tableHeaderBackground;
    public final Color tableHeaderForeground;
    public final Color statusBackground;
    public final Color statusForeground;
    public final Color tabBackground;
    public final Color tabForeground;
    public final Color caretColor;
    public final Color badgeBackground;
    public final Color badgeForeground;
    public final Color badgeBorder;
    public final Color headerBackground;
    public final Color headerBorder;
    public final Color accentBlue;

    public JsmonTheme(boolean isDark) {
        accentBlue = new Color(0, 122, 255); // #007aff — matches browser extensions

        if (isDark) {
            background = new Color(28, 28, 30);
            cardBackground = new Color(38, 38, 40);
            textPrimary = new Color(245, 245, 247);
            textSecondary = new Color(142, 142, 147);
            textValue = new Color(245, 245, 247);
            border = new Color(58, 58, 60);
            inputBackground = new Color(44, 44, 46);
            inputForeground = new Color(245, 245, 247);
            inputBorder = new Color(72, 72, 74);
            buttonPrimary = accentBlue;
            buttonPrimaryHover = new Color(0, 111, 230);
            buttonSecondary = new Color(44, 44, 46);
            buttonSecondaryHover = new Color(58, 58, 60);
            tableBackground = new Color(28, 28, 30);
            tableForeground = new Color(235, 235, 240);
            tableGrid = new Color(58, 58, 60);
            tableSelection = new Color(0, 90, 180);
            tableHeaderBackground = new Color(44, 44, 46);
            tableHeaderForeground = new Color(245, 245, 247);
            statusBackground = new Color(28, 28, 30);
            statusForeground = new Color(200, 200, 205);
            tabBackground = new Color(28, 28, 30);
            tabForeground = new Color(200, 200, 205);
            caretColor = accentBlue;
            badgeBackground = new Color(30, 58, 95);
            badgeForeground = new Color(147, 197, 253);
            badgeBorder = new Color(59, 130, 246);
            headerBackground = new Color(38, 38, 40);
            headerBorder = new Color(58, 58, 60);
        } else {
            background = new Color(245, 245, 247); // #f5f5f7 page
            cardBackground = Color.WHITE;
            textPrimary = new Color(10, 10, 15); // #0a0a0f
            textSecondary = new Color(142, 142, 147); // #8e8e93
            textValue = new Color(10, 10, 15);
            border = new Color(232, 232, 232); // #e8e8e8
            inputBackground = new Color(245, 245, 247);
            inputForeground = new Color(10, 10, 15);
            inputBorder = new Color(232, 232, 232);
            buttonPrimary = accentBlue;
            buttonPrimaryHover = new Color(0, 111, 230); // #006fe6
            buttonSecondary = new Color(245, 245, 247);
            buttonSecondaryHover = new Color(236, 236, 240);
            tableBackground = Color.WHITE;
            tableForeground = new Color(10, 10, 15);
            tableGrid = new Color(232, 232, 232);
            tableSelection = new Color(219, 234, 254);
            tableHeaderBackground = new Color(245, 245, 247);
            tableHeaderForeground = new Color(10, 10, 15);
            statusBackground = Color.WHITE;
            statusForeground = new Color(82, 82, 91);
            tabBackground = new Color(245, 245, 247);
            tabForeground = new Color(82, 82, 91);
            caretColor = accentBlue;
            badgeBackground = new Color(219, 234, 254); // #dbeafe
            badgeForeground = new Color(29, 78, 216); // #1d4ed8
            badgeBorder = new Color(147, 197, 253); // #93c5fd
            headerBackground = Color.WHITE;
            headerBorder = border;
        }
    }

    public static JsmonTheme getCurrentTheme() {
        Color defaultBg = UIManager.getColor("Panel.background");
        if (defaultBg == null) {
            defaultBg = UIManager.getColor("control");
        }
        if (defaultBg == null) {
            defaultBg = UIManager.getColor("TextArea.background");
        }
        if (defaultBg == null) {
            defaultBg = UIManager.getColor("TextField.background");
        }
        if (defaultBg == null) {
            defaultBg = UIManager.getColor("window");
        }
        if (defaultBg == null) {
            try {
                defaultBg = UIManager.getDefaults().getColor("Panel.background");
            } catch (Exception e) {
                return new JsmonTheme(false);
            }
        }
        if (defaultBg == null) {
            return new JsmonTheme(false);
        }

        float[] hsb = Color.RGBtoHSB(defaultBg.getRed(), defaultBg.getGreen(), defaultBg.getBlue(), null);
        boolean isDark = hsb[2] < 0.6;
        if (hsb[2] > 0.9) {
            isDark = false;
        }
        return new JsmonTheme(isDark);
    }
}
