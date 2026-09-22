package app.algorithms.sorting;

import java.util.ArrayList;
import java.util.List;

public final class SortingAlgorithms {
    /** type, i, j, pivot, sortedUpTo, value (value used only by WRITE) */
    public record Op(int type, int i, int j, int pivot, int sortedUpTo, int value) {}

    public static final int COMPARE     = 0;
    public static final int SWAP        = 1;
    public static final int WRITE       = 2;
    public static final int MARK_SORTED = 3;
    public static final int PIVOT       = 4;

    public record Result(List<Op> ops, long comparisons, long swaps, long writes, long timeNanos) {}

    private SortingAlgorithms() {}

    public static Result bubble(int[] a) {
        List<Op> ops = new ArrayList<>();
        long comp = 0, swaps = 0;
        long t0 = System.nanoTime();
        int n = a.length;
        for (int i = 0; i < n - 1; i++) {
            for (int j = 0; j < n - 1 - i; j++) {
                comp++;
                ops.add(new Op(COMPARE, j, j + 1, -1, n - i, 0));
                if (a[j] > a[j + 1]) {
                    int t = a[j]; a[j] = a[j + 1]; a[j + 1] = t;
                    swaps++;
                    ops.add(new Op(SWAP, j, j + 1, -1, n - i, 0));
                }
            }
        }
        for (int i = 0; i < n; i++) ops.add(new Op(MARK_SORTED, i, 0, -1, 0, 0));
        return new Result(ops, comp, swaps, 0, System.nanoTime() - t0);
    }

    public static Result selection(int[] a) {
        List<Op> ops = new ArrayList<>();
        long comp = 0, swaps = 0;
        long t0 = System.nanoTime();
        int n = a.length;
        for (int i = 0; i < n - 1; i++) {
            int min = i;
            for (int j = i + 1; j < n; j++) {
                comp++;
                ops.add(new Op(COMPARE, j, min, -1, i, 0));
                if (a[j] < a[min]) min = j;
            }
            if (min != i) {
                int t = a[i]; a[i] = a[min]; a[min] = t;
                swaps++;
                ops.add(new Op(SWAP, i, min, -1, i, 0));
            }
            ops.add(new Op(MARK_SORTED, i, 0, -1, i, 0));
        }
        ops.add(new Op(MARK_SORTED, n - 1, 0, -1, n - 1, 0));
        return new Result(ops, comp, swaps, 0, System.nanoTime() - t0);
    }

    public static Result insertion(int[] a) {
        List<Op> ops = new ArrayList<>();
        long comp = 0, writes = 0;
        long t0 = System.nanoTime();
        int n = a.length;
        for (int i = 1; i < n; i++) {
            int key = a[i];
            int j = i - 1;
            while (j >= 0) {
                comp++;
                ops.add(new Op(COMPARE, j, j + 1, -1, i, 0));
                if (a[j] > key) {
                    a[j + 1] = a[j];
                    writes++;
                    ops.add(new Op(WRITE, j + 1, -1, -1, i, a[j]));
                    j--;
                } else break;
            }
            a[j + 1] = key;
            writes++;
            ops.add(new Op(WRITE, j + 1, -1, -1, i, key));
        }
        for (int i = 0; i < n; i++) ops.add(new Op(MARK_SORTED, i, 0, -1, 0, 0));
        return new Result(ops, comp, 0, writes, System.nanoTime() - t0);
    }

    public static Result merge(int[] a) {
        List<Op> ops = new ArrayList<>();
        long[] stat = {0, 0};                 // {comparisons, writes}
        long t0 = System.nanoTime();
        mergeSort(a, 0, a.length - 1, ops, stat);
        for (int i = 0; i < a.length; i++) ops.add(new Op(MARK_SORTED, i, 0, -1, 0, 0));
        return new Result(ops, stat[0], 0, stat[1], System.nanoTime() - t0);
    }

