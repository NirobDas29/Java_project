public class Main {
    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(() -> {
            MovieBookingGUI gui = new MovieBookingGUI();
            gui.initUI();
            gui.setVisible(true);
        });
    }
}
 