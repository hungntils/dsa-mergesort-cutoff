import java.util.Arrays;
import java.util.Random;

public class TestMergeSort {

    public static boolean isSorted(int[] a, int lo, int hi) {
        for (int i = lo + 1; i <= hi; i++) {
            if (a[i] < a[i - 1]) return false;
        }
        return true;
    }

    public static boolean isSorted(int[] a) {
        return a.length <= 1 || isSorted(a, 0, a.length - 1);
    }

    private static int[] referenceSorted(int[] a) {
        int[] b = a.clone();
        for (int i = 1; i < b.length; i++) {
            int key = b[i];
            int j = i - 1;
            while (j >= 0 && b[j] > key) {
                b[j + 1] = b[j];
                j--;
            }
            b[j + 1] = key;
        }
        return b;
    }

    private static boolean runTest(String label, int[] input, int cutoff) {
        int[] expected = referenceSorted(input);
        int[] a = input.clone();
        MergeSort.sort(a, cutoff, false);
        boolean sorted = isSorted(a);
        boolean correct = Arrays.equals(a, expected);
        if (!sorted || !correct) {
            System.out.printf("  FAIL [%s] cutoff=%-3d sorted=%b correct=%b input=%s%n",
                    label, cutoff, sorted, correct, Arrays.toString(input));
            return false;
        }
        return true;
    }

    public static void main(String[] args) {
        int[] CUTOFFS = {1, 2, 3, 5, 7, 10, 12, 15, 20, 25, 30, 40, 50, 75, 100};
        Random rng = new Random(42);

        int totalTests = 0;
        int failCount = 0;

        System.out.println("=== Group 1: Special Cases ===");
        int[][] specials = {
            {},
            {0},
            {-1, 1},
            {1, -1},
            {5, 5, 5, 5, 5},
            {1, 2, 3, 4, 5, 6, 7, 8, 9, 10},
            {10, 9, 8, 7, 6, 5, 4, 3, 2, 1},
            {1, 1, 2, 2, 3, 3},
            {Integer.MIN_VALUE, 0, Integer.MAX_VALUE},
        };
        String[] specialLabels = {
            "empty", "single", "2-asc", "2-desc",
            "all-equal", "sorted-10", "reversed-10", "dup-pairs", "int-extremes"
        };
        for (int ci = 0; ci < specials.length; ci++) {
            for (int cutoff : CUTOFFS) {
                totalTests++;
                if (!runTest(specialLabels[ci], specials[ci], cutoff)) failCount++;
            }
        }

        System.out.println("=== Group 2: Random Arrays ===");
        int[] sizes = {0, 1, 2, 3, 5, 7, 10, 15, 20, 50, 100, 200, 500, 1000, 5000};
        int REPS = 5;
        for (int n : sizes) {
            for (int rep = 0; rep < REPS; rep++) {
                int[] arr = new int[n];
                for (int k = 0; k < n; k++) arr[k] = rng.nextInt(201) - 100;
                for (int cutoff : CUTOFFS) {
                    totalTests++;
                    if (!runTest("rand-n" + n + "-r" + rep, arr, cutoff)) failCount++;
                }
            }
        }

        System.out.println("=== Group 3: Arrays with Many Duplicates ===");
        int[] dupSizes = {10, 50, 200, 1000};
        int[] numKeys = {1, 2, 3, 5};
        for (int n : dupSizes) {
            for (int keys : numKeys) {
                int[] arr = new int[n];
                for (int k = 0; k < n; k++) arr[k] = rng.nextInt(keys);
                for (int cutoff : CUTOFFS) {
                    totalTests++;
                    if (!runTest("dup-n" + n + "-k" + keys, arr, cutoff)) failCount++;
                }
            }
        }

        System.out.println();
        System.out.printf("Total: %d tests | PASS: %d | FAIL: %d%n",
                totalTests, totalTests - failCount, failCount);
        if (failCount == 0) {
            System.out.println("[OK] All test cases passed. MergeSort is correct for all CUTOFF values.");
        } else {
            System.out.println("[FAIL] Errors found! See details above.");
        }
    }
}
