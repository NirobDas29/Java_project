import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class MovieBookingGUI extends JFrame {
    private static final long serialVersionUID = 1L;
    private static final String[] ROWS = {"A", "B", "C", "D"};
    private static final int SEATS_PER_ROW = 5;

    private static final Color AVAILABLE_REGULAR = new Color(144, 238, 144);
    private static final Color AVAILABLE_PREMIUM = new Color(255, 193, 7);
    private static final Color SELECTED_COLOR = new Color(100, 149, 237);
    private static final Color BOOKED_COLOR = new Color(240, 128, 128);

    private final BookingService bookingService = new BookingService();
    private User loggedInUser = null;

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cardPanel = new JPanel(cardLayout);

    // Login/Register Fields
    private JTextField loginUserField, regUserField, regEmailField, regPhoneField;
    private JPasswordField loginPassField, regPassField;
    private JLabel loginStatusLabel, regStatusLabel;

    // User Panel Components
    private final CardLayout userSubCardLayout = new CardLayout();
    private final JPanel userSubCardPanel = new JPanel(userSubCardLayout);
    private Movie selectedMovieForBooking = null;

    private DefaultListModel<Movie> allMoviesListModel;
    private JList<Movie> allMoviesJList;
    private DefaultListModel<Movie> comingSoonListModel;
    private JList<Movie> comingSoonJList;
    private JLabel comingSoonImageLabel;

    // View All Movies Components
    private DefaultListModel<Movie> viewAllListModel;
    private JList<Movie> viewAllJList;
    private JLabel viewAllImageLabel;

    private JComboBox<String> dateCombo, timeCombo;
    private JLabel movieSelectImageLabel, seatMovieImageLabel, currentMovieTitleLabel,
            bookingStatusLabel, browseMoviePriceLabel;

    private JButton[][] seatButtons;
    private final LinkedHashSet<String> selectedSeats = new LinkedHashSet<>();
    private String chosenTime = "", chosenDate = "", assignedHallName = "";
    private DefaultTableModel userBookingsModel;
    private JTable userBookingsTable;

    // Profile Components
    private JTextField profUserField, profEmailField, profPhoneField;
    private JPasswordField profPassField;
    private JLabel profStatusLabel;

    // Admin Panel Components
    private DefaultTableModel adminMoviesModel;
    private DefaultTableModel adminBookingsModel;
    private DefaultTableModel adminPaymentsModel;
    private JTextField adminMovieNameField, adminMoviePriceField, adminMovieTimesField;
    private JComboBox<String> adminMovieCategoryCombo;
    private JCheckBox adminMovieBlockbusterCheck;
    private JLabel adminImagePreviewLabel;
    private String selectedAdminImagePath = "";
    private int selectedAdminMovieId = -1;

    public MovieBookingGUI() {
        super("Movie Ticket Booking System");
        initUI();
    }

    public void initUI() {
        setSize(1050, 780);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        applyDarkBlueTheme();

        cardPanel.add(buildLoginPanel(), "login");
        cardPanel.add(buildRegisterPanel(), "register");
        cardPanel.add(buildUserMainPanel(), "user_main");
        cardPanel.add(buildAdminMainPanel(), "admin_main");

        add(cardPanel);
        cardLayout.show(cardPanel, "login");
    }

    // ================= LOGIN / REGISTER =================
    private JPanel buildLoginPanel() {
        JPanel outer = new JPanel(new GridBagLayout());
        JPanel form = new JPanel(new GridLayout(0, 1, 5, 8));
        form.setBorder(new EmptyBorder(20, 30, 20, 30));
        form.setBackground(new Color(18, 42, 80));

        JLabel title = new JLabel("Sign In", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 20));
        loginUserField = new JTextField();
        loginPassField = new JPasswordField();
        loginStatusLabel = new JLabel(" ", SwingConstants.CENTER);
        loginStatusLabel.setForeground(new Color(255, 126, 126));

        JButton loginBtn = new JButton("Sign In");
        loginBtn.addActionListener(this::handleLogin);
        JButton goToRegBtn = new JButton("Create New Account");
        goToRegBtn.addActionListener(e -> cardLayout.show(cardPanel, "register"));

        form.add(title); form.add(new JLabel("Username:")); form.add(loginUserField);
        form.add(new JLabel("Password:")); form.add(loginPassField);
        form.add(loginBtn); form.add(goToRegBtn); form.add(loginStatusLabel);
        outer.add(form);
        return outer;
    }

    private void handleLogin(ActionEvent e) {
        String user = loginUserField.getText().trim();
        String pass = new String(loginPassField.getPassword());
        User auth = User.authenticate(user, pass);
        if (auth != null) {
            loggedInUser = auth;
            loginStatusLabel.setText(" ");
            loginPassField.setText("");
            if (loggedInUser.isAdmin()) {
                refreshAdminMoviesTable();
                refreshAdminBookingsTable();
                refreshAdminPaymentsTable();
                refreshAdminWaitlistTable();
                cardLayout.show(cardPanel, "admin_main");
            } else {
                refreshUserBookingsTable();
                updateProfileTab();
                userSubCardLayout.show(userSubCardPanel, "category_select");
                cardLayout.show(cardPanel, "user_main");
            }
        } else {
            loginStatusLabel.setText("Invalid username or password.");
        }
    }

    private JPanel buildRegisterPanel() {
        JPanel outer = new JPanel(new GridBagLayout());
        JPanel form = new JPanel(new GridLayout(0, 1, 5, 8));
        form.setBorder(new EmptyBorder(20, 30, 20, 30));
        form.setBackground(new Color(18, 42, 80));

        JLabel title = new JLabel("Register Account", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 18));
        regUserField = new JTextField();
        regEmailField = new JTextField();
        regPhoneField = new JTextField();
        regPassField = new JPasswordField();
        regStatusLabel = new JLabel(" ", SwingConstants.CENTER);
        regStatusLabel.setForeground(new Color(255, 126, 126));

        JButton registerBtn = new JButton("Register");
        registerBtn.addActionListener(this::handleRegister);
        JButton backBtn = new JButton("Back to Sign In");
        backBtn.addActionListener(e -> cardLayout.show(cardPanel, "login"));

        form.add(title);
        form.add(new JLabel("Username:")); form.add(regUserField);
        form.add(new JLabel("Gmail Address:")); form.add(regEmailField);
        form.add(new JLabel("Phone Number:")); form.add(regPhoneField);
        form.add(new JLabel("Password:")); form.add(regPassField);
        form.add(registerBtn); form.add(backBtn); form.add(regStatusLabel);
        outer.add(form);
        return outer;
    }

    private void handleRegister(ActionEvent e) {
        String user = regUserField.getText().trim();
        String email = regEmailField.getText().trim();
        String phone = regPhoneField.getText().trim();
        String pass = new String(regPassField.getPassword());

        if (user.isEmpty() || email.isEmpty() || phone.isEmpty() || pass.isEmpty()) {
            regStatusLabel.setText("All fields are required.");
            return;
        }
        if (!User.isValidGmail(email)) {
            regStatusLabel.setText("Must be a valid @gmail.com address.");
            return;
        }

        if (User.registerUser(user, pass, email, phone)) {
            JOptionPane.showMessageDialog(this, "Registration Successful! You can sign in now.");
            cardLayout.show(cardPanel, "login");
        } else {
            regStatusLabel.setText("Username already exists.");
        }
    }

    // ================= USER PANEL =================
    private JPanel buildUserMainPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JTabbedPane tabs = new JTabbedPane();

        userSubCardPanel.add(buildCategorySelectPage(), "category_select");
        userSubCardPanel.add(buildBrowseNowShowingPage(), "browse_now_showing");
        userSubCardPanel.add(buildComingSoonPage(), "coming_soon_page");
        userSubCardPanel.add(buildViewAllMoviesPage(), "view_all_movies_page");
        userSubCardPanel.add(buildSeatSelectionPage(), "seat_selection_page");

        tabs.addTab("Book Ticket", userSubCardPanel);
        tabs.addTab("My Bookings", buildUserBookingsTab());
        tabs.addTab("My Profile", buildUserProfileTab());

        JButton logoutBtn = new JButton("Sign Out");
        logoutBtn.addActionListener(e -> { loggedInUser = null; cardLayout.show(cardPanel, "login"); });
        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        topBar.setBackground(new Color(10, 28, 58)); topBar.add(logoutBtn);

        panel.add(topBar, BorderLayout.NORTH);
        panel.add(tabs, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildUserProfileTab() {
        JPanel panel = new JPanel(new GridBagLayout());
        JPanel card = new JPanel(new GridLayout(0, 1, 6, 8));
        card.setBorder(new EmptyBorder(25, 35, 25, 35));
        card.setBackground(new Color(18, 42, 80));

        JLabel title = new JLabel("Edit Profile Details", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 18));
        
        profUserField = new JTextField();
        profEmailField = new JTextField();
        profPhoneField = new JTextField();
        profPassField = new JPasswordField();

        profStatusLabel = new JLabel(" ", SwingConstants.CENTER);
        profStatusLabel.setForeground(new Color(144, 238, 144));

        JButton updateBtn = new JButton("Save Changes");
        updateBtn.addActionListener(e -> {
            if (loggedInUser == null) return;

            String newUsername = profUserField.getText().trim();
            String newEmail = profEmailField.getText().trim();
            String newPhone = profPhoneField.getText().trim();
            String newPass = new String(profPassField.getPassword()).trim();

            if (newUsername.isEmpty() || newEmail.isEmpty() || newPhone.isEmpty()) {
                profStatusLabel.setForeground(new Color(255, 126, 126));
                profStatusLabel.setText("Username, Email, and Phone cannot be empty.");
                return;
            }

            if (!User.isValidGmail(newEmail)) {
                profStatusLabel.setForeground(new Color(255, 126, 126));
                profStatusLabel.setText("Must be a valid @gmail.com address.");
                return;
            }

            String oldUsername = loggedInUser.getUserName();
            boolean success = User.updateUserProfile(oldUsername, newUsername, newPass, newEmail, newPhone);

            if (success) {
                loggedInUser.setUserName(newUsername);
                if (!newPass.isEmpty()) {
                    loggedInUser.setPassword(newPass);
                }
                loggedInUser.setEmail(newEmail);
                loggedInUser.setPhoneNumber(newPhone);

                profStatusLabel.setForeground(new Color(144, 238, 144));
                profStatusLabel.setText("Profile updated successfully!");
            } else {
                profStatusLabel.setForeground(new Color(255, 126, 126));
                profStatusLabel.setText("Username already exists or failed to update.");
            }
        });

        card.add(title);
        card.add(new JLabel("Username:"));
        card.add(profUserField);
        card.add(new JLabel("Email Address:"));
        card.add(profEmailField);
        card.add(new JLabel("Phone Number:"));
        card.add(profPhoneField);
        card.add(new JLabel("New Password (Leave blank to keep same):"));
        card.add(profPassField);
        card.add(updateBtn);
        card.add(profStatusLabel);

        panel.add(card);
        return panel;
    }

    private void updateProfileTab() {
        if (loggedInUser != null) {
            profUserField.setText(loggedInUser.getUserName());
            profEmailField.setText(loggedInUser.getEmail());
            profPhoneField.setText(loggedInUser.getPhoneNumber());
            profPassField.setText("");
            profStatusLabel.setText(" ");
        }
    }

    private JPanel buildCategorySelectPage() {
        JPanel panel = new JPanel(new GridBagLayout());
        JPanel card = new JPanel(new GridLayout(0, 1, 15, 15));
        card.setBorder(new EmptyBorder(30, 50, 30, 50));
        card.setBackground(new Color(18, 42, 80));

        JLabel title = new JLabel("What would you like to see?", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 20));

        JButton nowShowingBtn = new JButton("1. Now Showing");
        nowShowingBtn.setFont(new Font("SansSerif", Font.BOLD, 16));
        nowShowingBtn.setPreferredSize(new Dimension(220, 50));
        nowShowingBtn.addActionListener(e -> {
            refreshBrowsePage();
            userSubCardLayout.show(userSubCardPanel, "browse_now_showing");
        });

        JButton comingSoonBtn = new JButton("2. Coming Soon");
        comingSoonBtn.setFont(new Font("SansSerif", Font.BOLD, 16));
        comingSoonBtn.setPreferredSize(new Dimension(220, 50));
        comingSoonBtn.addActionListener(e -> {
            refreshComingSoonList();
            userSubCardLayout.show(userSubCardPanel, "coming_soon_page");
        });

        JButton viewAllBtn = new JButton("3. View All Movies");
        viewAllBtn.setFont(new Font("SansSerif", Font.BOLD, 16));
        viewAllBtn.setPreferredSize(new Dimension(220, 50));
        viewAllBtn.addActionListener(e -> {
            refreshViewAllMoviesList();
            userSubCardLayout.show(userSubCardPanel, "view_all_movies_page");
        });

        card.add(title); 
        card.add(nowShowingBtn); 
        card.add(comingSoonBtn);
        card.add(viewAllBtn);

        panel.add(card);
        return panel;
    }

    private JPanel buildComingSoonPage() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JButton back = new JButton("< Back");
        back.addActionListener(e -> userSubCardLayout.show(userSubCardPanel, "category_select"));
        panel.add(back, BorderLayout.NORTH);

        comingSoonListModel = new DefaultListModel<>();
        comingSoonJList = new JList<>(comingSoonListModel);
        styleMovieList(comingSoonJList);
        comingSoonJList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) updateComingSoonPreview();
        });
        JScrollPane listPane = new JScrollPane(comingSoonJList);
        listPane.setPreferredSize(new Dimension(300, 320));

        comingSoonImageLabel = new JLabel("Select a movie to preview", SwingConstants.CENTER);
        comingSoonImageLabel.setPreferredSize(new Dimension(240, 320));
        comingSoonImageLabel.setBorder(BorderFactory.createLineBorder(Color.GRAY));

        JPanel center = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.gridx = 0; gbc.gridy = 0; center.add(listPane, gbc);
        gbc.gridx = 1; gbc.gridy = 0; center.add(comingSoonImageLabel, gbc);
        panel.add(center, BorderLayout.CENTER);

        JLabel note = new JLabel("Booking opens once these movies start showing.", SwingConstants.CENTER);
        panel.add(note, BorderLayout.SOUTH);
        return panel;
    }

    private void refreshComingSoonList() {
        comingSoonListModel.clear();
        for (Movie m : bookingService.getMovieList()) {
            if (Movie.COMING_SOON.equals(m.getCategory())) comingSoonListModel.addElement(m);
        }
        if (!comingSoonListModel.isEmpty()) {
            comingSoonJList.setSelectedIndex(0);
        } else {
            comingSoonImageLabel.setIcon(null);
            comingSoonImageLabel.setText("No upcoming movies yet");
        }
    }

    private void updateComingSoonPreview() {
        setPreviewImage(comingSoonImageLabel, comingSoonJList.getSelectedValue());
    }

    // ================= VIEW ALL MOVIES PAGE (UPDATED) =================
    private JPanel buildViewAllMoviesPage() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JButton back = new JButton("< Back");
        back.addActionListener(e -> userSubCardLayout.show(userSubCardPanel, "category_select"));
        panel.add(back, BorderLayout.NORTH);

        viewAllListModel = new DefaultListModel<>();
        viewAllJList = new JList<>(viewAllListModel);
        styleMovieList(viewAllJList);

        // প্রতিটি সারিতে পোস্টার, নাম ও রেটিং দেখানোর জন্য কাস্টম রেন্ডারার
        viewAllJList.setFixedCellHeight(76);
        viewAllJList.setCellRenderer(new ListCellRenderer<Movie>() {
            private final JPanel rowPanel = new JPanel(new BorderLayout(10, 0));
            private final JLabel posterLabel = new JLabel("", SwingConstants.CENTER);
            private final JLabel infoLabel = new JLabel();
            {
                posterLabel.setPreferredSize(new Dimension(50, 68));
                posterLabel.setOpaque(false);
                rowPanel.setBorder(new EmptyBorder(4, 6, 4, 6));
                rowPanel.add(posterLabel, BorderLayout.WEST);
                rowPanel.add(infoLabel, BorderLayout.CENTER);
                rowPanel.setOpaque(true);
            }

            @Override
            public Component getListCellRendererComponent(JList<? extends Movie> list, Movie m, int index,
                                                            boolean isSelected, boolean cellHasFocus) {
                if (m != null) {
                    posterLabel.setIcon(loadThumbnailIcon(m.getImagePath(), 50, 68));
                    posterLabel.setText(posterLabel.getIcon() == null ? "No Img" : "");

                    double avg = bookingService.getAverageRating(m.getMovieName());
                    String ratingText = avg > 0 ? String.format("\u2605 %.1f / 5", avg) : "No ratings yet";
                    String color = isSelected ? "#000000" : "#DDDDDD";
                    infoLabel.setText("<html><body style='width:180px'>"
                            + "<b style='color:" + color + "'>" + m.getMovieName() + "</b><br>"
                            + "<span style='color:" + color + "'>" + ratingText + "</span>"
                            + (m.isBlockbuster() ? "<br><span style='color:#FFD700'>BLOCKBUSTER</span>" : "")
                            + "</body></html>");
                }
                rowPanel.setBackground(isSelected ? list.getSelectionBackground() : list.getBackground());
                return rowPanel;
            }
        });

        viewAllJList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) updateViewAllPreview();
        });
        JScrollPane listPane = new JScrollPane(viewAllJList);
        listPane.setPreferredSize(new Dimension(300, 320));

        viewAllImageLabel = new JLabel("Select a movie", SwingConstants.CENTER);
        viewAllImageLabel.setPreferredSize(new Dimension(240, 320));
        viewAllImageLabel.setBorder(BorderFactory.createLineBorder(Color.GRAY));

        JPanel center = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.gridx = 0; gbc.gridy = 0; center.add(listPane, gbc);
        gbc.gridx = 1; gbc.gridy = 0; center.add(viewAllImageLabel, gbc);
        panel.add(center, BorderLayout.CENTER);

        return panel;
    }

    private void refreshViewAllMoviesList() {
        viewAllListModel.clear();
        for (Movie m : bookingService.getMovieList()) {
            viewAllListModel.addElement(m);
        }
        if (!viewAllListModel.isEmpty()) {
            viewAllJList.setSelectedIndex(0);
        } else {
            viewAllImageLabel.setIcon(null);
            viewAllImageLabel.setText("No movies available");
        }
    }

    private void updateViewAllPreview() {
        Movie m = viewAllJList.getSelectedValue();
        if (m != null) {
            setPreviewImage(viewAllImageLabel, m);
        } else {
            viewAllImageLabel.setIcon(null);
            viewAllImageLabel.setText("Select a movie");
        }
    }

    private JPanel buildBrowseNowShowingPage() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel top = new JPanel(new BorderLayout());
        JButton back = new JButton("< Back");
        back.addActionListener(e -> userSubCardLayout.show(userSubCardPanel, "category_select"));

        JPanel datePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        String[] dates = new String[3];
        SimpleDateFormat dFormat = new SimpleDateFormat("dd-MM-yyyy");
        Calendar cal = Calendar.getInstance();
        dates[0] = dFormat.format(cal.getTime()) + " (Today)";
        cal.add(Calendar.DATE, 1); dates[1] = dFormat.format(cal.getTime());
        cal.add(Calendar.DATE, 1); dates[2] = dFormat.format(cal.getTime());
        dateCombo = new JComboBox<>(dates);
        datePanel.add(new JLabel("Select Date: "));
        datePanel.add(dateCombo);

        JTextField searchField = new JTextField(14);
        JButton searchBtn = new JButton("Search");
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchPanel.add(new JLabel("Search:"));
        searchPanel.add(searchField);
        searchPanel.add(searchBtn);
        Runnable doSearch = () -> {
            allMoviesListModel.clear();
            for (Movie m : bookingService.searchMovies(searchField.getText().trim())) {
                if (Movie.NOW_SHOWING.equals(m.getCategory())) allMoviesListModel.addElement(m);
            }
        };
        searchBtn.addActionListener(e -> doSearch.run());
        searchField.addActionListener(e -> doSearch.run());

        top.add(back, BorderLayout.WEST);
        top.add(searchPanel, BorderLayout.CENTER);
        top.add(datePanel, BorderLayout.EAST);
        panel.add(top, BorderLayout.NORTH);

        allMoviesListModel = new DefaultListModel<>();
        allMoviesJList = new JList<>(allMoviesListModel);
        styleMovieList(allMoviesJList);

        allMoviesJList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && allMoviesJList.getSelectedValue() != null) {
                onMovieChosenInBrowse(allMoviesJList.getSelectedValue());
            }
        });

        JPanel movieListBox = new JPanel(new BorderLayout());
        movieListBox.setBorder(BorderFactory.createTitledBorder("Now Showing Movies"));
        movieListBox.add(new JScrollPane(allMoviesJList), BorderLayout.CENTER);
        movieListBox.setPreferredSize(new Dimension(500, 160));

        movieSelectImageLabel = new JLabel("Select a movie", SwingConstants.CENTER);
        movieSelectImageLabel.setPreferredSize(new Dimension(240, 320));
        movieSelectImageLabel.setBorder(BorderFactory.createLineBorder(Color.GRAY));

        timeCombo = new JComboBox<>();

        JPanel timePanel = new JPanel(new GridLayout(0, 1, 5, 8));
        timePanel.add(new JLabel("Select Show Time:"));
        timePanel.add(timeCombo);
        browseMoviePriceLabel = new JLabel(" ");
        timePanel.add(browseMoviePriceLabel);

        JPanel detailsPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 10, 8, 10); gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = 0; gbc.gridy = 0; detailsPanel.add(movieSelectImageLabel, gbc);
        gbc.gridx = 1; gbc.gridy = 0; detailsPanel.add(timePanel, gbc);

        JPanel center = new JPanel(new BorderLayout(10, 10));
        center.add(movieListBox, BorderLayout.NORTH);
        center.add(detailsPanel, BorderLayout.CENTER);
        panel.add(center, BorderLayout.CENTER);

        JButton proceedBtn = new JButton("Proceed to Seat Selection ->");
        proceedBtn.setPreferredSize(new Dimension(0, 45));
        proceedBtn.addActionListener(e -> {
            Movie chosen = allMoviesJList.getSelectedValue();
            if (chosen == null || timeCombo.getSelectedItem() == null) {
                JOptionPane.showMessageDialog(this, "Please select a movie and show time first.");
                return;
            }
            selectedMovieForBooking = chosen;
            chosenTime = (String) timeCombo.getSelectedItem();
            chosenDate = ((String) dateCombo.getSelectedItem()).split(" ")[0];

            assignedHallName = bookingService.getAssignedHall(selectedMovieForBooking, chosenDate, chosenTime);

            currentMovieTitleLabel.setText(selectedMovieForBooking.getMovieName() + "   |   " +
                    assignedHallName + "   |   " + chosenDate + "  " + chosenTime);
            setPreviewImage(seatMovieImageLabel, selectedMovieForBooking);
            refreshSeatGrid();
            userSubCardLayout.show(userSubCardPanel, "seat_selection_page");
        });
        panel.add(proceedBtn, BorderLayout.SOUTH);
        return panel;
    }

    private void styleMovieList(JList<Movie> list) {
        list.setBackground(new Color(23, 48, 88));
        list.setForeground(Color.WHITE);
    }

    private void onMovieChosenInBrowse(Movie m) {
        setPreviewImage(movieSelectImageLabel, m);
        timeCombo.removeAllItems();
        if (m != null && m.getShowTimes() != null) {
            for (String t : m.getShowTimes()) {
                timeCombo.addItem(t);
            }
        }
        browseMoviePriceLabel.setText("<html>Regular: Rs." + m.getTicketPrice() +
                "<br>Premium: Rs." + m.getSeatPrice("D1") + "</html>");
    }

    private void refreshBrowsePage() {
        allMoviesListModel.clear();
        for (Movie m : bookingService.getMovieList()) {
            if (Movie.NOW_SHOWING.equals(m.getCategory())) {
                allMoviesListModel.addElement(m);
            }
        }
        movieSelectImageLabel.setIcon(null);
        movieSelectImageLabel.setText("Select a movie");
        timeCombo.removeAllItems();
        browseMoviePriceLabel.setText(" ");
    }

    private JPanel buildSeatSelectionPage() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        JPanel top = new JPanel(new BorderLayout());
        currentMovieTitleLabel = new JLabel("Booking", SwingConstants.CENTER);
        JButton back = new JButton("< Back");
        back.addActionListener(e -> userSubCardLayout.show(userSubCardPanel, "browse_now_showing"));
        JButton reviewsBtn = new JButton("Reviews & Ratings");
        reviewsBtn.addActionListener(e -> {
            if (selectedMovieForBooking != null) openReviewsDialog(selectedMovieForBooking);
        });
        top.add(back, BorderLayout.WEST); top.add(currentMovieTitleLabel, BorderLayout.CENTER);
        top.add(reviewsBtn, BorderLayout.EAST);
        panel.add(top, BorderLayout.NORTH);

        seatMovieImageLabel = new JLabel("No Image", SwingConstants.CENTER);
        seatMovieImageLabel.setPreferredSize(new Dimension(200, 260));
        seatMovieImageLabel.setBorder(BorderFactory.createLineBorder(Color.GRAY));

        JPanel seatGrid = new JPanel(new GridLayout(ROWS.length, SEATS_PER_ROW, 6, 6));
        seatButtons = new JButton[ROWS.length][SEATS_PER_ROW];
        for (int r = 0; r < ROWS.length; r++) {
            for (int c = 0; c < SEATS_PER_ROW; c++) {
                String lbl = ROWS[r] + (c + 1);
                JButton btn = new JButton(lbl);
                btn.addActionListener(ev -> toggleSeatSelection(lbl, btn));
                seatButtons[r][c] = btn; seatGrid.add(btn);
            }
        }

        JPanel legend = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 5));
        legend.add(legendChip("Regular", AVAILABLE_REGULAR));
        legend.add(legendChip("Premium", AVAILABLE_PREMIUM));
        legend.add(legendChip("Selected", SELECTED_COLOR));
        legend.add(legendChip("Booked", BOOKED_COLOR));

        JPanel seatArea = new JPanel(new BorderLayout(0, 10));
        seatArea.add(seatGrid, BorderLayout.CENTER);
        seatArea.add(legend, BorderLayout.SOUTH);

        JPanel mainCenter = new JPanel(new BorderLayout(15, 0));
        mainCenter.add(seatMovieImageLabel, BorderLayout.WEST); mainCenter.add(seatArea, BorderLayout.CENTER);
        panel.add(mainCenter, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new GridLayout(3, 1, 5, 5));
        bookingStatusLabel = new JLabel(" ");
        JButton book = new JButton("Confirm & Pay");
        book.addActionListener(e -> {
            if (selectedMovieForBooking == null || selectedSeats.isEmpty()) {
                bookingStatusLabel.setText("Please select at least one seat.");
                return;
            }
            openPaymentDialog();
        });
        JButton waitlistBtn = new JButton("Hall Full? Join Waitlist");
        waitlistBtn.addActionListener(e -> openWaitlistDialog());
        bottom.add(bookingStatusLabel); bottom.add(book); bottom.add(waitlistBtn);
        panel.add(bottom, BorderLayout.SOUTH);
        return panel;
    }

    // ================= WAITLIST DIALOG =================
    private void openWaitlistDialog() {
        if (selectedMovieForBooking == null || loggedInUser == null) return;
        JPanel form = new JPanel(new GridLayout(0, 1, 5, 8));
        form.setBorder(new EmptyBorder(10, 10, 10, 10));
        JLabel info = new JLabel("<html>" + selectedMovieForBooking.getMovieName() + "<br>" +
                assignedHallName + " | " + chosenDate + " " + chosenTime + "</html>");
        JSpinner seatsWantedSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 10, 1));
        form.add(info);
        form.add(new JLabel("Seats wanted:"));
        form.add(seatsWantedSpinner);

        int choice = JOptionPane.showConfirmDialog(this, form, "Join Waitlist",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (choice == JOptionPane.OK_OPTION) {
            int wanted = (Integer) seatsWantedSpinner.getValue();
            bookingService.addToWaitlist(loggedInUser.getUserName(), selectedMovieForBooking.getMovieName(),
                    assignedHallName, chosenTime, chosenDate, wanted);
            JOptionPane.showMessageDialog(this,
                    "Added to waitlist! You'll be notified when " + wanted + " seat(s) free up.");
        }
    }

    // ================= REVIEWS & RATINGS DIALOG =================
    private void openReviewsDialog(Movie movie) {
        JDialog dialog = new JDialog(this, "Reviews - " + movie.getMovieName(), true);
        dialog.setSize(480, 500);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(8, 8));
        ((JPanel) dialog.getContentPane()).setBorder(new EmptyBorder(12, 15, 12, 15));

        double avg = bookingService.getAverageRating(movie.getMovieName());
        JLabel avgLabel = new JLabel(String.format("Average Rating: %.1f / 5", avg), SwingConstants.CENTER);
        avgLabel.setFont(new Font("SansSerif", Font.BOLD, 15));
        dialog.add(avgLabel, BorderLayout.NORTH);

        DefaultListModel<String> listModel = new DefaultListModel<>();
        for (Review r : bookingService.getReviewsForMovie(movie.getMovieName())) {
            listModel.addElement("[" + r.getRating() + "/5] " + r.getUserName() + ": " +
                    (r.getComment() == null || r.getComment().isEmpty() ? "(no comment)" : r.getComment()) +
                    "  (" + r.getReviewDate() + ")");
        }
        JList<String> reviewList = new JList<>(listModel);
        dialog.add(new JScrollPane(reviewList), BorderLayout.CENTER);

        JPanel addPanel = new JPanel(new BorderLayout(5, 5));
        JSpinner ratingSpinner = new JSpinner(new SpinnerNumberModel(5, 1, 5, 1));
        JTextField commentField = new JTextField();
        JButton submitBtn = new JButton("Submit Review");
        JPanel ratingRow = new JPanel(new BorderLayout(5, 0));
        ratingRow.add(new JLabel("Rating (1-5):"), BorderLayout.WEST);
        ratingRow.add(ratingSpinner, BorderLayout.CENTER);
        addPanel.add(ratingRow, BorderLayout.NORTH);
        addPanel.add(commentField, BorderLayout.CENTER);
        addPanel.add(submitBtn, BorderLayout.EAST);

        submitBtn.addActionListener(e -> {
            if (loggedInUser == null) return;
            int rating = (Integer) ratingSpinner.getValue();
            bookingService.addReview(movie.getMovieName(), loggedInUser.getUserName(), rating, commentField.getText().trim());
            listModel.clear();
            for (Review r : bookingService.getReviewsForMovie(movie.getMovieName())) {
                listModel.addElement("[" + r.getRating() + "/5] " + r.getUserName() + ": " +
                        (r.getComment() == null || r.getComment().isEmpty() ? "(no comment)" : r.getComment()) +
                        "  (" + r.getReviewDate() + ")");
            }
            avgLabel.setText(String.format("Average Rating: %.1f / 5", bookingService.getAverageRating(movie.getMovieName())));
            commentField.setText("");
        });

        dialog.add(addPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private JPanel legendChip(String label, Color color) {
        JPanel chip = new JPanel();
        chip.setBackground(new Color(10, 28, 58));
        JLabel swatch = new JLabel("  ");
        swatch.setOpaque(true);
        swatch.setBackground(color);
        swatch.setPreferredSize(new Dimension(18, 18));
        JLabel text = new JLabel(" " + label);
        text.setForeground(Color.WHITE);
        chip.add(swatch); chip.add(text);
        return chip;
    }

    private void toggleSeatSelection(String lbl, JButton btn) {
        if (selectedMovieForBooking == null || !btn.isEnabled()) return;
        if (selectedSeats.contains(lbl)) {
            selectedSeats.remove(lbl);
            btn.setBackground(selectedMovieForBooking.isPremiumSeat(lbl) ? AVAILABLE_PREMIUM : AVAILABLE_REGULAR);
        } else {
            selectedSeats.add(lbl);
            btn.setBackground(SELECTED_COLOR);
        }
        updateSeatSelectionSummary();
    }

    private void updateSeatSelectionSummary() {
        if (selectedSeats.isEmpty()) {
            bookingStatusLabel.setText("Select one or more seats.");
            return;
        }
        double total = 0;
        for (String s : selectedSeats) total += selectedMovieForBooking.getSeatPrice(s);
        bookingStatusLabel.setText("Selected: " + String.join(", ", selectedSeats) + "    Total: Rs." + total);
    }

    private void refreshSeatGrid() {
        selectedSeats.clear();
        bookingStatusLabel.setText("Select one or more seats.");
        if (selectedMovieForBooking == null) return;
        Set<String> booked = bookingService.getBookedSeatsForHall(selectedMovieForBooking, chosenDate, chosenTime, assignedHallName);
        for (int r = 0; r < ROWS.length; r++) {
            for (int c = 0; c < SEATS_PER_ROW; c++) {
                String label = ROWS[r] + (c + 1);
                JButton btn = seatButtons[r][c];
                boolean isBooked = booked.contains(label);
                btn.setEnabled(!isBooked);
                if (isBooked) {
                    btn.setBackground(BOOKED_COLOR);
                } else {
                    btn.setBackground(selectedMovieForBooking.isPremiumSeat(label) ? AVAILABLE_PREMIUM : AVAILABLE_REGULAR);
                }
            }
        }
    }

    private void openPaymentDialog() {
        double seatTotal = 0;
        StringBuilder seatListSb = new StringBuilder();
        for (String s : selectedSeats) {
            seatTotal += selectedMovieForBooking.getSeatPrice(s);
            if (seatListSb.length() > 0) seatListSb.append(", ");
            seatListSb.append(s).append(selectedMovieForBooking.isPremiumSeat(s) ? "(P)" : "(R)");
        }
        final double seatAmount = seatTotal;
        final String seatListStr = seatListSb.toString();
        final int seatCount = selectedSeats.size();
        final List<String> seatsSnapshot = new ArrayList<>(selectedSeats);

        JDialog dialog = new JDialog(this, "Payment", true);
        dialog.setSize(440, 400);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(8, 8));
        ((JPanel) dialog.getContentPane()).setBorder(new EmptyBorder(15, 20, 15, 20));

        JPanel top = new JPanel(new GridLayout(0, 1, 6, 6));
        JLabel summary = new JLabel("<html>" + selectedMovieForBooking.getMovieName() + "<br>" +
                assignedHallName + "<br>Seats: " + seatListStr +
                "<br>" + chosenDate + " &nbsp; " + chosenTime + "</html>");
        top.add(summary);

        JLabel amountLabel = new JLabel();
        amountLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        top.add(amountLabel);

        final double[] finalAmountHolder = {seatAmount};
        amountLabel.setText("<html><b>Amount Payable: Rs." + seatAmount + "</b></html>");

        JComboBox<String> methodCombo = new JComboBox<>(new String[]{"bKash", "Nagad", "Cash on Counter"});
        JLabel mobileLabel = new JLabel("bKash/Nagad Number:");
        JTextField mobileField = new JTextField();
        if (loggedInUser != null && loggedInUser.getPhoneNumber() != null && !loggedInUser.getPhoneNumber().equals("N/A")) {
            mobileField.setText(loggedInUser.getPhoneNumber());
        }
        top.add(new JLabel("Payment Method:"));
        top.add(methodCombo);
        top.add(mobileLabel);
        top.add(mobileField);

        methodCombo.addActionListener(e -> {
            boolean needsMobile = !"Cash on Counter".equals(methodCombo.getSelectedItem());
            mobileField.setEnabled(needsMobile);
            mobileLabel.setVisible(needsMobile);
            mobileField.setVisible(needsMobile);
        });

        dialog.add(top, BorderLayout.CENTER);

        JLabel payStatusLabel = new JLabel(" ", SwingConstants.CENTER);
        JButton payBtn = new JButton("Pay Now");
        JPanel bottom = new JPanel(new GridLayout(2, 1, 4, 4));
        bottom.add(payBtn);
        bottom.add(payStatusLabel);
        dialog.add(bottom, BorderLayout.SOUTH);

        payBtn.addActionListener(ev -> {
            String method = (String) methodCombo.getSelectedItem();
            String mobile = mobileField.getText().trim();
            if (!"Cash on Counter".equals(method)) {
                if (mobile.length() != 11 || !mobile.startsWith("01")) {
                    payStatusLabel.setText("Enter a valid 11-digit number (e.g. 01XXXXXXXXX).");
                    return;
                }
            } else {
                mobile = "-";
            }
            final String mobileFinal = mobile;
            final double finalAmount = finalAmountHolder[0];

            payBtn.setEnabled(false);
            methodCombo.setEnabled(false);
            mobileField.setEnabled(false);
            payStatusLabel.setText("Processing payment...");

            Payment payment = new Payment(0, loggedInUser.getUserName(), selectedMovieForBooking.getMovieName(),
                    finalAmount, seatCount, seatListStr, method, mobileFinal, BookingService.currentTimestamp());

            SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
                @Override
                protected Boolean doInBackground() {
                    return payment.processPayment();
                }

                @Override
                protected void done() {
                    boolean success;
                    try {
                        success = get();
                    } catch (Exception ex) {
                        success = false;
                    }

                    if (success) {
                        List<Booking> created = bookingService.bookSeats(selectedMovieForBooking,
                                seatsSnapshot, loggedInUser.getUserName(), chosenTime, chosenDate, assignedHallName);
                        if (!created.isEmpty()) {
                            int groupId = created.get(0).getGroupId();
                            payment.setGroupId(groupId);
                            bookingService.savePaymentRecord(payment);
                            String receipt = buildReceiptText(created, payment);
                            bookingService.saveReceiptFile(receipt, groupId);
                            dialog.dispose();
                            showReceiptDialog(receipt);
                            refreshSeatGrid();
                            refreshUserBookingsTable();
                        } else {
                            payStatusLabel.setText("Some seats were just taken. Please choose again.");
                            payBtn.setEnabled(true);
                            methodCombo.setEnabled(true);
                            mobileField.setEnabled(true);
                        }
                    } else {
                        payStatusLabel.setText("Payment failed. Please try again.");
                        payBtn.setEnabled(true);
                        methodCombo.setEnabled(true);
                        mobileField.setEnabled(true);
                    }
                }
            };
            worker.execute();
        });

        dialog.setVisible(true);
    }

    private String buildReceiptText(List<Booking> bookings, Payment payment) {
        StringBuilder sb = new StringBuilder();
        sb.append("========== PAYMENT RECEIPT ==========\n");
        sb.append("Order ID       : ").append(payment.getGroupId()).append("\n");
        sb.append("Customer       : ").append(payment.getUserName()).append("\n");
        sb.append("Movie          : ").append(payment.getMovieName()).append("\n");
        sb.append("Hall           : ").append(assignedHallName).append("\n");
        sb.append("Date / Time    : ").append(chosenDate).append("  ").append(chosenTime).append("\n");
        sb.append("--------------------------------------\n");
        for (Booking b : bookings) {
            sb.append("Seat ").append(b.getSeatNumber()).append(" (").append(b.getSeatType())
                    .append(")   Rs.").append(b.getAmount()).append("\n");
        }
        sb.append("--------------------------------------\n");
        sb.append("Total Amount   : Rs.").append(payment.getAmount()).append("\n");
        sb.append("Payment Method : ").append(payment.getMethod()).append("\n");
        if (!"Cash on Counter".equals(payment.getMethod())) {
            sb.append("Mobile Number  : ").append(payment.getMobileNumber()).append("\n");
        }
        sb.append("Transaction ID : ").append(payment.getTransactionId()).append("\n");
        sb.append("Payment Status : ").append(payment.getStatus()).append("\n");
        sb.append("Paid On        : ").append(payment.getPaymentDate()).append("\n");
        sb.append("======================================\n");
        sb.append("        Thank you for booking!\n");
        return sb.toString();
    }

    private void showReceiptDialog(String receiptText) {
        JDialog dialog = new JDialog(this, "Booking Receipt", true);
        dialog.setSize(440, 440);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());

        JTextArea area = new JTextArea(receiptText);
        area.setEditable(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        area.setBackground(new Color(18, 42, 80));
        area.setForeground(Color.WHITE);
        dialog.add(new JScrollPane(area), BorderLayout.CENTER);

        JButton okBtn = new JButton("Done");
        okBtn.addActionListener(e -> dialog.dispose());
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bottom.add(okBtn);
        dialog.add(bottom, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }

    // Small poster thumbnails used inside list rows (e.g. View All Movies)
    private final java.util.Map<String, ImageIcon> thumbnailCache = new java.util.HashMap<>();

    private ImageIcon loadThumbnailIcon(String imagePath, int w, int h) {
        if (imagePath == null || imagePath.isEmpty()) return null;
        String key = imagePath + "@" + w + "x" + h;
        if (thumbnailCache.containsKey(key)) return thumbnailCache.get(key);
        File f = new File(imagePath);
        ImageIcon result = null;
        if (f.exists()) {
            ImageIcon icon = new ImageIcon(imagePath);
            Image img = icon.getImage().getScaledInstance(w, h, Image.SCALE_SMOOTH);
            result = new ImageIcon(img);
        }
        thumbnailCache.put(key, result);
        return result;
    }

    private void setPreviewImage(JLabel label, Movie m) {
        if (m != null && m.getImagePath() != null && !m.getImagePath().isEmpty()) {
            File f = new File(m.getImagePath());
            if (f.exists()) {
                int w = label.getPreferredSize().width > 0 ? label.getPreferredSize().width : 240;
                int h = label.getPreferredSize().height > 0 ? label.getPreferredSize().height : 320;
                ImageIcon icon = new ImageIcon(m.getImagePath());
                Image img = icon.getImage().getScaledInstance(w, h, Image.SCALE_SMOOTH);
                label.setIcon(new ImageIcon(img));
                label.setText("");
                return;
            }
        }
        label.setIcon(null);
        label.setText("No Image Available");
    }

    private JPanel buildUserBookingsTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        userBookingsModel = new DefaultTableModel(
                new Object[]{"ID", "Order", "Movie", "Hall", "Seat", "Type", "Time", "Date", "Price", "Status"}, 0);
        userBookingsTable = new JTable(userBookingsModel);
        panel.add(new JScrollPane(userBookingsTable), BorderLayout.CENTER);

        JButton cancelSeatBtn = new JButton("Cancel Selected Seat");
        cancelSeatBtn.addActionListener(e -> {
            int row = userBookingsTable.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(this, "Select a booking row first.");
                return;
            }
            int bookingId = (int) userBookingsModel.getValueAt(row, 0);
            String status = (String) userBookingsModel.getValueAt(row, 9);
            if (!status.equalsIgnoreCase("Confirmed")) {
                JOptionPane.showMessageDialog(this, "This seat is already cancelled.");
                return;
            }
            int confirm = JOptionPane.showConfirmDialog(this, "Cancel this seat?", "Confirm Cancellation",
                    JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                if (bookingService.cancelBooking(bookingId)) {
                    JOptionPane.showMessageDialog(this, "Seat Cancelled.");
                    refreshUserBookingsTable();
                }
            }
        });

        JButton cancelOrderBtn = new JButton("Cancel Entire Order");
        cancelOrderBtn.addActionListener(e -> {
            int row = userBookingsTable.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(this, "Select a booking row first.");
                return;
            }
            int groupId = (int) userBookingsModel.getValueAt(row, 1);
            int confirm = JOptionPane.showConfirmDialog(this, "Cancel the whole order (all seats)?",
                    "Confirm Cancellation", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                int count = bookingService.cancelGroup(groupId);
                if (count > 0) {
                    JOptionPane.showMessageDialog(this, count + " seat(s) cancelled.");
                    refreshUserBookingsTable();
                }
            }
        });

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.add(cancelSeatBtn); bottom.add(cancelOrderBtn);
        panel.add(bottom, BorderLayout.SOUTH);
        return panel;
    }

    private void refreshUserBookingsTable() {
        userBookingsModel.setRowCount(0);
        for (Booking b : bookingService.getBookingList()) {
            if (b.getUserName().equalsIgnoreCase(loggedInUser.getUserName())) {
                userBookingsModel.addRow(new Object[]{ b.getBookingId(), b.getGroupId(), b.getMovieName(),
                        b.getHallName(), b.getSeatNumber(), b.getSeatType(), b.getShowTime(), b.getBookingDate(),
                        b.getAmount(), b.getStatus() });
            }
        }
    }

    // ================= ADMIN PANEL =================
    private JPanel buildAdminMainPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JTabbedPane tabs = new JTabbedPane();

        tabs.addTab("Manage Movies", buildAdminManageMoviesTab());
        tabs.addTab("All Reservations", buildAdminBookingsTab());
        tabs.addTab("All Payments", buildAdminPaymentsTab());
        tabs.addTab("Waitlist", buildAdminWaitlistTab());
        tabs.addTab("Dashboard", buildAdminDashboardTab());

        JButton logout = new JButton("Sign Out");
        logout.addActionListener(e -> cardLayout.show(cardPanel, "login"));
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bar.setBackground(new Color(10, 28, 58)); bar.add(logout);
        panel.add(bar, BorderLayout.NORTH); panel.add(tabs, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildAdminManageMoviesTab() {
        JPanel managePanel = new JPanel(new BorderLayout(10, 10));
        managePanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        adminMoviesModel = new DefaultTableModel(
                new Object[]{"ID", "Title", "Category", "Price", "Blockbuster", "Show Times"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        JTable table = new JTable(adminMoviesModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) loadSelectedMovieIntoForm(table);
        });
        managePanel.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel form = new JPanel(new GridLayout(0, 1, 5, 5));
        form.setBorder(new EmptyBorder(10, 10, 10, 10));
        adminMovieNameField = new JTextField();
        adminMoviePriceField = new JTextField();
        adminMovieTimesField = new JTextField();
        adminMovieCategoryCombo = new JComboBox<>(new String[]{Movie.NOW_SHOWING, Movie.COMING_SOON});
        adminMovieBlockbusterCheck = new JCheckBox("Blockbuster Movie");

        JButton uploadImgBtn = new JButton("Choose Picture from PC");
        adminImagePreviewLabel = new JLabel("No Image Selected", SwingConstants.CENTER);
        adminImagePreviewLabel.setPreferredSize(new Dimension(100, 120));
        adminImagePreviewLabel.setBorder(BorderFactory.createLineBorder(Color.GRAY));

        uploadImgBtn.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileFilter(new FileNameExtensionFilter("Image Files", "jpg", "jpeg", "png"));
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                File selectedFile = chooser.getSelectedFile();
                File dir = new File("movie_images");
                if (!dir.exists()) dir.mkdir();
                File dest = new File(dir, System.currentTimeMillis() + "_" + selectedFile.getName());
                try {
                    Files.copy(selectedFile.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    selectedAdminImagePath = dest.getAbsolutePath();
                    ImageIcon icon = new ImageIcon(selectedAdminImagePath);
                    Image img = icon.getImage().getScaledInstance(100, 120, Image.SCALE_SMOOTH);
                    adminImagePreviewLabel.setIcon(new ImageIcon(img));
                    adminImagePreviewLabel.setText("");
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
        });

        JButton addMovieBtn = new JButton("Add New Movie");
        addMovieBtn.setBackground(new Color(46, 139, 87));
        addMovieBtn.addActionListener(e -> {
            String name = adminMovieNameField.getText().trim();
            String priceStr = adminMoviePriceField.getText().trim();
            String timesStr = adminMovieTimesField.getText().trim();
            if (name.isEmpty() || priceStr.isEmpty() || timesStr.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Title, price, and show times are required.");
                return;
            }
            try {
                double price = Double.parseDouble(priceStr);
                String[] times = timesStr.split(",");
                for (int i = 0; i < times.length; i++) times[i] = times[i].trim();

                bookingService.addMovie(name, price, selectedAdminImagePath,
                        adminMovieBlockbusterCheck.isSelected(), (String) adminMovieCategoryCombo.getSelectedItem(), times);
                JOptionPane.showMessageDialog(this, "Movie Added Successfully!");
                refreshAdminMoviesTable();
                clearAdminMovieForm();
            } catch (NumberFormatException nfe) {
                JOptionPane.showMessageDialog(this, "Enter a valid price.");
            }
        });

        JButton updateMovieBtn = new JButton("Update Selected Movie");
        updateMovieBtn.addActionListener(e -> {
            if (selectedAdminMovieId == -1) {
                JOptionPane.showMessageDialog(this, "Select a movie from the table first.");
                return;
            }
            String name = adminMovieNameField.getText().trim();
            String priceStr = adminMoviePriceField.getText().trim();
            String timesStr = adminMovieTimesField.getText().trim();
            if (name.isEmpty() || priceStr.isEmpty() || timesStr.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Title, price, and show times are required.");
                return;
            }
            try {
                double price = Double.parseDouble(priceStr);
                String[] times = timesStr.split(",");
                for (int i = 0; i < times.length; i++) times[i] = times[i].trim();

                boolean ok = bookingService.updateMovie(selectedAdminMovieId, name, price, selectedAdminImagePath,
                        adminMovieBlockbusterCheck.isSelected(), (String) adminMovieCategoryCombo.getSelectedItem(), times);
                if (ok) {
                    JOptionPane.showMessageDialog(this, "Movie Updated Successfully!");
                    refreshAdminMoviesTable();
                    clearAdminMovieForm();
                }
            } catch (NumberFormatException nfe) {
                JOptionPane.showMessageDialog(this, "Enter a valid price.");
            }
        });

        JButton deleteMovieBtn = new JButton("Delete Selected Movie");
        deleteMovieBtn.setBackground(new Color(178, 34, 34));
        deleteMovieBtn.addActionListener(e -> {
            if (selectedAdminMovieId == -1) {
                JOptionPane.showMessageDialog(this, "Select a movie from the table first.");
                return;
            }
            int confirm = JOptionPane.showConfirmDialog(this, "Delete this movie?", "Confirm Delete",
                    JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                bookingService.deleteMovie(selectedAdminMovieId);
                refreshAdminMoviesTable();
                clearAdminMovieForm();
            }
        });

        form.add(new JLabel("Movie Title:")); form.add(adminMovieNameField);
        form.add(new JLabel("Ticket Price (Regular seat):")); form.add(adminMoviePriceField);
        form.add(new JLabel("Show Times (comma separated, e.g. 10:00 PM, 06:00 PM):")); form.add(adminMovieTimesField);
        form.add(new JLabel("Category:")); form.add(adminMovieCategoryCombo);
        form.add(adminMovieBlockbusterCheck);
        form.add(uploadImgBtn); form.add(adminImagePreviewLabel);
        form.add(addMovieBtn); form.add(updateMovieBtn); form.add(deleteMovieBtn);

        managePanel.add(form, BorderLayout.EAST);
        return managePanel;
    }

    private void loadSelectedMovieIntoForm(JTable table) {
        int row = table.getSelectedRow();
        if (row == -1) { selectedAdminMovieId = -1; return; }
        int id = (int) adminMoviesModel.getValueAt(row, 0);
        Movie m = bookingService.getMovieById(id);
        if (m == null) return;
        selectedAdminMovieId = id;
        adminMovieNameField.setText(m.getMovieName());
        adminMoviePriceField.setText(String.valueOf(m.getTicketPrice()));
        adminMovieTimesField.setText(String.join(", ", m.getShowTimes()));
        adminMovieCategoryCombo.setSelectedItem(m.getCategory());
        adminMovieBlockbusterCheck.setSelected(m.isBlockbuster());
        selectedAdminImagePath = m.getImagePath();
        setPreviewImage(adminImagePreviewLabel, m);
    }

    private void clearAdminMovieForm() {
        selectedAdminMovieId = -1;
        adminMovieNameField.setText(""); adminMoviePriceField.setText(""); adminMovieTimesField.setText("");
        adminMovieBlockbusterCheck.setSelected(false);
        adminMovieCategoryCombo.setSelectedIndex(0);
        adminImagePreviewLabel.setIcon(null); adminImagePreviewLabel.setText("No Image Selected");
        selectedAdminImagePath = "";
    }

    private JPanel buildAdminBookingsTab() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        adminBookingsModel = new DefaultTableModel(
                new Object[]{"Booking ID", "Order", "User", "Movie", "Hall", "Seat", "Type", "Time", "Date",
                        "Price", "Status"}, 0);
        JTable table = new JTable(adminBookingsModel);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildAdminPaymentsTab() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        adminPaymentsModel = new DefaultTableModel(
                new Object[]{"Payment ID", "Order", "User", "Movie", "Seats", "Amount", "Method",
                        "Transaction ID", "Status", "Date"}, 0);
        JTable table = new JTable(adminPaymentsModel);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    private DefaultTableModel adminWaitlistModel;

    private JPanel buildAdminWaitlistTab() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        adminWaitlistModel = new DefaultTableModel(
                new Object[]{"ID", "User", "Movie", "Hall", "Time", "Date", "Seats Wanted", "Notified", "Requested"}, 0);
        JTable table = new JTable(adminWaitlistModel);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> refreshAdminWaitlistTable());
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.add(refreshBtn);
        panel.add(bottom, BorderLayout.SOUTH);
        return panel;
    }

    private void refreshAdminWaitlistTable() {
        adminWaitlistModel.setRowCount(0);
        for (WaitlistEntry w : bookingService.getWaitlistList()) {
            adminWaitlistModel.addRow(new Object[]{ w.getWaitlistId(), w.getUserName(), w.getMovieName(),
                    w.getHallName(), w.getShowTime(), w.getShowDate(), w.getSeatsWanted(),
                    w.isNotified() ? "Yes" : "No", w.getRequestDate() });
        }
    }

    private JPanel buildAdminDashboardTab() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(15, 20, 15, 20);
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;

        JLabel title = new JLabel("Business Overview", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 20));
        panel.add(title, gbc);

        JLabel revenueLabel = new JLabel(" ", SwingConstants.CENTER);
        revenueLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        JLabel ticketsLabel = new JLabel(" ", SwingConstants.CENTER);
        ticketsLabel.setFont(new Font("SansSerif", Font.BOLD, 16));

        gbc.gridy = 1; panel.add(revenueLabel, gbc);
        gbc.gridy = 2; panel.add(ticketsLabel, gbc);

        DefaultTableModel topMoviesModel = new DefaultTableModel(new Object[]{"Movie", "Tickets Sold"}, 0);
        JTable topMoviesTable = new JTable(topMoviesModel);
        gbc.gridy = 3; gbc.fill = GridBagConstraints.BOTH; gbc.weightx = 1; gbc.weighty = 1;
        panel.add(new JScrollPane(topMoviesTable), gbc);

        JButton refreshBtn = new JButton("Refresh Dashboard");
        gbc.gridy = 4; gbc.fill = GridBagConstraints.NONE; gbc.weighty = 0;
        refreshBtn.addActionListener(e -> {
            revenueLabel.setText("Total Revenue: Rs." + bookingService.getTotalRevenue());
            ticketsLabel.setText("Total Tickets Sold: " + bookingService.getTotalTicketsSold());
            topMoviesModel.setRowCount(0);
            for (java.util.Map.Entry<String, Integer> en : bookingService.getTopSellingMovies().entrySet()) {
                topMoviesModel.addRow(new Object[]{ en.getKey(), en.getValue() });
            }
        });
        panel.add(refreshBtn, gbc);
        refreshBtn.doClick();
        return panel;
    }

    private void refreshAdminBookingsTable() {
        adminBookingsModel.setRowCount(0);
        for (Booking b : bookingService.getBookingList()) {
            adminBookingsModel.addRow(new Object[]{ b.getBookingId(), b.getGroupId(), b.getUserName(),
                    b.getMovieName(), b.getHallName(), b.getSeatNumber(), b.getSeatType(), b.getShowTime(),
                    b.getBookingDate(), b.getAmount(), b.getStatus() });
        }
    }

    private void refreshAdminPaymentsTable() {
        adminPaymentsModel.setRowCount(0);
        for (Payment p : bookingService.getPaymentList()) {
            adminPaymentsModel.addRow(new Object[]{ p.getPaymentId(), p.getGroupId(), p.getUserName(),
                    p.getMovieName(), p.getSeatList(), p.getAmount(), p.getMethod(), p.getTransactionId(),
                    p.getStatus(), p.getPaymentDate() });
        }
    }

    private void refreshAdminMoviesTable() {
        adminMoviesModel.setRowCount(0);
        for (Movie m : bookingService.getMovieList()) {
            adminMoviesModel.addRow(new Object[]{ m.getMovieId(), m.getMovieName(), m.getCategory(),
                    m.getTicketPrice(), m.isBlockbuster() ? "Yes" : "No",
                    String.join(", ", m.getShowTimes()) });
        }
    }

    private void applyDarkBlueTheme() {
        UIManager.put("Panel.background", new Color(10, 28, 58));
        UIManager.put("Label.foreground", Color.WHITE);
        UIManager.put("Button.background", new Color(28, 92, 182));
        UIManager.put("Button.foreground", Color.WHITE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MovieBookingGUI().setVisible(true));
    }
}