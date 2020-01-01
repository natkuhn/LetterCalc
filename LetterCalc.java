/*
 * LetterCalc v1.3.3, Nat Kuhn 1/1/20:
 * web page needs more instructions: explain constraints, base upto 62, etc.
 * 
 * Version history:
 * 1.3.3: fixed problem in ControlPanel with text fields being too narrow
 * 1.3.2: made app window non-resizable (1/31/05)
 * 1.3.1: small cleanup re: dimensions
 * 1.3: limiting number of answers tracked to 1000, selects formula when solve started
 * 1.2.4: variable base, elapsed time
 * 1.2.3: fixes the bug that continues to increment counters while suppressing
 * wrong answers
 * 1.2.2: reworked Cancel button to use exception-handling
 * 1.2.1: added a "cancel" button
 */

/* <applet code="LetterCalc.class" width="625" height="425"></applet>
 * 
 */

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.text.*;
import java.util.Timer;

public class LetterCalc extends JApplet {

	public static final String VERSION = "1.3.3";
	public static final String CREDIT_START = "LetterCalc v";
	public static final String CREDIT_END = ", by Nat Kuhn www.familykuhn.net/nat";
	public static JTextArea textArea;
	public static JScrollPane scrollPane;
	private JTextField formula;
	private JTextField constraints;
	public ControlPanel controls;

	private Counter right;
	private Counter wrong;
	private Counter total;

	private TimeDisplay ShowTime;

	public LapTimer timer;
	private Timer scheduler;
	public final static long TIMER_INTERVAL = 100; // update time every .1 sec

	private Problem prob;
	private boolean canceling = false; // flag for cancel button; it is
	// quicker to check this than to call isInterrupted on the thread thousands of
	// times
	public boolean Suppress; // tracks the "Suppress incorrect" checkbox; also quicker this way

	private static final int HORIZONTAL_SIZE = 625; // window size
	private static final int VERTICAL_SIZE = 450;

	public static final int TEXT_AREA_LINES = 10; // area for answers
	public static final int TEXT_AREA_COLUMNS = 35;

	public static final int INSET_BETWEEN = 5; // insets for control in GridBagLayout
	public static final int INSET_LEFT = 10;
	public static final int INSET_TOP = 5;
	public static final int INSET_COUNTER_RIGHT = 65; // RH alignment for counters and time

	private static final int TEXT_FIELD_WIDTH = 20; // for formula & constraint entry

	private static final boolean DEFAULT_SUPPRESS_INCORRECT = false;
	private static final boolean DEFAULT_WARN_FEWER = false;

	private static final int SLEEP_NUMBER = 3; // the number of delay spinners
	private static final int SLEEP_MAXIMUM = 99999; // 10sec max
	public static final int SLEEP_START = 0;
	public static final int SLEEP_RIGHT = 1;
	public static final int SLEEP_WRONG = 2;

	private static final int[] SLEEP_INITIALS = { 0, 0, 1 };
	private static final String[] SLEEP_LABELS = { "Before start:", "After solution:", "After incorrect:" };

	private static final int BASE_DEFAULT = 10;
	private static final int BASE_MIN = 2;
	private static final int BASE_MAX = 62;

	public static final int MAX_ANSWERS = 1000; // maximum # of answers to track

	final Font Dlog = new Font("Dialog", Font.BOLD, 12);
	final Font Mono = new Font("Monospaced", Font.PLAIN, 12);
	final Font MonoBold = new Font("Monospaced", Font.BOLD, 12);
	final Font ByLine = new Font("SansSerif", Font.PLAIN, 11);

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

