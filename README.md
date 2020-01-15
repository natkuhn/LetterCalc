This Java Applet finds all possible solutions to cryptarithms. A cryptarithm,
or cryptarithmetic puzzle, is an arithmetic problem made up of words. To solve
the problem, you need to find a digit for each letter so that the problem is
correct, and so that no two letters represent the same digit. For example,
the most famous cryptarithm is:

```
  S E N D
+ M O R E
=========
M O N E Y
```

The applet was written around 2003-2005, when it was possible to run Java
applets in the browser. The easiest way I know of to run it now is using
VS Code.

1. Clone this repository on your hard drive, and open the folder in VS Code.
1. Make sure you have the VS Code extension "Language Support for Java(TM)
   by Red Hat" installed an enabled.
1. Open LetterCalc.java in the repository.
1. Click on the debug and run icon on the left (command-shift-D on Mac,
   presumably control-shift-D on PC).
1. Click on "Run." The applet should open in its own little window.
1. Type `send+more=money` into the "Formula" box and hit the "Solve" button!

In the scroll pane to the right, you should see digits flipping over as the program
iterates through possible solutions; when a solution is found, it is "printed"
and the flipping digits move down. When all possibilities are exhausted, a
tally of the total number of solutions is "printed."

Unforunately there is a bug which can prevent the scroll pane from getting a
final update, so that the program appears to be stuck. Simply click the mouse
in the scroll pane to update it.

Other crypatrithms to try: `small - dough = deal`, `two \* six = twelve`
and `foot + foot + foot = yard`. Have fun!

If answers are flying by too fast, try increasing the "delays." For
instance, try setting "After solution" two 500 (one-half second) and
see what happens.

The "After rejection" delay is set to 1 millisecond because on some
machines when it's set to 0 you can't see the digits flipping, which is the
most fun part of the whole thing. Try setting it to 0, and if you still see
the digits flip it will speed things up a bit.

The "constraints" textbox allows you to fix certain letters with particular digit
values. For example solving the puzzle `send+more=money` with `s=9 m=1` in the
constraints box finds the solution faster and with many fewer tries; putting
`s=9 m=2` finds no solutions.

More links for cryptarithms:

- A [blog post](https://nat.familykuhn.net/2020/01/cryptarithm/) I wrote on this program, how it came to be, and why I resurrected it 15 years later. There is a 20-second video at the end showing
  the program in action.
- An online [version on repl.it](https://repl.it/@NatKuhn/LetterCalc).
  Unfortunately, there are some bugs in the repl.it implementation of the Swing
  graphic library, so the shift key does not work. You can't enter `send+more=money`,
  but you can enter `money-more=send` and it does work. You need to hit the "Run"
  button and be quite patient, and it helps to drag the pane edges to make the
  applet window larger.
- "[A Primer on Cryptarithmetic](http://cryptarithms.awardspace.us/primer.html)"
- "[Alphametic Puzzle Solver](http://www.tkcs-collins.com/truman/alphamet/"alpha_solve.shtml)
