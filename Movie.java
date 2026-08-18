import java.io.Serializable;

public class Movie implements Serializable {
    private static final long serialVersionUID = 4L;

    public static final String NOW_SHOWING = "Now Showing";
    public static final String COMING_SOON = "Coming Soon";
    public static final double PREMIUM_SURCHARGE = 100.0;

    private static final String[] ROWS = {"A", "B", "C", "D"};
    private static final int SEATS_PER_ROW = 5;
    public static final int TOTAL_SEATS = ROWS.length * SEATS_PER_ROW; // 20 Seats
    private static final String[] PREMIUM_ROWS = {"C", "D"};

    private int movieId;
    private String movieName;
    private double ticketPrice;
    private String imagePath;
    private boolean blockbuster;
    private String category;
    private String[] showTimes;

    private String[] seatLabels;
    private boolean[] seatPremium;

    public Movie(int movieId, String movieName, double ticketPrice, String imagePath,
                 boolean blockbuster, String category, String[] showTimes) {
        this.movieId = movieId;
        this.movieName = movieName;
        this.ticketPrice = ticketPrice;
        this.imagePath = imagePath;
        this.blockbuster = blockbuster;
        this.category = (category == null || category.isEmpty()) ? NOW_SHOWING : category;
        this.showTimes = (showTimes != null && showTimes.length > 0) ? showTimes : new String[]{"12:00 PM"};
        initializeSeats();
    }

    private void initializeSeats() {
        seatLabels = new String[ROWS.length * SEATS_PER_ROW];
        seatPremium = new boolean[ROWS.length * SEATS_PER_ROW];
        int index = 0;
        for (String row : ROWS) {
            boolean premiumRow = isPremiumRow(row);
            for (int i = 1; i <= SEATS_PER_ROW; i++) {
                seatLabels[index] = row + i;
                seatPremium[index] = premiumRow;
                index++;
            }
        }
    }

    private boolean isPremiumRow(String row) {
        for (String p : PREMIUM_ROWS) {
            if (p.equals(row)) return true;
        }
        return false;
    }

    public boolean isValidSeat(String seatNumber) {
        if (seatNumber == null) return false;
        for (String label : seatLabels) {
            if (label.equalsIgnoreCase(seatNumber)) return true;
        }
        return false;
    }

    public boolean isPremiumSeat(String seatNumber) {
        for (int i = 0; i < seatLabels.length; i++) {
            if (seatLabels[i].equalsIgnoreCase(seatNumber)) return seatPremium[i];
        }
        return false;
    }

    public double getSeatPrice(String seatNumber) {
        return ticketPrice + (isPremiumSeat(seatNumber) ? PREMIUM_SURCHARGE : 0.0);
    }

    public String[] getAllSeatLabels() {
        return seatLabels.clone();
    }

    public String[] getShowTimes() {
        return showTimes.clone();
    }

    public void setShowTimes(String[] showTimes) {
        this.showTimes = showTimes;
    }

    public int getMovieId() { return movieId; }
    public String getMovieName() { return movieName; }
    public void setMovieName(String movieName) { this.movieName = movieName; }
    public double getTicketPrice() { return ticketPrice; }
    public void setTicketPrice(double ticketPrice) { this.ticketPrice = ticketPrice; }
    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }
    public boolean isBlockbuster() { return blockbuster; }
    public void setBlockbuster(boolean blockbuster) { this.blockbuster = blockbuster; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    @Override
    public String toString() {
        return movieName + (blockbuster ? "  [BLOCKBUSTER]" : "") + "  -  Rs." + ticketPrice + "+";
    }
}