tl;dr You can [try this on the web](https://replit.com/@NatKuhn/LetterCalc). 
Some additional notes at the bottom.

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

There is a solution with `S=9`, `E=5`, `N=6`, `D=7`, `M=1`, `O=0`, `R=8`, `Y=2`, since

```
  9 5 6 7
+ 1 0 8 5
=========
1 0 6 5 2
```

and it turns out that this is the only solution.

The applet was written around 2003-2005, when it was possible to run Java
applets in the browser. The easiest way I know of to run it now is using
VS Code.

1. Clone this repository on your hard drive, and open the folder in VS Code.
1. Make sure you have the VS Code extension "Java Extension Pack" installed
   and enabled (or at least "Language Support for Java™ by Red Hat" and
   "Debugger for Java" installed and enabled).
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

Other crypatrithms to try: `small - dough = deal`, `two * six = twelve`
and `foot + foot + foot = yard`. Have fun! (July 2020: Evelyn Lamb's
[page-a-day math calendar](https://bookstore.ams.org/mbk-128) gives a number
of great ones from [Manan Shah](http://mathmisery.com/) including
`one+two+four=seven` and `failure+failure+failure+failure=success`.
There is also `three-pi=zero+sum`, but you will need to move the `pi`
to the other side of the equals sign.)

If answers are flying by too fast, try increasing the "delays." For
instance, try setting "After solution" two 500 (one-half second) and
see what happens.

The "After rejection" delay is set to 1 millisecond because on some
machines when it's set to 0 you can't see the digits flipping, which is the
most fun part of the whole thing. Try setting it to 0, and if you still see
the digits flip it will speed things up a bit. (Unfortunately, when there is
no delay, the main thread "blocks" and the program will let you cancel or
increase the delay while it is still running.)

The "constraints" textbox allows you to fix certain letters with particular digit
values. For example solving the puzzle `send+more=money` with `s=9 m=1` in the
constraints box finds the solution faster and with many fewer tries; putting
`s=9 m=2` finds no solutions.

The "Warn if fewer letters than maximum" checkbox will alert you that, for
example, `send+more=money` involves only 8 letters, and so not all 10 base-10
digits will be present in the solution. I'm not sure why I thought this was
a good idea.

By default, the puzzles are solved in base 10, but the program will solve
puzzles in any base up to 62, using digits `0`-`9`, letters `a`-`z`, and then capital
letters `A`-`Z` as digits.

More links for cryptarithms:

- A [blog post](https://nat.familykuhn.net/2020/01/cryptarithm/) I wrote on this program, 
  how it came to be, and why I resurrected it 15 years later. There is a 20-second video 
  at the end showing the program in action.
- An online [version on replit.com](https://replit.com/@NatKuhn/LetterCalc).
  You need to hit the "Run" button and be quite patient for 10+ seconds while it loads up. 
- "[A Primer on Cryptarithmetic](http://cryptarithms.awardspace.us/primer.html)"
- "[Alphametic Puzzle Solver](http://www.tkcs-collins.com/truman/alphamet/alpha_solve.shtml)"