    private static void mergeSort(int[] a, int lo, int hi, List<Op> ops, long[] stat) {
        if (lo >= hi) return;
        int mid = (lo + hi) / 2;
        mergeSort(a, lo, mid, ops, stat);
        mergeSort(a, mid + 1, hi, ops, stat);

        int[] tmp = new int[hi - lo + 1];
        int i = lo, j = mid + 1, k = 0;
        while (i <= mid && j <= hi) {
            stat[0]++;
            ops.add(new Op(COMPARE, i, j, -1, -1, 0));
            if (a[i] <= a[j]) tmp[k++] = a[i++];
            else tmp[k++] = a[j++];
        }
        while (i <= mid) tmp[k++] = a[i++];
        while (j <= hi)  tmp[k++] = a[j++];

        for (int m = 0; m < tmp.length; m++) {
            a[lo + m] = tmp[m];
            stat[1]++;
            ops.add(new Op(WRITE, lo + m, -1, -1, -1, tmp[m]));
        }
    }

    public static Result quick(int[] a) {
        List<Op> ops = new ArrayList<>();
        long[] stat = {0, 0};
        long t0 = System.nanoTime();
        quickSort(a, 0, a.length - 1, ops, stat);
        for (int i = 0; i < a.length; i++) ops.add(new Op(MARK_SORTED, i, 0, -1, 0, 0));
        return new Result(ops, stat[0], stat[1], 0, System.nanoTime() - t0);
    }

    private static void quickSort(int[] a, int lo, int hi, List<Op> ops, long[] stat) {
        if (lo >= hi) return;
        int pivot = a[hi];
        ops.add(new Op(PIVOT, hi, -1, hi, -1, 0));
        int i = lo;
        for (int j = lo; j < hi; j++) {
            stat[0]++;
            ops.add(new Op(COMPARE, j, hi, hi, -1, 0));
            if (a[j] < pivot) {
                int t = a[i]; a[i] = a[j]; a[j] = t;
                stat[1]++;
                ops.add(new Op(SWAP, i, j, hi, -1, 0));
                i++;
            }
        }
        int t = a[i]; a[i] = a[hi]; a[hi] = t;
        stat[1]++;
        ops.add(new Op(SWAP, i, hi, hi, -1, 0));
        quickSort(a, lo, i - 1, ops, stat);
        quickSort(a, i + 1, hi, ops, stat);
    }

    public static Result heap(int[] a) {
        List<Op> ops = new ArrayList<>();
        long[] stat = {0, 0};
        long t0 = System.nanoTime();
        int n = a.length;
        for (int i = n / 2 - 1; i >= 0; i--) heapify(a, n, i, ops, stat);
        for (int i = n - 1; i > 0; i--) {
            int t = a[0]; a[0] = a[i]; a[i] = t;
            stat[1]++;
            ops.add(new Op(SWAP, 0, i, -1, i, 0));
            heapify(a, i, 0, ops, stat);
        }
        for (int i = 0; i < n; i++) ops.add(new Op(MARK_SORTED, i, 0, -1, 0, 0));
        return new Result(ops, stat[0], stat[1], 0, System.nanoTime() - t0);
    }

    private static void heapify(int[] a, int n, int i, List<Op> ops, long[] stat) {
        int largest = i;
        int l = 2 * i + 1, r = 2 * i + 2;
        if (l < n) {
            stat[0]++;
            ops.add(new Op(COMPARE, l, largest, -1, -1, 0));
            if (a[l] > a[largest]) largest = l;
        }
        if (r < n) {
            stat[0]++;
            ops.add(new Op(COMPARE, r, largest, -1, -1, 0));
            if (a[r] > a[largest]) largest = r;
        }
        if (largest != i) {
            int t = a[i]; a[i] = a[largest]; a[largest] = t;
            stat[1]++;
            ops.add(new Op(SWAP, i, largest, -1, -1, 0));
            heapify(a, n, largest, ops, stat);
        }
    }
}