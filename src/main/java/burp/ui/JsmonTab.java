package burp.ui;

import burp.JsmonExtension;
import burp.api.montoya.MontoyaApi;
import burp.api.montoya.logging.Logging;
import burp.model.JsUrlEntry;
import burp.model.UserProfile;
import burp.model.Workspace;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.ArrayList;

public class JsmonTab extends JPanel {
    
    private MontoyaApi api;
    private Logging logging;
    private JsmonExtension extension;
    
    // Theme support
    // Use JsmonTheme class instead of inner Theme class
    private JsmonTheme theme = JsmonTheme.getCurrentTheme();
    
    // Legacy Theme class kept for backward compatibility - will be removed
    @Deprecated
    private static class Theme {
        final Color background;
        final Color cardBackground;
        final Color textPrimary;
        final Color textSecondary;
        final Color textValue;
        final Color border;
        final Color inputBackground;
        final Color inputForeground;
        final Color inputBorder;
        final Color buttonPrimary;
        final Color buttonPrimaryHover;
        final Color buttonSecondary;
        final Color buttonSecondaryHover;
        final Color tableBackground;
        final Color tableForeground;
        final Color tableGrid;
        final Color tableSelection;
        final Color tableHeaderBackground;
        final Color tableHeaderForeground;
        final Color statusBackground;
        final Color statusForeground;
        final Color tabBackground;
        final Color tabForeground;
        final Color caretColor;
        
        Theme(boolean isDark) {
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
        
        static Theme getCurrentTheme() {
            // Try multiple methods to detect theme
            Color defaultBg = null;
            
            // Method 1: Check Panel.background
            defaultBg = UIManager.getColor("Panel.background");
            
            // Method 2: Check control background
            if (defaultBg == null) {
                defaultBg = UIManager.getColor("control");
            }
            
            // Method 3: Check text area background
            if (defaultBg == null) {
                defaultBg = UIManager.getColor("TextArea.background");
            }
            
            // Method 4: Check text field background
            if (defaultBg == null) {
                defaultBg = UIManager.getColor("TextField.background");
            }
            
            // Method 5: Check window background
            if (defaultBg == null) {
                defaultBg = UIManager.getColor("window");
            }
            
            if (defaultBg == null) {
                // Fallback: check if we can get any background color from the system
                try {
                    defaultBg = UIManager.getDefaults().getColor("Panel.background");
                } catch (Exception e) {
                    // If all else fails, assume light mode (more common in modern UIs)
                    return new Theme(false);
                }
            }
            
            if (defaultBg == null) {
                // Final fallback: assume light mode
                return new Theme(false);
            }
            
            // Calculate brightness: if brightness < 0.6, it's dark (more lenient threshold)
            float[] hsb = Color.RGBtoHSB(defaultBg.getRed(), defaultBg.getGreen(), defaultBg.getBlue(), null);
            boolean isDark = hsb[2] < 0.6; // brightness < 0.6 means dark
            
            // Also check if it's a very light color (brightness > 0.9) to be sure it's light mode
            if (hsb[2] > 0.9) {
                isDark = false;
            }
            
            return new Theme(isDark);
        }
    }
    
    // Theme is now initialized above
    
    // UI Components
    private JTextField apiKeyField;
    private JButton fetchWorkspacesButton;
    private JButton getApiKeyButton;
    private JButton jsmonLinkButton; // Store reference to jsmon.sh button
    private JComboBox<String> workspaceComboBox;
    private java.util.Map<String, Workspace> workspaceMap;
    private JButton createWorkspaceButton;
    private JTextField newWorkspaceNameField;
    private JTextArea scopedDomainField;
    private JLabel userNameValue;
    private JLabel userEmailValue;
    private JLabel userLimitsValue;
    private JCheckBox automateScanCheckbox;
    private JTextArea statusArea;
    
    // Data display tabs
    private JTabbedPane dataTabs;
    private JTable secretsTable;
    private DefaultTableModel secretsTableModel;
    private JPanel jsIntelligencePanel;
    private JTable jsUrlsTable;
    private DefaultTableModel jsUrlsTableModel;
    private JTable apiPathsTable;
    private DefaultTableModel apiPathsTableModel;
    private JTable urlsTable;
    private DefaultTableModel urlsTableModel;
    private JTable domainsTable;
    private DefaultTableModel domainsTableModel;
    private JTable ipAddressesTable;
    private DefaultTableModel ipAddressesTableModel;
    private JTable emailsTable;
    private DefaultTableModel emailsTableModel;
    private JTable s3BucketsTable;
    private DefaultTableModel s3BucketsTableModel;
    private JTable invalidNodeModulesTable;
    private DefaultTableModel invalidNodeModulesTableModel;
    private JTabbedPane jsIntelligenceSubTabs;
    
    // Pagination and counts
    private java.util.Map<String, Integer> fieldCounts = new java.util.HashMap<>();
    private int secretsCurrentPage = 1;
    private int secretsTotalCount = 0; // Store total secrets count
    private JLabel secretsPageLabel; // Store reference to page label for updates
    private int jsUrlsCurrentPage = 1;
    private int apiPathsCurrentPage = 1;
    private int urlsCurrentPage = 1;
    private int domainsCurrentPage = 1;
    private int ipAddressesCurrentPage = 1;
    private int emailsCurrentPage = 1;
    private int s3BucketsCurrentPage = 1;
    private int invalidNodeModulesCurrentPage = 1;
    
    public JsmonTab(MontoyaApi api, JsmonExtension extension) {
        this.api = api;
        this.logging = api.logging();
        this.extension = extension;
        
        initializeUI();
    }
    
    @Override
    public void addNotify() {
        super.addNotify();
        // When component is added to parent, re-check theme from parent's background
        SwingUtilities.invokeLater(() -> {
            updateThemeFromParent();
            setupThemeListener();
        });
    }
    
    private void setupThemeListener() {
        // Listen for UIManager property changes (theme changes)
        UIManager.addPropertyChangeListener(evt -> {
            if ("lookAndFeel".equals(evt.getPropertyName()) || 
                evt.getPropertyName() != null && evt.getPropertyName().contains("background")) {
                SwingUtilities.invokeLater(() -> {
                    updateThemeFromParent();
                });
            }
        });
        
        // Also check theme when component becomes visible
        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentShown(java.awt.event.ComponentEvent e) {
                SwingUtilities.invokeLater(() -> updateThemeFromParent());
            }
        });
        
