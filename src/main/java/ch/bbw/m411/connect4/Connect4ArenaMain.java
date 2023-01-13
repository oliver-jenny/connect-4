package ch.bbw.m411.connect4;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

/**
 * Plays a game of Connect Four on a 4x7 board (a variation of the original 6x7 board).
 * The pieces fall straight down, occupying the lowest available space within the column.
 */
public class Connect4ArenaMain {

    static final int WIDTH = 7;

    static final int HEIGHT = 4;

    static final int NOMOVE = -1;

    public static void main(String[] args) {
        new Connect4ArenaMain().play(new Connect4AlphaBetaPlayer(11), new HumanPlayer());
    }

    static String toDebugString(Stone[] board) {
        var sb = new StringBuilder();
        for (int r = 0; r < HEIGHT; r++) {
            for (int c = 0; c < WIDTH; c++) {
                var value = board[r * WIDTH + c];
                sb.append(value == null ? "." : (value == Stone.RED ? "X" : "O"));
            }
            sb.append("-");
        }
        return sb.toString();
    }

    Connect4Player play(Connect4Player red, Connect4Player blue) {
        if (red == blue) {
            throw new IllegalStateException("must be different players (simply create two instances)");
        }
        var board = new Stone[WIDTH * HEIGHT];
        red.initialize(Arrays.copyOf(board, board.length), Stone.RED);
        blue.initialize(Arrays.copyOf(board, board.length), Stone.BLUE);
        var lastMove = NOMOVE;
        var currentPlayer = red;
        for (int round = 0; round < board.length; round++) {
            var currentColor = currentPlayer == red ? Stone.RED : Stone.BLUE;
            System.out.println(HumanPlayer.toPrettyString(board) + currentColor + " to play next...");
            lastMove = currentPlayer.play(lastMove);
            if (lastMove < 0 || lastMove >= WIDTH * HEIGHT || board[lastMove] != null && (lastMove < WIDTH || board[lastMove - WIDTH] != null)) {
                throw new IllegalStateException("cannot play to position " + lastMove + " @ " + toDebugString(board));
            }
            board[lastMove] = currentColor;
            if (isWinning(board, currentColor)) {
                System.out.println(HumanPlayer.toPrettyString(board) + "...and the winner is: " + currentColor + " @ " + toDebugString(board));
                return currentPlayer;
            }
            currentPlayer = currentPlayer == red ? blue : red;
        }
        System.out.println(HumanPlayer.toPrettyString(board) + "...it's a DRAW @ " + toDebugString(board));
        return null; // null implies a draw
    }

    static boolean isWinning(Stone[] board, Stone forColor) {
        return wonUpDown(board, forColor)
                || wonLeftRight(board, forColor)
                || wonParallelLeftRight(board, forColor)
                || wonParallelRightLeft(board, forColor);
    }

    static boolean wonUpDown(Stone[] board, Stone forColor) {
        boolean win;

        for (int i = 21; i <= 27; i++) {
            win = board[i] == forColor && board[i - 7] == forColor && board[i - 14] == forColor && board[i - 21] == forColor;
            if (win) {
                return true;
            }
        }

        return false;
    }

    static boolean wonLeftRight(Stone[] board, Stone forColor) {
        boolean win;

        for (int i = 0; i < 24; i++) {
            if (i == 3) {
                if (board[i] == null) {
                    return false;
                }
            }
            if (i == 3 || i == 10 || i == 17) {
                if (board[i] != forColor) {
                    i = i + 4; // Skip to next line
                }
            }
            if (i <= 3 || (i >= 7 && i <= 10) || (i >= 14 && i <= 17) || i >= 21) {
                win = board[i] == forColor && board[i + 1] == forColor && board[i + 2] == forColor && board[i + 3] == forColor;
                if (win) {
                    return true;
                }
            }
        }

        return false;
    }

    static boolean wonParallelLeftRight(Stone[] board, Stone forColor) {
        boolean win;

        for (int i = 0; i <= 3; i++) {
            win = board[i] == forColor && board[i + 8] == forColor && board[i + 16] == forColor && board[i + 24] == forColor;
            if (win) {
                return true;
            }
        }

        return false;
    }

    static boolean wonParallelRightLeft(Stone[] board, Stone forColor) {
        boolean win;

        for (int i = 3; i <= 6; i++) {
            win = board[i] == forColor && board[i + 6] == forColor && board[i + 12] == forColor && board[i + 18] == forColor;
            if (win) {
                return true;
            }
        }

        return false;
    }

    public enum Stone {
        RED, BLUE;

        public Stone opponent() {
            return this == RED ? BLUE : RED;
        }
    }

    public interface Connect4Player {

        /**
         * Called before the game starts and guaranteed to only be called once per livetime of the player.
         *
         * @param board       the starting board, usually an empty board.
         * @param colorToPlay the color of this player
         */
        void initialize(Stone[] board, Stone colorToPlay);