	public void init() {
		// GUI setup code is supposed to go in
		// the event-handling thread
		// BUT it turns out init is called from
		// event-handling thread when this is an app
		// what follows is a little hyper-correct, and it is probably
		// adequate to just do what createGUI does
		if (SwingUtilities.isEventDispatchThread()) {
			createGUI();
		} else {
			try { // need to invoke and wait because if running as app,
					// main needs to do more after the GUI is set up
				SwingUtilities.invokeAndWait(new Runnable() {
					public void run() {
						createGUI();
					}
				});
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	} // end init()

	private void createGUI() { // ** ? shd be static?
		Container cp = this.getContentPane(); // backward compatible with earlier Java

		controls = new ControlPanel();

		scheduler = new Timer();

		cp.add(controls, BorderLayout.CENTER);
		// set up the text area for output
		textArea = new JTextArea(TEXT_AREA_LINES, TEXT_AREA_COLUMNS);
		textArea.setEditable(false);
		textArea.setFont(Mono);

		scrollPane = new JScrollPane(textArea);

		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

		cp.add(scrollPane, BorderLayout.EAST);
	}

	private class ControlPanel extends JPanel {
		public JCheckBox SuppressIncorrect;
		public JCheckBox WarnFewer;
		public JButton SolveButton;
		public JButton CancelButton;
		public JSpinner[] Spinners = new JSpinner[SLEEP_NUMBER];
		public JSpinner BaseSpinner;

		public ControlPanel() {
			SolveListener SolveL = new SolveListener();

			// set up the layout
			GridBagLayout layout = new GridBagLayout();
			setLayout(layout);

			add(new JLabel("Formula:"), new GBC(0, 0).setWeight(0, 0).setAnchor(GBC.WEST).setInsets(INSET_TOP,
					INSET_LEFT, INSET_BETWEEN, INSET_BETWEEN));
			formula = new JTextField(TEXT_FIELD_WIDTH);
			formula.addActionListener(SolveL);
			formula.setMinimumSize(formula.getPreferredSize()); // to fix disappearing field problem,
			// per https://stackoverflow.com/a/6018738/1527750
			add(formula, new GBC(1, 0, 2, 1).setWeight(100, 0)/* 100, 0 */);

			Insets stdInsets = new Insets(INSET_TOP, INSET_LEFT, INSET_BETWEEN, INSET_BETWEEN);
			Insets ctrInsets = new Insets(INSET_TOP, INSET_LEFT, INSET_BETWEEN, INSET_COUNTER_RIGHT);

			add(new JLabel("Constraints:"), new GBC(0, 1).setWeight(0, 0).setAnchor(GBC.WEST).setInsets(stdInsets));
			constraints = new JTextField(TEXT_FIELD_WIDTH);
			constraints.addActionListener(SolveL);
			constraints.setMinimumSize(constraints.getPreferredSize()); // as above
			add(constraints, new GBC(1, 1, 2, 1).setWeight(100, 0)/* 100, 0 */);

			SuppressIncorrect = new JCheckBox("Suppress incorrect answers (faster)");
			SuppressIncorrect.setSelected(DEFAULT_SUPPRESS_INCORRECT);
			add(SuppressIncorrect, new GBC(0, 2, 3, 1).setWeight(100, 0).setInsets(stdInsets).setAnchor(GBC.WEST));
			SuppressIncorrect.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					Suppress = SuppressIncorrect.isSelected();
				}
			});

			WarnFewer = new JCheckBox("Warn if fewer letters than maximum");
			WarnFewer.setSelected(DEFAULT_WARN_FEWER);
			add(WarnFewer, new GBC(0, 3, 3, 1).setWeight(100, 0).setInsets(stdInsets).setAnchor(GBC.WEST));

			// the placement of these button is kludgy but it works, though it
			// might break if window is sized differently
			SolveButton = new JButton("Solve");
			SolveButton.addActionListener(SolveL);
			add(SolveButton, new GBC(0, 4, 2, 1).setWeight(100, 0).setInsets(stdInsets));

			CancelButton = new JButton("Cancel");
			CancelButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent event) {
					canceling = true;
					formula.requestFocus();
					prob.interrupt();
				}
			});
			add(CancelButton, new GBC(2, 4/* , 2, 1 */).setWeight(100, 0).setAnchor(GBC.WEST).setInsets(stdInsets));
			CancelButton.setEnabled(false);
			// setSolvable(true); can't do this b/c Controls is not yet initialized

			add(new JLabel("Solutions:"),
					new GBC(0, 5, 2, 1).setWeight(100, 0).setAnchor(GBC.WEST).setInsets(stdInsets));
			right = new Counter(MonoBold, LetterCalc.this);
			add(right, new GBC(2, 5).setWeight(100, 0).setAnchor(GBC.EAST).setInsets(ctrInsets));

			add(new JLabel("Incorrect attempts:"),
					new GBC(0, 6, 2, 1).setWeight(100, 0).setAnchor(GBC.WEST).setInsets(stdInsets));
			wrong = new Counter(MonoBold, LetterCalc.this);
			add(wrong, new GBC(2, 6).setWeight(100, 0).setAnchor(GBC.EAST).setInsets(ctrInsets));

			add(new JLabel("Total attempts:"),
					new GBC(0, 7, 2, 1).setWeight(100, 0).setAnchor(GBC.WEST).setInsets(stdInsets));
			total = new Counter(MonoBold, LetterCalc.this);
			add(total, new GBC(2, 7).setWeight(100, 0).setAnchor(GBC.EAST).setInsets(ctrInsets));

			add(new JLabel("Elapsed time:"),
					new GBC(0, 8, 2, 1).setWeight(100, 0).setAnchor(GBC.WEST).setInsets(stdInsets));
			ShowTime = new TimeDisplay(MonoBold);
			add(ShowTime, new GBC(2, 8).setWeight(100, 0).setAnchor(GBC.EAST).setInsets(ctrInsets));
			// ShowTime.show(0); //set elapsed time to 0:00.0

			add(new JLabel("Delays (ms):"), new GBC(0, 9).setWeight(0, 0).setAnchor(GBC.WEST).setInsets(stdInsets));

			// create a spacer to indent the delay labels
			// add(new JLabel(" "), new GBC(0, 9).setWeight(100,0));

			// set up the delay spinners
			for (int i = 0; i < SLEEP_NUMBER; i++) {
				add(new JLabel(SLEEP_LABELS[i]),
						new GBC(1, 9 + i).setWeight(100, 0).setAnchor(GBC.WEST).setInsets(stdInsets));
				Spinners[i] = new JSpinner(new SpinnerNumberModel(SLEEP_INITIALS[i], 0, SLEEP_MAXIMUM, 1));
				add(Spinners[i], new GBC(2, 9 + i).setWeight(100, 0).setAnchor(GBC.WEST).setInsets(stdInsets));
			}

			int y = 9 + SLEEP_NUMBER;

			add(new JLabel("Base:"), new GBC(0, y).setWeight(0, 0).setAnchor(GBC.WEST).setInsets(stdInsets));

			BaseSpinner = new JSpinner(new SpinnerNumberModel(BASE_DEFAULT, BASE_MIN, BASE_MAX, 1));
			add(BaseSpinner, new GBC(1, y).setWeight(100, 0).setAnchor(GBC.WEST).setInsets(stdInsets));

			JLabel credits = new JLabel(CREDIT_START + VERSION + CREDIT_END);
			credits.setFont(ByLine);
			add(credits, new GBC(0, y + 1, 3, 1).setWeight(100, 0).setInsets(stdInsets));

		}
	}// end inner class ControlPanel

	public boolean ifWarnFewer() {
		return controls.WarnFewer.isSelected();
	}

	class SolveListener implements ActionListener {
		public void actionPerformed(ActionEvent event) {
			setSolvable(false);
			canceling = false;

			formula.requestFocus();
			formula.selectAll();

			try {
				prob = new Problem(formula.getText(), LetterCalc.this,
						((Integer) controls.BaseSpinner.getValue()).intValue());
				prob.setConstraints(constraints.getText());
				prob.setCounters(right, wrong, total);
				prob.setOutputter(new Outputter(prob, textArea));
				timer = new LapTimer(scheduler, ShowTime);
				timer.go();
				prob.start();
			} catch (TextedParseException p) {
				SyntaxError(p.getMessage(), p.getText(), p.getErrorOffset());
				setSolvable(true);
			} catch (LetterException l) {
				showError(l);
				setSolvable(true);
			} catch (Throwable t) {
				t.printStackTrace();
				setSolvable(true);
			}
		}// end method
	}// end inner class SolveListener

	public void setSolvable(boolean b) {
		controls.SolveButton.setEnabled(b);
		controls.CancelButton.setEnabled(!b);
	}

	public void showError(LetterException l) {
		JOptionPane.showMessageDialog(LetterCalc.this, l.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
	}

	private void SyntaxError(String msg, String text, int pos) {
		String flagLine = "^";
		for (; pos > 0; pos--)
			flagLine = " " + flagLine;

		Box msgBox = Box.createVerticalBox();
		JLabel msgLabel = new JLabel(msg);
		msgLabel.setFont(Dlog);
		msgBox.add(msgLabel);
		JLabel textLabel = new JLabel(text);
		textLabel.setFont(Mono);
		msgBox.add(textLabel);
		JLabel flagLabel = new JLabel(flagLine);
		flagLabel.setFont(Mono);
		msgBox.add(flagLabel);

		JOptionPane.showMessageDialog(this, msgBox, "Syntax Error", JOptionPane.ERROR_MESSAGE);
		return;
	}

	public void Warning(String msg) {
		JOptionPane.showMessageDialog(this, msg, "Warning", JOptionPane.WARNING_MESSAGE);
	}

	public void sleep(int whichCounter) throws InterruptedException {
		// get the spinner value and sleep for that length
		int sleepLength = ((Integer) controls.Spinners[whichCounter].getValue()).intValue();
		if (sleepLength > 0)
			Thread.sleep(sleepLength);
		else if (canceling)
			throw new InterruptedException(); // need to check if not sleeping
	} // end method sleep
} // end class LetterCalc

