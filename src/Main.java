import javax.swing.*;

public class Main {
	private static final int HORIZONTAL_SIZE = 625; // window size
	private static final int VERTICAL_SIZE = 450;

	public static void main(String[] args) {
		final AppletFrame lc = new AppletFrame(new LetterCalc());
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				lc.setTitle("Cryptarithm Calculator");
				lc.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
				lc.setResizable(false);
				lc.setSize(HORIZONTAL_SIZE, VERTICAL_SIZE);
				lc.setVisible(true);
			}
		});
	}
} // end class Main