        // Periodically check for theme changes (every 2 seconds when visible)
        javax.swing.Timer themeCheckTimer = new javax.swing.Timer(2000, e -> {
            if (isVisible()) {
                updateThemeFromParent();
            }
        });
        themeCheckTimer.setRepeats(true);
        themeCheckTimer.start();
    }
    
    private void updateThemeFromParent() {
        try {
            // First try to get theme from parent component's actual background
            boolean isDark = false;
            java.awt.Container parent = getParent();
            int depth = 0;
            while (parent != null && depth < 10) { // Limit depth to avoid infinite loops
                Color parentBg = parent.getBackground();
                if (parentBg != null && parent.isOpaque()) {
                    float[] hsb = Color.RGBtoHSB(parentBg.getRed(), parentBg.getGreen(), parentBg.getBlue(), null);
                    if (hsb[2] < 0.6) {
                        isDark = true;
                        break;
                    } else if (hsb[2] > 0.9) {
                        isDark = false;
                        break;
                    }
                }
                parent = parent.getParent();
                depth++;
            }
            
            // Fallback to UIManager detection
            if (depth >= 10 || parent == null) {
                JsmonTheme detectedTheme = JsmonTheme.getCurrentTheme();
                isDark = detectedTheme.background.getRed() < 100;
            }
            
            JsmonTheme newTheme = new JsmonTheme(isDark);
            // Check if theme changed by comparing background colors
            boolean currentIsDark = theme.background.getRed() < 100; // Dark theme has low red value
            
            if (currentIsDark != isDark) {
                theme = newTheme;
                applyTheme();
            }
        } catch (Exception e) {
            // If theme update fails, keep current theme
        }
    }
    
    private void applyTheme() {
        // Re-apply theme colors to all components
        SwingUtilities.invokeLater(() -> {
            setBackground(theme.background);
            
            // Update main components directly
            if (apiKeyField != null) {
                apiKeyField.setBackground(theme.inputBackground);
                apiKeyField.setForeground(theme.inputForeground);
                apiKeyField.setCaretColor(theme.caretColor);
            }
            if (newWorkspaceNameField != null) {
                newWorkspaceNameField.setBackground(theme.inputBackground);
                newWorkspaceNameField.setForeground(theme.inputForeground);
                newWorkspaceNameField.setCaretColor(theme.caretColor);
            }
            if (scopedDomainField != null) {
                scopedDomainField.setBackground(theme.inputBackground);
                scopedDomainField.setForeground(theme.inputForeground);
                scopedDomainField.setCaretColor(theme.caretColor);
            }
            if (statusArea != null) {
                statusArea.setBackground(theme.statusBackground);
                statusArea.setForeground(theme.statusForeground);
            }
            if (secretsTable != null) {
                secretsTable.setBackground(theme.tableBackground);
                secretsTable.setForeground(theme.tableForeground);
                secretsTable.setGridColor(theme.tableGrid);
                secretsTable.setSelectionBackground(theme.tableSelection);
                secretsTable.getTableHeader().setBackground(theme.tableHeaderBackground);
                secretsTable.getTableHeader().setForeground(theme.tableHeaderForeground);
            }
            if (jsUrlsTable != null) {
                jsUrlsTable.setBackground(theme.tableBackground);
                jsUrlsTable.setForeground(theme.tableForeground);
                jsUrlsTable.setGridColor(theme.tableGrid);
                jsUrlsTable.setSelectionBackground(theme.tableSelection);
                jsUrlsTable.getTableHeader().setBackground(theme.tableHeaderBackground);
                jsUrlsTable.getTableHeader().setForeground(theme.tableHeaderForeground);
            }
            if (apiPathsTable != null) {
                apiPathsTable.setBackground(theme.tableBackground);
                apiPathsTable.setForeground(theme.tableForeground);
                apiPathsTable.setGridColor(theme.tableGrid);
                apiPathsTable.setSelectionBackground(theme.tableSelection);
                apiPathsTable.getTableHeader().setBackground(theme.tableHeaderBackground);
                apiPathsTable.getTableHeader().setForeground(theme.tableHeaderForeground);
            }
            if (dataTabs != null) {
                dataTabs.setBackground(theme.tabBackground);
                dataTabs.setForeground(theme.tabForeground);
            }
            if (jsIntelligenceSubTabs != null) {
                jsIntelligenceSubTabs.setBackground(theme.tabBackground);
                jsIntelligenceSubTabs.setForeground(theme.tabForeground);
            }
            if (userNameValue != null) userNameValue.setForeground(theme.textValue);
            if (userEmailValue != null) userEmailValue.setForeground(theme.textValue);
            if (userLimitsValue != null) userLimitsValue.setForeground(theme.textValue);
            
            // Update Get API Key button specifically
            if (getApiKeyButton != null) {
                getApiKeyButton.setBackground(theme.buttonSecondary);
                getApiKeyButton.setForeground(theme.textPrimary);
                getApiKeyButton.repaint();
            }
            
            // Update Fetch Workspaces button
            if (fetchWorkspacesButton != null) {
                fetchWorkspacesButton.setBackground(theme.buttonPrimary);
                fetchWorkspacesButton.setForeground(Color.WHITE);
            }
            
            // Update jsmon.sh link button
            if (jsmonLinkButton != null) {
                jsmonLinkButton.setForeground(theme.textSecondary);
            }
            
            // Update all cards and labels recursively
            updateComponentTree(this);
        });
    }
    
    private void updateComponentTree(java.awt.Container container) {
        for (java.awt.Component comp : container.getComponents()) {
            if (comp instanceof JPanel) {
                JPanel panel = (JPanel) comp;
                // Check if it's a card (has specific background)
                if (panel.getBackground() != null) {
                    Color bg = panel.getBackground();
                    // If it matches old card background, update it
                    if (bg.getRed() == 50 && bg.getGreen() == 50 && bg.getBlue() == 50) {
                        panel.setBackground(theme.cardBackground);
                    } else if (bg.getRed() == 255 && bg.getGreen() == 255 && bg.getBlue() == 255) {
                        panel.setBackground(theme.cardBackground);
                    }
                }
                updateComponentTree(panel);
            } else if (comp instanceof JLabel) {
                JLabel label = (JLabel) comp;
                Color fg = label.getForeground();
                if (fg != null) {
                    // Update text colors
                    if ((fg.getRed() == 220 && fg.getGreen() == 220 && fg.getBlue() == 220) ||
                        (fg.getRed() == 200 && fg.getGreen() == 200 && fg.getBlue() == 200)) {
                        label.setForeground(theme.textPrimary);
                    } else if (fg.getRed() == 30 && fg.getGreen() == 30 && fg.getBlue() == 30) {
                        label.setForeground(theme.textPrimary);
                    } else if (fg.getRed() == 60 && fg.getGreen() == 60 && fg.getBlue() == 60) {
                        label.setForeground(theme.textSecondary);
                    }
                }
            } else if (comp instanceof JTextField) {
                JTextField field = (JTextField) comp;
                field.setBackground(theme.inputBackground);
                field.setForeground(theme.inputForeground);
                field.setCaretColor(theme.caretColor);
                if (field.getBorder() instanceof javax.swing.border.CompoundBorder) {
                    javax.swing.border.CompoundBorder cb = (javax.swing.border.CompoundBorder) field.getBorder();
                    if (cb.getOutsideBorder() instanceof javax.swing.border.LineBorder) {
                        field.setBorder(BorderFactory.createCompoundBorder(
                            new LineBorder(theme.inputBorder, 1),
                            cb.getInsideBorder()
                        ));
                    }
                }
            } else if (comp instanceof JTextArea) {
                JTextArea area = (JTextArea) comp;
                if (area == statusArea) {
                    area.setBackground(theme.statusBackground);
                    area.setForeground(theme.statusForeground);
                } else {
                    area.setBackground(theme.inputBackground);
                    area.setForeground(theme.inputForeground);
                    area.setCaretColor(theme.caretColor);
                }
                if (area.getBorder() instanceof javax.swing.border.CompoundBorder) {
                    javax.swing.border.CompoundBorder cb = (javax.swing.border.CompoundBorder) area.getBorder();
                    if (cb.getOutsideBorder() instanceof javax.swing.border.LineBorder) {
                        area.setBorder(BorderFactory.createCompoundBorder(
                            new LineBorder(theme.inputBorder, 1),
                            cb.getInsideBorder()
                        ));
                    }
                }
            } else if (comp instanceof JTable) {
                JTable table = (JTable) comp;
                table.setBackground(theme.tableBackground);
                table.setForeground(theme.tableForeground);
                table.setGridColor(theme.tableGrid);
                table.setSelectionBackground(theme.tableSelection);
                table.getTableHeader().setBackground(theme.tableHeaderBackground);
                table.getTableHeader().setForeground(theme.tableHeaderForeground);
            } else if (comp instanceof JButton) {
                JButton button = (JButton) comp;
                // Skip the Get API Key button as it's handled separately
                if (button == getApiKeyButton) {
                    return;
                }
                
                // Update button colors based on their current state
                Color currentFg = button.getForeground();
                
                // Check if it's a primary button (usually has white text)
                if (currentFg != null && currentFg.equals(Color.WHITE)) {
                    button.setBackground(theme.buttonPrimary);
                    button.setForeground(Color.WHITE);
                    // Update border if it exists
                    if (button.getBorder() instanceof javax.swing.border.CompoundBorder) {
                        javax.swing.border.CompoundBorder cb = (javax.swing.border.CompoundBorder) button.getBorder();
                        if (cb.getOutsideBorder() instanceof javax.swing.border.LineBorder) {
                            button.setBorder(BorderFactory.createCompoundBorder(
                                new LineBorder(theme.border, 1),
                                cb.getInsideBorder()
                            ));
                        }
                    }
                } else {
                    // Secondary button or custom button
                    button.setBackground(theme.buttonSecondary);
                    button.setForeground(theme.textPrimary);
                    // Update border if it exists
                    if (button.getBorder() instanceof javax.swing.border.CompoundBorder) {
                        javax.swing.border.CompoundBorder cb = (javax.swing.border.CompoundBorder) button.getBorder();
                        if (cb.getOutsideBorder() instanceof javax.swing.border.LineBorder) {
                            button.setBorder(BorderFactory.createCompoundBorder(
                                new LineBorder(theme.border, 1),
                                cb.getInsideBorder()
                            ));
                        }
                    }
                }
                button.repaint();
            } else if (comp instanceof JTabbedPane) {
                JTabbedPane tabs = (JTabbedPane) comp;
                tabs.setBackground(theme.tabBackground);
                tabs.setForeground(theme.tabForeground);
            } else if (comp instanceof JScrollPane) {
                JScrollPane scroll = (JScrollPane) comp;
                if (scroll.getViewport() != null && scroll.getViewport().getView() instanceof JTable) {
                    scroll.getViewport().setBackground(theme.tableBackground);
                } else if (scroll.getViewport() != null && scroll.getViewport().getView() instanceof JTextArea) {
                    JTextArea view = (JTextArea) scroll.getViewport().getView();
                    if (view == statusArea) {
                        scroll.getViewport().setBackground(theme.statusBackground);
                    }
                }
                updateComponentTree(scroll);
            } else if (comp instanceof java.awt.Container) {
                updateComponentTree((java.awt.Container) comp);
            }
        }
    }
    
    private void initializeUI() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setBackground(theme.background);
        
        // Top panel (empty now, jsmon.sh button moved to JS Intelligence panel)
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);
        topPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
        
        // Split pane: Left (configuration) and Right (data display - to be implemented)
        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainSplitPane.setDividerLocation(500); // Moved right to avoid overlapping with buttons
        mainSplitPane.setDividerSize(5);
        mainSplitPane.setResizeWeight(0.32); // Keep right side room for data tabs
        mainSplitPane.setBorder(BorderFactory.createEmptyBorder());
        mainSplitPane.setOpaque(false);
        
        // Left panel: Configuration sections
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setOpaque(false);
        
        // Main container with card-like sections
        JPanel mainContainer = new JPanel();
        mainContainer.setLayout(new BoxLayout(mainContainer, BoxLayout.Y_AXIS));
        mainContainer.setOpaque(false);
        
        // ========== API KEY SECTION ==========
        JPanel apiKeyCard = createModernCard(""); // Empty title, we'll add custom header
        // Reduced padding for API key card
        apiKeyCard.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(theme.border, 1, true),
            new EmptyBorder(8, 12, 10, 12) // Reduced padding
        ));
        
        // Create header panel with title on left and Get API Key button on right
        JPanel apiKeyHeader = new JPanel(new BorderLayout());
        apiKeyHeader.setOpaque(false);
        JLabel apiKeyTitle = new JLabel("🔑 API Key");
        apiKeyTitle.setFont(apiKeyTitle.getFont().deriveFont(Font.BOLD, 14f));
        apiKeyTitle.setForeground(theme.textPrimary);
        apiKeyHeader.add(apiKeyTitle, BorderLayout.WEST);
        
        // Get API Key button - styled with round background and arrow
        getApiKeyButton = new JButton("Get API Key ↗") {
            @Override
            protected void paintComponent(java.awt.Graphics g) {
                java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
                g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        getApiKeyButton.setToolTipText("Open JSMon API key settings page");
        getApiKeyButton.setOpaque(false);
        getApiKeyButton.setContentAreaFilled(false);
        getApiKeyButton.setBorderPainted(false);
        getApiKeyButton.setFocusPainted(false);
        getApiKeyButton.setBackground(theme.buttonSecondary);
        getApiKeyButton.setForeground(theme.textPrimary);
        getApiKeyButton.setFont(getApiKeyButton.getFont().deriveFont(Font.PLAIN, 11f));
        getApiKeyButton.setPreferredSize(new Dimension(115, 24));
        getApiKeyButton.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        getApiKeyButton.addActionListener(e -> {
            try {
                java.awt.Desktop.getDesktop().browse(new java.net.URI("https://app.jsmon.sh/settings?category=keys"));
                appendStatus("✓ Opening API key page in browser...");
            } catch (Exception ex) {
                appendStatus("✗ Error opening browser: " + ex.getMessage());
                logging.logToError("Error opening browser: " + ex.getMessage());
            }
        });
        // Add hover effect - use final reference to theme for proper updates
        final JButton finalGetApiKeyButton = getApiKeyButton;
        getApiKeyButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                finalGetApiKeyButton.setBackground(theme.buttonSecondaryHover);
                finalGetApiKeyButton.repaint();
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                finalGetApiKeyButton.setBackground(theme.buttonSecondary);
                finalGetApiKeyButton.repaint();
            }
        });
        apiKeyHeader.add(getApiKeyButton, BorderLayout.EAST);
        apiKeyHeader.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));
        
        // API Key field
        JPanel apiKeyContent = new JPanel(new BorderLayout(0, 6));
        apiKeyContent.setOpaque(false);
        apiKeyContent.add(apiKeyHeader, BorderLayout.NORTH);
        
        JPanel apiKeyInputContainer = new JPanel(new BorderLayout(0, 6));
        apiKeyInputContainer.setOpaque(false);
        apiKeyInputContainer.add(createLabel("API Key:", true), BorderLayout.NORTH);
        
        JPanel apiKeyInputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        apiKeyInputPanel.setOpaque(false);
        apiKeyField = createStyledTextField();
        apiKeyField.setToolTipText("Enter your JSMon API key");
        apiKeyField.setPreferredSize(new Dimension(250, 32));
        apiKeyField.setMaximumSize(new Dimension(250, 32));
        // Auto-save API key when user finishes entering it
        apiKeyField.addActionListener(e -> autoSaveConfiguration());
        apiKeyField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                autoSaveConfiguration();
            }
        });
        apiKeyInputPanel.add(apiKeyField);
        
        // Fetch button in API key section
        fetchWorkspacesButton = createPrimaryButton("Fetch Workspaces");
        fetchWorkspacesButton.addActionListener(e -> fetchWorkspaces());
        fetchWorkspacesButton.setPreferredSize(new Dimension(150, 32));
        fetchWorkspacesButton.setMaximumSize(new Dimension(150, 32));
        fetchWorkspacesButton.setFont(fetchWorkspacesButton.getFont().deriveFont(Font.BOLD, 12f));
        apiKeyInputPanel.add(fetchWorkspacesButton);
        
        apiKeyInputContainer.add(apiKeyInputPanel, BorderLayout.CENTER);
        apiKeyContent.add(apiKeyInputContainer, BorderLayout.CENTER);
        apiKeyCard.add(apiKeyContent, BorderLayout.CENTER);
        mainContainer.add(apiKeyCard);
        mainContainer.add(Box.createVerticalStrut(8));

        // ========== USER INFO SECTION ==========
        JPanel userCard = createModernCard("👤 User");
        JPanel userContent = new JPanel();
        userContent.setLayout(new BoxLayout(userContent, BoxLayout.Y_AXIS));
        userContent.setOpaque(false);

        userNameValue = createValueLabel("—");
        userEmailValue = createValueLabel("—");
        userLimitsValue = createValueLabel("—");

        userContent.add(createKeyValueRow("Name:", userNameValue));
        userContent.add(Box.createVerticalStrut(4));
        userContent.add(createKeyValueRow("Email:", userEmailValue));
        userContent.add(Box.createVerticalStrut(4));
        userContent.add(createKeyValueRow("JSScan Credits:", userLimitsValue));

        userCard.add(userContent, BorderLayout.CENTER);
        mainContainer.add(userCard);
        mainContainer.add(Box.createVerticalStrut(8));
        
        // ========== WORKSPACE SECTION ==========
        JPanel workspaceCard = createModernCard("📁 Workspace");
        
        // Workspace selection
        JPanel selectPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        selectPanel.setOpaque(false);
        selectPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        selectPanel.add(createLabel("Select Workspace:", false));
        workspaceComboBox = new JComboBox<String>();
        workspaceComboBox.setPreferredSize(new Dimension(250, 32));
        workspaceComboBox.setMaximumSize(new Dimension(250, 32));
        workspaceComboBox.setEnabled(true);
        workspaceComboBox.setVisible(true);
        workspaceComboBox.setToolTipText("Select a workspace");
        workspaceComboBox.setMaximumRowCount(10); // Make it scrollable with max 10 visible items
        workspaceComboBox.setEditable(false); // Make sure it's not editable
        workspaceComboBox.addItem("-- Select Workspace --"); // Add placeholder initially
        workspaceMap = new java.util.HashMap<>();
        // Auto-save when workspace is selected
        workspaceComboBox.addActionListener(e -> {
            String selected = (String) workspaceComboBox.getSelectedItem();
            if (selected != null && !selected.equals("-- Select Workspace --") && workspaceMap.containsKey(selected)) {
                autoSaveConfiguration();
                // Fetch counts when workspace is selected
                fetchAndUpdateCounts();
                // Note: Secrets will be fetched automatically after at least one scan (success or failure)
            }
        });
        selectPanel.add(workspaceComboBox);
        selectPanel.setVisible(true);
        
        // Create new workspace
        JPanel createPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        createPanel.setOpaque(false);
        createPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        createPanel.add(createLabel("Create New:", false));
        
        newWorkspaceNameField = createStyledTextField();
        newWorkspaceNameField.setEnabled(true);
        newWorkspaceNameField.setVisible(true);
        newWorkspaceNameField.setPreferredSize(new Dimension(200, 32));
        newWorkspaceNameField.setMaximumSize(new Dimension(200, 32));
        newWorkspaceNameField.setToolTipText("Enter name for new workspace");
        createPanel.add(newWorkspaceNameField);
        
        createWorkspaceButton = createPrimaryButton("Create");
        createWorkspaceButton.setEnabled(true);
        createWorkspaceButton.setVisible(true);
        createWorkspaceButton.setToolTipText("Create a new workspace");
        createWorkspaceButton.addActionListener(e -> createWorkspace());
        createWorkspaceButton.setPreferredSize(new Dimension(120, 32));
        createWorkspaceButton.setMaximumSize(new Dimension(120, 32));
        createWorkspaceButton.setFont(createWorkspaceButton.getFont().deriveFont(Font.BOLD, 12f));
        createPanel.add(createWorkspaceButton);
        createPanel.setVisible(true);
        
        // Workspace content container
        JPanel workspaceContent = new JPanel();
        workspaceContent.setLayout(new BoxLayout(workspaceContent, BoxLayout.Y_AXIS));
        workspaceContent.setOpaque(false);
        workspaceContent.setVisible(true);
        workspaceContent.setAlignmentX(Component.LEFT_ALIGNMENT);
        workspaceContent.add(selectPanel);
        workspaceContent.add(Box.createVerticalStrut(10));
        workspaceContent.add(createPanel);
        workspaceContent.add(Box.createVerticalGlue());
        
        workspaceCard.add(workspaceContent, BorderLayout.CENTER);
        workspaceCard.setVisible(true);
        mainContainer.add(workspaceCard);
        mainContainer.add(Box.createVerticalStrut(8));
        
        // ========== DOMAIN SCOPING SECTION ==========
        JPanel domainCard = createModernCard("🌐 Domain Scoping");
        JPanel domainContent = new JPanel(new BorderLayout(0, 8));
        domainContent.setOpaque(false);
        domainContent.add(createLabel("Scoped Domain(s) - one per line (includes subdomains):", false), BorderLayout.NORTH);
        scopedDomainField = new JTextArea();
        scopedDomainField.setBackground(theme.inputBackground);
        scopedDomainField.setForeground(theme.inputForeground);
        scopedDomainField.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(theme.inputBorder, 1),
            new EmptyBorder(6, 10, 6, 10)
        ));
        scopedDomainField.setFont(scopedDomainField.getFont().deriveFont(12f));
        scopedDomainField.setCaretColor(theme.caretColor);
        scopedDomainField.setLineWrap(true);
        scopedDomainField.setWrapStyleWord(true);
        scopedDomainField.setToolTipText("Enter domain(s), one per line, or separated by comma/space (e.g., example.com\\ntest.com). Leave empty to scan all JS files.");
        scopedDomainField.setPreferredSize(new Dimension(300, 44));
        scopedDomainField.setMaximumSize(new Dimension(300, 60));
        scopedDomainField.setRows(2);
        // Auto-save scoped domain when user finishes entering it
        scopedDomainField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                autoSaveConfiguration();
            }
        });
        // Add text area directly (no scroll bar) to keep height compact
        domainContent.add(scopedDomainField, BorderLayout.CENTER);
        domainCard.add(domainContent, BorderLayout.CENTER);
        mainContainer.add(domainCard);
        mainContainer.add(Box.createVerticalStrut(8));
        
        // ========== AUTOMATION SECTION ==========
        JPanel automateCard = createModernCard("⚡ Automation");
        JPanel automateContent = new JPanel();
        automateContent.setLayout(new BoxLayout(automateContent, BoxLayout.Y_AXIS));
        automateContent.setOpaque(false);
        
        automateScanCheckbox = new JCheckBox("Enable Automatic Scanning");
        automateScanCheckbox.setToolTipText("Automatically scan JS files as you browse (respects scoped domain)");
        automateScanCheckbox.setOpaque(false);
        automateScanCheckbox.setFont(automateScanCheckbox.getFont().deriveFont(13f));
        automateScanCheckbox.addActionListener(e -> {
            boolean enabled = automateScanCheckbox.isSelected();
            // Save configuration first
            autoSaveConfiguration();
            // Then enable/disable automatic scanning
            extension.setAutomateScan(enabled);
            if (enabled) {
                // Check if API key and workspace are configured
                String apiKey = apiKeyField.getText().trim();
                String selectedName = (String) workspaceComboBox.getSelectedItem();
                boolean hasWorkspace = selectedName != null && !selectedName.equals("-- Select Workspace --") && workspaceMap.containsKey(selectedName);
                
                if (apiKey.isEmpty() || !hasWorkspace) {
                    appendStatus("✗ Automatic scanning requires API key and workspace to be configured");
                    automateScanCheckbox.setSelected(false);
                    extension.setAutomateScan(false);
                } else {
                    appendStatus("✓ Automatic scanning enabled - starting scan of existing JS files...");
                    extension.clearProcessedUrls();
                    // Trigger initial scan if configuration is ready
                    extension.triggerInitialScanIfEnabled();
                }
            } else {
                appendStatus("✗ Automatic scanning disabled");
            }
        });
        automateContent.add(automateScanCheckbox);
        automateContent.add(Box.createVerticalStrut(10));
        
        // Manual scan button - centered
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        buttonPanel.setOpaque(false);
        JButton startScanButton = createPrimaryButton("🚀 Start Manual Scan");
        startScanButton.setToolTipText("Manually scan all JS files from Burp's HTTP history");
        startScanButton.setPreferredSize(new Dimension(200, 35));
        startScanButton.setMaximumSize(new Dimension(200, 35));
        startScanButton.setFont(startScanButton.getFont().deriveFont(Font.BOLD, 12f));
        startScanButton.addActionListener(e -> startManualScan());
        buttonPanel.add(startScanButton);
        automateContent.add(buttonPanel);
        
        automateCard.add(automateContent, BorderLayout.CENTER);
        mainContainer.add(automateCard);
        
        // Add all configuration cards to left panel
        leftPanel.add(mainContainer);
        
        // Scroll pane for left panel (configuration)
        JScrollPane leftScroll = new JScrollPane(leftPanel);
        leftScroll.setBorder(null);
        leftScroll.setOpaque(false);
        leftScroll.getViewport().setOpaque(false);
        leftScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        leftScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        
        // Right panel: Data display tabs
        dataTabs = new JTabbedPane();
        dataTabs.setBackground(theme.tabBackground);
        dataTabs.setForeground(theme.tabForeground);
        dataTabs.setBorder(BorderFactory.createEmptyBorder());
        
        // Tab 1: Secrets
        JPanel secretsPanel = new JPanel(new BorderLayout());
        secretsPanel.setOpaque(false);
        
        // Create table model with columns (order: Module Name, Matched Word, Severity, Created At)
        String[] columnNames = {"Module Name", "Matched Word", "Severity", "Created At"};
        secretsTableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        secretsTable = new JTable(secretsTableModel);
        secretsTable.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        secretsTable.setBackground(theme.tableBackground);
        secretsTable.setForeground(theme.tableForeground);
        secretsTable.setGridColor(theme.tableGrid);
        secretsTable.setSelectionBackground(theme.tableSelection);
        secretsTable.setSelectionForeground(theme.textPrimary);
        secretsTable.setRowHeight(20);
        secretsTable.setShowGrid(true);
        secretsTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN); // Last column fills remaining space
        
        // Enable single-cell selection (no full-row or full-column selection)
        secretsTable.setCellSelectionEnabled(true);
        secretsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        secretsTable.setRowSelectionAllowed(true);
        secretsTable.setColumnSelectionAllowed(true);
        
        // Custom copy handler to copy only selected cell(s)
        secretsTable.getActionMap().put("copy", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                int[] selectedRows = secretsTable.getSelectedRows();
                int[] selectedCols = secretsTable.getSelectedColumns();

                if (selectedRows.length == 0 || selectedCols.length == 0) {
                    return;
                }

                StringBuilder sb = new StringBuilder();
                for (int r = 0; r < selectedRows.length; r++) {
                    int row = selectedRows[r];
                    for (int c = 0; c < selectedCols.length; c++) {
                        int col = selectedCols[c];
                        Object value = secretsTable.getValueAt(row, col);
                        if (value != null) {
                            if (c > 0) {
                                sb.append("\t"); // Tab separator between columns
                            }
                            sb.append(value.toString());
                        }
                    }
                    if (r < selectedRows.length - 1) {
                        sb.append("\n"); // New line between rows
                    }
                }

                if (sb.length() > 0) {
                    java.awt.datatransfer.StringSelection selection = new java.awt.datatransfer.StringSelection(sb.toString());
                    java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
                }
            }
        });
        
        // Style table header
        secretsTable.getTableHeader().setBackground(theme.tableHeaderBackground);
        secretsTable.getTableHeader().setForeground(theme.tableHeaderForeground);
        secretsTable.getTableHeader().setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
        
        // Set column widths to fill available space
        secretsTable.getColumnModel().getColumn(0).setPreferredWidth(250); // Module Name
        secretsTable.getColumnModel().getColumn(1).setPreferredWidth(600); // Matched Word (largest)
        secretsTable.getColumnModel().getColumn(2).setPreferredWidth(100);  // Severity
        secretsTable.getColumnModel().getColumn(3).setPreferredWidth(200); // Created At
        
        JScrollPane secretsScroll = new JScrollPane(secretsTable);
        secretsScroll.setBorder(BorderFactory.createEmptyBorder());
        secretsScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        secretsScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        secretsScroll.getViewport().setBackground(theme.tableBackground);
        secretsPanel.add(secretsScroll, BorderLayout.CENTER);
        
        // Pagination and Copy All controls for Secrets
        JPanel secretsButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
        secretsButtonPanel.setOpaque(false);
        secretsButtonPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        
        // Pagination controls
        JButton secretsPrevButton = createSecondaryButton("◀ Prev");
        JLabel secretsPageLabel = createLabel("Page 1 of ?", false);
        JButton secretsNextButton = createSecondaryButton("Next ▶");
        
        // Store reference to page label for updates (as class field)
        this.secretsPageLabel = secretsPageLabel;
        
        // Prev button action
        secretsPrevButton.addActionListener(e -> {
            if (secretsCurrentPage > 1) {
                int newPage = secretsCurrentPage - 1;
                fetchAndDisplaySecrets(newPage);
            }
        });
        
        // Next button action
        secretsNextButton.addActionListener(e -> {
            int totalPages = getTotalPagesForSecrets();
            int currentPageCount = (secretsTableModel != null) ? secretsTableModel.getRowCount() : 0;
            
            // Allow going to next page if:
            // 1. Current page is less than calculated total pages, OR
            // 2. Current page has exactly 100 items (indicating there might be more)
            if (secretsCurrentPage < totalPages || currentPageCount == 100) {
                int newPage = secretsCurrentPage + 1;
                fetchAndDisplaySecrets(newPage);
            } else {
                appendStatus("ℹ No more pages available");
            }
        });
        
        // Copy All button for Secrets
        JButton secretsCopyButton = createSecondaryButton("📋 Copy All Secrets");
        secretsCopyButton.setToolTipText("Copy all secrets to clipboard");
        secretsCopyButton.addActionListener(e -> {
            StringBuilder sb = new StringBuilder();
            int rowCount = secretsTableModel.getRowCount();
            
            if (rowCount == 0) {
                appendStatus("✗ No secrets to copy");
                return;
            }
            
            // Copy all values from the first column (matched word)
            for (int i = 0; i < rowCount; i++) {
                Object value = secretsTableModel.getValueAt(i, 1); // Column 1 is "Matched Word"
                if (value != null && !value.toString().trim().isEmpty()) {
                    sb.append(value.toString().trim());
                    if (i < rowCount - 1) {
                        sb.append("\n");
                    }
                }
            }
            
            if (sb.length() > 0) {
                java.awt.datatransfer.StringSelection selection = new java.awt.datatransfer.StringSelection(sb.toString());
                java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
                appendStatus("✓ Copied " + rowCount + " secret(s) to clipboard");
            } else {
                appendStatus("✗ No secrets to copy");
            }
        });
        
        secretsButtonPanel.add(secretsPrevButton);
        secretsButtonPanel.add(secretsPageLabel);
        secretsButtonPanel.add(secretsNextButton);
        secretsButtonPanel.add(Box.createHorizontalStrut(20)); // Spacer
        secretsButtonPanel.add(secretsCopyButton);
        
        secretsPanel.add(secretsButtonPanel, BorderLayout.SOUTH);
        
        dataTabs.addTab("🔐 Secrets", secretsPanel);
        
        // Tab 2: JS-Intelligence Data
        jsIntelligencePanel = new JPanel(new BorderLayout());
        jsIntelligencePanel.setOpaque(false);
        
        // Create sub-tabs for JS Intelligence data
        jsIntelligenceSubTabs = new JTabbedPane();
        jsIntelligenceSubTabs.setBackground(theme.tabBackground);
        jsIntelligenceSubTabs.setForeground(theme.tabForeground);
        jsIntelligenceSubTabs.setBorder(BorderFactory.createEmptyBorder());
        
        // Sub-tab 1: JS URLs
        JPanel jsUrlsPanel = new JPanel(new BorderLayout());
        jsUrlsPanel.setOpaque(false);
        
        // Create JS URLs table with URL and timestamp columns
        String[] jsUrlsColumnNames = {"JS URL", "Scanned At"};
        jsUrlsTableModel = new DefaultTableModel(jsUrlsColumnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        jsUrlsTable = new JTable(jsUrlsTableModel);
        jsUrlsTable.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        jsUrlsTable.setBackground(theme.tableBackground);
        jsUrlsTable.setForeground(theme.tableForeground);
        jsUrlsTable.setGridColor(theme.tableGrid);
        jsUrlsTable.setSelectionBackground(theme.tableSelection);
        jsUrlsTable.setSelectionForeground(theme.textPrimary);
        jsUrlsTable.setRowHeight(20);
        jsUrlsTable.setShowGrid(true);
        jsUrlsTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        
        // Enable single-cell selection
        jsUrlsTable.setCellSelectionEnabled(true);
        jsUrlsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        jsUrlsTable.setRowSelectionAllowed(true);
        jsUrlsTable.setColumnSelectionAllowed(true);
        
        // Custom copy handler to copy only selected cell(s)
        jsUrlsTable.getActionMap().put("copy", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                int[] selectedRows = jsUrlsTable.getSelectedRows();
                int[] selectedCols = jsUrlsTable.getSelectedColumns();
                
                if (selectedRows.length == 0 || selectedCols.length == 0) {
                    return;
                }
                
                StringBuilder sb = new StringBuilder();
                for (int r = 0; r < selectedRows.length; r++) {
                    int row = selectedRows[r];
                    for (int c = 0; c < selectedCols.length; c++) {
                        int col = selectedCols[c];
                        Object value = jsUrlsTable.getValueAt(row, col);
                        if (value != null) {
                            if (c > 0) {
                                sb.append("\t"); // Tab separator between columns
                            }
                            sb.append(value.toString());
                        }
                    }
                    if (r < selectedRows.length - 1) {
                        sb.append("\n"); // New line between rows
                    }
                }
                
                if (sb.length() > 0) {
                    java.awt.datatransfer.StringSelection selection = new java.awt.datatransfer.StringSelection(sb.toString());
                    java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
                }
            }
        });
        
        // Style table header
        jsUrlsTable.getTableHeader().setBackground(theme.tableHeaderBackground);
        jsUrlsTable.getTableHeader().setForeground(theme.tableHeaderForeground);
        jsUrlsTable.getTableHeader().setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
        
        // Set column widths to fill available space
        jsUrlsTable.getColumnModel().getColumn(0).setPreferredWidth(800); // JS URL (larger)
        jsUrlsTable.getColumnModel().getColumn(1).setPreferredWidth(200); // Scanned At
        
        JScrollPane jsUrlsScroll = new JScrollPane(jsUrlsTable);
        jsUrlsScroll.setBorder(BorderFactory.createEmptyBorder());
        jsUrlsScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        jsUrlsScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        jsUrlsScroll.getViewport().setBackground(theme.tableBackground);
        
        // Pagination and Copy All controls for JS URLs
        JPanel jsUrlsButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
        jsUrlsButtonPanel.setOpaque(false);
        jsUrlsButtonPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        
        // Pagination controls
        JButton jsUrlsPrevButton = createSecondaryButton("◀ Prev");
        JLabel jsUrlsPageLabel = createLabel("Page 1 of ?", false);
        JButton jsUrlsNextButton = createSecondaryButton("Next ▶");
        
        // Store reference to page label for updates
        final JLabel finalJsUrlsPageLabel = jsUrlsPageLabel;
        
        // Prev button action
        jsUrlsPrevButton.addActionListener(e -> {
            if (jsUrlsCurrentPage > 1) {
                int newPage = jsUrlsCurrentPage - 1;
                fetchAndDisplayJsUrlsWithLabel(newPage, finalJsUrlsPageLabel);
            }
        });
        
        // Next button action
        jsUrlsNextButton.addActionListener(e -> {
            int totalPages = getTotalPagesForField("jsurls");
            if (jsUrlsCurrentPage < totalPages) {
                int newPage = jsUrlsCurrentPage + 1;
                fetchAndDisplayJsUrlsWithLabel(newPage, finalJsUrlsPageLabel);
            }
        });
        
        // Copy All button for JS URLs
        JButton jsUrlsCopyButton = createSecondaryButton("📋 Copy All JS URLs");
        jsUrlsCopyButton.setToolTipText("Copy all JS URLs to clipboard");
        jsUrlsCopyButton.addActionListener(e -> {
            fetchAllIntelligenceDataAndCopy("jsurls", "JS URL");
        });
        
        jsUrlsButtonPanel.add(jsUrlsPrevButton);
        jsUrlsButtonPanel.add(jsUrlsPageLabel);
        jsUrlsButtonPanel.add(jsUrlsNextButton);
        jsUrlsButtonPanel.add(Box.createHorizontalStrut(20)); // Spacer
        jsUrlsButtonPanel.add(jsUrlsCopyButton);
        
        jsUrlsPanel.add(jsUrlsScroll, BorderLayout.CENTER);
        jsUrlsPanel.add(jsUrlsButtonPanel, BorderLayout.SOUTH);
        
        jsIntelligenceSubTabs.addTab("🔗 JS URLs", jsUrlsPanel);
        
        // Sub-tab 2: API Paths
        JPanel apiPathsPanel = new JPanel(new BorderLayout());
        apiPathsPanel.setOpaque(false);
        
        // Create API Paths table with path and timestamp columns
        String[] apiPathsColumnNames = {"API Path", "Scanned At"};
        apiPathsTableModel = new DefaultTableModel(apiPathsColumnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        apiPathsTable = new JTable(apiPathsTableModel);
        apiPathsTable.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        apiPathsTable.setBackground(theme.tableBackground);
        apiPathsTable.setForeground(theme.tableForeground);
        apiPathsTable.setGridColor(theme.tableGrid);
        apiPathsTable.setSelectionBackground(theme.tableSelection);
        apiPathsTable.setSelectionForeground(theme.textPrimary);
        apiPathsTable.setRowHeight(20);
        apiPathsTable.setShowGrid(true);
        apiPathsTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        
        // Enable single-cell selection
        apiPathsTable.setCellSelectionEnabled(true);
        apiPathsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        apiPathsTable.setRowSelectionAllowed(true);
        apiPathsTable.setColumnSelectionAllowed(true);
        
        // Custom copy handler to copy only selected cell(s)
        apiPathsTable.getActionMap().put("copy", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                int[] selectedRows = apiPathsTable.getSelectedRows();
                int[] selectedCols = apiPathsTable.getSelectedColumns();
                
                if (selectedRows.length == 0 || selectedCols.length == 0) {
                    return;
                }
                
                StringBuilder sb = new StringBuilder();
                for (int r = 0; r < selectedRows.length; r++) {
                    int row = selectedRows[r];
                    for (int c = 0; c < selectedCols.length; c++) {
                        int col = selectedCols[c];
                        Object value = apiPathsTable.getValueAt(row, col);
                        if (value != null) {
                            if (c > 0) {
                                sb.append("\t"); // Tab separator between columns
                            }
                            sb.append(value.toString());
                        }
                    }
                    if (r < selectedRows.length - 1) {
                        sb.append("\n"); // New line between rows
                    }
                }
                
                if (sb.length() > 0) {
                    java.awt.datatransfer.StringSelection selection = new java.awt.datatransfer.StringSelection(sb.toString());
                    java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
                }
            }
        });
        
        // Style table header
        apiPathsTable.getTableHeader().setBackground(theme.tableHeaderBackground);
        apiPathsTable.getTableHeader().setForeground(theme.tableHeaderForeground);
        apiPathsTable.getTableHeader().setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
        
        // Set column widths to fill available space
        apiPathsTable.getColumnModel().getColumn(0).setPreferredWidth(800); // API Path (larger)
        apiPathsTable.getColumnModel().getColumn(1).setPreferredWidth(200); // Scanned At
        
        JScrollPane apiPathsScroll = new JScrollPane(apiPathsTable);
        apiPathsScroll.setBorder(BorderFactory.createEmptyBorder());
        apiPathsScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        apiPathsScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        apiPathsScroll.getViewport().setBackground(theme.tableBackground);
        
        // Pagination and Copy All controls for API Paths
        JPanel apiPathsButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
        apiPathsButtonPanel.setOpaque(false);
        apiPathsButtonPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        
        // Pagination controls
        JButton apiPathsPrevButton = createSecondaryButton("◀ Prev");
        JLabel apiPathsPageLabel = createLabel("Page 1 of ?", false);
        JButton apiPathsNextButton = createSecondaryButton("Next ▶");
        
        // Store reference to page label for updates
        final JLabel finalApiPathsPageLabel = apiPathsPageLabel;
        
        // Prev button action
        apiPathsPrevButton.addActionListener(e -> {
            if (apiPathsCurrentPage > 1) {
                int newPage = apiPathsCurrentPage - 1;
                fetchAndDisplayApiPathsWithLabel(newPage, finalApiPathsPageLabel);
            }
        });
        
        // Next button action
        apiPathsNextButton.addActionListener(e -> {
            int totalPages = getTotalPagesForField("apipaths");
            if (apiPathsCurrentPage < totalPages) {
                int newPage = apiPathsCurrentPage + 1;
                fetchAndDisplayApiPathsWithLabel(newPage, finalApiPathsPageLabel);
            }
        });
        
        // Copy All button for API Paths
        JButton apiPathsCopyButton = createSecondaryButton("📋 Copy All API Paths");
        apiPathsCopyButton.setToolTipText("Copy all API paths to clipboard");
        apiPathsCopyButton.addActionListener(e -> {
            fetchAllIntelligenceDataAndCopy("apipaths", "API Path");
        });
        
        apiPathsButtonPanel.add(apiPathsPrevButton);
        apiPathsButtonPanel.add(apiPathsPageLabel);
        apiPathsButtonPanel.add(apiPathsNextButton);
        apiPathsButtonPanel.add(Box.createHorizontalStrut(20)); // Spacer
        apiPathsButtonPanel.add(apiPathsCopyButton);
        
        apiPathsPanel.add(apiPathsScroll, BorderLayout.CENTER);
        apiPathsPanel.add(apiPathsButtonPanel, BorderLayout.SOUTH);
        
        jsIntelligenceSubTabs.addTab("🛣️ API Paths", apiPathsPanel);
        
        // Sub-tab 3: URLs
        JPanel urlsPanel = createIntelligenceTablePanel("URL", "urls");
        jsIntelligenceSubTabs.addTab("🔗 URLs", urlsPanel);
        
        // Sub-tab 4: Domains
        JPanel domainsPanel = createIntelligenceTablePanel("Domain", "domains");
        jsIntelligenceSubTabs.addTab("🌐 Domains", domainsPanel);
        
        // Sub-tab 5: IP Addresses
        JPanel ipAddressesPanel = createIntelligenceTablePanel("IP Address", "ipaddresses");
        jsIntelligenceSubTabs.addTab("🌐 IP Addresses", ipAddressesPanel);
        
        // Sub-tab 6: Emails
        JPanel emailsPanel = createIntelligenceTablePanel("Email", "emails");
        jsIntelligenceSubTabs.addTab("📧 Emails", emailsPanel);
        
        // Sub-tab 7: S3 Buckets
        JPanel s3BucketsPanel = createIntelligenceTablePanel("S3 Bucket", "s3domains");
        jsIntelligenceSubTabs.addTab("🪣 S3 Buckets", s3BucketsPanel);
        
        // Sub-tab 8: Invalid Node Modules
        JPanel invalidNodeModulesPanel = createIntelligenceTablePanel("Invalid Node Module", "invalidnodemodules");
        jsIntelligenceSubTabs.addTab("📦 Invalid Node Modules", invalidNodeModulesPanel);
        
        // Create header panel for JS Intelligence with "Go To jsmon.sh" button on the right
        JPanel jsIntelligenceHeader = new JPanel(new BorderLayout());
        jsIntelligenceHeader.setOpaque(false);
        jsIntelligenceHeader.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        // Go To jsmon.sh button - styled as link with arrow
        jsmonLinkButton = new JButton("Go To jsmon.sh ↗") {
            @Override
            protected void paintComponent(java.awt.Graphics g) {
                // Transparent background, just text
                super.paintComponent(g);
            }
        };
        jsmonLinkButton.setToolTipText("Visit JSMon website");
        jsmonLinkButton.setOpaque(false);
        jsmonLinkButton.setContentAreaFilled(false);
        jsmonLinkButton.setBorderPainted(false);
        jsmonLinkButton.setFocusPainted(false);
        jsmonLinkButton.setForeground(theme.textSecondary);
        jsmonLinkButton.setFont(jsmonLinkButton.getFont().deriveFont(Font.PLAIN, 11f));
        jsmonLinkButton.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        jsmonLinkButton.addActionListener(e -> {
            try {
                java.awt.Desktop.getDesktop().browse(new java.net.URI("https://jsmon.sh"));
                appendStatus("✓ Opening JSMon website in browser...");
            } catch (Exception ex) {
                appendStatus("✗ Error opening browser: " + ex.getMessage());
                logging.logToError("Error opening browser: " + ex.getMessage());
            }
        });
        // Add hover effect
        jsmonLinkButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                jsmonLinkButton.setForeground(theme.textPrimary);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                jsmonLinkButton.setForeground(theme.textSecondary);
            }
        });
        
        jsIntelligenceHeader.add(jsmonLinkButton, BorderLayout.EAST);
        
        // Add header and sub-tabs to main JS Intelligence panel
        jsIntelligencePanel.add(jsIntelligenceHeader, BorderLayout.NORTH);
        jsIntelligencePanel.add(jsIntelligenceSubTabs, BorderLayout.CENTER);
        
        // Add listener to fetch data when sub-tabs are selected - always refetch to get latest data
        jsIntelligenceSubTabs.addChangeListener(e -> {
            int selectedIndex = jsIntelligenceSubTabs.getSelectedIndex();
            if (selectedIndex == 0) { // JS URLs tab
                fetchAndDisplayJsUrls(jsUrlsCurrentPage);
            } else if (selectedIndex == 1) { // API Paths tab
                fetchAndDisplayApiPaths(apiPathsCurrentPage);
            } else if (selectedIndex == 2) { // URLs tab
                fetchAndDisplayUrls(urlsCurrentPage);
            } else if (selectedIndex == 3) { // Domains tab
                fetchAndDisplayDomains(domainsCurrentPage);
            } else if (selectedIndex == 4) { // IP Addresses tab
                fetchIntelligenceData("ipaddresses", ipAddressesCurrentPage, ipAddressesTable, ipAddressesTableModel);
            } else if (selectedIndex == 5) { // Emails tab
                fetchIntelligenceData("emails", emailsCurrentPage, emailsTable, emailsTableModel);
            } else if (selectedIndex == 6) { // S3 Buckets tab
                fetchIntelligenceData("s3domains", s3BucketsCurrentPage, s3BucketsTable, s3BucketsTableModel);
            } else if (selectedIndex == 7) { // Invalid Node Modules tab
                fetchIntelligenceData("invalidnodemodules", invalidNodeModulesCurrentPage, invalidNodeModulesTable, invalidNodeModulesTableModel);
            }
        });
        
        dataTabs.addTab("📊 JS-Intelligence Data", jsIntelligencePanel);
        
        // Add listener to fetch data when JS Intelligence tab is selected
        // Add listener to fetch data when tabs are selected - always refetch to get latest data
        dataTabs.addChangeListener(e -> {
            int selectedIndex = dataTabs.getSelectedIndex();
            if (selectedIndex == 1) { // JS Intelligence tab is index 1
                // Fetch data for the currently selected sub-tab
                if (jsIntelligenceSubTabs != null) {
                    int subTabIndex = jsIntelligenceSubTabs.getSelectedIndex();
                    if (subTabIndex == 0) { // JS URLs
                        fetchAndDisplayJsUrls(jsUrlsCurrentPage);
                    } else if (subTabIndex == 1) { // API Paths
                        fetchAndDisplayApiPaths(apiPathsCurrentPage);
                    } else if (subTabIndex == 2) { // URLs
                        fetchAndDisplayUrls(urlsCurrentPage);
                    } else if (subTabIndex == 3) { // Domains
                        fetchAndDisplayDomains(domainsCurrentPage);
                    } else if (subTabIndex == 4) { // IP Addresses
                        fetchIntelligenceData("ipaddresses", ipAddressesCurrentPage, ipAddressesTable, ipAddressesTableModel);
                    } else if (subTabIndex == 5) { // Emails
                        fetchIntelligenceData("emails", emailsCurrentPage, emailsTable, emailsTableModel);
                    } else if (subTabIndex == 6) { // S3 Buckets
                        fetchIntelligenceData("s3domains", s3BucketsCurrentPage, s3BucketsTable, s3BucketsTableModel);
                    } else if (subTabIndex == 7) { // Invalid Node Modules
                        fetchIntelligenceData("invalidnodemodules", invalidNodeModulesCurrentPage, invalidNodeModulesTable, invalidNodeModulesTableModel);
                    }
                }
            } else if (selectedIndex == 0) { // Secrets tab
                // Always refetch secrets when Secrets tab is selected
                fetchAndDisplaySecrets();
            }
        });
        
        // Add left and right panels to split pane
        mainSplitPane.setLeftComponent(leftScroll);
        mainSplitPane.setRightComponent(dataTabs);
        
        // ========== STATUS AREA (Bottom - Full Width) ==========
        JPanel statusCard = createModernCard("📊 Status Log");
        statusArea = new JTextArea();
        statusArea.setEditable(false);
        statusArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        statusArea.setBackground(theme.statusBackground);
        statusArea.setForeground(theme.statusForeground);
        statusArea.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        statusArea.setLineWrap(true);
        statusArea.setWrapStyleWord(true);
        statusArea.setRows(8);
        
        JScrollPane statusScrollPane = new JScrollPane(statusArea);
        statusScrollPane.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        statusScrollPane.setPreferredSize(new Dimension(Integer.MAX_VALUE, 200));
        statusScrollPane.setMinimumSize(new Dimension(0, 150));
        statusScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        statusScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        
        statusCard.add(statusScrollPane, BorderLayout.CENTER);
        
        // Main container: Top (split pane) and Bottom (status log)
        JPanel topBottomContainer = new JPanel(new BorderLayout(0, 10));
        topBottomContainer.setOpaque(false);
        topBottomContainer.add(mainSplitPane, BorderLayout.CENTER);
        topBottomContainer.add(statusCard, BorderLayout.SOUTH);
        
        // Add top panel with jsmon.sh link and main content
        JPanel mainContentPanel = new JPanel(new BorderLayout(0, 0));
        mainContentPanel.setOpaque(false);
        mainContentPanel.add(topPanel, BorderLayout.NORTH);
        mainContentPanel.add(topBottomContainer, BorderLayout.CENTER);
        
        add(mainContentPanel, BorderLayout.CENTER);
        
        // Load saved configuration and populate UI
        loadSavedConfiguration();
        
        // Initial status message
        appendStatus("JSMon Extension loaded. Please configure your API key and workspace.");
    }
    
    /**
     * Load saved configuration from persistence and populate UI fields
     * This loads project-specific data (each Burp Suite project has its own configuration)
     */
    private void loadSavedConfiguration() {
        try {
            // Load API key
            String savedApiKey = extension.getApiKey();
            if (savedApiKey != null && !savedApiKey.isEmpty()) {
                apiKeyField.setText(savedApiKey);
                appendStatus("✓ Loaded saved API key from project data");
            }
            
            // Load scoped domain
            String savedScopedDomain = extension.getScopedDomain();
            if (savedScopedDomain != null && !savedScopedDomain.isEmpty()) {
                scopedDomainField.setText(savedScopedDomain);
            }
            
            // Load automate scan setting
            boolean savedAutomateScan = extension.isAutomateScan();
            automateScanCheckbox.setSelected(savedAutomateScan);
            
            // Load workspace and fetch data if API key and workspace are available
            String savedWorkspaceId = extension.getWorkspaceId();
            if (savedApiKey != null && !savedApiKey.isEmpty()) {
                appendStatus("✓ Loading saved workspace configuration...");
                // Fetch workspaces and user profile in background
                new Thread(() -> {
                    try {
                        // Fetch workspaces first
                        List<Workspace> workspaces = extension.fetchWorkspaces();
                        SwingUtilities.invokeLater(() -> {
                            if (workspaces != null && !workspaces.isEmpty()) {
                                // Populate workspace combo box
                                workspaceComboBox.removeAllItems();
                                workspaceMap.clear();
                                
                                for (Workspace workspace : workspaces) {
                                    String displayName = workspace.getName() + " (" + workspace.getId() + ")";
                                    workspaceComboBox.addItem(displayName);
                                    workspaceMap.put(displayName, workspace);
                                }
                                
                                // Select the saved workspace if available
                                if (savedWorkspaceId != null && !savedWorkspaceId.isEmpty()) {
                                    boolean found = false;
                                    for (java.util.Map.Entry<String, Workspace> entry : workspaceMap.entrySet()) {
                                        if (entry.getValue().getId().equals(savedWorkspaceId)) {
                                            workspaceComboBox.setSelectedItem(entry.getKey());
                                            appendStatus("✓ Loaded saved workspace: " + entry.getValue().getName());
                                            // Fetch counts when workspace is selected
                                            fetchAndUpdateCounts();
                                            found = true;
                                            break;
                                        }
                                    }
                                    if (!found) {
                                        appendStatus("⚠ Saved workspace not found - please select a workspace");
                                    }
                                }
                            }
                            
                            // Fetch user profile
                            fetchAndDisplayUserProfile(false);
                        });
                    } catch (Exception e) {
                        logging.logToError("Error loading saved configuration: " + e.getMessage());
                        SwingUtilities.invokeLater(() -> {
                            appendStatus("✗ Error loading saved configuration: " + e.getMessage());
                            fetchAndDisplayUserProfile(false);
                        });
                    }
                }).start();
            } else {
                appendStatus("ℹ No saved configuration found - please configure API key and workspace");
            }
        } catch (Exception e) {
            logging.logToError("Error loading saved configuration: " + e.getMessage());
            appendStatus("✗ Error loading saved configuration: " + e.getMessage());
        }
    }
    
    private JPanel createModernCard(String title) {
        JPanel card = new JPanel(new BorderLayout(8, 8));
        card.setOpaque(true);
        card.setBackground(theme.cardBackground);
        card.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(theme.border, 1, true),
            new EmptyBorder(10, 12, 10, 12) // Reduced padding
        ));
        
        // Title label
        if (title != null && !title.isEmpty()) {
            JLabel titleLabel = new JLabel(title);
            titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 14f));
            titleLabel.setForeground(theme.textPrimary);
            titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));
            card.add(titleLabel, BorderLayout.NORTH);
        }
        
        return card;
    }
    
    private JLabel createLabel(String text, boolean bold) {
        JLabel label = new JLabel(text);
        label.setForeground(theme.textSecondary);
        if (bold) {
            label.setFont(label.getFont().deriveFont(Font.BOLD, 12f));
        } else {
            label.setFont(label.getFont().deriveFont(12f));
        }
        return label;
    }

    private JLabel createValueLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(theme.textValue);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 12f));
        return label;
    }

    private JPanel createKeyValueRow(String key, JLabel valueLabel) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        JLabel keyLabel = createLabel(key, false);
        keyLabel.setPreferredSize(new Dimension(120, 18));
        row.add(keyLabel, BorderLayout.WEST);
        row.add(valueLabel, BorderLayout.CENTER);
        return row;
    }
    
    /**
     * Create an intelligence table panel with pagination controls
     */
    private JPanel createIntelligenceTablePanel(String columnName, String fieldName) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        
        // Create table model
        String[] columnNames = {columnName, "Scanned At"};
        DefaultTableModel tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        // Assign to appropriate instance variable
        JTable table;
        if ("urls".equals(fieldName)) {
            urlsTableModel = tableModel;
            table = new JTable(tableModel);
            urlsTable = table;
        } else if ("domains".equals(fieldName)) {
            domainsTableModel = tableModel;
            table = new JTable(tableModel);
            domainsTable = table;
        } else if ("ipaddresses".equals(fieldName)) {
            ipAddressesTableModel = tableModel;
            table = new JTable(tableModel);
            ipAddressesTable = table;
        } else if ("emails".equals(fieldName)) {
            emailsTableModel = tableModel;
            table = new JTable(tableModel);
            emailsTable = table;
        } else if ("s3domains".equals(fieldName)) {
            s3BucketsTableModel = tableModel;
            table = new JTable(tableModel);
            s3BucketsTable = table;
        } else if ("invalidnodemodules".equals(fieldName)) {
            invalidNodeModulesTableModel = tableModel;
            table = new JTable(tableModel);
            invalidNodeModulesTable = table;
        } else {
            table = new JTable(tableModel);
        }
        table.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        table.setBackground(theme.tableBackground);
        table.setForeground(theme.tableForeground);
        table.setGridColor(theme.tableGrid);
        table.setSelectionBackground(theme.tableSelection);
        table.setSelectionForeground(theme.textPrimary);
        table.setRowHeight(20);
        table.setShowGrid(true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        
        // Enable single-cell selection
        table.setCellSelectionEnabled(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowSelectionAllowed(true);
        table.setColumnSelectionAllowed(true);
        
        // Custom copy handler
        table.getActionMap().put("copy", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                int[] selectedRows = table.getSelectedRows();
                int[] selectedCols = table.getSelectedColumns();
                
                if (selectedRows.length == 0 || selectedCols.length == 0) {
                    return;
                }
                
                StringBuilder sb = new StringBuilder();
                for (int r = 0; r < selectedRows.length; r++) {
                    int row = selectedRows[r];
                    for (int c = 0; c < selectedCols.length; c++) {
                        int col = selectedCols[c];
                        Object value = table.getValueAt(row, col);
                        if (value != null) {
                            if (c > 0) {
                                sb.append("\t");
                            }
                            sb.append(value.toString());
                        }
                    }
                    if (r < selectedRows.length - 1) {
                        sb.append("\n");
                    }
                }
                
                if (sb.length() > 0) {
                    java.awt.datatransfer.StringSelection selection = new java.awt.datatransfer.StringSelection(sb.toString());
                    java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
                }
            }
        });
        
        // Style table header
        table.getTableHeader().setBackground(theme.tableHeaderBackground);
        table.getTableHeader().setForeground(theme.tableHeaderForeground);
        table.getTableHeader().setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
        
        // Set column widths
        table.getColumnModel().getColumn(0).setPreferredWidth(800);
        table.getColumnModel().getColumn(1).setPreferredWidth(200);
        
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.getViewport().setBackground(theme.tableBackground);
        
        // Pagination controls
        JPanel paginationPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
        paginationPanel.setOpaque(false);
        paginationPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        
        JButton prevButton = createSecondaryButton("◀ Prev");
        JLabel pageLabel = createLabel("Page 1 of ?", false);
        JButton nextButton = createSecondaryButton("Next ▶");
        
        // Copy All button - copies all data from the first column
        JButton copyAllButton = createSecondaryButton("📋 Copy All");
        copyAllButton.setToolTipText("Copy all " + columnName.toLowerCase() + "s to clipboard");
        
        // Store references for pagination handlers
        final String field = fieldName;
        final JTable finalTable = table;
        final DefaultTableModel finalModel = tableModel;
        
        final JLabel finalPageLabel = pageLabel;
        
        // Copy All button action - copies all data from all pages
        copyAllButton.addActionListener(e -> {
            fetchAllIntelligenceDataAndCopy(field, columnName);
        });
        prevButton.addActionListener(e -> {
            int currentPage = getCurrentPageForField(field);
            if (currentPage > 1) {
                int newPage = currentPage - 1;
                fetchIntelligenceDataWithLabel(field, newPage, finalTable, finalModel, finalPageLabel);
            }
        });
        
        nextButton.addActionListener(e -> {
            int currentPage = getCurrentPageForField(field);
            int totalPages = getTotalPagesForField(field);
            if (currentPage < totalPages) {
                int newPage = currentPage + 1;
                fetchIntelligenceDataWithLabel(field, newPage, finalTable, finalModel, finalPageLabel);
            }
        });
        
        paginationPanel.add(prevButton);
        paginationPanel.add(pageLabel);
        paginationPanel.add(nextButton);
        paginationPanel.add(Box.createHorizontalStrut(20)); // Spacer
        paginationPanel.add(copyAllButton);
        
        panel.add(scroll, BorderLayout.CENTER);
        panel.add(paginationPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    /**
     * Map field name to API count field name
     */
    private String mapFieldToCountFieldName(String fieldName) {
        switch (fieldName) {
            case "jsurls": return "totalJsUrls";
            case "apipaths": return "totalApiPaths";
            case "urls": return "totalUrls";
            case "domains": return "totalDomains";
            case "ipaddresses": return "totalIpAddresses";
            case "emails": return "totalEmails";
            case "s3domains": return "totalS3Domains";
            case "invalidnodemodules": 
                // Try multiple possible field names
                return "totalInvalidNodeModules"; // Will fallback to other names in getOrDefault
            default: return fieldName;
        }
    }
    
    private int getCurrentPageForField(String fieldName) {
        switch (fieldName) {
            case "jsurls": return jsUrlsCurrentPage;
            case "apipaths": return apiPathsCurrentPage;
            case "urls": return urlsCurrentPage;
            case "domains": return domainsCurrentPage;
            case "ipaddresses": return ipAddressesCurrentPage;
            case "emails": return emailsCurrentPage;
            case "s3domains": return s3BucketsCurrentPage;
            case "invalidnodemodules": return invalidNodeModulesCurrentPage;
            default: return 1;
        }
    }
    
    private void setCurrentPageForField(String fieldName, int page) {
        switch (fieldName) {
            case "jsurls": jsUrlsCurrentPage = page; break;
            case "apipaths": apiPathsCurrentPage = page; break;
            case "urls": urlsCurrentPage = page; break;
            case "domains": domainsCurrentPage = page; break;
            case "ipaddresses": ipAddressesCurrentPage = page; break;
            case "emails": emailsCurrentPage = page; break;
            case "s3domains": s3BucketsCurrentPage = page; break;
            case "invalidnodemodules": invalidNodeModulesCurrentPage = page; break;
        }
    }
    
    /**
     * Calculate total pages for a field based on count
     */
    private int getTotalPagesForField(String fieldName) {
        String countFieldName = mapFieldToCountFieldName(fieldName);
        int totalCount = fieldCounts.getOrDefault(countFieldName, 
            fieldCounts.getOrDefault(fieldName, 0));
        
        // Special handling for invalid node modules - try multiple field names
        if ("invalidnodemodules".equals(fieldName) && totalCount == 0) {
            totalCount = fieldCounts.getOrDefault("totalNpmConfusion", 
                fieldCounts.getOrDefault("totalNpmConfusions",
                fieldCounts.getOrDefault("npmconfusion", 0)));
        }
        
        // Assuming 100 items per page
        int totalPages = (int) Math.ceil(totalCount / 100.0);
        return totalPages > 0 ? totalPages : 1; // At least 1 page
    }
    
    /**
     * Update page label with "Page X of Y" format
     */
    private void updatePageLabel(JLabel pageLabel, int currentPage, String fieldName) {
        if (pageLabel != null) {
            int totalPages = getTotalPagesForField(fieldName);
            pageLabel.setText("Page " + currentPage + " of " + totalPages);
        }
    }
    
    
    private void fetchIntelligenceDataWithLabel(String fieldName, int page, JTable table, DefaultTableModel model, JLabel pageLabel) {
        fetchIntelligenceData(fieldName, page, table, model, pageLabel);
    }
    
    private List<JsUrlEntry> fetchIntelligenceDataForField(String fieldName, String workspaceId, String apiKey, int page) {
        switch (fieldName) {
            case "jsurls":
                return extension.fetchJsUrls(workspaceId, apiKey, page);
            case "apipaths":
                return extension.fetchApiPaths(workspaceId, apiKey, page);
            case "urls":
                return extension.fetchUrls(workspaceId, apiKey, page);
            case "domains":
                return extension.fetchDomains(workspaceId, apiKey, page);
            case "ipaddresses":
                return extension.fetchIpAddresses(workspaceId, apiKey, page);
            case "emails":
                return extension.fetchEmails(workspaceId, apiKey, page);
            case "s3domains":
                return extension.fetchS3Buckets(workspaceId, apiKey, page);
            case "invalidnodemodules":
                return extension.fetchInvalidNodeModules(workspaceId, apiKey, page);
            default:
                return new ArrayList<>();
        }
    }
    
    /**
     * Fetch all pages of intelligence data for a field and return all URLs
     */
    private void fetchAllIntelligenceDataAndCopy(String fieldName, String columnName) {
        String apiKey = apiKeyField.getText().trim();
        String selectedName = (String) workspaceComboBox.getSelectedItem();
        
        if (apiKey.isEmpty() || selectedName == null || selectedName.equals("-- Select Workspace --") || !workspaceMap.containsKey(selectedName)) {
            appendStatus("✗ Cannot copy: API key or workspace not configured");
            return;
        }
        
        Workspace workspace = workspaceMap.get(selectedName);
        String workspaceId = workspace.getId();
        
        // Disable the button and show status
        appendStatus("⏳ Fetching all " + columnName.toLowerCase() + "s from all pages...");
        
        // Fetch in background thread
        new Thread(() -> {
            try {
                // Get total pages
                int totalPages = getTotalPagesForField(fieldName);
                if (totalPages == 0) {
                    SwingUtilities.invokeLater(() -> {
                        appendStatus("✗ No " + columnName.toLowerCase() + "s found");
                    });
                    return;
                }
                
                // Fetch all pages
                java.util.Set<String> allValues = new java.util.LinkedHashSet<>(); // Use LinkedHashSet to avoid duplicates and maintain order
                
                for (int page = 1; page <= totalPages; page++) {
                    final int currentPage = page; // Make final for lambda
                    SwingUtilities.invokeLater(() -> {
                        appendStatus("⏳ Fetching page " + currentPage + " of " + totalPages + "...");
                    });
                    
                    List<JsUrlEntry> entries = fetchIntelligenceDataForField(fieldName, workspaceId, apiKey, currentPage);
                    
                    if (entries != null) {
                        for (JsUrlEntry entry : entries) {
                            String url = entry.getUrl();
                            if (url != null && !url.trim().isEmpty()) {
                                allValues.add(url.trim());
                            }
                        }
                    }
                }
                
                // Copy to clipboard
                if (!allValues.isEmpty()) {
                    StringBuilder sb = new StringBuilder();
                    int index = 0;
                    for (String value : allValues) {
                        sb.append(value);
                        if (index < allValues.size() - 1) {
                            sb.append("\n");
                        }
                        index++;
                    }
                    
                    final String finalText = sb.toString();
                    final int finalCount = allValues.size();
                    
                    SwingUtilities.invokeLater(() -> {
                        java.awt.datatransfer.StringSelection selection = new java.awt.datatransfer.StringSelection(finalText);
                        java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
                        appendStatus("✓ Copied " + finalCount + " " + columnName.toLowerCase() + "(s) from all pages to clipboard");
                    });
                } else {
                    SwingUtilities.invokeLater(() -> {
                        appendStatus("✗ No " + columnName.toLowerCase() + "s found to copy");
                    });
                }
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    appendStatus("✗ Error copying all " + columnName.toLowerCase() + "s: " + e.getMessage());
                });
                logging.logToError("Error copying all " + columnName.toLowerCase() + "s: " + e.getMessage());
            }
        }).start();
    }
    
    private void fetchIntelligenceData(String fieldName, int page, JTable table, DefaultTableModel model) {
        fetchIntelligenceData(fieldName, page, table, model, null);
    }
    
    private void fetchIntelligenceData(String fieldName, int page, JTable table, DefaultTableModel model, JLabel pageLabel) {
        String apiKey = apiKeyField.getText().trim();
        String selectedName = (String) workspaceComboBox.getSelectedItem();
        
        if (apiKey.isEmpty() || selectedName == null || selectedName.equals("-- Select Workspace --") || !workspaceMap.containsKey(selectedName)) {
            return;
        }
        
        Workspace workspace = workspaceMap.get(selectedName);
        String workspaceId = workspace.getId();
        
        new Thread(() -> {
            try {
                appendStatus("Fetching " + fieldName + " (page " + page + ")...");
                final List<JsUrlEntry> entries = fetchIntelligenceDataForField(fieldName, workspaceId, apiKey, page);

                if (entries != null && !entries.isEmpty()) {
                    // Sort by timestamp descending
                    entries.sort((e1, e2) -> {
                        String t1 = e1.getScannedAt();
                        String t2 = e2.getScannedAt();
                        if ("—".equals(t1) && "—".equals(t2)) return 0;
                        if ("—".equals(t1)) return 1;
                        if ("—".equals(t2)) return -1;
                        return t2.compareTo(t1);
                    });
                    
                    final List<JsUrlEntry> entriesFinal = new ArrayList<>(entries);
                    SwingUtilities.invokeLater(() -> {
                        model.setRowCount(0);
                        for (JsUrlEntry entry : entriesFinal) {
                            model.addRow(new Object[]{entry.getUrl(), entry.getScannedAt()});
                        }
                        setCurrentPageForField(fieldName, page);
                        appendStatus("✓ Loaded " + entriesFinal.size() + " " + fieldName + " (page " + page + ")");
                        
                        // Update page label with total pages
                        updatePageLabel(pageLabel, page, fieldName);
                        
                        // Refresh counts after fetching data (only on first page to avoid too many API calls)
                        if (page == 1) {
                            fetchAndUpdateCounts();
                        } else {
                            // Update page label again after counts are refreshed (if counts were updated)
                            SwingUtilities.invokeLater(() -> {
                                updatePageLabel(pageLabel, page, fieldName);
                            });
                        }
                    });
                } else {
                    SwingUtilities.invokeLater(() -> {
                        model.setRowCount(0);
                        appendStatus("✓ No " + fieldName + " found (page " + page + ")");
                        updatePageLabel(pageLabel, page, fieldName);
                    });
                }
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    appendStatus("✗ Error fetching " + fieldName + ": " + e.getMessage());
                });
                logging.logToError("Error fetching " + fieldName + ": " + e.getMessage());
            }
        }).start();
    }
    
    private JTextField createStyledTextField() {
        JTextField field = new JTextField();
        field.setBackground(theme.inputBackground);
        field.setForeground(theme.inputForeground);
        field.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(theme.inputBorder, 1),
            new EmptyBorder(6, 10, 6, 10)
        ));
        field.setFont(field.getFont().deriveFont(12f));
        field.setCaretColor(theme.caretColor);
        return field;
    }
    
    private JButton createPrimaryButton(String text) {
        JButton button = new JButton(text);
        button.setBackground(theme.buttonPrimary);
        button.setForeground(Color.WHITE);
        button.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        button.setFocusPainted(false);
        button.setFont(button.getFont().deriveFont(12f));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                if (button.isEnabled()) {
                    button.setBackground(theme.buttonPrimaryHover);
                }
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                if (button.isEnabled()) {
                    button.setBackground(theme.buttonPrimary);
                }
            }
        });
        
        return button;
    }
    
    private JButton createSecondaryButton(String text) {
        JButton button = new JButton(text);
        button.setBackground(theme.buttonSecondary);
        button.setForeground(theme.textPrimary);
        button.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(theme.border, 1),
            new EmptyBorder(6, 12, 6, 12)
        ));
        button.setFocusPainted(false);
        button.setFont(button.getFont().deriveFont(12f));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                if (button.isEnabled()) {
                    button.setBackground(theme.buttonSecondaryHover);
                }
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                if (button.isEnabled()) {
                    button.setBackground(theme.buttonSecondary);
                }
            }
        });
        
        return button;
    }
    
    private JButton createSuccessButton(String text) {
        JButton button = new JButton(text);
        button.setBackground(new Color(60, 150, 80));
        button.setForeground(Color.WHITE);
        button.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        button.setFocusPainted(false);
        button.setFont(button.getFont().deriveFont(13f));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                if (button.isEnabled()) {
                    button.setBackground(new Color(70, 170, 90));
                }
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                if (button.isEnabled()) {
                    button.setBackground(new Color(60, 150, 80));
                }
            }
        });
        
        return button;
    }
    
    private void fetchWorkspaces() {
        String apiKey = apiKeyField.getText().trim();
        if (apiKey.isEmpty()) {
            showError("Please enter an API key first");
            return;
        }
        
        appendStatus("Starting to fetch workspaces...");
        extension.setApiKey(apiKey);
        appendStatus("API key set, calling JSMon API...");
        fetchAndDisplayUserProfile(true); // Show status when manually fetching
        fetchWorkspacesButton.setEnabled(false);
        fetchWorkspacesButton.setText("Fetching...");
        
        new Thread(() -> {
            try {
                appendStatus("Calling extension.fetchWorkspaces()...");
                List<Workspace> workspaces = extension.fetchWorkspaces();
                appendStatus("Received " + workspaces.size() + " workspace(s) from extension");
                
                // Update UI on EDT
                final List<Workspace> finalWorkspaces = workspaces;
                SwingUtilities.invokeLater(() -> {
                    try {
                        appendStatus("Processing " + finalWorkspaces.size() + " workspace(s)...");
                        
                        // Clear existing items first
                        workspaceComboBox.removeAllItems();
                        
                        if (finalWorkspaces.isEmpty()) {
                            appendStatus("✗ No workspaces found or error occurred.");
                            appendStatus("  Verify your API key is correct");
                            appendStatus("  Check Burp Suite Output tab for detailed error messages");
                            // Keep controls enabled - user can still create new workspace
                        } else {
                            appendStatus("✓ Found " + finalWorkspaces.size() + " workspace(s), populating dropdown...");
                            
                            // Clear existing items and map
                            workspaceComboBox.removeAllItems();
                            workspaceMap.clear();
                            
                            // Add workspaces as strings - simpler and more reliable
                            DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
                            
                            // Add placeholder as first item
                            String placeholder = "-- Select Workspace --";
                            model.addElement(placeholder);
                            
                            int addedCount = 0;
                            for (Workspace workspace : finalWorkspaces) {
                                try {
                                    String workspaceName = workspace.getName();
                                    model.addElement(workspaceName);
                                    workspaceMap.put(workspaceName, workspace);
                                    addedCount++;
                                    if (addedCount <= 3) {
                                        appendStatus("  • Added: " + workspaceName);
                                    }
                                } catch (Exception e) {
                                    appendStatus("  ✗ Error adding workspace: " + workspace.getName());
                                    logging.logToError("Error adding workspace: " + e.getMessage());
                                }
                            }
                            
                            if (addedCount > 3) {
                                appendStatus("  • ... and " + (addedCount - 3) + " more workspaces");
                            }
                            
                            // Set the model
                            workspaceComboBox.setModel(model);
                            
                            // Select placeholder (index 0) - don't auto-select a workspace
                            workspaceComboBox.setSelectedIndex(0);
                            
                            // Verify immediately
                            int actualCount = workspaceComboBox.getItemCount() - 1; // Exclude placeholder
                            appendStatus("  ✓ Combo box now contains " + actualCount + " workspace(s)");
                            
                            // Make combo box scrollable
                            workspaceComboBox.setMaximumRowCount(10);
                            workspaceComboBox.setToolTipText("Select a workspace from " + actualCount + " available");
                            
                            appendStatus("✓ Successfully loaded " + actualCount + " workspace(s)!");
                            appendStatus("  Please select a workspace from the dropdown");
                            
                            // Force UI refresh
                            workspaceComboBox.revalidate();
                            workspaceComboBox.repaint();
                        }
                        
                        fetchWorkspacesButton.setEnabled(true);
                        fetchWorkspacesButton.setText("Fetch Workspaces");
                    } catch (Exception e) {
                        appendStatus("✗ Error updating UI: " + e.getMessage());
                        e.printStackTrace();
                        logging.logToError("Error updating UI: " + e.getMessage());
                        logging.logToError("Stack trace: " + java.util.Arrays.toString(e.getStackTrace()));
                        fetchWorkspacesButton.setEnabled(true);
                        fetchWorkspacesButton.setText("Fetch Workspaces");
                    }
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    fetchWorkspacesButton.setEnabled(true);
                    fetchWorkspacesButton.setText("Fetch Workspaces");
                    String errorMsg = e.getMessage();
                    if (errorMsg == null) {
                        errorMsg = e.getClass().getSimpleName();
                    }
                    appendStatus("✗ Error fetching workspaces: " + errorMsg);
                    logging.logToError("Error fetching workspaces: " + errorMsg);
                    e.printStackTrace();
                });
            }
        }).start();
    }
    
    private void createWorkspace() {
        String apiKey = apiKeyField.getText().trim();
        if (apiKey.isEmpty()) {
            showError("Please enter an API key first");
            return;
        }
        
        String workspaceName = newWorkspaceNameField.getText().trim();
        if (workspaceName.isEmpty()) {
            showError("Please enter a workspace name");
            return;
        }
        
        extension.setApiKey(apiKey);
        appendStatus("Creating workspace: " + workspaceName + "...");
        createWorkspaceButton.setEnabled(false);
        createWorkspaceButton.setText("Creating...");
        
        new Thread(() -> {
            try {
                String workspaceId = extension.createWorkspace(workspaceName);
                
                SwingUtilities.invokeLater(() -> {
                    createWorkspaceButton.setEnabled(true);
                    createWorkspaceButton.setText("Create");
                    
                    if (workspaceId != null) {
                        extension.setWorkspaceId(workspaceId);
                        appendStatus("✓ Workspace created successfully! ID: " + workspaceId);
                        newWorkspaceNameField.setText("");
                        
                        fetchWorkspaces();
                        
                        new Thread(() -> {
                            try {
                                Thread.sleep(1000);
                                SwingUtilities.invokeLater(() -> {
                                    String selectedName = null;
                                    for (java.util.Map.Entry<String, Workspace> entry : workspaceMap.entrySet()) {
                                        if (entry.getValue().getId().equals(workspaceId)) {
                                            selectedName = entry.getKey();
                                            break;
                                        }
                                    }
                                    if (selectedName != null) {
                                        workspaceComboBox.setSelectedItem(selectedName);
                                    }
                                });
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                        }).start();
                    } else {
                        appendStatus("✗ Failed to create workspace. Check the logs.");
                    }
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    createWorkspaceButton.setEnabled(true);
                    createWorkspaceButton.setText("Create");
                    appendStatus("✗ Error creating workspace: " + e.getMessage());
                    logging.logToError("Error creating workspace: " + e.getMessage());
                });
            }
        }).start();
    }
    
    private void startManualScan() {
        // Check if API key and workspace are configured
        String apiKey = apiKeyField.getText().trim();
        if (apiKey.isEmpty()) {
            showError("Please enter an API key first");
            return;
        }
        
        String selectedName = (String) workspaceComboBox.getSelectedItem();
        if (selectedName == null || selectedName.equals("-- Select Workspace --") || !workspaceMap.containsKey(selectedName)) {
            showError("Please select a workspace first");
            return;
        }
        
        // Save configuration first
        extension.setApiKey(apiKey);
        Workspace selectedWorkspace = workspaceMap.get(selectedName);
        extension.setWorkspaceId(selectedWorkspace.getId());
        
        String scopedDomain = scopedDomainField.getText().trim();
        extension.setScopedDomain(scopedDomain.isEmpty() ? null : scopedDomain);
        
        appendStatus("🚀 Starting manual scan...");
        appendStatus("  Scanning JS files from Burp's HTTP history...");
        if (!scopedDomain.isEmpty()) {
            appendStatus("  Scoped domain: " + scopedDomain);
        } else {
            appendStatus("  No domain scope - scanning all JS files");
        }
        
        // Run scan in background thread with real-time status updates
        new Thread(() -> {
            try {
                appendStatus("  Checking HTTP history for JS files...");
                
                // Pass a callback to update UI in real-time
                int scannedCount = extension.scanHttpHistory((statusMessage) -> {
                    SwingUtilities.invokeLater(() -> {
                        appendStatus("  " + statusMessage);
                    });
                });
                
                SwingUtilities.invokeLater(() -> {
                    appendStatus("✓ Manual scan completed!");
                    appendStatus("  Check Burp Suite Output tab for detailed API logs");
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    appendStatus("✗ Error during manual scan: " + e.getMessage());
                    logging.logToError("Error during manual scan: " + e.getMessage());
                });
            }
        }).start();
    }
    
    /**
     * Auto-save configuration as user enters values
     * Saves API key, workspace, and scoped domain automatically
     */
    private void autoSaveConfiguration() {
        // Save API key
        String apiKey = apiKeyField.getText().trim();
        if (!apiKey.isEmpty()) {
            extension.setApiKey(apiKey);
        }
        
        // Save workspace if selected
        String selectedName = (String) workspaceComboBox.getSelectedItem();
        if (selectedName != null && !selectedName.equals("-- Select Workspace --") && workspaceMap.containsKey(selectedName)) {
            Workspace selectedWorkspace = workspaceMap.get(selectedName);
            extension.setWorkspaceId(selectedWorkspace.getId());
        }
        
        // Save scoped domain (can be empty - that means scan all)
        String scopedDomain = scopedDomainField.getText().trim();
        extension.setScopedDomain(scopedDomain.isEmpty() ? null : scopedDomain);
        
        // Refresh user profile whenever API key changes
        if (!apiKey.isEmpty()) {
            fetchAndDisplayUserProfile();
        }
        
        // Configuration is saved automatically (no user notification needed)
    }
    
    private void appendStatus(String message) {
        if (statusArea == null) {
            return;
        }
        
        SwingUtilities.invokeLater(() -> {
            try {
                if (statusArea != null) {
                    String timestamp = java.time.LocalTime.now().toString().substring(0, 8);
                    String logMessage = "[" + timestamp + "] " + message + "\n";
                    statusArea.append(logMessage);
                    
                    // Always scroll to bottom
                    int length = statusArea.getDocument().getLength();
                    statusArea.setCaretPosition(length);
                    
                    // Ensure the scroll pane shows the latest content
                    JScrollPane scrollPane = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, statusArea);
                    if (scrollPane != null) {
                        JScrollBar vertical = scrollPane.getVerticalScrollBar();
                        if (vertical != null) {
                            vertical.setValue(vertical.getMaximum());
                        }
                    }
                }
            } catch (Exception e) {
                // Silently handle any append errors
                logging.logToError("Error appending status: " + e.getMessage());
            }
        });
    }
    
    /**
     * Public method for extension to append status messages to UI
     */
    public void appendStatusMessage(String message) {
        appendStatus(message);
    }
    
    /**
     * Fetch and display secrets from JSMon
     */
    /**
     * Fetch and display secrets - always starts from page 1 when called without parameters
     * This ensures fresh data is loaded after scans
     */
    public void fetchAndDisplaySecrets() {
        fetchAndDisplaySecrets(1); // Always fetch page 1 when auto-called
    }
    
    /**
     * Fetch and display secrets for a specific page
     */
    public void fetchAndDisplaySecrets(int page) {
        fetchAndDisplaySecrets(page, secretsPageLabel);
    }
    
    private void fetchAndDisplaySecrets(int page, JLabel pageLabel) {
        String apiKey = apiKeyField.getText().trim();
        String selectedName = (String) workspaceComboBox.getSelectedItem();
        
        if (apiKey.isEmpty() || selectedName == null || selectedName.equals("-- Select Workspace --") || !workspaceMap.containsKey(selectedName)) {
            // Not configured yet - skip silently
            return;
        }
        
        Workspace workspace = workspaceMap.get(selectedName);
        String workspaceId = workspace.getId();
        
        // Fetch secrets in background thread
        new Thread(() -> {
            try {
                appendStatus("Fetching secrets (page " + page + ")...");
                String secretsOutput = extension.fetchSecrets(workspaceId, apiKey, page);
                List<Object[]> rows = parseSecretsJson(secretsOutput);
                
                SwingUtilities.invokeLater(() -> {
                    if (secretsTableModel != null) {
                        // Clear existing rows
                        secretsTableModel.setRowCount(0);
                        
                        // Add new rows
                        for (Object[] row : rows) {
                            secretsTableModel.addRow(row);
                        }
                        
                        secretsCurrentPage = page;
                        appendStatus("✓ Loaded " + rows.size() + " secret(s) (page " + page + ")");
                        
                        // Update page label with total pages
                        updateSecretsPageLabel(pageLabel);
                        
                        // Update secrets count directly from table (temporary, will be updated with total)
                        updateTabTitlesWithCounts();
                    }
                });
                
                // Fetch total secrets count on first page by fetching all pages
                if (page == 1) {
                    // Fetch total count in background (fetches all pages to get accurate count)
                    fetchTotalSecretsCount();
                    // Also fetch regular counts
                    fetchAndUpdateCounts();
                } else {
                    // Update page label again after counts might be refreshed
                    SwingUtilities.invokeLater(() -> {
                        updateSecretsPageLabel(pageLabel);
                    });
                }
                
                // Also fetch JS URLs when secrets are fetched (only on first page)
                if (page == 1) {
                    fetchAndDisplayJsUrls();
                }
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    appendStatus("✗ Error fetching secrets: " + e.getMessage());
                    updateSecretsPageLabel(pageLabel);
                });
                logging.logToError("Error fetching secrets: " + e.getMessage());
            }
        }).start();
    }
    
    /**
     * Fetch total secrets count by fetching all pages
     * This runs in background to get accurate total count
     */
    private void fetchTotalSecretsCount() {
        String apiKey = apiKeyField.getText().trim();
        String selectedName = (String) workspaceComboBox.getSelectedItem();
        
        if (apiKey.isEmpty() || selectedName == null || selectedName.equals("-- Select Workspace --") || !workspaceMap.containsKey(selectedName)) {
            return;
        }
        
        Workspace workspace = workspaceMap.get(selectedName);
        String workspaceId = workspace.getId();
        
        // Fetch total count in background thread
        new Thread(() -> {
            try {
                int totalCount = 0;
                int currentPage = 1;
                boolean hasMorePages = true;
                
                // Fetch pages until we get less than 100 items (indicating last page)
                while (hasMorePages && currentPage <= 100) { // Safety limit: max 100 pages
                    String secretsOutput = extension.fetchSecrets(workspaceId, apiKey, currentPage);
                    List<Object[]> rows = parseSecretsJson(secretsOutput);
                    
                    int pageCount = rows.size();
                    totalCount += pageCount;
                    
                    // If we got less than 100 items, this is the last page
                    if (pageCount < 100) {
                        hasMorePages = false;
                    } else {
                        currentPage++;
                    }
                }
                
                // Update total count
                final int finalTotalCount = totalCount;
                SwingUtilities.invokeLater(() -> {
                    secretsTotalCount = finalTotalCount;
                    updateTabTitlesWithCounts();
                    appendStatus("✓ Total secrets count: " + finalTotalCount);
                });
            } catch (Exception e) {
                logging.logToError("Error fetching total secrets count: " + e.getMessage());
                // On error, try to use fieldCounts as fallback
                SwingUtilities.invokeLater(() -> {
                    int fallbackCount = fieldCounts.getOrDefault("totalJwtTokens", 
                        fieldCounts.getOrDefault("totalSecrets",
                        fieldCounts.getOrDefault("secrets", 0)));
                    if (fallbackCount > 0) {
                        secretsTotalCount = fallbackCount;
                        updateTabTitlesWithCounts();
                    }
                });
            }
        }).start();
    }
    
    /**
     * Get total pages for secrets based on count
     */
    private int getTotalPagesForSecrets() {
        // Try multiple possible field names for secrets count
        int totalCount = fieldCounts.getOrDefault("totalJwtTokens", 
            fieldCounts.getOrDefault("totalSecrets",
            fieldCounts.getOrDefault("secrets", 0)));
        
        // If we have a total count from the API, use it
        if (totalCount > 0) {
            // Assuming 100 items per page
            int totalPages = (int) Math.ceil(totalCount / 100.0);
            return totalPages > 0 ? totalPages : 1; // At least 1 page
        }
        
        // If no count available yet, check current page
        // If current page has 100 items, assume there's at least one more page
        if (secretsTableModel != null) {
            int currentPageCount = secretsTableModel.getRowCount();
            if (currentPageCount == 100) {
                // Current page is full, so there's at least one more page
                // Return current page + 1 as minimum, but allow more
                return secretsCurrentPage + 1;
            } else if (currentPageCount > 0) {
                // Current page has items but less than 100, so this is likely the last page
                return secretsCurrentPage;
            }
        }
        
        // Default: at least 1 page
        return 1;
    }
    
    /**
     * Update secrets page label with "Page X of Y" format
     */
    private void updateSecretsPageLabel(JLabel pageLabel) {
        // Use class field if pageLabel parameter is null
        JLabel labelToUpdate = (pageLabel != null) ? pageLabel : secretsPageLabel;
        if (labelToUpdate != null) {
            int totalPages = getTotalPagesForSecrets();
            labelToUpdate.setText("Page " + secretsCurrentPage + " of " + totalPages);
        }
    }
    
    /**
     * Fetch and display JS URLs from JSMon intelligence API
     */
    public void fetchAndDisplayJsUrls() {
        fetchAndDisplayJsUrls(1);
    }
    
    public void fetchAndDisplayJsUrls(int page) {
        fetchAndDisplayJsUrlsWithLabel(page, null);
    }
    
    private void fetchAndDisplayJsUrlsWithLabel(int page, JLabel pageLabel) {
        String apiKey = apiKeyField.getText().trim();
        String selectedName = (String) workspaceComboBox.getSelectedItem();
        
        if (apiKey.isEmpty() || selectedName == null || selectedName.equals("-- Select Workspace --") || !workspaceMap.containsKey(selectedName)) {
            // Not configured yet - skip silently
            return;
        }
        
        Workspace workspace = workspaceMap.get(selectedName);
        String workspaceId = workspace.getId();
        
        // Fetch JS URLs in background thread
        new Thread(() -> {
            try {
                appendStatus("Fetching JS URLs (page " + page + ")...");
                List<JsUrlEntry> jsUrlEntries = extension.fetchJsUrls(workspaceId, apiKey, page);
                
                if (jsUrlEntries != null && !jsUrlEntries.isEmpty()) {
                    // Sort by timestamp descending (newest first)
                    // Entries with "—" (no timestamp) go to the end
                    jsUrlEntries.sort((e1, e2) -> {
                        String t1 = e1.getScannedAt();
                        String t2 = e2.getScannedAt();
                        
                        // If either is "—", put it at the end
                        if ("—".equals(t1) && "—".equals(t2)) return 0;
                        if ("—".equals(t1)) return 1;
                        if ("—".equals(t2)) return -1;
                        
                        // Compare timestamps (newest first)
                        return t2.compareTo(t1);
                    });
                    
                    final List<JsUrlEntry> entriesFinal = new ArrayList<>(jsUrlEntries);
                    SwingUtilities.invokeLater(() -> {
                        if (jsUrlsTableModel != null) {
                            // Clear existing rows
                            jsUrlsTableModel.setRowCount(0);
                            
                            // Add new rows (newest first)
                            for (JsUrlEntry entry : entriesFinal) {
                                jsUrlsTableModel.addRow(new Object[]{
                                    entry.getUrl(),
                                    entry.getScannedAt()
                                });
                            }
                            
                            jsUrlsCurrentPage = page;
                            appendStatus("✓ Loaded " + entriesFinal.size() + " JS URL(s) (page " + page + ")");
                            
                            // Update page label with total pages
                            updatePageLabel(pageLabel, page, "jsurls");
                            
                            // Refresh counts after fetching data
                            if (page == 1) {
                                fetchAndUpdateCounts();
                            } else {
                                // Update page label again after counts might be refreshed
                                SwingUtilities.invokeLater(() -> {
                                    updatePageLabel(pageLabel, page, "jsurls");
                                });
                            }
                        }
                    });
                } else {
                    SwingUtilities.invokeLater(() -> {
                        if (jsUrlsTableModel != null) {
                            jsUrlsTableModel.setRowCount(0);
                            appendStatus("✓ No JS URLs found (page " + page + ")");
                        }
                        // Update page label even if no data
                        updatePageLabel(pageLabel, page, "jsurls");
                    });
                }
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    appendStatus("✗ Error fetching JS URLs: " + e.getMessage());
                });
                logging.logToError("Error fetching JS URLs: " + e.getMessage());
            }
        }).start();
    }
    
    /**
     * Fetch and display API paths from JSMon intelligence API
     */
    public void fetchAndDisplayApiPaths() {
        fetchAndDisplayApiPaths(1);
    }
    
    public void fetchAndDisplayApiPaths(int page) {
        fetchAndDisplayApiPathsWithLabel(page, null);
    }
    
    private void fetchAndDisplayApiPathsWithLabel(int page, JLabel pageLabel) {
        String apiKey = apiKeyField.getText().trim();
        String selectedName = (String) workspaceComboBox.getSelectedItem();
        
        if (apiKey.isEmpty() || selectedName == null || selectedName.equals("-- Select Workspace --") || !workspaceMap.containsKey(selectedName)) {
            // Not configured yet - skip silently
            return;
        }
        
        Workspace workspace = workspaceMap.get(selectedName);
        String workspaceId = workspace.getId();
        
        // Fetch API paths in background thread
        new Thread(() -> {
            try {
                appendStatus("Fetching API paths (page " + page + ")...");
                List<JsUrlEntry> apiPathEntries = extension.fetchApiPaths(workspaceId, apiKey, page);
                
                if (apiPathEntries != null && !apiPathEntries.isEmpty()) {
                    // Sort by timestamp descending (newest first)
                    // Entries with "—" (no timestamp) go to the end
                    apiPathEntries.sort((e1, e2) -> {
                        String t1 = e1.getScannedAt();
                        String t2 = e2.getScannedAt();
                        
                        // If either is "—", put it at the end
                        if ("—".equals(t1) && "—".equals(t2)) return 0;
                        if ("—".equals(t1)) return 1;
                        if ("—".equals(t2)) return -1;
                        
                        // Compare timestamps (newest first)
                        return t2.compareTo(t1);
                    });
                    
                    final List<JsUrlEntry> entriesFinal = new ArrayList<>(apiPathEntries);
                    SwingUtilities.invokeLater(() -> {
                        if (apiPathsTableModel != null) {
                            // Clear existing rows
                            apiPathsTableModel.setRowCount(0);
                            
                            // Add new rows (newest first)
                            for (JsUrlEntry entry : entriesFinal) {
                                apiPathsTableModel.addRow(new Object[]{
                                    entry.getUrl(),
                                    entry.getScannedAt()
                                });
                            }
                            
                            apiPathsCurrentPage = page;
                            appendStatus("✓ Loaded " + entriesFinal.size() + " API path(s) (page " + page + ")");
                            
                            // Update page label with total pages
                            updatePageLabel(pageLabel, page, "apipaths");
                            
                            // Refresh counts after fetching data (only on first page to avoid too many API calls)
                            if (page == 1) {
                                fetchAndUpdateCounts();
                            } else {
                                // Update page label again after counts might be refreshed
                                SwingUtilities.invokeLater(() -> {
                                    updatePageLabel(pageLabel, page, "apipaths");
                                });
                            }
                        }
                    });
                } else {
                    SwingUtilities.invokeLater(() -> {
                        if (apiPathsTableModel != null) {
                            apiPathsTableModel.setRowCount(0);
                            appendStatus("✓ No API paths found (page " + page + ")");
                        }
                        // Update page label even if no data
                        updatePageLabel(pageLabel, page, "apipaths");
                    });
                }
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    appendStatus("✗ Error fetching API paths: " + e.getMessage());
                });
                logging.logToError("Error fetching API paths: " + e.getMessage());
            }
        }).start();
    }
    
    /**
     * Fetch and display URLs from JSMon intelligence API
     */
    public void fetchAndDisplayUrls(int page) {
        String apiKey = apiKeyField.getText().trim();
        String selectedName = (String) workspaceComboBox.getSelectedItem();
        
        if (apiKey.isEmpty() || selectedName == null || selectedName.equals("-- Select Workspace --") || !workspaceMap.containsKey(selectedName)) {
            return;
        }
        
        Workspace workspace = workspaceMap.get(selectedName);
        String workspaceId = workspace.getId();
        
        new Thread(() -> {
            try {
                appendStatus("Fetching URLs (page " + page + ")...");
                List<JsUrlEntry> entries = extension.fetchUrls(workspaceId, apiKey, page);
                
                if (entries != null && !entries.isEmpty()) {
                    entries.sort((e1, e2) -> {
                        String t1 = e1.getScannedAt();
                        String t2 = e2.getScannedAt();
                        if ("—".equals(t1) && "—".equals(t2)) return 0;
                        if ("—".equals(t1)) return 1;
                        if ("—".equals(t2)) return -1;
                        return t2.compareTo(t1);
                    });
                    
                    final List<JsUrlEntry> entriesFinal = new ArrayList<>(entries);
                    SwingUtilities.invokeLater(() -> {
                        if (urlsTableModel != null) {
                            urlsTableModel.setRowCount(0);
                            for (JsUrlEntry entry : entriesFinal) {
                                urlsTableModel.addRow(new Object[]{entry.getUrl(), entry.getScannedAt()});
                            }
                            urlsCurrentPage = page;
                            appendStatus("✓ Loaded " + entriesFinal.size() + " URL(s) (page " + page + ")");
                        }
                    });
                } else {
                    SwingUtilities.invokeLater(() -> {
                        if (urlsTableModel != null) {
                            urlsTableModel.setRowCount(0);
                            appendStatus("✓ No URLs found (page " + page + ")");
                        }
                    });
                }
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    appendStatus("✗ Error fetching URLs: " + e.getMessage());
                });
                logging.logToError("Error fetching URLs: " + e.getMessage());
            }
        }).start();
    }
    
    /**
     * Fetch and display Domains from JSMon intelligence API
     */
    public void fetchAndDisplayDomains(int page) {
        String apiKey = apiKeyField.getText().trim();
        String selectedName = (String) workspaceComboBox.getSelectedItem();
        
        if (apiKey.isEmpty() || selectedName == null || selectedName.equals("-- Select Workspace --") || !workspaceMap.containsKey(selectedName)) {
            return;
        }
        
        Workspace workspace = workspaceMap.get(selectedName);
        String workspaceId = workspace.getId();
        
        new Thread(() -> {
            try {
                appendStatus("Fetching Domains (page " + page + ")...");
                List<JsUrlEntry> entries = extension.fetchDomains(workspaceId, apiKey, page);
                
                if (entries != null && !entries.isEmpty()) {
                    entries.sort((e1, e2) -> {
                        String t1 = e1.getScannedAt();
                        String t2 = e2.getScannedAt();
                        if ("—".equals(t1) && "—".equals(t2)) return 0;
                        if ("—".equals(t1)) return 1;
                        if ("—".equals(t2)) return -1;
                        return t2.compareTo(t1);
                    });
                    
                    final List<JsUrlEntry> entriesFinal = new ArrayList<>(entries);
                    SwingUtilities.invokeLater(() -> {
                        if (domainsTableModel != null) {
                            domainsTableModel.setRowCount(0);
                            for (JsUrlEntry entry : entriesFinal) {
                                domainsTableModel.addRow(new Object[]{entry.getUrl(), entry.getScannedAt()});
                            }
                            domainsCurrentPage = page;
                            appendStatus("✓ Loaded " + entriesFinal.size() + " Domain(s) (page " + page + ")");
                        }
                    });
                } else {
                    SwingUtilities.invokeLater(() -> {
                        if (domainsTableModel != null) {
                            domainsTableModel.setRowCount(0);
                            appendStatus("✓ No Domains found (page " + page + ")");
                        }
                    });
                }
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    appendStatus("✗ Error fetching Domains: " + e.getMessage());
                });
                logging.logToError("Error fetching Domains: " + e.getMessage());
            }
        }).start();
    }
    
    /**
     * Fetch and update counts for all intelligence fields
     */
    public void fetchAndUpdateCounts() {
        String apiKey = apiKeyField.getText().trim();
        String selectedName = (String) workspaceComboBox.getSelectedItem();
        
        if (apiKey.isEmpty() || selectedName == null || selectedName.equals("-- Select Workspace --") || !workspaceMap.containsKey(selectedName)) {
            return;
        }
        
        Workspace workspace = workspaceMap.get(selectedName);
        String workspaceId = workspace.getId();
        
        new Thread(() -> {
            try {
                appendStatus("Fetching intelligence counts...");
                java.util.Map<String, Integer> counts = extension.fetchTotalCounts(workspaceId, apiKey);
                SwingUtilities.invokeLater(() -> {
                    fieldCounts = counts;
                    updateTabTitlesWithCounts();
                    appendStatus("✓ Counts updated");
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    appendStatus("✗ Error fetching counts: " + e.getMessage());
                });
                logging.logToError("Error fetching counts: " + e.getMessage());
            }
        }).start();
    }
    
    /**
     * Update tab titles with counts
     * Maps API response field names to UI display
     */
    private void updateTabTitlesWithCounts() {
        if (dataTabs != null) {
            // Update Secrets tab - use total count (fetch all pages if needed)
            int secretsCount = secretsTotalCount;
            // If total count is 0, try to get from fieldCounts or table as fallback
            if (secretsCount == 0) {
                secretsCount = fieldCounts.getOrDefault("totalJwtTokens", 
                    fieldCounts.getOrDefault("totalSecrets",
                    fieldCounts.getOrDefault("secrets", 0)));
                // If still 0, use table count as temporary fallback
                if (secretsCount == 0 && secretsTableModel != null) {
                    secretsCount = secretsTableModel.getRowCount();
                }
            }
            dataTabs.setTitleAt(0, "🔐 Secrets (" + secretsCount + ")");
            
            // Update JS Intelligence sub-tabs
            // API returns: totalJsUrls, totalApiPaths, totalUrls, totalDomains, 
            // totalIpAddresses, totalEmails, totalS3Domains
            if (jsIntelligenceSubTabs != null) {
                int jsUrlsCount = fieldCounts.getOrDefault("totalJsUrls", 
                    fieldCounts.getOrDefault("jsurls", 0));
                int apiPathsCount = fieldCounts.getOrDefault("totalApiPaths", 
                    fieldCounts.getOrDefault("apipaths", 0));
                int urlsCount = fieldCounts.getOrDefault("totalUrls", 
                    fieldCounts.getOrDefault("urls", 0));
                int domainsCount = fieldCounts.getOrDefault("totalDomains", 
                    fieldCounts.getOrDefault("domains", 0));
                int ipAddressesCount = fieldCounts.getOrDefault("totalIpAddresses", 
                    fieldCounts.getOrDefault("ipaddresses", 0));
                int emailsCount = fieldCounts.getOrDefault("totalEmails", 
                    fieldCounts.getOrDefault("emails", 0));
                int s3BucketsCount = fieldCounts.getOrDefault("totalS3Domains", 
                    fieldCounts.getOrDefault("s3domains", 0));
                // Try multiple possible field names for invalid node modules / npm confusion
                int invalidNodeModulesCount = fieldCounts.getOrDefault("totalInvalidNodeModules", 
                    fieldCounts.getOrDefault("totalNpmConfusion", 
                    fieldCounts.getOrDefault("totalNpmConfusions",
                    fieldCounts.getOrDefault("npmconfusion",
                    fieldCounts.getOrDefault("invalidnodemodules", 0)))));
                
                jsIntelligenceSubTabs.setTitleAt(0, "🔗 JS URLs (" + jsUrlsCount + ")");
                jsIntelligenceSubTabs.setTitleAt(1, "🛣️ API Paths (" + apiPathsCount + ")");
                jsIntelligenceSubTabs.setTitleAt(2, "🔗 URLs (" + urlsCount + ")");
                jsIntelligenceSubTabs.setTitleAt(3, "🌐 Domains (" + domainsCount + ")");
                jsIntelligenceSubTabs.setTitleAt(4, "🌐 IP Addresses (" + ipAddressesCount + ")");
                jsIntelligenceSubTabs.setTitleAt(5, "📧 Emails (" + emailsCount + ")");
                jsIntelligenceSubTabs.setTitleAt(6, "🪣 S3 Buckets (" + s3BucketsCount + ")");
                jsIntelligenceSubTabs.setTitleAt(7, "📦 Invalid Node Modules (" + invalidNodeModulesCount + ")");
            }
        }
    }

    /**
     * Fetch and display user profile (name, email, limits)
     */
    public void fetchAndDisplayUserProfile() {
        fetchAndDisplayUserProfile(false);
    }
    
    public void fetchAndDisplayUserProfile(boolean showStatus) {
        String apiKey = apiKeyField.getText().trim();
        if (apiKey.isEmpty()) {
            updateUserProfileUI(null);
            return;
        }

        new Thread(() -> {
            try {
                UserProfile profile = extension.fetchUserProfile();
                SwingUtilities.invokeLater(() -> {
                    updateUserProfileUI(profile);
                    if (showStatus && profile != null) {
                        appendStatus("✓ Loaded user profile");
                    }
                });
            } catch (Exception e) {
                if (showStatus) {
                    SwingUtilities.invokeLater(() -> appendStatus("✗ Error fetching profile: " + e.getMessage()));
                }
                logging.logToError("Error fetching profile: " + e.getMessage());
            }
        }).start();
    }

    private void updateUserProfileUI(UserProfile profile) {
        String name = (profile != null && profile.name != null && !profile.name.isEmpty()) ? profile.name : "—";
        String email = (profile != null && profile.email != null && !profile.email.isEmpty()) ? profile.email : "—";
        String limits = (profile != null && profile.remaining != null && !profile.remaining.isEmpty()) ? profile.remaining : "—";

        if (userNameValue != null) userNameValue.setText(name);
        if (userEmailValue != null) userEmailValue.setText(email);
        if (userLimitsValue != null) userLimitsValue.setText(limits);
    }
    
    /**
     * Parse JSON output from JSMon secrets API
     * Each line is a JSON object with: createdAt, matchedWord, moduleName, severity, source
     */
    private List<Object[]> parseSecretsJson(String output) {
        List<Object[]> rows = new ArrayList<>();
        
        if (output == null || output.trim().isEmpty()) {
            return rows;
        }
        
        // Handle multi-line JSON objects by combining lines until we have a complete JSON object
        StringBuilder jsonBuffer = new StringBuilder();
        int braceCount = 0;
        
        String[] lines = output.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("===") || line.startsWith("✗")) {
                continue;
            }
            
            // Track braces to detect complete JSON objects
            for (char c : line.toCharArray()) {
                if (c == '{') braceCount++;
                if (c == '}') braceCount--;
            }
            
            jsonBuffer.append(line);
            
            // When braces are balanced, we have a complete JSON object
            if (braceCount == 0 && jsonBuffer.length() > 0) {
                String jsonLine = jsonBuffer.toString();
                jsonBuffer.setLength(0); // Reset buffer
                
                // Parse JSON object
                try {
                    // Extract fields from JSON
                    String moduleName = extractJsonField(jsonLine, "moduleName");
                    String matchedWord = extractJsonField(jsonLine, "matchedWord");
                    String severity = extractJsonField(jsonLine, "severity");
                    String createdAt = extractJsonField(jsonLine, "createdAt");
                    
                    // Add row if at least one field is found
                    if (moduleName != null || matchedWord != null || severity != null || createdAt != null) {
                        // Format createdAt to be more readable
                        if (createdAt != null && createdAt.length() > 19) {
                            createdAt = createdAt.substring(0, 19).replace("T", " ");
                        }
                        
                        // Order: Module Name, Matched Word, Severity, Created At
                        rows.add(new Object[]{
                            moduleName != null ? moduleName : "",
                            matchedWord != null ? matchedWord : "",
                            severity != null ? severity : "",
                            createdAt != null ? createdAt : ""
                        });
                    }
                } catch (Exception e) {
                    // Skip malformed JSON lines
                    logging.logToError("Error parsing JSON line: " + e.getMessage());
                }
            }
        }
        
        // Handle any remaining JSON in buffer (incomplete objects)
        if (jsonBuffer.length() > 0 && braceCount == 0) {
            try {
                String moduleName = extractJsonField(jsonBuffer.toString(), "moduleName");
                String matchedWord = extractJsonField(jsonBuffer.toString(), "matchedWord");
                String severity = extractJsonField(jsonBuffer.toString(), "severity");
                String createdAt = extractJsonField(jsonBuffer.toString(), "createdAt");
                
                if (moduleName != null || matchedWord != null || severity != null || createdAt != null) {
                    if (createdAt != null && createdAt.length() > 19) {
                        createdAt = createdAt.substring(0, 19).replace("T", " ");
                    }
                    
                    rows.add(new Object[]{
                        moduleName != null ? moduleName : "",
                        matchedWord != null ? matchedWord : "",
                        severity != null ? severity : "",
                        createdAt != null ? createdAt : ""
                    });
                }
            } catch (Exception e) {
                // Skip
            }
        }
        
        return rows;
    }
    
    /**
     * Extract a field value from a JSON string
     * Handles escaped quotes in values and both quoted and unquoted values
     */
    private String extractJsonField(String json, String fieldName) {
        try {
            // Try pattern 1: "fieldName": "value" (quoted string)
            String quotedPattern = "\"" + fieldName + "\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"";
            java.util.regex.Pattern pattern1 = java.util.regex.Pattern.compile(quotedPattern);
            java.util.regex.Matcher matcher1 = pattern1.matcher(json);
            if (matcher1.find()) {
                String value = matcher1.group(1);
                // Unescape common escape sequences
                if (value != null) {
                    value = value.replace("\\\"", "\"");
                    value = value.replace("\\\\", "\\");
                    value = value.replace("\\n", "\n");
                    value = value.replace("\\r", "\r");
                    value = value.replace("\\t", "\t");
                }
                return value;
            }
            
            // Try pattern 2: "fieldName": value (unquoted or number)
            String unquotedPattern = "\"" + fieldName + "\"\\s*:\\s*([^,}\\]]+)";
            java.util.regex.Pattern pattern2 = java.util.regex.Pattern.compile(unquotedPattern);
            java.util.regex.Matcher matcher2 = pattern2.matcher(json);
            if (matcher2.find()) {
                String value = matcher2.group(1).trim();
                // Remove trailing quotes if any
                if (value.startsWith("\"") && value.endsWith("\"")) {
                    value = value.substring(1, value.length() - 1);
                }
                return value;
            }
        } catch (Exception e) {
            // Return null if field not found
        }
        return null;
    }
    
    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
        appendStatus("✗ ERROR: " + message);
    }
    
    private static class WorkspaceComboItem {
        private Workspace workspace;
        
        public WorkspaceComboItem(Workspace workspace) {
            this.workspace = workspace;
        }
        
        public Workspace getWorkspace() {
            return workspace;
        }
        
        @Override
        public String toString() {
            if (workspace != null && workspace.getName() != null) {
                return workspace.getName();
            }
            return "-- Select Workspace --";
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            WorkspaceComboItem that = (WorkspaceComboItem) obj;
            if (workspace == null && that.workspace == null) return true;
            if (workspace == null || that.workspace == null) return false;
            return workspace.getId().equals(that.workspace.getId());
        }
        
        @Override
        public int hashCode() {
            return workspace != null ? workspace.getId().hashCode() : 0;
        }
    }
}