class Counter extends JLabel {
	int count = 0;
	// int places;
	// String spaces;
	LetterCalc lc;

	Counter(Font f, LetterCalc lcpasser) {
		super("0", SwingConstants.RIGHT);
		// places = pl;
		setFont(f);
		lc = lcpasser;
		// spaces = "";
		// for (int i=0 ; i<places ; i++) spaces = spaces+" ";
		reset();
	}

	public void reset() {
		count = 0;
		show();
	}

	public void increment() {
		count++;
		if (!lc.Suppress)
			show();
	}

	public void show() {
		// String s = spaces + count;
		// int l = s.length();
		// final String text = s.substring(l-places,l);
		final String text = "" + count;

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				setText(text);
			}
		});

		// Thread.yield();
		// shouldn't be necessary because every increment is followed
		// by a change in something in Outputter which yields
	}

	public int getCount() {
		return count;
	}
}

class TimeDisplay extends JLabel {
	public TimeDisplay(Font f) {
		super("0:00.0", SwingConstants.RIGHT); // OK, so we cheat a bit
		setFont(f);
	}

	public void show(int tenths) {
		int t = tenths % 10;
		int seconds = tenths / 10;
		int s60 = seconds % 60;
		int s1 = s60 % 10;
		int s10 = s60 / 10;
		int minutes = seconds / 60;

		final String time = "" + minutes + ":" + (char) ('0' + s10) + (char) ('0' + s1) + "." + (char) ('0' + t);
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				setText(time);
			}
		});
	}
}

