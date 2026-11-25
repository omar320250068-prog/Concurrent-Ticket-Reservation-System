package client;

import shared.models.*;
import shared.protocol.*;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * ═══════════════════════════════════════════════════════════════════
 *  ClientGUI — Swing-Based Client for Ticket Reservation System
 * ═══════════════════════════════════════════════════════════════════
 *  Features a modern dark-themed UI with:
 *  - Vehicle browsing with seat availability
 *  - Seat booking with class selection and price preview
 *  - Reservation viewing and cancellation
 *  - Real-time server connection status
 * ═══════════════════════════════════════════════════════════════════
 */
public class ClientGUI extends JFrame {

    // ── Network ─────────────────────────────────────────────────
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private boolean connected = false;

    // ── Theme Colors ────────────────────────────────────────────
    private static final Color BG_DARK = new Color(15, 15, 35);
    private static final Color BG_PANEL = new Color(25, 25, 50);
    private static final Color BG_CARD = new Color(35, 35, 65);
    private static final Color BG_INPUT = new Color(45, 45, 80);
    private static final Color ACCENT = new Color(233, 69, 96);
    private static final Color ACCENT_HOVER = new Color(255, 100, 120);
    private static final Color SUCCESS = new Color(46, 213, 115);
    private static final Color TEXT_PRIMARY = new Color(230, 230, 250);
    private static final Color TEXT_SECONDARY = new Color(160, 160, 190);
    private static final Color BORDER_COLOR = new Color(60, 60, 100);
    private static final Color BLUE_ACCENT = new Color(14, 165, 233);

