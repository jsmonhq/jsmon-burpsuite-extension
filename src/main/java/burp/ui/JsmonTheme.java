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
    
    public JsmonTheme(boolean isDark) {
        if (isDark) {
            // Dark theme
            background = new Color(43, 43, 43);
            cardBackground = new Color(50, 50, 50);
            textPrimary = new Color(220, 220, 220);
            textSecondary = new Color(200, 200, 200);
            textValue = new Color(220, 220, 220);
            border = new Color(70, 70, 70);
            inputBackground = new Color(35, 35, 35);
            inputForeground = new Color(220, 220, 220);
            inputBorder = new Color(70, 70, 70);
            buttonPrimary = new Color(70, 130, 200);
            buttonPrimaryHover = new Color(90, 150, 220);
            buttonSecondary = new Color(60, 60, 60);
            buttonSecondaryHover = new Color(75, 75, 75);
            tableBackground = new Color(30, 30, 30);
            tableForeground = new Color(200, 200, 200);
            tableGrid = new Color(60, 60, 60);
            tableSelection = new Color(60, 100, 150);
            tableHeaderBackground = new Color(50, 50, 50);
            tableHeaderForeground = new Color(220, 220, 220);
            statusBackground = new Color(30, 30, 30);
            statusForeground = new Color(200, 200, 200);
            tabBackground = new Color(43, 43, 43);
            tabForeground = new Color(200, 200, 200);
            caretColor = new Color(100, 150, 255);
        } else {
            // Light theme
            background = new Color(245, 245, 245);
            cardBackground = new Color(255, 255, 255);
            textPrimary = new Color(30, 30, 30);
            textSecondary = new Color(60, 60, 60);
            textValue = new Color(30, 30, 30);
            border = new Color(200, 200, 200);
            inputBackground = new Color(255, 255, 255);
            inputForeground = new Color(30, 30, 30);
            inputBorder = new Color(180, 180, 180);
            buttonPrimary = new Color(70, 130, 200);
            buttonPrimaryHover = new Color(90, 150, 220);
            buttonSecondary = new Color(240, 240, 240);
            buttonSecondaryHover = new Color(220, 220, 220);
            tableBackground = new Color(255, 255, 255);
            tableForeground = new Color(30, 30, 30);
            tableGrid = new Color(220, 220, 220);
            tableSelection = new Color(200, 220, 255);
            tableHeaderBackground = new Color(240, 240, 240);
            tableHeaderForeground = new Color(30, 30, 30);
            statusBackground = new Color(255, 255, 255);
            statusForeground = new Color(30, 30, 30);
            tabBackground = new Color(245, 245, 245);
            tabForeground = new Color(60, 60, 60);
            caretColor = new Color(70, 130, 200);
        }
    }
    
    public static JsmonTheme getCurrentTheme() {
        Color defaultBg = null;
        
        defaultBg = UIManager.getColor("Panel.background");
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