class LapTimer extends TimerTask {

	private Timer timer;
	private TimeDisplay display;
	private long start;

	public LapTimer(Timer t, TimeDisplay s) {
		timer = t; // thread should run as daemon, i.e. not prolong the life of app
		display = s;
	}

	public void run() {
		int tenths = (int) (System.currentTimeMillis() - start) / 100;
		display.show(tenths);
	}

	public void go() {
		start = System.currentTimeMillis();
		timer.scheduleAtFixedRate(this, 0, LetterCalc.TIMER_INTERVAL);
	}

	public void stop() {
		this.cancel();
	}
}

// class myInteger { //my own wrapper class because Eclipse 3.0 could not
// resolve
// //the Integer(int) constructor
// private final int value;
// myInteger(int v) {
// value = v;
// }
// public int intValue() {
// myInteger i;
// i = myInteger(value);
// return value;
// myInteger(value);
// }
// }
//
class Outputter {

	private JTextArea box;
	private Problem p;
	private final int numLines;
	private final int stringLen;
	private final LetterCalc letc;

	private int solutionStart = 0;
	private int solutionEnd = 0;
	private boolean isSuppressed; /*
									 * since the checkbox could change in the middle of a calculation, we need to
									 * track whether the work of suppressing the intermediate answer has been done
									 */

	private boolean overflow;
	private LinkedList ansList; // points to the start of each answer, to delete later
	private int zapped; // counts the net number of characters removed from the
						// output stream due to overflow

	public static final boolean LETTERS = false;
	public static final boolean NUMBERS = true;

	public Outputter(Problem prob, JTextArea area) {
		p = prob;
		box = area;
		numLines = p.lines.length;
		letc = p.lc; // this is a kludge, but we need to access the Suppress checkbox
		stringLen = (numLines + 1) * (p.numPlaces + 2) + 1;
		/*
		 * this is to make the StringBuffer/Builder long enough: extra line for === and
		 * extra spaces for leading space/operator and new-line plus one for an extra
		 * new-line at end
		 */
		ansList = new LinkedList();
	}

	public void showStatement() {
		StringBuffer statement = problemOut(LETTERS);

		updateWork(statement.toString(), 0, box.getText().length(), false);

		solutionStart = 0;
		solutionEnd = statement.length();
	}