        /**
         * Perform a next move, will only be called if the Game is not over yet.
         * Each player has to keep an internal state of the 4x7 board, wher the 0-index is on the bottom row.
         * The index-layout looks as:
         * <pre>
         * 30 31 32 33 34 35 36
         * 14 15 16 17 18 19 29
         *  7  8  9 10 11 12 13
         *  0  1  2  3  4  5  6
         * </pre>
         *
         * @param opponendPlayed the last index where the opponent played to (in range 0 - width*height exclusive)
         *                       or -1 if this is the first move.
         * @return an index to play to (in range 0 - width*height exclusive)
         */
        int play(int opponendPlayed);
    }

    /**
     * An abstract helper class to keep track of a board (and whatever we or the opponent played).
     */
    public abstract static class DefaultPlayer implements Connect4Player {

        Stone[] board;

        Stone myColor;

        @Override
        public void initialize(Stone[] board, Stone colorToPlay) {
            this.board = board;
            myColor = colorToPlay;
        }

        @Override
        public int play(int opponendPlayed) {
            if (opponendPlayed != NOMOVE) {
                board[opponendPlayed] = myColor.opponent();
            }
            var playTo = play();
            board[playTo] = myColor;
            return playTo;
        }

        /**
         * Givent the current {@link #board}, find a suitable position-index to play to.
         *
         * @return the position to play to as defined by {@link Connect4Player#play(int)}.
         */
        abstract int play();

    }

    public static class HumanPlayer extends DefaultPlayer {

        static String toPrettyString(Stone[] board) {
            var sb = new StringBuilder();
            for (int r = HEIGHT - 1; r >= 0; r--) {
                for (int c = 0; c < WIDTH; c++) {
                    var index = r * WIDTH + c;
                    if (board[index] == null) {
                        if (index < WIDTH || board[index - WIDTH] != null) {
                            sb.append("\033[37m").append(index).append("\033[0m ");
                            if (index < 10) {
                                sb.append(" ");
                            }
                        } else {
                            sb.append("\033[37m.\033[0m  ");
                        }
                    } else if (board[index] == Stone.RED) {
                        sb.append("\033[1;31mX\033[0m  ");
                    } else {
                        sb.append("\033[1;34mO\033[0m  ");
                    }
                }
                sb.append("\n");
            }
            return sb.toString();
        }

        @Override
        int play() {
            System.out.println("where to to put the next " + myColor + "?");
            var scanner = new Scanner(System.in, StandardCharsets.UTF_8);
            return Integer.parseInt(scanner.nextLine());
        }

    }

    public static class GreedyPlayer extends DefaultPlayer {

        @Override
        int play() {
            for (int c = 0; c < WIDTH; c++) {
                for (int r = 0; r < HEIGHT; r++) {
                    var index = r * WIDTH + c;
                    if (board[index] == null) {
                        return index;
                    }
                }
            }
            throw new IllegalStateException("cannot play at all");
        }
    }

    public static class Connect4AlphaBetaPlayer extends DefaultPlayer {
        private Integer bestMove = null;
        private int maxDepth;

        public Connect4AlphaBetaPlayer(int maxDepth) {
            this.maxDepth = maxDepth;
        }

        @Override
        int play() {
            bestMove = null;
            alphabeta(maxDepth, -10_000_000, 10_000_000, myColor);
            return bestMove;
        }

        public int alphabeta(int depth, int alpha, int beta, Stone forColor) {
            if (isWinning(board, forColor.opponent()))
                return -100_000;
            else if (isWinning(board, forColor))
                return 100_000;
            if (depth == 0)
                return rate(forColor);
            ArrayList<Integer> possibleMoves = getPossibleMoves(board);
            if (possibleMoves.size() == 0)
                return 0;  // it's a draw / no moves available

            int maxValue = alpha;
            for (Integer move : possibleMoves) {
                board[move] = forColor; // make a move
                int curValue = -alphabeta(depth - 1, -beta, -maxValue, forColor.opponent());
                board[move] = null;
                if (depth == this.maxDepth)
                    System.out.println("move " + move + " has score <=" + curValue);
                if (curValue > maxValue) {
                    maxValue = curValue;
                    if (depth == this.maxDepth)
                        this.bestMove = move;
                    if (maxValue >= beta)
                        break;

                }
            }
            return maxValue;
        }

        public ArrayList<Integer> getPossibleMoves(Stone[] myBoard) {
            ArrayList<Integer> possibleMoves = new ArrayList<>();
            for (Integer i : List.of(0, 1, 2, 3, 4, 5, 6)) {
                for (int j = i; j < board.length; j += 7) {
                    if (myBoard[j] == null) {
                        possibleMoves.add(j);
                        break;
                    }
                }
            }
            return possibleMoves;
        }

        public int rate(Stone stone) {
            int rating = 0;
            if (isWinning(board, stone))
                rating += 100_000;
            if (isWinning(board, stone.opponent()))
                rating += -100_000;
            for (int i = 0; i < board.length; i++) {
                if (board[i] == stone) {
                    switch (i % 7) {
                        case 0, 6 -> rating += 1;
                        case 1, 5 -> rating += 2;
                        case 2, 4 -> rating += 3;
                        case 3 -> rating += 4;
                    }
                }
            }
            return rating;
        }
    }
}
