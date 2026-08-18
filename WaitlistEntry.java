public class WaitlistEntry {
    private int waitlistId;
    private String userName;
    private String movieName;
    private String hallName;
    private String showTime;
    private String showDate;
    private int seatsWanted;
    private String requestDate;
    private boolean notified;

    public WaitlistEntry(int waitlistId, String userName, String movieName, String hallName,
                         String showTime, String showDate, int seatsWanted, String requestDate) {
        this.waitlistId = waitlistId;
        this.userName = userName;
        this.movieName = movieName;
        this.hallName = hallName;
        this.showTime = showTime;
        this.showDate = showDate;
        this.seatsWanted = seatsWanted;
        this.requestDate = requestDate;
        this.notified = false;
    }

    public int getWaitlistId() {
        return waitlistId;
    }

    public void setWaitlistId(int waitlistId) {
        this.waitlistId = waitlistId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getMovieName() {
        return movieName;
    }

    public void setMovieName(String movieName) {
        this.movieName = movieName;
    }

    public String getHallName() {
        return hallName;
    }

    public void setHallName(String hallName) {
        this.hallName = hallName;
    }

    public String getShowTime() {
        return showTime;
    }

    public void setShowTime(String showTime) {
        this.showTime = showTime;
    }

    public String getShowDate() {
        return showDate;
    }

    public void setShowDate(String showDate) {
        this.showDate = showDate;
    }

    public int getSeatsWanted() {
        return seatsWanted;
    }

    public void setSeatsWanted(int seatsWanted) {
        this.seatsWanted = seatsWanted;
    }

    public String getRequestDate() {
        return requestDate;
    }

    public void setRequestDate(String requestDate) {
        this.requestDate = requestDate;
    }

    public boolean isNotified() {
        return notified;
    }

    public void setNotified(boolean notified) {
        this.notified = notified;
    }

    @Override
    public String toString() {
        return "WaitlistEntry{" +
                "waitlistId=" + waitlistId +
                ", userName='" + userName + '\'' +
                ", movieName='" + movieName + '\'' +
                ", hallName='" + hallName + '\'' +
                ", showTime='" + showTime + '\'' +
                ", showDate='" + showDate + '\'' +
                ", seatsWanted=" + seatsWanted +
                ", requestDate='" + requestDate + '\'' +
                ", notified=" + notified +
                '}';
    }
}