	public void showSolution() {
		StringBuffer sol;
		String ovString;

		// take care of overflow, if necessary
		ovString = ""; // the compiler complains unless we do it this way

		// for first overflow: (need to display 1001 answers to get 1000
		// because final tentative answer is deleted
		if (!overflow && ansList.size() > LetterCalc.MAX_ANSWERS) {
			overflow = true;
			ovString = "Too many solutions!\nRetaining only the last " + LetterCalc.MAX_ANSWERS + "\n\n";
		}

		if (overflow) {
			int start = ((Integer) ansList.removeFirst()).intValue();
			int end = ((Integer) ansList.getFirst()).intValue(); // peek but don't remove
			updateWork(ovString, start, end, false); // perhaps we should invoke and wait?

			zapped += end - start - ovString.length();
		}

		// generate the output for the next answer
		if (letc.Suppress) {
			sol = newLines(numLines + 2);
			isSuppressed = true;
		} else {
			sol = problemOut(NUMBERS).append('\n');
			isSuppressed = false;
		}

		// extra blank line is so that finishSolution doesn't make screen jump

		updateWork("\n" + sol.toString(), solutionEnd, solutionEnd, true); // append to textbox

		solutionStart = ++solutionEnd; // move pointer to beg of this soln
										// allowing for extra new-line
		solutionEnd += sol.length(); // end of new soln
		ansList.add(new Integer(solutionStart));

	}

	public void update() {
		StringBuffer sol;
		if (letc.Suppress) {
			if (isSuppressed)
				return; // let the blank lines stand
			sol = newLines(numLines + 2);
			isSuppressed = true;
		} else {
			isSuppressed = false; // faster to do this than to check and then turn off
			sol = problemOut(NUMBERS).append('\n');
		}

		updateWork(sol.toString(), solutionStart, solutionEnd, false);
		solutionEnd = solutionStart + sol.length(); // end of new soln

	}

	public void finishSolution() {
		String s;
		int start, end;

		if (isSuppressed) { // if so, need to display the solution
			s = problemOut(NUMBERS).toString();
			start = solutionStart; // replace the blank solution with correct soln
			end = solutionEnd;
		} else { // just need to replace last blank line with text
			s = "";
			start = solutionEnd - 1;
			end = solutionEnd;
		}

		s += "Solution " + p.cRight.getCount() + " on " + p.cTotal.getCount() + " tries\n";

		updateWork(s, start, end, false);

		solutionEnd = start + s.length();
		solutionStart = solutionEnd; // probably unnecessary but just in case
	}

	public void showTotals() {
		String text = "Solutions: " + p.cRight.getCount() + "\nTotal attempts: " + p.cTotal.getCount()
				+ newLines(numLines + 1).toString();
		// newlines at end allows the totals to replace the final
		// rejected solution without making the scroll box jump

		updateWork(text, solutionStart, solutionEnd, true);
	}

	private void updateWork(final String str, int start, int end, final boolean scroll) {
		final int st = start - zapped;
		final int en = end - zapped; // correct for what's been removed already
		// if 'scroll' is set, this scrolls to the end of the window
		SwingUtilities.invokeLater(new Runnable() { // GUI work needs to happen
			public void run() { // in the event-handling thread
				box.replaceRange(str, st, en);
				if (scroll) {
					int height = box.getHeight();
					box.scrollRectToVisible(new Rectangle(0, height, 100, 100));
				}
			}
		});

		Thread.yield(); // allow the event-handling thread to update screen
	}

	private StringBuffer problemOut(boolean flag) {
		// get enough space to start with: extra line for === and extra
		// spaces for leading space/operator and new-line
		// plus one for an extra new-lines
		StringBuffer text = new StringBuffer((numLines + 1) * (p.numPlaces + 2) + 1);

		for (int i = 1; i < numLines; i++) {
			text.append(lineOut(i, flag));
		}

		for (int j = 0; j <= p.numPlaces; j++)
			text.append("=");// row of ===========
		text.append('\n');

		text.append(lineOut(0, flag));

		return text;

	}

	// put together the word or number (depending on flag), with initial padding
	// and operator if it's the line before the ======
	private StringBuffer lineOut(int lineNum, boolean flag) {

		StringBuffer out = new StringBuffer(p.numPlaces + 2);

		if (lineNum == numLines - 1)
			out.append(p.operator);
		else
			out.append(' ');

		int len = p.lines[lineNum].length();

		for (int j = p.numPlaces; j > len; j--)
			out.append(' ');

		if (flag == NUMBERS) { // put in the string of digits

			for (int j = len - 1; j >= 0; j--) {
				char c;
				int dig = p.lines[lineNum].getDig(j).get();
				if (dig < 10)
					c = (char) ('0' + dig);
				else if (dig < 36)
					c = (char) ('a' + dig - 10);
				else
					c = (char) ('A' + dig - 36);
				out.append(c);
			}

		} else { // or put in the text of the line
			out.append(p.lines[lineNum].getText());
		}

		out.append("\n");

		return out;
	}

