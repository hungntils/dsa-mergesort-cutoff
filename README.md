# Mergesort CUTOFF Experiment

**Môn học:** Cấu trúc Dữ liệu và Giải thuật — Chủ đề 1.3: Algorithm Analysis  
**Sinh viên:** Nguyễn Tuấn Hưng · MSSV: 25021812  
**Trường:** Trường Đại học Công nghệ  

---

## Mô tả

Thí nghiệm tìm giá trị `CUTOFF` tối ưu cho thuật toán **Top-Down Mergesort** theo giáo trình *Algorithms, 4th Edition* (Sedgewick & Wayne, Princeton).

Cải tiến CUTOFF: khi mảng con có kích thước ≤ CUTOFF, chuyển sang dùng Insertion Sort thay vì tiếp tục đệ quy. Giáo trình đề xuất CUTOFF = 7 (bản cũ) và CUTOFF = 10 (bản mới). Thí nghiệm này đo thực nghiệm để tìm giá trị tốt nhất trên một máy cụ thể.

---

## Cấu trúc thư mục

```
CUTOFF/
├── .gitignore
├── README.md
├── chat-log/
│   └── chat-log.md          # Log chat với AI
├── data/
│   ├── MANIFEST.txt         # Danh sách và mô tả các file dữ liệu
│   ├── *.bin                # Dữ liệu nhị phân (do GenData.java sinh ra)
│   └── *.txt                # Dữ liệu văn bản (N ≤ 10,000, để kiểm tra)
├── figures/
│   └── *.png                # Đồ thị kết quả (do analyze.py sinh ra, 150 DPI)
├── report/
│   ├── report.pdf           # Báo cáo thí nghiệm (PDF)
│   └── report.tex           # Mã nguồn LaTeX của báo cáo
├── results/
│   ├── results.csv          # Dữ liệu đo thô (type, n, seed, rep, cutoff, ms)
│   └── summary.csv          # Thống kê tóm tắt (mean, std, CI 95%, norm_mean)
└── src/
    ├── MergeSort.java        # Mergesort (Top-Down) + biến CUTOFF
    ├── QuickSort.java        # Quicksort 1-pivot (tham khảo)
    ├── TestMergeSort.java    # 1500 test cases kiểm tra tính đúng đắn
    ├── GenData.java          # Sinh dữ liệu → data/
    ├── Bench.java            # Đo hiệu năng → results/results.csv
    └── analyze.py            # Phân tích thống kê + vẽ đồ thị
```

---

## Môi trường

| Thành phần | Giá trị |
|---|---|
| CPU | Intel Core i7-7600U @ 2.80 GHz |
| RAM | ~15.9 GB DDR4 |
| OS | Windows 10 (Build 19042) |
| JDK | Java 25.0.2 LTS (HotSpot 64-bit) |
| Python | 3.x với pandas, numpy, scipy, matplotlib |

---

## Hướng dẫn chạy

### 1. Biên dịch

```bash
cd src
javac MergeSort.java QuickSort.java TestMergeSort.java GenData.java Bench.java
```

### 2. Kiểm tra tính đúng đắn

```bash
cd src
java TestMergeSort
# Expected: Total: 1500 tests | PASS: 1500 | FAIL: 0
```

### 3. Chạy bản thử nhanh (~30 giây)

```bash
cd src
java GenData --quick
java -Xms2g -Xmx2g Bench --quick
cd ..
python src/analyze.py
```

> Sinh 8 file data (4 loại × 2 seeds, N = 10,000), đo 6 CUTOFF, xuất `results/results.csv`, `results/summary.csv`, và 4 đồ thị vào `figures/`.

### 4. Chạy bản đầy đủ (~18–20 phút)

```bash
cd src
java GenData
java -Xms4g -Xmx4g Bench
cd ..
python src/analyze.py
```

> Sinh 80 file data (4 loại × 4 kích thước × 5 seeds), đo 15 CUTOFF.

### 5. Vòng đo mịn (tùy chọn — nếu cực tiểu nằm trong [4..25])

```bash
cd src
java -Xms4g -Xmx4g Bench --fine --out ../results/results_fine.csv
cd ..
python src/analyze.py ../results/results_fine.csv
```

---

## Thiết kế thí nghiệm

| Tham số | Bản quick | Bản đầy đủ |
|---|---|---|
| Kích thước N | 10,000 | 10⁴, 10⁵, 10⁶, 2×10⁶ |
| Giá trị CUTOFF | {1, 3, 7, 10, 15, 25} | {1, 2, 3, 5, 7, 10, 12, 15, 20, 25, 30, 40, 50, 75, 100} |
| Loại dữ liệu | random, nearly, reversed, dups | (như bản quick) |
| Số seeds | 2 | 5 |
| Số lần lặp/seed | 3 | 10 |
| Chỉ số đo | Thời gian sort (ms) | (như bản quick) |

**Các biện pháp giảm nhiễu:** JIT warm-up 15 lần, clone mảng ngoài đoạn tính giờ, xáo thứ tự CUTOFF mỗi rep, cố định heap JVM (`-Xms = -Xmx`), kiểm tra `isSorted()` sau mỗi phép sort, thống kê bằng phân phối Student-t.

---

## Kết quả (Quick Test — N = 10,000, k = 2 seeds)

| Loại dữ liệu | CUTOFF tốt nhất | Mean (ms) | Tiết kiệm vs CUTOFF=1 |
|---|---|---|---|
| random | 25 | 0.749 | −17.8% |
| nearly | 25 | 0.283 | −43.6% |
| reversed | 15 | 0.282 | −41.9% |
| dups | 10 | 0.433 | −36.0% |

**Khoảng khuyến nghị tạm thời:** CUTOFF ∈ [7, 15] (cần chạy bản đầy đủ để xác nhận).

> ⚠️ Với k = 2 seeds (df = 1), t_crit = 12.706 → CI 95% rất rộng → phần lớn các so sánh chưa đủ ý nghĩa thống kê. Xem `report/report.pdf` để biết chi tiết.

---

## Phụ thuộc Python

```bash
pip install pandas numpy scipy matplotlib
```

---

## Tham khảo

- R. Sedgewick & K. Wayne, *Algorithms*, 4th Edition, Princeton University Press
- Lecture slides: *2.2 Mergesort*, *2.3 Quicksort*
