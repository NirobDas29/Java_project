import java.io.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

/*
 * BookingService.java - All persistence now goes through SQLite (cinema.db)
 * via Db.get(). The only remaining plain-text file in the whole app is
 * booking_history.txt - an append-only human-readable log of every booking
 * action, kept on purpose as a simple text trail alongside the database.
 */
public class BookingService {
    private static final String HISTORY_FILE = "booking_history.txt";
    private static final String RECEIPT_DIR = "receipts";

    public BookingService() {
        Db.initSchema();
        seedDefaultMoviesIfEmpty();
    }

    // ================= MOVIES =================
    private void seedDefaultMoviesIfEmpty() {
        try (Statement st = Db.get().createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM movies")) {
            rs.next();
            if (rs.getInt(1) == 0) {
                addMovie("Spider-Man: Brand New Day", 450.0, "", true, Movie.NOW_SHOWING, new String[]{"02:00 PM", "06:00 PM"});
                addMovie("The Odyssey", 400.0, "", false, Movie.NOW_SHOWING, new String[]{"10:00 PM"});
                addMovie("The Silent Orchard", 220.0, "", false, Movie.NOW_SHOWING, new String[]{"11:00 AM"});
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to seed default movies", e);
        }
    }

    private Movie mapMovie(ResultSet rs) throws SQLException {
        String[] times = rs.getString("show_times").split(",");
        for (int i = 0; i < times.length; i++) times[i] = times[i].trim();
        return new Movie(
                rs.getInt("movie_id"),
                rs.getString("movie_name"),
                rs.getDouble("ticket_price"),
                rs.getString("image_path"),
                rs.getInt("blockbuster") == 1,
                rs.getString("category"),
                times
        );
    }

    public void addMovie(String name, double price, String imagePath, boolean blockbuster, String category, String[] showTimes) {
        String sql = "INSERT INTO movies (movie_id, movie_name, ticket_price, image_path, blockbuster, category, show_times) " +
                "VALUES ((SELECT COALESCE(MAX(movie_id), 0) + 1 FROM movies), ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = Db.get().prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setDouble(2, price);
            ps.setString(3, imagePath == null ? "" : imagePath);
            ps.setInt(4, blockbuster ? 1 : 0);
            ps.setString(5, (category == null || category.isEmpty()) ? Movie.NOW_SHOWING : category);
            ps.setString(6, String.join(",", (showTimes != null && showTimes.length > 0) ? showTimes : new String[]{"12:00 PM"}));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to add movie", e);
        }
    }