	private StringBuffer newLines(int n) {
		StringBuffer str = new StringBuffer(n);
		for (int i = 0; i < n; i++)
			str.append('\n');
		return str;
	}

}// end class Outputter

class LetterException extends Exception {
	LetterException(String s) {
		super(s);
	}
}

class TextedParseException extends ParseException {
	String TextHolder;

	TextedParseException(String msg, String text, int position) {
		super(msg, position);
		TextHolder = text;
	}

	String getText() {
		return TextHolder;
	}
}

class Problem extends SwingWorker {

	// public static final int BASE = 10;
	public int Base;

	public Counter cRight;
	public Counter cWrong;
	public Counter cTotal;

	public line[] lines; // horizontal lines of the problem

	public char operator; // the operator for the problem

	public int numPlaces = 0;
	private int lineCounter = 1;

	private letterBox theLetters; // keeps track of the letter assignments

	private Outputter putter;

	public LetterCalc lc;

	public Problem(String formula, LetterCalc myLC, int b) throws TextedParseException, LetterException { // set-up and
																											// error
																											// checking
		lc = myLC;
		Base = b;

		inputParser parser = new inputParser(formula);

		int numLines = parser.getNumLines();

		lines = new line[numLines];
		theLetters = new letterBox(Base);

		operator = parser.getOperator();

		if (parser.isAnswerLeft()) { // answer to left of =
			for (int j = 0; j < numLines; j++)
				lines[j] = new line(parser.getWord(j));
		} else { // answer to right of =
			for (int j = 1; j < numLines; j++)
				lines[j] = new line(parser.getWord(j - 1));
			lines[0] = new line(parser.getWord(numLines - 1));
		}

		// check that there are 10 letters?
		int n = theLetters.number();
		if (n < Base && lc.ifWarnFewer()) {
			if (n == 1)
				lc.Warning("1 letter can't make a very interesting problem, can it?");
			else
				lc.Warning(n + " letters are not a complete set of " + Base);
		}
	}

	public void setConstraints(String constraints) throws TextedParseException, LetterException {
		if (constraints.trim().equals(""))
			return; // done if constraints are blank
		parser p = new parser(constraints);

		char c = p.getNextNW();

		while (true) {

			if (!Character.isLetter(c))
				p.error("Looking for a letter");
			digit dig = theLetters.findDigit(c);
			if (dig == null)
				p.error("The letter '" + c + "' does not appear in the formula");
			if (dig.isFixed())
				p.error("Value of '" + dig.getName() + "' is already fixed");

			if (p.getNextNW() != '=')
				p.error("Expecting '='");

			char d = p.getNextNW();

			if (d < '0' || d > '9')
				p.error("Expecting digit");

			int digit = d - '0';

			// set the value of d to digit
			dig.set(digit);
			dig.setFix(true);

			// check that next thing is end, blank, or comma
			if (!(p.atEnd() || (c = p.getNextChar()) == ',' || c == ' '))
				p.error("Unexpected character");
			p.skipWhite();

			if (p.atEnd())
				return;

			c = p.getNextChar();
			if (c == ',')
				c = p.getNextNW(); // if there is a comma, we must have a next clause
		}
	}

	public void setCounters(Counter r, Counter w, Counter t) {
		cRight = r;
		cWrong = w;
		cTotal = t;
	}

	public void setOutputter(Outputter putPasser) {
		putter = putPasser; // the object used to output solutions
	}

	public Object construct() { // return null if OK, otherwise return the exception
		int solNum = 0;
		boolean[] inUse = new boolean[Base];

		// clear the array that tracks which digits are already used ***
		for (int i = 0; i < Base; i++)
			inUse[i] = false; // none of the letters are in use
		for (int i = 0; i < theLetters.number(); i++) { // except for those fixed by constraints
			digit d = theLetters.ithDigit(i);
			if (!d.isFixed())
				continue;
			inUse[d.get()] = true;
		}

		// clear the trackers
		cRight.reset();
		cWrong.reset();
		cTotal.reset();

		putter.showStatement(); // diplay the statement

		try {
			lc.sleep(LetterCalc.SLEEP_START);
		} catch (InterruptedException ie) {
			return null;
		}

		putter.showSolution(); // display the working solution

		try {
			iterate(0, 0, 0, inUse); // find the solutions
		} catch (LetterException le) {
			return le;
		} catch (InterruptedException ie) {
			return null;
		}

		// if suppressing, want the final tallies to be correct
		if (lc.Suppress) {
			cRight.show();
			cWrong.show();
			cTotal.show();
		}

		putter.showTotals();

		return null;

	}

