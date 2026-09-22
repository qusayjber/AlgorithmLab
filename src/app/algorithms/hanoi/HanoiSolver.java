package app.algorithms.hanoi;

import java.util.ArrayList;
import java.util.List;

public final class HanoiSolver {
    public record Frame(int n, char src, char aux, char dst) {}
    public record Step(int moveNumber, int disk, char from, char to, List<Frame> stack, int codeLine) {}

    public static final int LINE_IF       = 1;
    public static final int LINE_BASE     = 2;
    public static final int LINE_RECURSE1 = 3;
    public static final int LINE_MOVE     = 4;
    public static final int LINE_RECURSE2 = 5;

    private HanoiSolver() {}

    public static List<Step> solve(int n) {
        List<Step> steps = new ArrayList<>();
        List<Frame> stack = new ArrayList<>();
        rec(n, 'A', 'C', 'B', steps, stack);
        return steps;
    }

    private static void rec(int n, char src, char dst, char aux, List<Step> steps, List<Frame> stack) {
        stack.add(new Frame(n, src, aux, dst));
        if (n == 1) {
            steps.add(new Step(steps.size() + 1, 1, src, dst, List.copyOf(stack), LINE_MOVE));
            stack.remove(stack.size() - 1);
            return;
        }
        rec(n - 1, src, aux, dst, steps, stack);
        steps.add(new Step(steps.size() + 1, n, src, dst, List.copyOf(stack), LINE_MOVE));
        rec(n - 1, aux, dst, src, steps, stack);
        stack.remove(stack.size() - 1);
    }

    public static long minMoves(int n) { return (1L << n) - 1; }
}