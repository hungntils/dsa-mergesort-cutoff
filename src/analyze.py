"""
analyze.py
==========
Script phan tich thong ke va ve do thi cho thi nghiem CUTOFF Mergesort.
Su dung: pandas, numpy, scipy, matplotlib.

Cac chuc nang chinh:
  1. Tinh trung binh cac lan lap (rep) theo tung seed.
  2. Tinh Mean, Std, Khoang tin cay 95% (dung phan phoi Student-t, df = n_seeds - 1).
  3. Xuat bang summary.csv va in bang ket qua tong hop.
  4. Tim CUTOFF toi uu va vung CUTOFF co khoang tin cay chong lan (overlapping CI).
  5. Thuc hien kiem dinh t ghep cap (paired t-test) voi CUTOFF = 7 va CUTOFF = 10.
  6. Ve 4 nhom do thi chuan 150 DPI luu vao thu muc figures/:
     (a) Thoi gian theo CUTOFF (error bar cho moi N).
     (b) Thoi gian chuan hoa theo CUTOFF = 1 (toc do tuong doi).
     (c) Do thi rieng cho tung loai du lieu (random, nearly, reversed, dups).
     (d) Do thi phong to quanh vung toi uu (CUTOFF tu 2 den 30).
"""

import os
import sys
import numpy as np
import pandas as pd
from scipy import stats
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt

plt.rcParams['font.sans-serif'] = ['DejaVu Sans', 'Arial', 'Calibri', 'sans-serif']
plt.rcParams['axes.unicode_minus'] = False

def run_analysis(csv_file="../results/results.csv", out_dir="../figures", summary_file="../results/summary.csv"):
    if not os.path.exists(csv_file):
        print(f"[ERROR] Khong tim thay file {csv_file}")
        return

    os.makedirs(out_dir, exist_ok=True)
    df = pd.read_csv(csv_file)
    print(f"Đã nạp {len(df)} dòng dữ liệu từ {csv_file}")

    # 1. Trung binh cac lan lap (rep) trong cung mot seed
    # Day la buoc bat buoc de tranh thoi phong co mau (pseudoreplication)
    seed_avg = df.groupby(['type', 'n', 'seed', 'cutoff'])['ms'].mean().reset_index()

    # 2. Tinh thong ke qua cac seed cho tung (type, n, cutoff)
    summary_rows = []
    grouped = seed_avg.groupby(['type', 'n', 'cutoff'])

    for (dtype, n, cutoff), group in grouped:
        vals = group['ms'].values
        n_seeds = len(vals)
        mean_val = np.mean(vals)
        std_val = np.std(vals, ddof=1) if n_seeds > 1 else 0.0

        # Khoang tin cay 95% theo phan phoi Student-t
        if n_seeds > 1 and std_val > 0:
            t_crit = stats.t.ppf(0.975, df=n_seeds - 1)
            ci_margin = t_crit * (std_val / np.sqrt(n_seeds))
        else:
            ci_margin = 0.0

        ci_lower = mean_val - ci_margin
        ci_upper = mean_val + ci_margin

        summary_rows.append({
            'type': dtype,
            'n': n,
            'cutoff': cutoff,
            'n_seeds': n_seeds,
            'mean_ms': mean_val,
            'std_ms': std_val,
            'ci_margin': ci_margin,
            'ci95_lower': ci_lower,
            'ci95_upper': ci_upper
        })

    summary_df = pd.DataFrame(summary_rows)

    # Tinh thoi gian chuan hoa theo CUTOFF = 1 cho tung (type, n)
    baseline_map = summary_df[summary_df['cutoff'] == 1].set_index(['type', 'n'])['mean_ms'].to_dict()
    summary_df['norm_mean'] = summary_df.apply(
        lambda r: r['mean_ms'] / baseline_map.get((r['type'], r['n']), r['mean_ms']), axis=1
    )

    summary_df.to_csv(summary_file, index=False)
    print(f"✓ Đã ghi thống kê tóm tắt vào {summary_file}")

    # 3. Phan tich CUTOFF toi uu va kiem dinh thong ke Paired t-test
    print("\n" + "=" * 90)
    print(f"{'TYPE':<10} {'N':<10} {'BEST CUTOFF':<14} {'MEAN(ms)':<12} {'VÙNG CI TRÙNG LẶP':<22} {'vs C=7 (p-val)':<16} {'vs C=10 (p-val)':<16}")
    print("=" * 90)

    for (dtype, n), group in summary_df.groupby(['type', 'n']):
        # Tim cutoff co mean nho nhat
        best_row = group.loc[group['mean_ms'].idxmin()]
        best_c = int(best_row['cutoff'])
        best_mean = best_row['mean_ms']
        best_low = best_row['ci95_lower']
        best_up = best_row['ci95_upper']

        # Tim cac cutoff co khoang tin cay 95% chong lan voi best cutoff
        overlap_cutoffs = group[
            (group['ci95_lower'] <= best_up) & (group['ci95_upper'] >= best_low)
        ]['cutoff'].sort_values().values
        overlap_str = f"[{int(overlap_cutoffs.min())} .. {int(overlap_cutoffs.max())}]"

        # Paired t-test theo tung seed giua best va 7, 10
        seeds_data = seed_avg[(seed_avg['type'] == dtype) & (seed_avg['n'] == n)]
        
        def paired_test(c_ref):
            s_best = seeds_data[seeds_data['cutoff'] == best_c].sort_values('seed')['ms'].values
            s_ref = seeds_data[seeds_data['cutoff'] == c_ref].sort_values('seed')['ms'].values
            if len(s_best) == len(s_ref) and len(s_best) >= 2 and np.any(s_best != s_ref):
                diff_pct = (np.mean(s_ref) - np.mean(s_best)) / np.mean(s_ref) * 100
                res = stats.ttest_rel(s_ref, s_best)
                return f"{diff_pct:+.1f}% (p={res.pvalue:.3f})"
            elif c_ref == best_c:
                return "Là best"
            return "N/A"

        res_vs_7 = paired_test(7)
        res_vs_10 = paired_test(10)

        print(f"{dtype:<10} {n:<10} {best_c:<14} {best_mean:<12.4f} {overlap_str:<22} {res_vs_7:<16} {res_vs_10:<16}")

    print("=" * 90)

    # 4. Ve do thi
    draw_plots(summary_df, out_dir)