	public void finished() { // this is invoked in the event-handling thread, so
		// it can do GUI things; it also handles the LetterException
		// note that hitting cancel will arrive here with a 'null' return value.
		// if this routine needs to know in the future whether the calculation was
		// canceled, the 'return null' at the end of construct() could be changed to
		// 'return ie' but then this routine would need to do some type-checking
		lc.setSolvable(true);
		lc.timer.stop();

		LetterException l = (LetterException) this.get();
		if (l != null) {
			lc.showError(l);
		}
	}

	// internal procedure to find solutions recursively
	private void iterate(int startLine, int place, int carryIn, boolean[] inUse)
			throws LetterException, InterruptedException {

		// if past the end, the solution will be correct if no carry and no leading 0
		if (place >= numPlaces) {
			if (carryIn == 0) {

				for (int i = 0; i < lines.length; i++) { // can't have leading zero
					if (digitAt(i, lines[i].length() - 1) == 0) {
						nogo();
						return;
					}
				}

				cRight.increment();
				cTotal.increment();

				// if we're suppressing, none of these will be updated, and
				// we want to update every time there is a correct answer
				if (lc.Suppress) {
					cRight.show();
					cWrong.show();
					cTotal.show();
				}

				putter.update();

				putter.finishSolution(); // add the line at the end with Solution #

				lc.sleep(LetterCalc.SLEEP_RIGHT);

				putter.showSolution(); // put up new working solution

				return; // solution is OK
			}

			else { // there is a carry, so solution is not valid
				nogo();
				return;
			}
		}

		// if startLine<numLines, check if we need to iterate on any letters
		for (int i = startLine; i < lines.length; i++) {
			if (place >= lines[i].length())
				continue; // past the end of line i
			if (lines[i].getDig(place).isFixed())
				continue; // letter value is set
			// already

			// if not, iterate on the letter in this place

			lines[i].getDig(place).setFix(true);

			for (int j = 0; j < Base; j++) { // go through each value for this letter
				if (inUse[j])
					continue; // but eliminate duplicates

				inUse[j] = true;

				lines[i].getDig(place).set(j);

				iterate(i + 1, place, carryIn, inUse); // on to next line

				inUse[j] = false;
			}

			lines[i].getDig(place).setFix(false);

			return;

		}

		/*
		 * if we get here, all values for this place are fixed, so check whether the
		 * solution works
		 */

		int difference = compute(place, carryIn) - digitAt(0, place);

		if (difference % Base == 0) { // good so far! on to next place
			iterate(0, place + 1, difference / Base, inUse);
			return;
		} else { // solution not compatible
			nogo();
			return;
		}
	}

	private void nogo() throws InterruptedException {
		cWrong.increment();
		cTotal.increment();
		putter.update();
		if (lc.Suppress) {
			lc.sleep(0); // if Suppress is checked, don't sleep; but call sleep to check for cancel
		} else {
			lc.sleep(LetterCalc.SLEEP_WRONG);
		}
	}

	private int compute(int place, int carryIn) throws LetterException {

		if (operator == '+') {
			for (int i = 1; i < lines.length; i++) {
				carryIn += digitAt(i, place);
			}
			return carryIn;
		}

		if (operator == '-')
			return carryIn + digitAt(1, place) - digitAt(2, place);

		if (operator == '*') {
			for (int j = 0; j <= place; j++) {
				carryIn += digitAt(1, j) * digitAt(2, place - j);
			}

			return carryIn;

		}

		throw new LetterException("Internal error: illegal operator (" + operator + ")");

	}

	private int digitAt(int line, int place) {
		if (place >= lines[line].length())
			return 0;
		else
			return lines[line].getDig(place).get();
	}

	class line { // inner class of Problem
		digit[] myLine;
		String text;

		public line(String word) throws LetterException {
			text = word;
			int len = text.length();

			if (len > numPlaces)
				numPlaces = len; // track longest word

			myLine = new digit[len];

			for (int i = 0; i < len; i++) {
				myLine[i] = theLetters.getDigit(text.charAt(len - i - 1));

			}
		}

		public int length() {
			return myLine.length;
		}

		public digit getDig(int i) {
			return myLine[i];
		}

		public String getText() {
			return text;
		}

	} // end inner class line

}// end class Problem

class parser {
	private String myString;
	private char[] chars;
	private int i;

