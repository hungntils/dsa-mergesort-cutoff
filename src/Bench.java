import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.*;

/**
 * Bench.java
 * ==========
 * Chuong trinh do hieu nang thuc nghiem cua Mergesort voi cac gia tri CUTOFF khac nhau.
 * 
 * Cac ky thuat giam thieu nhieu (Noise Reduction):
 *   1. Warm-up JIT truoc khi bat dau do tung co N de JVM bien dich ma may toi uu.
 *   2. Clone mang du lieu ngoai khoang thoi gian do (chi do dung thoi gian sort).
 *   3. Xao tron ngau nhien thu tu chay cua cac gia tri CUTOFF trong moi rep de loai bo hieu ung bo nho dem / JIT co he thong.
 *   4. Kiem tra tinh dung dan (isSorted) sau moi phep sort de dam bao ket qua tuyet doi chinh xac.
 *   5. Ghi ket qua vao results.csv ngay sau moi seed (flush ngay lap tuc) de khong bao gio mat du lieu.
 */
public class Bench {

    private static final String DATA_DIR = "../data/";
    private static final String DEFAULT_OUT_CSV = "../results/results.csv";

    // Kiem tra tinh dung dan cua mang da sap xep
    public static boolean isSorted(int[] a) {
        for (int i = 1; i < a.length; i++) {
            if (a[i] < a[i - 1]) return false;
        }
        return true;
    }

