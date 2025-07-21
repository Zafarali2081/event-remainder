import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import javax.sound.sampled.*;
import java.io.File;

public class EventReminderApp {
    private ArrayList<Event> events = new ArrayList<>();
    private DefaultListModel<Event> eventListModel = new DefaultListModel<>();
    private Timer notificationTimer = new Timer();
    private Clip notificationSound;
    private JList<Event> eventList;
    private boolean soundEnabled = true;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new EventReminderApp().createAndShowGUI());
    }

    private void createAndShowGUI() {
        // Load notification sound
        try {
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(
                new File("notification.wav"));
            notificationSound = AudioSystem.getClip();
            notificationSound.open(audioInputStream);
        } catch (Exception e) {
            System.out.println("Couldn't load sound file, continuing without sound");
            soundEnabled = false;
        }

        // Main frame setup
        JFrame frame = new JFrame("Enhanced Event Reminder");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(900, 650);
        frame.setLayout(new BorderLayout());
        frame.getContentPane().setBackground(new Color(240, 240, 245));

        // Header panel
        JPanel headerPanel = new JPanel();
        headerPanel.setBackground(new Color(108, 92, 231));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        headerPanel.setLayout(new BorderLayout());

        JLabel titleLabel = new JLabel("Enhanced Event Reminder");
        titleLabel.setFont(new Font("Poppins", Font.BOLD, 24));
        titleLabel.setForeground(Color.WHITE);
        headerPanel.add(titleLabel, BorderLayout.WEST);

        JButton addButton = new JButton("Add Event");
        addButton.setFont(new Font("Poppins", Font.BOLD, 14));
        addButton.setBackground(new Color(253, 121, 168));
        addButton.setForeground(Color.WHITE);
        addButton.setFocusPainted(false);
        addButton.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        addButton.addActionListener(e -> showAddEventDialog(frame, null));
        headerPanel.add(addButton, BorderLayout.EAST);

        frame.add(headerPanel, BorderLayout.NORTH);

        // Quick add panel
        JPanel quickAddPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        quickAddPanel.setBackground(new Color(230, 230, 240));
        quickAddPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        String[] quickEvents = {"Birthday", "Party", "Homework"};
        Color[] quickColors = {new Color(253, 121, 168), new Color(0, 184, 148), new Color(253, 203, 110)};
        
        for (int i = 0; i < quickEvents.length; i++) {
            final String eventName = quickEvents[i];
            JButton quickButton = new JButton("Quick Add: " + eventName);
            quickButton.setBackground(quickColors[i]);
            quickButton.setForeground(Color.WHITE);
            quickButton.setFocusPainted(false);
            quickButton.addActionListener(e -> addQuickEvent(eventName, frame));
            quickAddPanel.add(quickButton);
        }

        frame.add(quickAddPanel, BorderLayout.SOUTH);

        // Main content panel
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Event list
        eventList = new JList<>(eventListModel);
        eventList.setCellRenderer(new EventListRenderer());
        eventList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        eventList.setFixedCellHeight(80);
        eventList.setBackground(new Color(250, 250, 255));

        // Add right-click menu for editing/deleting
        JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem editItem = new JMenuItem("Edit Event");
        editItem.addActionListener(e -> editSelectedEvent(frame));
        JMenuItem deleteItem = new JMenuItem("Delete Event");
        deleteItem.addActionListener(e -> deleteSelectedEvent());
        popupMenu.add(editItem);
        popupMenu.add(deleteItem);
        
        eventList.setComponentPopupMenu(popupMenu);
        eventList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    editSelectedEvent(frame);
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(eventList);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        // Sidebar with categories and settings
        JPanel sidebarPanel = new JPanel();
        sidebarPanel.setLayout(new BoxLayout(sidebarPanel, BoxLayout.Y_AXIS));
        sidebarPanel.setBackground(new Color(230, 230, 240));
        sidebarPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        sidebarPanel.setPreferredSize(new Dimension(200, 0));

        JLabel categoryLabel = new JLabel("Categories");
        categoryLabel.setFont(new Font("Poppins", Font.BOLD, 16));
        categoryLabel.setForeground(new Color(108, 92, 231));
        sidebarPanel.add(categoryLabel);
        sidebarPanel.add(Box.createRigidArea(new Dimension(0, 15)));

        String[] categories = {"All", "Birthday", "Party", "Homework", "Work", "Meeting"};
        for (String category : categories) {
            JButton categoryButton = new JButton(category);
            categoryButton.setFont(new Font("Poppins", Font.PLAIN, 14));
            categoryButton.setAlignmentX(Component.LEFT_ALIGNMENT);
            categoryButton.setBackground(Color.WHITE);
            categoryButton.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
            categoryButton.addActionListener(e -> filterEvents(category));
            sidebarPanel.add(categoryButton);
            sidebarPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        }

        // Settings section
        sidebarPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        JLabel settingsLabel = new JLabel("Settings");
        settingsLabel.setFont(new Font("Poppins", Font.BOLD, 16));
        settingsLabel.setForeground(new Color(108, 92, 231));
        sidebarPanel.add(settingsLabel);
        sidebarPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        JCheckBox soundCheckbox = new JCheckBox("Enable Sound", soundEnabled);
        soundCheckbox.addActionListener(e -> soundEnabled = soundCheckbox.isSelected());
        sidebarPanel.add(soundCheckbox);

        mainPanel.add(sidebarPanel, BorderLayout.WEST);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        frame.add(mainPanel, BorderLayout.CENTER);

        // Start notification checker
        startNotificationChecker();

        frame.setVisible(true);
    }

    private void addQuickEvent(String type, JFrame parent) {
        String title = "";
        String description = "";
        LocalDateTime defaultTime = LocalDateTime.now().plusHours(1);

        switch (type) {
            case "Birthday":
                title = "Birthday Party";
                description = "Don't forget the cake and gifts!";
                break;
            case "Party":
                title = "Friends Gathering";
                description = "Bring snacks and drinks!";
                break;
            case "Homework":
                title = "Homework Deadline";
                description = "Finish and submit before deadline";
                defaultTime = LocalDateTime.now().plusDays(1);
                break;
        }

        Event newEvent = new Event(title, description, defaultTime, type);
        events.add(newEvent);
        eventListModel.addElement(newEvent);
        JOptionPane.showMessageDialog(parent, "Quick event added: " + type);
    }

    private void editSelectedEvent(JFrame parent) {
        Event selected = eventList.getSelectedValue();
        if (selected != null) {
            showAddEventDialog(parent, selected);
        }
    }

    private void deleteSelectedEvent() {
        Event selected = eventList.getSelectedValue();
        if (selected != null) {
            events.remove(selected);
            eventListModel.removeElement(selected);
        }
    }

    private void showAddEventDialog(JFrame parent, Event eventToEdit) {
        JDialog dialog = new JDialog(parent, eventToEdit == null ? "Add New Event" : "Edit Event", true);
        dialog.setSize(400, 450);
        dialog.setLayout(new BorderLayout());
        dialog.setLocationRelativeTo(parent);

        JPanel formPanel = new JPanel();
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel titleLabel = new JLabel("Event Title:");
        titleLabel.setFont(new Font("Poppins", Font.BOLD, 14));
        JTextField titleField = new JTextField();
        titleField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

        JLabel descLabel = new JLabel("Description:");
        descLabel.setFont(new Font("Poppins", Font.BOLD, 14));
        JTextArea descArea = new JTextArea(3, 20);
        descArea.setLineWrap(true);
        JScrollPane descScroll = new JScrollPane(descArea);

        JLabel dateLabel = new JLabel("Date & Time:");
        dateLabel.setFont(new Font("Poppins", Font.BOLD, 14));
        JTextField dateField = new JTextField();
        dateField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        dateField.setText(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));

        JLabel categoryLabel = new JLabel("Category:");
        categoryLabel.setFont(new Font("Poppins", Font.BOLD, 14));
        String[] categories = {"Birthday", "Party", "Homework", "Work", "Meeting", "Other"};
        JComboBox<String> categoryCombo = new JComboBox<>(categories);

        // If editing, populate fields
        if (eventToEdit != null) {
            titleField.setText(eventToEdit.getTitle());
            descArea.setText(eventToEdit.getDescription());
            dateField.setText(eventToEdit.getDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            categoryCombo.setSelectedItem(eventToEdit.getCategory());
        }

        formPanel.add(titleLabel);
        formPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        formPanel.add(titleField);
        formPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        formPanel.add(descLabel);
        formPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        formPanel.add(descScroll);
        formPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        formPanel.add(dateLabel);
        formPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        formPanel.add(dateField);
        formPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        formPanel.add(categoryLabel);
        formPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        formPanel.add(categoryCombo);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> dialog.dispose());
        JButton saveButton = new JButton(eventToEdit == null ? "Save" : "Update");
        saveButton.setBackground(new Color(108, 92, 231));
        saveButton.setForeground(Color.WHITE);
        saveButton.addActionListener(e -> {
            try {
                String title = titleField.getText();
                String description = descArea.getText();
                LocalDateTime dateTime = LocalDateTime.parse(dateField.getText(), 
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
                String category = (String) categoryCombo.getSelectedItem();

                if (eventToEdit == null) {
                    Event newEvent = new Event(title, description, dateTime, category);
                    events.add(newEvent);
                    eventListModel.addElement(newEvent);
                } else {
                    eventToEdit.setTitle(title);
                    eventToEdit.setDescription(description);
                    eventToEdit.setDateTime(dateTime);
                    eventToEdit.setCategory(category);
                    eventToEdit.setNotified(false);
                    eventList.repaint();
                }
                dialog.dispose();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Invalid date format. Please use yyyy-MM-dd HH:mm", 
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        buttonPanel.add(cancelButton);
        buttonPanel.add(saveButton);

        dialog.add(formPanel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private void filterEvents(String category) {
        eventListModel.clear();
        if (category.equals("All")) {
            events.forEach(eventListModel::addElement);
        } else {
            events.stream()
                .filter(event -> event.getCategory().equals(category))
                .forEach(eventListModel::addElement);
        }
    }

    private void startNotificationChecker() {
        notificationTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                LocalDateTime now = LocalDateTime.now();
                for (Event event : events) {
                    if (event.getDateTime().isBefore(now.plusMinutes(1)) && !event.isNotified()) {
                        showNotification(event);
                        event.setNotified(true);
                        
                        // Play sound if enabled
                        if (soundEnabled && notificationSound != null) {
                            try {
                                notificationSound.setFramePosition(0);
                                notificationSound.start();
                                Timer stopSoundTimer = new Timer();
                                stopSoundTimer.schedule(new TimerTask() {
                                    @Override
                                    public void run() {
                                        notificationSound.stop();
                                    }
                                }, 3000);
                            } catch (Exception e) {
                                System.out.println("Error playing sound");
                            }
                        }
                    }
                }
            }
        }, 0, 60000); // Check every minute
    }

    private void showNotification(Event event) {
        SwingUtilities.invokeLater(() -> {
            if (SystemTray.isSupported()) {
                try {
                    SystemTray tray = SystemTray.getSystemTray();
                    Image image = Toolkit.getDefaultToolkit().createImage("icon.png");
                    TrayIcon trayIcon = new TrayIcon(image, "Event Reminder");
                    tray.add(trayIcon);
                    trayIcon.displayMessage(
                        "Event Reminder: " + event.getTitle(),
                        event.getDescription() + "\nTime: " + 
                        event.getDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                        TrayIcon.MessageType.INFO
                    );
                    tray.remove(trayIcon);
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(null, 
                        "Event: " + event.getTitle() + "\n" + event.getDescription(),
                        "Event Reminder", JOptionPane.INFORMATION_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(null, 
                    "Event: " + event.getTitle() + "\n" + event.getDescription(),
                    "Event Reminder", JOptionPane.INFORMATION_MESSAGE);
            }
        });
    }

    class EventListRenderer extends JPanel implements ListCellRenderer<Event> {
        private JLabel titleLabel = new JLabel();
        private JLabel dateLabel = new JLabel();
        private JLabel descLabel = new JLabel();
        private JLabel categoryLabel = new JLabel();

        public EventListRenderer() {
            setLayout(new BorderLayout());
            setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
            setOpaque(true);

            JPanel topPanel = new JPanel(new BorderLayout());
            topPanel.add(titleLabel, BorderLayout.WEST);
            topPanel.add(dateLabel, BorderLayout.EAST);

            JPanel bottomPanel = new JPanel(new BorderLayout());
            bottomPanel.add(descLabel, BorderLayout.WEST);
            bottomPanel.add(categoryLabel, BorderLayout.EAST);

            add(topPanel, BorderLayout.NORTH);
            add(bottomPanel, BorderLayout.SOUTH);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends Event> list, Event event, 
                int index, boolean isSelected, boolean cellHasFocus) {
            
            titleLabel.setText(event.getTitle());
            titleLabel.setFont(new Font("Poppins", Font.BOLD, 16));
            
            dateLabel.setText(event.getDateTime().format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")));
            dateLabel.setFont(new Font("Poppins", Font.PLAIN, 12));
            
            descLabel.setText(event.getDescription());
            descLabel.setFont(new Font("Poppins", Font.PLAIN, 14));
            
            categoryLabel.setText(event.getCategory());
            categoryLabel.setFont(new Font("Poppins", Font.BOLD, 12));
            categoryLabel.setForeground(getCategoryColor(event.getCategory()));

            if (isSelected) {
                setBackground(new Color(220, 220, 255));
            } else {
                setBackground(Color.WHITE);
            }

            return this;
        }

        private Color getCategoryColor(String category) {
            switch (category) {
                case "Birthday": return new Color(253, 121, 168);
                case "Party": return new Color(0, 184, 148);
                case "Homework": return new Color(253, 203, 110);
                case "Work": return new Color(214, 48, 49);
                case "Meeting": return new Color(162, 155, 254);
                default: return new Color(108, 92, 231);
            }
        }
    }

    class Event {
        private String title;
        private String description;
        private LocalDateTime dateTime;
        private String category;
        private boolean notified;

        public Event(String title, String description, LocalDateTime dateTime, String category) {
            this.title = title;
            this.description = description;
            this.dateTime = dateTime;
            this.category = category;
            this.notified = false;
        }

        public String getTitle() { return title; }
        public String getDescription() { return description; }
        public LocalDateTime getDateTime() { return dateTime; }
        public String getCategory() { return category; }
        public boolean isNotified() { return notified; }
        
        public void setTitle(String title) { this.title = title; }
        public void setDescription(String description) { this.description = description; }
        public void setDateTime(LocalDateTime dateTime) { this.dateTime = dateTime; }
        public void setCategory(String category) { this.category = category; }
        public void setNotified(boolean notified) { this.notified = notified; }

        @Override
        public String toString() {
            return title + " - " + dateTime.format(DateTimeFormatter.ofPattern("MMM dd, HH:mm"));
        }
    }
}