	public parser(String str) {
		myString = str;
		chars = str.toCharArray();
		i = 0;
	}

	public void skipWhite() {
		while (i < chars.length && chars[i] == ' ')
			i++;
	}

	public char getNextNW() throws TextedParseException {
		skipWhite();
		return getNextChar();
	}

	public boolean isLetterNext() {
		if (atEnd())
			return false;
		return Character.isLetter(chars[i]);
	}

	public boolean atEnd() {
		if (i == chars.length)
			i++; // moves caret past end of line, compensating for "i-1" in error()
		return i >= chars.length;
	}

	public char getNextChar() throws TextedParseException {
		if (atEnd())
			error("Unexpected end of line");
		return chars[i++];
	}

	public void error(String msg) throws TextedParseException {
		throw new TextedParseException(msg, myString, i - 1); // i-1 because pointer already advanced
	}
} // end class parser

class inputParser {
	private boolean AnswerLeft;
	private char op;

	private ArrayList wordList = new ArrayList();

	public inputParser(String myText) throws TextedParseException, LetterException {
		String word;

		boolean gotEq = false;
		boolean gotOp = false;

		if (myText.trim().equals(""))
			throw new LetterException("Enter a formula to solve");
		parser p = new parser(myText);

		while (true) {
			char c = p.getNextNW();
			if (!Character.isLetter(c))
				p.error("Expected a word");
			word = "" + c;
			while (p.isLetterNext())
				word = word + p.getNextChar();

			wordList.add(word);

			p.skipWhite();

			if (p.atEnd())
				break; // at end

			char opLet = p.getNextChar();
			if (opLet == '=') {
				if (gotEq)
					p.error("Duplicate equal sign");
				gotEq = true;
				if (gotOp)
					AnswerLeft = false;
				else
					AnswerLeft = true;
				// eqPos = wordList.size();
			} else if (gotOp) // already got the operator?
			{
				if (gotEq && !AnswerLeft)
					p.error("Can't have operators on both sides of '='");
				if (op != '+')
					p.error("Multiple operators permitted only for '+'");
				if (opLet != '+')
					p.error("Expected '+'" + (gotEq ? "" : " or '='"));
			} else if (opLet == '+' || opLet == '-' || opLet == '*') // no, first operator, check if valid
			{
				op = opLet;
				gotOp = true;
			} else
				p.error("Expected + - " + (gotEq ? "or *" : "* or =")); // invalid operator
		} // end while true

		if (!gotEq)
			p.error("Problem must have '='");
		if (!gotOp)
			p.error("Problem must have + - or *");
	} // end inputParser constructor

	public char getOperator() {
		return op;
	}

	public boolean isAnswerLeft() {
		return AnswerLeft;
	}

	public String getWord(int i) {
		return (String) wordList.get(i);
	}

	public int getNumLines() {
		return wordList.size();
	}
} // end class inputParser

class letterBox { // this collects the letter/digits
	private int base;
	int numLetters = 0;

	private digit[] digits;

	public letterBox(int b) {
		base = b;
		digits = new digit[base];
	}

	public int number() {
		return numLetters;
	}

	public digit ithDigit(int i) {
		return digits[i];
	}

	public digit findDigit(char digitName) { // find the desired digit, return null if not found
		char dn = Character.toLowerCase(digitName);

		for (int i = 0; i < numLetters; i++) {
			if (dn == digits[i].getName()) {
				return digits[i];
			}
		}
		return null;
	}

	public digit getDigit(char digitName) throws LetterException { // return the digit or create it

		digit d = findDigit(digitName);
		if (d != null)
			return d;

		// digitName is not found, so create it
		if (numLetters < base) {
			d = new digit();
			d.setName(digitName);
			digits[numLetters++] = d;
			return d;
		} else
			throw new LetterException("Too many letters! (More than " + base + ")");
	}
}// end class letterBox

class digit {
	private int value;

	private char name;

	private boolean fixed = false;

	public void set(int newValue) throws LetterException {
		// if (newValue < 0 || newValue >= Problem.BASE)
		// throw new LetterException("Digit out of range: " + newValue + "[digit]");
		// now that BASE is variable and not static, we don't have access. But this code
		// works
		value = newValue;
	}

	public int get() {
		return value;
	}

	public void setName(char newName) {
		name = Character.toLowerCase(newName);
	}

	public char getName() {
		return name;
	}

	public void setFix(boolean flag) {
		fixed = flag;
	}

	public boolean isFixed() {
		return fixed;
	}
} // end class digit
