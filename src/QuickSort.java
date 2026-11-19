import java.util.Arrays;
import java.util.Random;

public class QuickSort {
    public static int CUTOFF = 1;
    private static final Random RNG = new Random();

    private static void insertionSort(int[] a, int lo, int hi) {
        for (int i = lo + 1; i <= hi; i++) {
            int key = a[i];
            int j = i - 1;
            while (j >= lo && a[j] > key) {
                a[j + 1] = a[j];
                j--;
            }
            a[j + 1] = key;
        }
    }

    private static void shuffle(int[] a) {
        for (int i = a.length - 1; i > 0; i--) {
            int j = RNG.nextInt(i + 1);
            int tmp = a[i]; a[i] = a[j]; a[j] = tmp;
        }
    }

    private static int partition(int[] a, int lo, int hi) {
        int v = a[lo];
        int i = lo;
        int j = hi + 1;
        while (true) {
            while (++i <= hi && a[i] < v) {}
            while (--j >= lo && a[j] > v) {}
            if (i >= j) break;
            int tmp = a[i]; a[i] = a[j]; a[j] = tmp;
        }
        int tmp = a[lo]; a[lo] = a[j]; a[j] = tmp;
        return j;
    }

    private static void sort(int[] a, int lo, int hi, int cutoff) {
        if (hi <= lo + cutoff - 1) {
            insertionSort(a, lo, hi);
            return;
        }
        int j = partition(a, lo, hi);
        sort(a, lo, j - 1, cutoff);
        sort(a, j + 1, hi, cutoff);
    }

    public static void sort(int[] a, int cutoff, boolean doShuffle) {
        if (a == null || a.length <= 1) return;
        if (doShuffle) shuffle(a);
        sort(a, 0, a.length - 1, cutoff);
    }

    public static void sort(int[] a, int cutoff) {
        sort(a, cutoff, true);
    }

    public static void sort(int[] a) {
        sort(a, CUTOFF, true);
    }

    public static void main(String[] args) {
        if (args.length > 0) {
            CUTOFF = Integer.parseInt(args[0]);
        }
        System.out.println("=== QuickSort - Correctness Test (CUTOFF=" + CUTOFF + ") ===");
        int[][] tests = {
            {},
            {42},
            {2, 1},
            {5, 3, 1, 4, 2},
            {10, 9, 8, 7, 6, 5, 4, 3, 2, 1},
            {1, 1, 1, 1},
            {1, 2, 3, 4, 5},
        };
        boolean allOk = true;
        int[] cutoffs = {1, 3, 5, 7, 10, 15};
        for (int[] test : tests) {
            int[] expected = test.clone();
            Arrays.sort(expected);
            for (int c : cutoffs) {
                for (int run = 0; run < 10; run++) {
                    int[] a = test.clone();
                    sort(a, c, true);
                    if (!Arrays.equals(a, expected)) {
                        System.out.printf("FAIL cutoff=%d, input=%s%n", c, Arrays.toString(test));
                        allOk = false;
                    }
                }
            }
        }
        System.out.println(allOk ? "[OK] All test cases passed." : "[FAIL] Errors found!");
    }
}