    // Doc mang tu file nhi phan .bin
    public static int[] loadBinary(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file);
             FileChannel channel = fis.getChannel()) {
            
            // 4 byte dau ghi N
            ByteBuffer header = ByteBuffer.allocate(4);
            while (header.hasRemaining()) {
                if (channel.read(header) == -1) {
                    throw new EOFException("Unexpected EOF reading header from " + file.getName());
                }
            }
            header.flip();
            int n = header.getInt();

            int[] a = new int[n];
            int bufferSize = 1024 * 1024; // 1MB buffer
            ByteBuffer buffer = ByteBuffer.allocateDirect(bufferSize);

            int idx = 0;
            while (idx < n) {
                buffer.clear();
                int bytesRead = channel.read(buffer);
                if (bytesRead == -1) break;
                buffer.flip();
                while (buffer.remaining() >= 4 && idx < n) {
                    a[idx++] = buffer.getInt();
                }
            }
            return a;
        }
    }

    // Warm-up JIT compiler de loai bo overhead khoi dong JVM
    private static void warmUp(int n) {
        System.out.printf("  [Warm-up JIT] Running warm-up for N=%d... ", n);
        Random rng = new Random(12345);
        int[] dummy = new int[Math.min(n, 100_000)];
        for (int i = 0; i < dummy.length; i++) dummy[i] = rng.nextInt();

        int[] cutoffs = {1, 5, 10, 20};
        for (int iter = 0; iter < 15; iter++) {
            for (int c : cutoffs) {
                int[] work = dummy.clone();
                MergeSort.sort(work, c, false);
            }
        }
        System.out.println("Done.");
    }

    public static void main(String[] args) throws Exception {
        boolean quick = false;
        boolean fine = false;
        String customCutoffsStr = null;
        String outCsvPath = DEFAULT_OUT_CSV;

        for (int i = 0; i < args.length; i++) {
            if (args[i].equalsIgnoreCase("--quick")) quick = true;
            else if (args[i].equalsIgnoreCase("--fine")) fine = true;
            else if (args[i].equalsIgnoreCase("--out") && i + 1 < args.length) outCsvPath = args[++i];
            else if (args[i].equalsIgnoreCase("--cutoffs") && i + 1 < args.length) customCutoffsStr = args[++i];
        }

        // Danh sach CUTOFF
        List<Integer> cutoffs = new ArrayList<>();
        if (customCutoffsStr != null) {
            for (String part : customCutoffsStr.split(",")) {
                cutoffs.add(Integer.parseInt(part.trim()));
            }
        } else if (fine) {
            // Vong do min: cac so nguyen tu 4 den 25
            for (int c = 4; c <= 25; c++) cutoffs.add(c);
        } else if (quick) {
            // Vong thu nhanh: lay mau dai dien
            cutoffs.addAll(Arrays.asList(1, 3, 7, 10, 15, 25));
        } else {
            // Vong tho day du (Coarse benchmark)
            cutoffs.addAll(Arrays.asList(1, 2, 3, 5, 7, 10, 12, 15, 20, 25, 30, 40, 50, 75, 100));
        }

        int[] sizes = quick ? new int[]{10_000} : new int[]{10_000, 100_000, 1_000_000, 2_000_000};
        long[] seeds = quick ? new long[]{42, 43} : new long[]{42, 43, 44, 45, 46};
        String[] types = {"random", "nearly", "reversed", "dups"};
        int reps = quick ? 3 : 10;

        File outCsv = new File(outCsvPath);
        if (outCsv.getParentFile() != null && !outCsv.getParentFile().exists()) {
            outCsv.getParentFile().mkdirs();
        }
        boolean writeHeader = !outCsv.exists() || outCsv.length() == 0;

        System.out.println("=== Bench: Running Mergesort CUTOFF Benchmark ===");
        System.out.printf("Output: %s%n", outCsv.getAbsolutePath());
        System.out.printf("Mode: %s | Reps: %d | Cutoffs (%d): %s%n",
                (quick ? "QUICK" : (fine ? "FINE" : "FULL")), reps, cutoffs.size(), cutoffs);
        System.out.printf("Sizes: %s | Seeds: %s%n", Arrays.toString(sizes), Arrays.toString(seeds));

        File dataDir = new File(DATA_DIR);

        try (PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(outCsv, true)))) {
            if (writeHeader) {
                writer.println("type,n,seed,rep,cutoff,ms");
                writer.flush();
            }

            Random rngOrder = new Random(98765);

            for (int n : sizes) {
                System.out.printf("%n>>> Benchmarking N = %d <<<%n", n);
                // Warm-up JIT compiler cho kich thuoc N
                warmUp(n);

                for (String type : types) {
                    for (long seed : seeds) {
                        String fileName = String.format("%s_%d_s%d.bin", type, n, seed);
                        File dataFile = new File(dataDir, fileName);

                        if (!dataFile.exists()) {
                            System.err.printf("  [MISSING] Data file not found: %s (Skipping)%n", dataFile.getPath());
                            continue;
                        }

                        // Doc mang ban goc vao bo nho (1 lan duy nhat cho moi seed)
                        int[] original = loadBinary(dataFile);
                        System.out.printf("  Running [%s | N=%d | seed=%d] ... ", type, n, seed);
                        long seedStartTime = System.currentTimeMillis();

                        for (int rep = 0; rep < reps; rep++) {
                            // Xao tron ngau nhien danh sach CUTOFF trong moi rep de triet tieu cache bias
                            List<Integer> shuffledCutoffs = new ArrayList<>(cutoffs);
                            Collections.shuffle(shuffledCutoffs, rngOrder);

                            for (int cutoff : shuffledCutoffs) {
                                // Clone mang NGOAI khoang thoi gian do
                                int[] work = original.clone();

                                long t0 = System.nanoTime();
                                MergeSort.sort(work, cutoff, false);
                                long t1 = System.nanoTime();

                                double ms = (t1 - t0) / 1_000_000.0;

                                // Kiem tra tinh dung dan nghiem ngat
                                if (!isSorted(work)) {
                                    throw new IllegalStateException(String.format(
                                            "Correctness verification failed! type=%s, N=%d, seed=%d, cutoff=%d",
                                            type, n, seed, cutoff));
                                }

                                writer.printf(Locale.US, "%s,%d,%d,%d,%d,%.4f%n",
                                        type, n, seed, rep, cutoff, ms);
                            }
                        }

                        // Flush sau moi seed de dam bao an toan du lieu
                        writer.flush();
                        long seedElapsed = System.currentTimeMillis() - seedStartTime;
                        System.out.printf("OK (%.2fs)%n", seedElapsed / 1000.0);
                    }
                }
            }
        }

        System.out.println("\n=== Benchmark Completed Successfully! ===");
        System.out.printf("Results written to: %s%n", outCsv.getAbsolutePath());
    }
}