    public boolean updateMovie(int movieId, String name, double price, String imagePath,
                                boolean blockbuster, String category, String[] showTimes) {
        StringBuilder sql = new StringBuilder("UPDATE movies SET movie_name=?, ticket_price=?, blockbuster=?, category=?");
        boolean setImage = imagePath != null && !imagePath.isEmpty();
        boolean setTimes = showTimes != null && showTimes.length > 0;
        if (setImage) sql.append(", image_path=?");
        if (setTimes) sql.append(", show_times=?");
        sql.append(" WHERE movie_id=?");

        try (PreparedStatement ps = Db.get().prepareStatement(sql.toString())) {
            int i = 1;
            ps.setString(i++, name);
            ps.setDouble(i++, price);
            ps.setInt(i++, blockbuster ? 1 : 0);
            ps.setString(i++, category);
            if (setImage) ps.setString(i++, imagePath);
            if (setTimes) ps.setString(i++, String.join(",", showTimes));
            ps.setInt(i, movieId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update movie", e);
        }
    }

    public boolean deleteMovie(int movieId) {
        try (PreparedStatement ps = Db.get().prepareStatement("DELETE FROM movies WHERE movie_id=?")) {
            ps.setInt(1, movieId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete movie", e);
        }
    }

    public Movie getMovieById(int movieId) {
        try (PreparedStatement ps = Db.get().prepareStatement("SELECT * FROM movies WHERE movie_id=?")) {
            ps.setInt(1, movieId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapMovie(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch movie", e);
        }
        return null;
    }

    public ArrayList<Movie> getMovieList() {
        ArrayList<Movie> list = new ArrayList<Movie>();
        try (Statement st = Db.get().createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM movies ORDER BY movie_id")) {
            while (rs.next()) list.add(mapMovie(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch movies", e);
        }
        return list;
    }

    // ================= SEARCH / FILTER =================
    public List<Movie> searchMovies(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) return getMovieList();
        List<Movie> result = new ArrayList<Movie>();
        try (PreparedStatement ps = Db.get().prepareStatement(
                "SELECT * FROM movies WHERE LOWER(movie_name) LIKE ? ORDER BY movie_id")) {
            ps.setString(1, "%" + keyword.trim().toLowerCase() + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(mapMovie(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to search movies", e);
        }
        return result;
    }

    public List<Movie> filterMovies(String category, Boolean blockbusterOnly) {
        StringBuilder sql = new StringBuilder("SELECT * FROM movies WHERE 1=1");
        List<Object> params = new ArrayList<Object>();
        if (category != null && !category.isEmpty()) {
            sql.append(" AND category = ?");
            params.add(category);
        }
        if (blockbusterOnly != null && blockbusterOnly) {
            sql.append(" AND blockbuster = 1");
        }
        sql.append(" ORDER BY movie_id");
        List<Movie> result = new ArrayList<Movie>();
        try (PreparedStatement ps = Db.get().prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(mapMovie(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to filter movies", e);
        }
        return result;
    }

    // ================= BOOKINGS =================
    private Booking mapBooking(ResultSet rs) throws SQLException {
        return new Booking(
                rs.getInt("booking_id"), rs.getInt("group_id"), rs.getString("user_name"),
                rs.getString("movie_name"), rs.getString("hall_name"), rs.getString("seat_number"),
                rs.getString("seat_type"), rs.getString("show_time"), rs.getDouble("amount"),
                rs.getString("status"), rs.getString("booking_date")
        );
    }

    public ArrayList<Booking> getBookingList() {
        ArrayList<Booking> list = new ArrayList<Booking>();
        try (Statement st = Db.get().createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM bookings ORDER BY booking_id")) {
            while (rs.next()) list.add(mapBooking(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch bookings", e);
        }
        return list;
    }

    public Set<String> getBookedSeatsForHall(Movie movie, String date, String time, String hallName) {
        Set<String> booked = new HashSet<String>();
        String sql = "SELECT seat_number FROM bookings WHERE status='Confirmed' AND LOWER(movie_name)=LOWER(?) " +
                "AND booking_date=? AND show_time=? AND LOWER(hall_name)=LOWER(?)";
        try (PreparedStatement ps = Db.get().prepareStatement(sql)) {
            ps.setString(1, movie.getMovieName());
            ps.setString(2, date);
            ps.setString(3, time);
            ps.setString(4, hallName);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) booked.add(rs.getString(1).toUpperCase());
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch booked seats", e);
        }
        return booked;
    }

    public String getAssignedHall(Movie movie, String date, String time) {
        if (!movie.isBlockbuster()) return "Hall 3 - General";
        Set<String> hall1Bookings = getBookedSeatsForHall(movie, date, time, "Hall 1 - Blockbuster");
        if (hall1Bookings.size() >= Movie.TOTAL_SEATS) return "Hall 2 - Blockbuster Overflow";
        return "Hall 1 - Blockbuster";
    }

    public List<Booking> bookSeats(Movie movie, List<String> seats, String userName, String time, String date, String hallName) {
        List<Booking> created = new ArrayList<Booking>();
        if (movie == null || seats == null || seats.isEmpty()) return created;

        Set<String> booked = getBookedSeatsForHall(movie, date, time, hallName);
        for (String s : seats) {
            if (!movie.isValidSeat(s) || booked.contains(s.toUpperCase())) return new ArrayList<Booking>();
        }

        String insertSql = "INSERT INTO bookings (booking_id, group_id, user_name, movie_name, hall_name, seat_number, " +
                "seat_type, show_time, amount, status, booking_date) VALUES (?,?,?,?,?,?,?,?,?,?,?)";
        try {
            int groupId = nextGroupId();
            int bId = nextBookingId();
            for (String s : seats) {
                String seatType = movie.isPremiumSeat(s) ? "Premium" : "Regular";
                double price = movie.getSeatPrice(s);
                try (PreparedStatement ps = Db.get().prepareStatement(insertSql)) {
                    ps.setInt(1, bId);
                    ps.setInt(2, groupId);
                    ps.setString(3, userName);
                    ps.setString(4, movie.getMovieName());
                    ps.setString(5, hallName);
                    ps.setString(6, s);
                    ps.setString(7, seatType);
                    ps.setString(8, time);
                    ps.setDouble(9, price);
                    ps.setString(10, "Confirmed");
                    ps.setString(11, date);
                    ps.executeUpdate();
                }
                Booking booking = new Booking(bId, groupId, userName, movie.getMovieName(), hallName,
                        s, seatType, time, price, "Confirmed", date);
                created.add(booking);
                appendToHistory(booking);
                bId++;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save booking", e);
        }
        return created;
    }

    private int nextGroupId() throws SQLException {
        try (Statement st = Db.get().createStatement();
             ResultSet rs = st.executeQuery("SELECT COALESCE(MAX(group_id), 0) + 1 FROM bookings")) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private int nextBookingId() throws SQLException {
        try (Statement st = Db.get().createStatement();
             ResultSet rs = st.executeQuery("SELECT COALESCE(MAX(booking_id), 1000) + 1 FROM bookings")) {
            rs.next();
            return rs.getInt(1);
        }
    }

    public boolean cancelBooking(int bookingId) {
        String selectSql = "SELECT * FROM bookings WHERE booking_id=? AND status='Confirmed'";
        try (PreparedStatement sel = Db.get().prepareStatement(selectSql)) {
            sel.setInt(1, bookingId);
            Booking b;
            try (ResultSet rs = sel.executeQuery()) {
                if (!rs.next()) return false;
                b = mapBooking(rs);
            }
            setBookingStatus(bookingId, "Cancelled");
            b.setStatus("Cancelled");
            appendToHistory(b);
            notifyNextWaitlisted(b.getMovieName(), b.getHallName(), b.getShowTime(), b.getBookingDate(), 1);
            return true;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to cancel booking", e);
        }
    }

    private void setBookingStatus(int bookingId, String status) throws SQLException {
        try (PreparedStatement ps = Db.get().prepareStatement("UPDATE bookings SET status=? WHERE booking_id=?")) {
            ps.setString(1, status);
            ps.setInt(2, bookingId);
            ps.executeUpdate();
        }
    }

    public int cancelGroup(int groupId) {
        String selectSql = "SELECT * FROM bookings WHERE group_id=? AND status='Confirmed'";
        int count = 0;
        String movieName = null, hallName = null, showTime = null, date = null;
        try (PreparedStatement sel = Db.get().prepareStatement(selectSql)) {
            sel.setInt(1, groupId);
            List<Booking> toCancel = new ArrayList<Booking>();
            try (ResultSet rs = sel.executeQuery()) {
                while (rs.next()) toCancel.add(mapBooking(rs));
            }
            for (Booking b : toCancel) {
                setBookingStatus(b.getBookingId(), "Cancelled");
                b.setStatus("Cancelled");
                appendToHistory(b);
                movieName = b.getMovieName(); hallName = b.getHallName();
                showTime = b.getShowTime(); date = b.getBookingDate();
                count++;
            }
            if (count > 0) notifyNextWaitlisted(movieName, hallName, showTime, date, count);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to cancel group booking", e);
        }
        return count;
    }

    // ================= REFUND POLICY =================
    public double calculateRefundAmount(double originalAmount, double hoursBeforeShow) {
        if (hoursBeforeShow >= 24) return originalAmount;
        if (hoursBeforeShow >= 6) return originalAmount * 0.5;
        return 0.0;
    }

    // ================= HISTORY (the one plain text file) =================
    private void appendToHistory(Booking b) {
        try (PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(HISTORY_FILE, true)))) {
            pw.println(b.toFileFormat());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ================= PAYMENTS =================
    public void savePaymentRecord(Payment payment) {
        String sql = "INSERT INTO payments (payment_id, group_id, user_name, movie_name, amount, seat_count, " +
                "seat_list, method, mobile_number, transaction_id, payment_date, status) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";
        try {
            int pId = nextPaymentId();
            payment.setPaymentId(pId);
            try (PreparedStatement ps = Db.get().prepareStatement(sql)) {
                ps.setInt(1, pId);
                ps.setInt(2, payment.getGroupId());
                ps.setString(3, payment.getUserName());
                ps.setString(4, payment.getMovieName());
                ps.setDouble(5, payment.getAmount());
                ps.setInt(6, payment.getSeatCount());
                ps.setString(7, payment.getSeatList());
                ps.setString(8, payment.getMethod());
                ps.setString(9, payment.getMobileNumber());
                ps.setString(10, payment.getTransactionId());
                ps.setString(11, payment.getPaymentDate());
                ps.setString(12, payment.getStatus());
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save payment", e);
        }
    }

    private int nextPaymentId() throws SQLException {
        try (Statement st = Db.get().createStatement();
             ResultSet rs = st.executeQuery("SELECT COALESCE(MAX(payment_id), 5000) + 1 FROM payments")) {
            rs.next();
            return rs.getInt(1);
        }
    }

    public ArrayList<Payment> getPaymentList() {
        ArrayList<Payment> list = new ArrayList<Payment>();
        String sql = "SELECT * FROM payments ORDER BY payment_id";
        try (Statement st = Db.get().createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Payment p = new Payment(rs.getInt("group_id"), rs.getString("user_name"), rs.getString("movie_name"),
                        rs.getDouble("amount"), rs.getInt("seat_count"), rs.getString("seat_list"),
                        rs.getString("method"), rs.getString("mobile_number"), rs.getString("payment_date"));
                p.setPaymentId(rs.getInt("payment_id"));
                list.add(p);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch payments", e);
        }
        return list;
    }

    public String saveReceiptFile(String receiptText, int groupId) {
        try {
            File dir = new File(RECEIPT_DIR);
            if (!dir.exists()) dir.mkdir();
            File out = new File(dir, "receipt_order_" + groupId + ".txt");
            try (PrintWriter pw = new PrintWriter(new FileWriter(out))) {
                pw.print(receiptText);
            }
            return out.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    // ================= REVIEWS & RATINGS =================
    public Review addReview(String movieName, String userName, int rating, String comment) {
        String sql = "INSERT INTO reviews (review_id, movie_name, user_name, rating, comment, review_date) " +
                "VALUES ((SELECT COALESCE(MAX(review_id), 0) + 1 FROM reviews), ?, ?, ?, ?, ?)";
        String date = currentTimestamp();
        int clampedRating = Math.max(1, Math.min(5, rating));
        try (PreparedStatement ps = Db.get().prepareStatement(sql)) {
            ps.setString(1, movieName);
            ps.setString(2, userName);
            ps.setInt(3, clampedRating);
            ps.setString(4, comment);
            ps.setString(5, date);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save review", e);
        }
        List<Review> reviews = getReviewsForMovie(movieName);
        return reviews.isEmpty() ? null : reviews.get(reviews.size() - 1);
    }

    public List<Review> getReviewsForMovie(String movieName) {
        List<Review> result = new ArrayList<Review>();
        try (PreparedStatement ps = Db.get().prepareStatement(
                "SELECT * FROM reviews WHERE LOWER(movie_name)=LOWER(?) ORDER BY review_id")) {
            ps.setString(1, movieName);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new Review(rs.getInt("review_id"), rs.getString("movie_name"),
                            rs.getString("user_name"), rs.getInt("rating"), rs.getString("comment"),
                            rs.getString("review_date")));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch reviews", e);
        }
        return result;
    }

    public double getAverageRating(String movieName) {
        try (PreparedStatement ps = Db.get().prepareStatement(
                "SELECT AVG(rating) FROM reviews WHERE LOWER(movie_name)=LOWER(?)")) {
            ps.setString(1, movieName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to compute average rating", e);
        }
        return 0.0;
    }

    // ================= WAITLIST =================
    public WaitlistEntry addToWaitlist(String userName, String movieName, String hallName,
                                        String showTime, String date, int seatsWanted) {
        String sql = "INSERT INTO waitlist (waitlist_id, user_name, movie_name, hall_name, show_time, show_date, " +
                "seats_wanted, notified, request_date) VALUES ((SELECT COALESCE(MAX(waitlist_id),0)+1 FROM waitlist), ?,?,?,?,?,?,0,?)";
        String requestDate = currentTimestamp();
        try (PreparedStatement ps = Db.get().prepareStatement(sql)) {
            ps.setString(1, userName);
            ps.setString(2, movieName);
            ps.setString(3, hallName);
            ps.setString(4, showTime);
            ps.setString(5, date);
            ps.setInt(6, seatsWanted);
            ps.setString(7, requestDate);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to join waitlist", e);
        }
        List<WaitlistEntry> current = getWaitlistForShow(movieName, hallName, showTime, date);
        return current.isEmpty() ? null : current.get(current.size() - 1);
    }

    public List<WaitlistEntry> getWaitlistForShow(String movieName, String hallName, String showTime, String date) {
        List<WaitlistEntry> result = new ArrayList<WaitlistEntry>();
        String sql = "SELECT * FROM waitlist WHERE notified=0 AND LOWER(movie_name)=LOWER(?) " +
                "AND LOWER(hall_name)=LOWER(?) AND show_time=? AND show_date=? ORDER BY waitlist_id";
        try (PreparedStatement ps = Db.get().prepareStatement(sql)) {
            ps.setString(1, movieName);
            ps.setString(2, hallName);
            ps.setString(3, showTime);
            ps.setString(4, date);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(mapWaitlist(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch waitlist", e);
        }
        return result;
    }

    private WaitlistEntry mapWaitlist(ResultSet rs) throws SQLException {
        WaitlistEntry w = new WaitlistEntry(rs.getInt("waitlist_id"), rs.getString("user_name"),
                rs.getString("movie_name"), rs.getString("hall_name"), rs.getString("show_time"),
                rs.getString("show_date"), rs.getInt("seats_wanted"), rs.getString("request_date"));
        if (rs.getInt("notified") == 1) w.setNotified(true);
        return w;
    }

    public ArrayList<WaitlistEntry> getWaitlistList() {
        ArrayList<WaitlistEntry> list = new ArrayList<WaitlistEntry>();
        try (Statement st = Db.get().createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM waitlist ORDER BY waitlist_id")) {
            while (rs.next()) list.add(mapWaitlist(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch waitlist", e);
        }
        return list;
    }

    private void notifyNextWaitlisted(String movieName, String hallName, String showTime, String date, int freedSeats) {
        List<WaitlistEntry> waiting = getWaitlistForShow(movieName, hallName, showTime, date);
        int remaining = freedSeats;
        try (PreparedStatement ps = Db.get().prepareStatement("UPDATE waitlist SET notified=1 WHERE waitlist_id=?")) {
            for (WaitlistEntry w : waiting) {
                if (remaining <= 0) break;
                if (w.getSeatsWanted() <= remaining) {
                    ps.setInt(1, w.getWaitlistId());
                    ps.executeUpdate();
                    remaining -= w.getSeatsWanted();
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update waitlist", e);
        }
    }

    // ================= ANALYTICS (ADMIN DASHBOARD) =================
    public double getTotalRevenue() {
        try (Statement st = Db.get().createStatement();
             ResultSet rs = st.executeQuery("SELECT COALESCE(SUM(amount),0) FROM payments WHERE status='Success'")) {
            rs.next();
            return rs.getDouble(1);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to compute total revenue", e);
        }
    }

    public LinkedHashMap<String, Integer> getTopSellingMovies() {
        LinkedHashMap<String, Integer> sorted = new LinkedHashMap<String, Integer>();
        String sql = "SELECT movie_name, COUNT(*) c FROM bookings WHERE status='Confirmed' " +
                "GROUP BY movie_name ORDER BY c DESC";
        try (Statement st = Db.get().createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) sorted.put(rs.getString(1), rs.getInt(2));
        } catch (SQLException e) {
            throw new RuntimeException("Failed to compute top selling movies", e);
        }
        return sorted;
    }

    public int getTotalTicketsSold() {
        try (Statement st = Db.get().createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM bookings WHERE status='Confirmed'")) {
            rs.next();
            return rs.getInt(1);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to count tickets sold", e);
        }
    }

    public static String currentTimestamp() {
        return new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(new Date());
    }
}