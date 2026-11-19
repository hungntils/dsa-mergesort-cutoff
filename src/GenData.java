import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Random;

/**
 * GenData.java
 * ============
 * Sinh bo du lieu thi nghiem cho danh gia hieu nang CUTOFF tren Mergesort.
 * 
 * Dinh dang file:
 *   - File nhi phan .bin: 4 byte dau ghi do dai N, tiep theo la N so nguyen 32-bit (int).
 *   - File van ban .txt: sinh cho N <= 10000 de de kiem tra bang mat thuong.
 * 
 * Quy uoc dat ten file: <type>_<N>_s<seed>.bin
 */
public class GenData {

    private static final String DATA_DIR = "../data/";

    public static int[] generateArray(String type, int n, long seed) {
        Random rng = new Random(seed);
        int[] a = new int[n];

        switch (type.toLowerCase()) {
            case "random":
                // Phan bo deu tren toan dai gia tri int duong
                for (int i = 0; i < n; i++) {
                    a[i] = rng.nextInt();
                }
                break;

            case "nearly":
                // Mang gan nhu da sap: sap xep tang dan roi hoan doi mot so luong nho (1% cap)
                for (int i = 0; i < n; i++) {
                    a[i] = i;
                }
                int swaps = Math.max(1, n / 100);
                for (int s = 0; s < swaps; s++) {
                    int i = rng.nextInt(n);
                    int j = rng.nextInt(n);
                    int tmp = a[i]; a[i] = a[j]; a[j] = tmp;
                }
                break;

            case "reversed":
                // Mang giam dan hoan toan
                for (int i = 0; i < n; i++) {
                    a[i] = n - i;
                }
                break;

            case "dups":
                // Mang nhieu phan tu trung lap: chi co 10 gia tri khoa khac nhau (0 den 9)
                for (int i = 0; i < n; i++) {
                    a[i] = rng.nextInt(10);
                }
                break;

            default:
                throw new IllegalArgumentException("Unknown data type: " + type);
        }
        return a;
    }

    public static void saveBinary(File file, int[] a) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(file);
             FileChannel channel = fos.getChannel()) {
            
            // Buffer chua N va cac phan tu (dung direct buffer hoac heap buffer chia khoi 1MB)
            int bufferSize = 1024 * 1024; // 1MB buffer
            ByteBuffer buffer = ByteBuffer.allocateDirect(bufferSize);

            // Ghi 4 byte dau la do dai N
            buffer.putInt(a.length);

            for (int val : a) {
                if (buffer.remaining() < 4) {
                    buffer.flip();
                    while (buffer.hasRemaining()) channel.write(buffer);
                    buffer.clear();
                }
                buffer.putInt(val);
            }
            if (buffer.position() > 0) {
                buffer.flip();
                while (buffer.hasRemaining()) channel.write(buffer);
            }
        }
    }

    public static void saveText(File file, int[] a) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(String.valueOf(a.length));
            writer.newLine();
            for (int val : a) {
                writer.write(String.valueOf(val));
                writer.newLine();
            }
        }
    }

    public static void main(String[] args) {
        boolean quick = false;
        for (String arg : args) {
            if (arg.equalsIgnoreCase("--quick")) quick = true;
        }

        int[] sizes = quick ? new int[]{10_000} : new int[]{10_000, 100_000, 1_000_000, 2_000_000};
        long[] seeds = quick ? new long[]{42, 43} : new long[]{42, 43, 44, 45, 46};
        String[] types = {"random", "nearly", "reversed", "dups"};

        File dir = new File(DATA_DIR);
        if (!dir.exists()) dir.mkdirs();

        System.out.println("=== GenData: Generating Benchmark Datasets ===");
        System.out.printf("Mode: %s | Sizes: %d | Seeds: %d | Types: %d%n",
                (quick ? "QUICK" : "FULL"), sizes.length, seeds.length, types.length);

        int totalFiles = 0;
        long startTime = System.currentTimeMillis();

        for (int n : sizes) {
            for (String type : types) {
                for (long seed : seeds) {
                    String baseName = String.format("%s_%d_s%d", type, n, seed);
                    File binFile = new File(dir, baseName + ".bin");

                    if (binFile.exists()) {
                        System.out.printf("  [SKIP] %s exists.%n", binFile.getName());
                        continue;
                    }

                    int[] a = generateArray(type, n, seed);
                    try {
                        saveBinary(binFile, a);
                        totalFiles++;

                        // Luu them ban text cho N nho de de dang kiem tra/doc truc tiep
                        if (n <= 10_000) {
                            File txtFile = new File(dir, baseName + ".txt");
                            saveText(txtFile, a);
                        }
                        System.out.printf("  [OK] Generated %s (N=%d, %d bytes)%n",
                                binFile.getName(), n, binFile.length());
                    } catch (IOException e) {
                        System.err.printf("  [FAIL] Error writing %s: %s%n", binFile.getName(), e.getMessage());
                    }
                }
            }
        }

        long elapsed = System.currentTimeMillis() - startTime;
        System.out.printf("Done! Generated %d files in %.2f seconds.%n", totalFiles, elapsed / 1000.0);
    }
}