def draw_plots(summary_df, out_dir):
    types = summary_df['type'].unique()
    sizes = sorted(summary_df['n'].unique())

    # (a) Thoi gian theo CUTOFF cho du lieu RANDOM voi thanh sai so CI 95%
    plt.figure(figsize=(10, 6))
    rand_df = summary_df[summary_df['type'] == 'random']
    markers = ['o', 's', '^', 'D', 'v']

    for i, n in enumerate(sizes):
        subset = rand_df[rand_df['n'] == n].sort_values('cutoff')
        plt.errorbar(
            subset['cutoff'], subset['mean_ms'],
            yerr=subset['ci_margin'],
            label=f'N = {n:,}',
            marker=markers[i % len(markers)],
            capsize=4, capthick=1.2, linewidth=1.8
        )

    plt.title('Thời gian thực thi Mergesort theo CUTOFF (Dữ liệu Ngẫu nhiên)', fontsize=13, fontweight='bold')
    plt.xlabel('Ngưỡng CUTOFF', fontsize=11)
    plt.ylabel('Thời gian sắp xếp trung bình (ms)', fontsize=11)
    plt.grid(True, linestyle='--', alpha=0.6)
    plt.legend(fontsize=10)
    plt.tight_layout()
    plot_a_path = os.path.join(out_dir, 'cutoff_time_curves.png')
    plt.savefig(plot_a_path, dpi=150)
    plt.close()
    print(f"✓ Đã lưu đồ thị (a): {plot_a_path}")

    # (b) Thoi gian chuan hoa theo CUTOFF = 1 (Tốc độ tương đối)
    plt.figure(figsize=(10, 6))
    for i, n in enumerate(sizes):
        subset = rand_df[rand_df['n'] == n].sort_values('cutoff')
        plt.plot(
            subset['cutoff'], subset['norm_mean'] * 100,
            label=f'N = {n:,}',
            marker=markers[i % len(markers)],
            linewidth=1.8
        )

    plt.axhline(100, color='gray', linestyle=':', label='Mốc CUTOFF=1 (100%)')
    plt.title('Tốc độ chuẩn hóa theo CUTOFF = 1 (Dữ liệu Ngẫu nhiên)', fontsize=13, fontweight='bold')
    plt.xlabel('Ngưỡng CUTOFF', fontsize=11)
    plt.ylabel('Tỉ lệ thời gian so với CUTOFF=1 (%)', fontsize=11)
    plt.grid(True, linestyle='--', alpha=0.6)
    plt.legend(fontsize=10)
    plt.tight_layout()
    plot_b_path = os.path.join(out_dir, 'cutoff_normalized_curves.png')
    plt.savefig(plot_b_path, dpi=150)
    plt.close()
    print(f"✓ Đã lưu đồ thị (b): {plot_b_path}")

    # (c) Do thi so sanh giua 4 loai du lieu tren cung mot kich thuoc N lon nhat
    max_n = max(sizes)
    plt.figure(figsize=(10, 6))
    type_labels = {
        'random': 'Ngẫu nhiên đều (Random)',
        'nearly': 'Gần như đã sắp (Nearly sorted)',
        'reversed': 'Đảo ngược (Reversed)',
        'dups': 'Nhiều khóa trùng lặp (Duplicates)'
    }

    for dtype in types:
        subset = summary_df[(summary_df['type'] == dtype) & (summary_df['n'] == max_n)].sort_values('cutoff')
        if not subset.empty:
            plt.plot(
                subset['cutoff'], subset['norm_mean'] * 100,
                label=type_labels.get(dtype, dtype),
                marker='o', linewidth=1.8
            )

    plt.axhline(100, color='gray', linestyle=':', label='Mốc CUTOFF=1 (100%)')
    plt.title(f'So sánh hành vi CUTOFF theo 4 loại dữ liệu (N = {max_n:,})', fontsize=13, fontweight='bold')
    plt.xlabel('Ngưỡng CUTOFF', fontsize=11)
    plt.ylabel('Tỉ lệ thời gian so với CUTOFF=1 (%)', fontsize=11)
    plt.grid(True, linestyle='--', alpha=0.6)
    plt.legend(fontsize=10)
    plt.tight_layout()
    plot_c_path = os.path.join(out_dir, 'cutoff_data_types_comparison.png')
    plt.savefig(plot_c_path, dpi=150)
    plt.close()
    print(f"✓ Đã lưu đồ thị (c): {plot_c_path}")

    # (d) Ban phong to quanh vung toi uu (CUTOFF tu 2 den 30)
    plt.figure(figsize=(10, 6))
    for i, n in enumerate(sizes):
        subset = rand_df[(rand_df['n'] == n) & (rand_df['cutoff'] <= 35) & (rand_df['cutoff'] >= 2)].sort_values('cutoff')
        if not subset.empty:
            plt.errorbar(
                subset['cutoff'], subset['mean_ms'],
                yerr=subset['ci_margin'],
                label=f'N = {n:,}',
                marker=markers[i % len(markers)],
                capsize=3, linewidth=1.8
            )

    plt.title('Phóng to vùng tối ưu của CUTOFF (từ 2 đến 35 - Dữ liệu Ngẫu nhiên)', fontsize=13, fontweight='bold')
    plt.xlabel('Ngưỡng CUTOFF', fontsize=11)
    plt.ylabel('Thời gian sắp xếp (ms)', fontsize=11)
    plt.axvline(7, color='green', linestyle='--', alpha=0.7, label='CUTOFF = 7 (Giáo trình cũ)')
    plt.axvline(10, color='red', linestyle='--', alpha=0.7, label='CUTOFF = 10 (Giáo trình mới)')
    plt.grid(True, linestyle='--', alpha=0.6)
    plt.legend(fontsize=10)
    plt.tight_layout()
    plot_d_path = os.path.join(out_dir, 'cutoff_zoom_optimal.png')
    plt.savefig(plot_d_path, dpi=150)
    plt.close()
    print(f"✓ Đã lưu đồ thị (d): {plot_d_path}")


if __name__ == "__main__":
    csv_input = sys.argv[1] if len(sys.argv) > 1 else "../results/results.csv"
    run_analysis(csv_file=csv_input)
