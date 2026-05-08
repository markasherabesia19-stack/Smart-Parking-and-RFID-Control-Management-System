package ui.admin;

import dao.VehicleDAO;
import dao.UserAccountDAO;
import dao.VehicleOwnerDAO;
import model.AppState;
import model.Vehicle;
import model.UserAccount;
import model.VehicleOwner;
import ui.shared.SidebarPanel;
import util.UIFactory;
import util.DialogUtil;
import static util.UIConstants.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.*;
import java.awt.*;
import java.util.List;
import java.util.Optional;

public class AdminVehiclesScreen {

    // Column indices — update here if columns ever change order
    private static final int COL_PLATE   = 0;
    private static final int COL_OWNER   = 1;
    private static final int COL_USER    = 2;
    private static final int COL_TYPE    = 3;
    private static final int COL_COLOR   = 4;
    private static final int COL_STATUS  = 5;

    public static JPanel build(CardLayout cardLayout, JPanel rootPanel, AppState state) {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(C_BG_DARK);
        root.setOpaque(true);
        root.setName("ADMIN_VEHICLES");
        root.add(SidebarPanel.build(cardLayout, rootPanel, state, "ADMIN", "ADMIN_VEHICLES"), BorderLayout.WEST);

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(C_BG_DARK);
        content.setOpaque(true);

        // ── Top Bar ──────────────────────────────────────────────────────────
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(C_BG_PANEL);
        topBar.setBorder(new EmptyBorder(14, 24, 14, 24));
        topBar.add(UIFactory.lbl("VEHICLES", Font.BOLD, 20, C_WHITE), BorderLayout.WEST);

        JPanel topRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        topRight.setOpaque(false);

        // ── Filter dropdown ───────────────────────────────────────────────────
        String[] filterOptions = {"All", "Plate", "Owner", "Username", "Type", "Color", "Parking Status"};
        JComboBox<String> filterBox = new JComboBox<>(filterOptions);
        filterBox.setFont(new Font("SansSerif", Font.PLAIN, 12));
        filterBox.setBackground(C_BG_CARD);
        filterBox.setForeground(C_WHITE);
        filterBox.setPreferredSize(new Dimension(140, 32));
        filterBox.setBorder(BorderFactory.createLineBorder(C_INPUT_BD));
        // Style the popup list
        filterBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                setBackground(isSelected ? C_PURPLE : C_BG_CARD);
                setForeground(C_WHITE);
                setBorder(new EmptyBorder(4, 10, 4, 10));
                return this;
            }
        });

        // ── Search field ──────────────────────────────────────────────────────
        JTextField searchField = new JTextField(22);
        searchField.setFont(new Font("SansSerif", Font.PLAIN, 13));
        searchField.setBackground(C_BG_CARD);
        searchField.setForeground(C_WHITE);
        searchField.setCaretColor(C_WHITE);
        searchField.setPreferredSize(new Dimension(220, 32));
        searchField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(C_INPUT_BD),
            new EmptyBorder(4, 10, 4, 10)
        ));

        final String PLACEHOLDER = "\uD83D\uDD0D  Search vehicles...";
        searchField.setText(PLACEHOLDER);
        searchField.setForeground(C_MUTED);
        searchField.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusGained(java.awt.event.FocusEvent e) {
                if (searchField.getText().equals(PLACEHOLDER)) {
                    searchField.setText("");
                    searchField.setForeground(C_WHITE);
                }
            }
            @Override public void focusLost(java.awt.event.FocusEvent e) {
                if (searchField.getText().isBlank()) {
                    searchField.setText(PLACEHOLDER);
                    searchField.setForeground(C_MUTED);
                }
            }
        });

        JButton addBtn = new JButton("+ Add Vehicle");
        UIFactory.styleSmallBtn(addBtn);
        addBtn.addActionListener(e -> cardLayout.show(rootPanel, "ADMIN_REGISTER"));

        topRight.add(filterBox);
        topRight.add(searchField);
        topRight.add(addBtn);
        topBar.add(topRight, BorderLayout.EAST);
        content.add(topBar, BorderLayout.NORTH);

        // ── Table model ───────────────────────────────────────────────────────
        String[] cols = {"Plate", "Owner", "Username", "Type", "Color", "Parking Status"};

        DefaultTableModel tableModel = new DefaultTableModel(new Object[0][6], cols) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        JTable table = new JTable(tableModel) {
            @Override
            public Component prepareRenderer(TableCellRenderer renderer, int row, int col) {
                Component c = super.prepareRenderer(renderer, row, col);
                if (col == COL_STATUS) {
                    String status = (String) getValueAt(row, col);
                    if ("Parked".equals(status)) {
                        c.setForeground(new Color(80, 220, 100));
                    } else if ("Suspended".equals(status)) {
                        c.setForeground(new Color(220, 80, 80));
                    } else {
                        c.setForeground(new Color(200, 200, 200));
                    }
                } else {
                    c.setForeground(Color.WHITE);
                }
                return c;
            }
        };
        styleTable(table);

        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(tableModel);
        table.setRowSorter(sorter);

        // ── "No Results Found" overlay ────────────────────────────────────────
        JLabel noResultsLabel = new JLabel("No results found", SwingConstants.CENTER);
        noResultsLabel.setFont(new Font("SansSerif", Font.BOLD, 15));
        noResultsLabel.setForeground(C_MUTED);
        noResultsLabel.setVisible(false);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setOpaque(true);
        scroll.getViewport().setOpaque(true);
        scroll.getViewport().setBackground(C_BG_CARD);
        scroll.setBorder(BorderFactory.createLineBorder(C_INPUT_BD));

        JPanel tableLayer = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                g.setColor(C_BG_CARD);
                g.fillRect(0, 0, getWidth(), getHeight());
                super.paintComponent(g);
            }
        };
        tableLayer.setLayout(new OverlayLayout(tableLayer));
        tableLayer.setOpaque(true);
        tableLayer.setBackground(C_BG_CARD);
        noResultsLabel.setOpaque(false);
        noResultsLabel.setAlignmentX(0.5f);
        noResultsLabel.setAlignmentY(0.5f);
        tableLayer.add(noResultsLabel);
        tableLayer.add(scroll);

        // ── Search + filter logic ─────────────────────────────────────────────
        Runnable applyFilter = () -> {
            String raw        = searchField.getText();
            String filterSel  = (String) filterBox.getSelectedItem();

            if (raw.equals(PLACEHOLDER) || raw.isBlank()) {
                sorter.setRowFilter(null);
                noResultsLabel.setVisible(false);
                return;
            }

            String regex = "(?i)" + java.util.regex.Pattern.quote(raw.trim());
            RowFilter<DefaultTableModel, Object> rf;

            if ("All".equals(filterSel)) {
                // Search across every column
                rf = RowFilter.regexFilter(regex);
            } else {
                // Map dropdown label → column index
                int col = switch (filterSel) {
                    case "Plate"          -> COL_PLATE;
                    case "Owner"          -> COL_OWNER;
                    case "Username"       -> COL_USER;
                    case "Type"           -> COL_TYPE;
                    case "Color"          -> COL_COLOR;
                    case "Parking Status" -> COL_STATUS;
                    default               -> -1;
                };
                rf = col >= 0 ? RowFilter.regexFilter(regex, col) : RowFilter.regexFilter(regex);
            }

            sorter.setRowFilter(rf);
            noResultsLabel.setVisible(table.getRowCount() == 0);
        };

        // Trigger on typing
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e)  { applyFilter.run(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e)  { applyFilter.run(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { applyFilter.run(); }
        });

        // Trigger on dropdown change
        filterBox.addActionListener(e -> applyFilter.run());

        // Reload data when navigating to this screen — no rebuild needed
        root.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentShown(java.awt.event.ComponentEvent e) {
                reloadTable(tableModel, sorter, searchField, PLACEHOLDER, noResultsLabel);
            }
        });

        // ── Reload when entry/exit fires notifySlotChange ─────────────────────
        state.addSlotChangeListener(() ->
            SwingUtilities.invokeLater(() ->
                reloadTable(tableModel, sorter, searchField, PLACEHOLDER, noResultsLabel)
            )
        );

        // Initial load
        reloadTable(tableModel, sorter, searchField, PLACEHOLDER, noResultsLabel);

        // ── Layout Assembly ───────────────────────────────────────────────────
        JPanel body = new JPanel(new BorderLayout());
        body.setBackground(C_BG_DARK);
        body.setOpaque(true);
        body.setBorder(new EmptyBorder(20, 20, 20, 20));
        body.add(tableLayer, BorderLayout.CENTER);

        content.add(body, BorderLayout.CENTER);
        root.add(content, BorderLayout.CENTER);
        return root;
    }

    private static void reloadTable(DefaultTableModel tableModel,
                                    TableRowSorter<DefaultTableModel> sorter,
                                    JTextField searchField,
                                    String placeholder,
                                    JLabel noResultsLabel) {
        tableModel.setRowCount(0);
        sorter.setRowFilter(null);
        searchField.setText(placeholder);
        searchField.setForeground(C_MUTED);
        noResultsLabel.setVisible(false);

        try {
            VehicleDAO vehicleDAO         = new VehicleDAO();
            VehicleOwnerDAO ownerDAO      = new VehicleOwnerDAO();
            UserAccountDAO userAccountDAO = new UserAccountDAO();
            List<Vehicle> vehicles        = vehicleDAO.findAll();

            for (Vehicle v : vehicles) {
                String ownerName = "Unknown";
                String username  = "—";
                try {
                    Optional<VehicleOwner> owner = ownerDAO.findById(v.getOwnerId());
                    if (owner.isPresent()) {
                        VehicleOwner o = owner.get();
                        ownerName = o.getFirstName() + " " + o.getLastName();
                        if (o.getUserId() != null) {
                            Optional<UserAccount> account = userAccountDAO.findById(o.getUserId());
                            username = account.map(UserAccount::getUsername).orElse("—");
                        }
                    }
                } catch (Exception ex) {
                    // keep defaults
                }

                tableModel.addRow(new Object[]{
                    v.getPlateNumber(),
                    ownerName,
                    username,
                    v.getVehicleType()   != null ? v.getVehicleType()   : "N/A",
                    v.getColor()         != null ? v.getColor()         : "N/A",
                    v.getParkingStatus() != null ? v.getParkingStatus() : "Not Parked"
                });
            }

        } catch (Exception e) {
            e.printStackTrace();
            DialogUtil.showMessageDialog(null,
                "Error loading vehicles: " + e.getMessage(),
                "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static void styleTable(JTable table) {
        table.setBackground(C_BG_CARD);
        table.setForeground(C_WHITE);
        table.setFont(new Font("SansSerif", Font.PLAIN, 13));
        table.setRowHeight(32);
        table.setGridColor(new Color(60, 50, 100));
        table.setSelectionBackground(C_PURPLE);
        table.setSelectionForeground(C_WHITE);
        table.setOpaque(true);
        JTableHeader header = table.getTableHeader();
        header.setBackground(C_BG_PANEL);
        header.setForeground(C_MUTED);
        header.setFont(new Font("SansSerif", Font.BOLD, 11));
        header.setBorder(BorderFactory.createLineBorder(C_INPUT_BD));
        table.setFillsViewportHeight(true);
    }
}