    // ── Fonts ───────────────────────────────────────────────────
    private static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 22);
    private static final Font FONT_HEADER = new Font("Segoe UI", Font.BOLD, 14);
    private static final Font FONT_BODY = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font FONT_SMALL = new Font("Segoe UI", Font.PLAIN, 11);
    private static final Font FONT_BUTTON = new Font("Segoe UI", Font.BOLD, 13);

    // ── Connection UI ───────────────────────────────────────────
    private JTextField hostField;
    private JTextField portField;
    private JButton connectBtn;
    private JLabel statusLabel;

    // ── Vehicles Tab ────────────────────────────────────────────
    private JTable vehicleTable;
    private DefaultTableModel vehicleTableModel;

    // ── Booking Tab ─────────────────────────────────────────────
    private JComboBox<String> vehicleCombo;
    private JTextField nameField;
    private JSpinner seatSpinner;
    private JComboBox<String> classCombo;
    private JLabel pricePreviewLabel;
    private JTextArea bookingResultArea;
    private List<Vehicle> cachedVehicles = new ArrayList<>();

    // ── Reservations Tab ────────────────────────────────────────
    private JTextField searchNameField;
    private JTable reservationTable;
    private DefaultTableModel reservationTableModel;
    private JTextField cancelTicketField;

    // ── Status Bar ──────────────────────────────────────────────
    private JLabel statusBarLabel;

    public ClientGUI() {
        super("Ticket Reservation System — Train & Airplane");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(950, 700);
        setMinimumSize(new Dimension(800, 600));
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG_DARK);
        setLayout(new BorderLayout(0, 0));

        // Build UI
        add(createTopPanel(), BorderLayout.NORTH);
        add(createMainTabs(), BorderLayout.CENTER);
        add(createStatusBar(), BorderLayout.SOUTH);

        // Disconnect on close
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                disconnect();
            }
        });

        setVisible(true);
    }

    // ═══════════════════════════════════════════════════════════
    //  TOP PANEL (Title + Connection)
    // ═══════════════════════════════════════════════════════════

    private JPanel createTopPanel() {
        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(BG_PANEL);
        top.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, ACCENT));

        // Title
        JLabel title = new JLabel("  \u2708 Ticket Reservation System", SwingConstants.LEFT);
        title.setFont(FONT_TITLE);
        title.setForeground(TEXT_PRIMARY);
        title.setBorder(new EmptyBorder(12, 10, 12, 0));
        top.add(title, BorderLayout.WEST);

        // Connection panel
        JPanel connPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 10));
        connPanel.setOpaque(false);

        hostField = createStyledTextField("localhost", 10);
        portField = createStyledTextField("5000", 5);
        connectBtn = createStyledButton("Connect", ACCENT);
        statusLabel = new JLabel("\u25CF Disconnected");
        statusLabel.setForeground(ACCENT);
        statusLabel.setFont(FONT_SMALL);

        connPanel.add(label("Server:"));
        connPanel.add(hostField);
        connPanel.add(label("Port:"));
        connPanel.add(portField);
        connPanel.add(connectBtn);
        connPanel.add(statusLabel);

        top.add(connPanel, BorderLayout.EAST);

        connectBtn.addActionListener(e -> toggleConnection());

        return top;
    }

    // ═══════════════════════════════════════════════════════════
    //  MAIN TABBED PANE
    // ═══════════════════════════════════════════════════════════

    private JTabbedPane createMainTabs() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.setBackground(BG_DARK);
        tabs.setForeground(TEXT_PRIMARY);
        tabs.setFont(FONT_HEADER);

        tabs.addTab(" Browse Vehicles ", createVehiclesTab());
        tabs.addTab(" Book Ticket ", createBookingTab());
        tabs.addTab(" My Reservations ", createReservationsTab());

        // Style the tab pane
        tabs.setOpaque(true);
        UIManager.put("TabbedPane.selected", BG_CARD);

        return tabs;
    }

    // ═══════════════════════════════════════════════════════════
    //  VEHICLES TAB
    // ═══════════════════════════════════════════════════════════

    private JPanel createVehiclesTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBackground(BG_DARK);
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // Header with refresh button
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel lbl = new JLabel("Available Vehicles");
        lbl.setFont(FONT_HEADER);
        lbl.setForeground(TEXT_PRIMARY);
        header.add(lbl, BorderLayout.WEST);

        JButton refreshBtn = createStyledButton("Refresh", BLUE_ACCENT);
        refreshBtn.addActionListener(e -> loadVehicles());
        header.add(refreshBtn, BorderLayout.EAST);
        panel.add(header, BorderLayout.NORTH);

        // Table
        String[] cols = {"ID", "Type", "Route", "Total Seats", "Available", "Base Price ($)"};
        vehicleTableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        vehicleTable = createStyledTable(vehicleTableModel);
        JScrollPane scroll = new JScrollPane(vehicleTable);
        styleScrollPane(scroll);
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    // ═══════════════════════════════════════════════════════════
    //  BOOKING TAB
    // ═══════════════════════════════════════════════════════════

    private JPanel createBookingTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBackground(BG_DARK);
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // Form panel
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(BG_CARD);
        form.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1),
                new EmptyBorder(20, 20, 20, 20)
        ));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Vehicle selection
        gbc.gridx = 0; gbc.gridy = 0;
        form.add(formLabel("Vehicle:"), gbc);
        vehicleCombo = new JComboBox<>();
        vehicleCombo.setFont(FONT_BODY);
        vehicleCombo.setBackground(BG_INPUT);
        vehicleCombo.setForeground(TEXT_PRIMARY);
        gbc.gridx = 1; gbc.weightx = 1;
        form.add(vehicleCombo, gbc);

        // Passenger name
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        form.add(formLabel("Passenger Name:"), gbc);
        nameField = createStyledTextField("", 20);
        gbc.gridx = 1; gbc.weightx = 1;
        form.add(nameField, gbc);

        // Seat number
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0;
        form.add(formLabel("Seat Number:"), gbc);
        seatSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 500, 1));
        seatSpinner.setFont(FONT_BODY);
        JComponent editor = seatSpinner.getEditor();
        if (editor instanceof JSpinner.DefaultEditor) {
            ((JSpinner.DefaultEditor) editor).getTextField().setBackground(BG_INPUT);
            ((JSpinner.DefaultEditor) editor).getTextField().setForeground(TEXT_PRIMARY);
        }
        gbc.gridx = 1; gbc.weightx = 1;
        form.add(seatSpinner, gbc);

        // Seat class
        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0;
        form.add(formLabel("Seat Class:"), gbc);
        classCombo = new JComboBox<>(new String[]{"Economy", "Business", "First"});
        classCombo.setFont(FONT_BODY);
        classCombo.setBackground(BG_INPUT);
        classCombo.setForeground(TEXT_PRIMARY);
        gbc.gridx = 1; gbc.weightx = 1;
        form.add(classCombo, gbc);

        // Price preview
        gbc.gridx = 0; gbc.gridy = 4; gbc.weightx = 0;
        form.add(formLabel("Price Preview:"), gbc);
        pricePreviewLabel = new JLabel("Select a vehicle and class");
        pricePreviewLabel.setFont(FONT_HEADER);
        pricePreviewLabel.setForeground(SUCCESS);
        gbc.gridx = 1; gbc.weightx = 1;
        form.add(pricePreviewLabel, gbc);

        // Book button
        JButton bookBtn = createStyledButton("  Book Ticket  ", SUCCESS);
        bookBtn.setFont(new Font("Segoe UI", Font.BOLD, 15));
        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 2;
        gbc.insets = new Insets(20, 8, 8, 8);
        form.add(bookBtn, gbc);

        panel.add(form, BorderLayout.NORTH);

        // Result area
        bookingResultArea = new JTextArea(5, 40);
        bookingResultArea.setEditable(false);
        bookingResultArea.setFont(FONT_BODY);
        bookingResultArea.setBackground(BG_CARD);
        bookingResultArea.setForeground(TEXT_PRIMARY);
        bookingResultArea.setBorder(new EmptyBorder(10, 10, 10, 10));
        JScrollPane resultScroll = new JScrollPane(bookingResultArea);
        styleScrollPane(resultScroll);
        panel.add(resultScroll, BorderLayout.CENTER);

        // Listeners
        bookBtn.addActionListener(e -> bookTicket());

        // Price preview updater
        ActionListener priceUpdater = e -> updatePricePreview();
        vehicleCombo.addActionListener(priceUpdater);
        classCombo.addActionListener(priceUpdater);

        return panel;
    }

    // ═══════════════════════════════════════════════════════════
    //  RESERVATIONS TAB
    // ═══════════════════════════════════════════════════════════

    private JPanel createReservationsTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBackground(BG_DARK);
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // Search bar
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        searchPanel.setOpaque(false);
        searchPanel.add(label("Passenger Name:"));
        searchNameField = createStyledTextField("", 15);
        searchPanel.add(searchNameField);
        JButton searchBtn = createStyledButton("Search", BLUE_ACCENT);
        searchBtn.addActionListener(e -> loadReservations());
        searchPanel.add(searchBtn);
        JButton allBtn = createStyledButton("Show All", TEXT_SECONDARY);
        allBtn.addActionListener(e -> { searchNameField.setText(""); loadReservations(); });
        searchPanel.add(allBtn);
        panel.add(searchPanel, BorderLayout.NORTH);

        // Reservations table
        String[] cols = {"Ticket ID", "Passenger", "Vehicle", "Seat #", "Class", "Price ($)"};
        reservationTableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        reservationTable = createStyledTable(reservationTableModel);
        JScrollPane scroll = new JScrollPane(reservationTable);
        styleScrollPane(scroll);
        panel.add(scroll, BorderLayout.CENTER);

        // Cancel panel
        JPanel cancelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        cancelPanel.setBackground(BG_CARD);
        cancelPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1),
                new EmptyBorder(8, 8, 8, 8)
        ));
        cancelPanel.add(label("Ticket ID to Cancel:"));
        cancelTicketField = createStyledTextField("", 12);
        cancelPanel.add(cancelTicketField);
        JButton cancelBtn = createStyledButton("Cancel Reservation", ACCENT);
        cancelBtn.addActionListener(e -> cancelReservation());
        cancelPanel.add(cancelBtn);
        panel.add(cancelPanel, BorderLayout.SOUTH);

        // Auto-fill ticket ID on table row click
        reservationTable.getSelectionModel().addListSelectionListener(e -> {
            int row = reservationTable.getSelectedRow();
            if (row >= 0) {
                cancelTicketField.setText((String) reservationTableModel.getValueAt(row, 0));
            }
        });

        return panel;
    }

    // ═══════════════════════════════════════════════════════════
    //  STATUS BAR
    // ═══════════════════════════════════════════════════════════

    private JPanel createStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(BG_PANEL);
        bar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_COLOR));
        statusBarLabel = new JLabel("  Ready — Connect to a server to begin");
        statusBarLabel.setFont(FONT_SMALL);
        statusBarLabel.setForeground(TEXT_SECONDARY);
        statusBarLabel.setBorder(new EmptyBorder(6, 8, 6, 8));
        bar.add(statusBarLabel, BorderLayout.WEST);
        return bar;
    }

    // ═══════════════════════════════════════════════════════════
    //  NETWORK OPERATIONS
    // ═══════════════════════════════════════════════════════════

    private void toggleConnection() {
        if (connected) {
            disconnect();
        } else {
            connect();
        }
    }

    private void connect() {
        String host = hostField.getText().trim();
        int port;
        try {
            port = Integer.parseInt(portField.getText().trim());
        } catch (NumberFormatException e) {
            showError("Invalid port number.");
            return;
        }

        try {
            socket = new Socket(host, port);
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());
            connected = true;

            connectBtn.setText("Disconnect");
            statusLabel.setText("\u25CF Connected");
            statusLabel.setForeground(SUCCESS);
            setStatus("Connected to " + host + ":" + port);

            // Auto-load data
            loadVehicles();
            loadReservations();

        } catch (IOException e) {
            showError("Connection failed: " + e.getMessage());
        }
    }

    private void disconnect() {
        if (!connected) return;
        try {
            sendRequest(Request.disconnect());
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
        } catch (Exception ignored) {}
        connected = false;
        connectBtn.setText("Connect");
        statusLabel.setText("\u25CF Disconnected");
        statusLabel.setForeground(ACCENT);
        setStatus("Disconnected");
    }

    private Response sendRequest(Request request) {
        if (!connected) {
            showError("Not connected to server.");
            return null;
        }
        try {
            out.writeObject(request);
            out.flush();
            out.reset();
            return (Response) in.readObject();
        } catch (Exception e) {
            showError("Communication error: " + e.getMessage());
            connected = false;
            connectBtn.setText("Connect");
            statusLabel.setText("\u25CF Disconnected");
            statusLabel.setForeground(ACCENT);
            return null;
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  ACTION HANDLERS
    // ═══════════════════════════════════════════════════════════

    @SuppressWarnings("unchecked")
    private void loadVehicles() {
        Response resp = sendRequest(Request.viewVehicles());
        if (resp == null) return;

        if (resp.isSuccess() && resp.getData() instanceof List) {
            cachedVehicles = (List<Vehicle>) resp.getData();
            vehicleTableModel.setRowCount(0);
            vehicleCombo.removeAllItems();

            for (Vehicle v : cachedVehicles) {
                vehicleTableModel.addRow(new Object[]{
                        v.getVehicleId(), v.getType(), v.getRoute(),
                        v.getTotalSeats(), v.getAvailableSeats(),
                        String.format("%.2f", v.getBasePrice())
                });
                vehicleCombo.addItem(v.getVehicleId() + " — " + v.getType() + " (" + v.getRoute() + ")");
            }
            setStatus(resp.getMessage());
        } else {
            showError(resp.getMessage());
        }
    }

    private void bookTicket() {
        if (vehicleCombo.getSelectedIndex() < 0) {
            showError("Please select a vehicle.");
            return;
        }
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            showError("Please enter passenger name.");
            return;
        }

        Vehicle selected = cachedVehicles.get(vehicleCombo.getSelectedIndex());
        int seat = (Integer) seatSpinner.getValue();
        String seatClass = (String) classCombo.getSelectedItem();

        Request req = Request.bookSeat(selected.getVehicleId(), name, seat, seatClass);
        Response resp = sendRequest(req);
        if (resp == null) return;

        if (resp.isSuccess()) {
            Ticket ticket = (Ticket) resp.getData();
            bookingResultArea.append("=== BOOKING CONFIRMED ===\n");
            bookingResultArea.append("  Ticket ID   : " + ticket.getTicketId() + "\n");
            bookingResultArea.append("  Passenger   : " + ticket.getPassengerName() + "\n");
            bookingResultArea.append("  Vehicle     : " + ticket.getVehicleId() + "\n");
            bookingResultArea.append("  Seat        : " + ticket.getSeatNumber() + " (" + ticket.getSeatClass() + ")\n");
            bookingResultArea.append("  Final Price : $" + String.format("%.2f", ticket.getFinalPrice()) + "\n\n");
            setStatus("Booked! " + resp.getMessage());
            loadVehicles(); // Refresh availability
        } else {
            bookingResultArea.append("BOOKING FAILED: " + resp.getMessage() + "\n\n");
            showError(resp.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void loadReservations() {
        String name = searchNameField.getText().trim();
        Response resp = sendRequest(Request.viewReservations(name.isEmpty() ? null : name));
        if (resp == null) return;

        if (resp.isSuccess() && resp.getData() instanceof List) {
            List<Ticket> tickets = (List<Ticket>) resp.getData();
            reservationTableModel.setRowCount(0);
            for (Ticket t : tickets) {
                reservationTableModel.addRow(new Object[]{
                        t.getTicketId(), t.getPassengerName(), t.getVehicleId(),
                        t.getSeatNumber(), t.getSeatClass(),
                        String.format("%.2f", t.getFinalPrice())
                });
            }
            setStatus(resp.getMessage());
        } else {
            showError(resp.getMessage());
        }
    }

    private void cancelReservation() {
        String ticketId = cancelTicketField.getText().trim();
        if (ticketId.isEmpty()) {
            showError("Please enter a Ticket ID to cancel.");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
                "Cancel reservation " + ticketId + "?", "Confirm Cancellation",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        Response resp = sendRequest(Request.cancelReservation(ticketId));
        if (resp == null) return;

        if (resp.isSuccess()) {
            JOptionPane.showMessageDialog(this, resp.getMessage(), "Cancelled", JOptionPane.INFORMATION_MESSAGE);
            cancelTicketField.setText("");
            loadReservations();
            loadVehicles();
        } else {
            showError(resp.getMessage());
        }
    }

    private void updatePricePreview() {
        int idx = vehicleCombo.getSelectedIndex();
        if (idx < 0 || idx >= cachedVehicles.size()) {
            pricePreviewLabel.setText("Select a vehicle");
            return;
        }
        Vehicle v = cachedVehicles.get(idx);
        String cls = (String) classCombo.getSelectedItem();
        if (cls == null) cls = "Economy";
        double price = v.calculateTicketPrice(cls);
        pricePreviewLabel.setText(String.format("$%.2f  (%s on %s)", price, cls, v.getType()));
    }

    // ═══════════════════════════════════════════════════════════
    //  UI HELPER METHODS
    // ═══════════════════════════════════════════════════════════

    private JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(TEXT_SECONDARY);
        l.setFont(FONT_BODY);
        return l;
    }

    private JLabel formLabel(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(TEXT_PRIMARY);
        l.setFont(FONT_BODY);
        return l;
    }

    private JTextField createStyledTextField(String text, int cols) {
        JTextField tf = new JTextField(text, cols);
        tf.setFont(FONT_BODY);
        tf.setBackground(BG_INPUT);
        tf.setForeground(TEXT_PRIMARY);
        tf.setCaretColor(TEXT_PRIMARY);
        tf.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR),
                new EmptyBorder(5, 8, 5, 8)
        ));
        return tf;
    }

    private JButton createStyledButton(String text, Color bgColor) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_BUTTON);
        btn.setForeground(Color.WHITE);
        btn.setBackground(bgColor);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(8, 16, 8, 16));
        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) { btn.setBackground(bgColor.brighter()); }
            @Override
            public void mouseExited(MouseEvent e) { btn.setBackground(bgColor); }
        });
        return btn;
    }

    private JTable createStyledTable(DefaultTableModel model) {
        JTable table = new JTable(model);
        table.setFont(FONT_BODY);
        table.setBackground(BG_CARD);
        table.setForeground(TEXT_PRIMARY);
        table.setSelectionBackground(ACCENT);
        table.setSelectionForeground(Color.WHITE);
        table.setGridColor(BORDER_COLOR);
        table.setRowHeight(30);
        table.setShowGrid(true);
        table.setIntercellSpacing(new Dimension(1, 1));

        // Header styling
        JTableHeader header = table.getTableHeader();
        header.setFont(FONT_HEADER);
        header.setBackground(BG_PANEL);
        header.setForeground(BLUE_ACCENT);
        header.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));

        // Alternating row colors
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val,
                    boolean sel, boolean focus, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, val, sel, focus, row, col);
                if (!sel) {
                    c.setBackground(row % 2 == 0 ? BG_CARD : BG_PANEL);
                    c.setForeground(TEXT_PRIMARY);
                }
                ((JLabel) c).setBorder(new EmptyBorder(0, 8, 0, 8));
                return c;
            }
        });
        return table;
    }

    private void styleScrollPane(JScrollPane sp) {
        sp.getViewport().setBackground(BG_CARD);
        sp.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
    }

    private void setStatus(String msg) {
        statusBarLabel.setText("  " + msg);
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
        setStatus("Error: " + msg);
    }

    // ═══════════════════════════════════════════════════════════
    //  MAIN (Entry Point)
    // ═══════════════════════════════════════════════════════════

    public static void main(String[] args) {
        // Set system look and feel hints
        try {
            UIManager.put("Panel.background", BG_DARK);
            UIManager.put("OptionPane.background", BG_CARD);
            UIManager.put("OptionPane.messageForeground", TEXT_PRIMARY);
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(ClientGUI::new);
    }
}
