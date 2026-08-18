import java.io.Serializable;
public class Booking implements Serializable {
    private static final long serialVersionUID = 2L;
    private int bookingId;
    private int groupId;      
    private String userName;
    private String movieName;
    private String hallName;
    private String seatNumber;
    private String seatType;   
    private String showTime;
    private double amount;
    private String status;     
    private String bookingDate;

    public Booking(int bookingId, int groupId, String userName, String movieName, String hallName,
                    String seatNumber, String seatType, String showTime, double amount,
                    String status, String bookingDate) {
        this.bookingId = bookingId;
        this.groupId = groupId;
        this.userName = userName;
        this.movieName = movieName;
        this.hallName = hallName;
        this.seatNumber = seatNumber;
        this.seatType = seatType;
        this.showTime = showTime;
        this.amount = amount;
        this.status = status;
        this.bookingDate = bookingDate;
    }
    public int getBookingId() { return bookingId; }
    public int getGroupId() { return groupId; }
    public String getUserName() { return userName; }
    public String getMovieName() { return movieName; }
    public String getHallName() { return hallName; }
    public String getSeatNumber() { return seatNumber; }
    public String getSeatType() { return seatType; }
    public String getShowTime() { return showTime; }
    public double getAmount() { return amount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getBookingDate() { return bookingDate; }
    public void printTicket() {
        System.out.println("==========================");
        System.out.println("        Movie Ticket");
        System.out.println("==========================");
        System.out.println("Booking ID : " + bookingId);
        System.out.println("Order ID   : " + groupId);
        System.out.println("User       : " + userName);
        System.out.println("Movie      : " + movieName);
        System.out.println("Hall       : " + hallName);
        System.out.println("Show Time  : " + showTime);
        System.out.println("Seat       : " + seatNumber + " (" + seatType + ")");
        System.out.println("Amount     : Rs." + amount);
        System.out.println("Status     : " + status);
        System.out.println("==========================");
    }

    public String toFileFormat() {
        return bookingId + " | " + groupId + " | " + movieName + " | " + seatNumber + " (" + seatType + ")"
                + " | " + amount + " | " + status + " | " + bookingDate + " " + showTime;
    }

    @Override
    public String toString() {
        return "Booking#" + bookingId + " - " + movieName + " - Seat " + seatNumber + " - " + status;
    }
}
