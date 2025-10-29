package backend;

import java.util.Random;

public class TicTacToeBackend {
    private final char[] board = new char[9];
    private char winner = ' ';
    private final Random rand = new Random();

    public TicTacToeBackend() { reset(); }

    public void reset() {
        for (int i = 0; i < 9; i++) board[i] = '-';
        winner = ' ';
    }

    public char[] getBoard() { return board; }
    public char getWinner() { return winner; }
    public boolean isGameOver() { return winner != ' ' || isBoardFull(); }
    private boolean isBoardFull() {
        for (char c : board) if (c == '-') return false;
        return true;
    }

    // player's move (X)
    public boolean playerMove(int pos) {
        if (pos < 0 || pos >= 9 || board[pos] != '-') return false;
        board[pos] = 'X';
        checkWinner();
        return true;
    }

    // simple random AI (O)
    public void computerMove() {
        if (isGameOver()) return;
        int[] empties = java.util.stream.IntStream.range(0,9)
                .filter(i -> board[i] == '-').toArray();
        if (empties.length == 0) return;
        board[empties[rand.nextInt(empties.length)]] = 'O';
        checkWinner();
    }

    // used by UI to place any symbol (if needed)
    public boolean placeSymbol(int pos, char s) {
        if (pos < 0 || pos >= 9 || board[pos] != '-') return false;
        board[pos] = s; checkWinner(); return true;
    }

    private void checkWinner() {
        int[][] wins = {
                {0,1,2},{3,4,5},{6,7,8},
                {0,3,6},{1,4,7},{2,5,8},
                {0,4,8},{2,4,6}
        };
        for (var w : wins) {
            if (board[w[0]] != '-' &&
                    board[w[0]] == board[w[1]] &&
                    board[w[1]] == board[w[2]]) {
                winner = board[w[0]];
                return;
            }
        }
        if (isBoardFull()) winner = 'D'; // D = draw
    }

    public void setState(char[] board, char winner) {
        for (int i = 0; i < 9; i++) this.board[i] = board[i];
        this.winner = winner;
    }
}
