import java.util.Arrays;

public class MergeSort {

    public static int CUTOFF = 1;

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

    private static void merge(int[] a, int[] aux, int lo, int mid, int hi) {
        for (int k = lo; k <= hi; k++) {
            aux[k] = a[k];
        }
        int i = lo;
        int j = mid + 1;
        for (int k = lo; k <= hi; k++) {
            if (i > mid) a[k] = aux[j++];
            else if (j > hi) a[k] = aux[i++];
            else if (aux[j] < aux[i]) a[k] = aux[j++];
            else a[k] = aux[i++];
        }
    }

    private static void sort(int[] a, int[] aux, int lo, int hi, int cutoff, boolean stopCheck) {
        if (hi <= lo + cutoff - 1) {
            insertionSort(a, lo, hi);
            return;
        }
        int mid = lo + (hi - lo) / 2;
        sort(a, aux, lo, mid, cutoff, stopCheck);
        sort(a, aux, mid + 1, hi, cutoff, stopCheck);
        if (stopCheck && a[mid + 1] >= a[mid]) return;
        merge(a, aux, lo, mid, hi);
    }

    public static void sort(int[] a, int cutoff, boolean stopCheck) {
        if (a == null || a.length <= 1) return;
        int[] aux = new int[a.length];
        sort(a, aux, 0, a.length - 1, cutoff, stopCheck);
    }

    public static void sort(int[] a, int cutoff) {
        sort(a, cutoff, false);
    }

    public static void sort(int[] a) {
        sort(a, CUTOFF, false);
    }

    public static void main(String[] args) {
        if (args.length > 0) {
            CUTOFF = Integer.parseInt(args[0]);
        }
        System.out.println("=== MergeSort - Test (CUTOFF=" + CUTOFF + ") ===");
        int[][] tests = {{}, {42}, {2, 1}, {5, 3, 1, 4, 2}, {10, 9, 8, 7, 6, 5, 4, 3, 2, 1}, {1, 1, 1, 1}, {1, 2, 3, 4, 5}};
        boolean allOk = true;
        int[] cutoffs = {1, 3, 5, 7, 10, 15};
        for (int[] test : tests) {
            int[] expected = test.clone();
            Arrays.sort(expected);
            for (int c : cutoffs) {
                int[] a = test.clone();
                sort(a, c, true);
                if (!Arrays.equals(a, expected)) {
                    System.out.printf("FAIL cutoff=%d, input=%s%n", c, Arrays.toString(test));
                    allOk = false;
                }
            }
        }
        System.out.println(allOk ? "[OK] All test cases passed." : "[FAIL] Errors found.");
    }
}
