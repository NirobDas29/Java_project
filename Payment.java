import java.io.Serializable;
import java.util.Random;
public class Payment implements Serializable {
    private static final long serialVersionUID = 2L;

    private int paymentId;
    private int groupId;          
    private String userName;
    private String movieName;
    private double amount;        
    private int seatCount;
    private String seatList;      
    private String method;        
    private String mobileNumber;  
    private String transactionId;
    private String paymentDate;
    private String status;        

    public Payment(int groupId, String userName, String movieName, double amount, int seatCount,
                    String seatList, String method, String mobileNumber, String paymentDate) {
        this.groupId = groupId;
        this.userName = userName;
        this.movieName = movieName;
        this.amount = amount;
        this.seatCount = seatCount;
        this.seatList = seatList;
        this.method = method;
        this.mobileNumber = mobileNumber;
        this.paymentDate = paymentDate;
        this.status = "Pending";
    }

    private class ProgressAnimation implements Runnable {
        @Override
        public void run() {
            try {
                String dots = "";
                for (int i = 0; i < 4; i++) {
                    dots = dots + ".";
                    System.out.println(dots);
                    Thread.sleep(400); 
                }
            } catch (InterruptedException e) {
                System.out.println("Payment animation interrupted: " + e.getMessage());
            }
        }
    }

    public boolean processPayment() {
        System.out.println("\nProcessing Payment of BDT" + amount + " via " + method + " ...");
        Thread animationThread = new Thread(new ProgressAnimation());
        try {
            animationThread.start();
            animationThread.join();
            status = "Success";
            transactionId = "Cash on Counter".equals(method) ? "N/A" : generateTransactionId();
            System.out.println("Payment Successful!\n");
        } catch (InterruptedException e) {
            status = "Failed";
            transactionId = "N/A";
            System.out.println("Payment failed due to interruption: " + e.getMessage());
        }
        return "Success".equals(status);
    }

    private String generateTransactionId() {
        Random rand = new Random();
        String prefix = "bKash".equals(method) ? "BKS" : "Nagad".equals(method) ? "NGD" : "TXN";
        StringBuilder sb = new StringBuilder(prefix);
        for (int i = 0; i < 8; i++) sb.append(rand.nextInt(10));
        return sb.toString();
    }

    public String toFileFormat() {
        return paymentId + " | " + groupId + " | " + userName + " | " + movieName + " | " +
                amount + " | " + seatCount + " seat(s) | " + method + " | " + status + " | " + paymentDate;
    }
    public int getPaymentId() { return paymentId; }
    public void setPaymentId(int paymentId) { this.paymentId = paymentId; }
    public int getGroupId() { return groupId; }
    public void setGroupId(int groupId) { this.groupId = groupId; }
    public String getUserName() { return userName; }
    public String getMovieName() { return movieName; }
    public double getAmount() { return amount; }
    public int getSeatCount() { return seatCount; }
    public String getSeatList() { return seatList; }
    public String getMethod() { return method; }
    public String getMobileNumber() { return mobileNumber; }
    public String getTransactionId() { return transactionId; }
    public String getPaymentDate() { return paymentDate; }
    public String getStatus() { return status; }

    @Override
    public String toString() {
        return "Payment#" + paymentId + " - Rs." + amount + " - " + method + " - " + status;
    }
}
