import os

def scan_kt_files():
    # 获取当前脚本所在的目录
    current_dir = os.getcwd()
    output_file = "scan_result.txt"
    
    target_str = "import android."
    results = []

    print("开始扫描...")
    
    # os.walk 会自动递归遍历所有子文件夹
    for root, dirs, files in os.walk(current_dir):
        for file in files:
            # 只处理 .kt 结尾的文件
            if file.endswith('.kt'):
                file_path = os.path.join(root, file)
                
                try:
                    # 使用 utf-8 编码读取，errors='ignore' 防止因为特殊字符报错中断
                    with open(file_path, 'r', encoding='utf-8', errors='ignore') as f:
                        for line_num, line in enumerate(f, 1):
                            if target_str in line:
                                # 相对路径更易读，这里转换一下
                                relative_path = os.path.relpath(file_path, current_dir)
                                results.append(f"文件: {relative_path} | 行号: {line_num} | 内容: {line.strip()}")
                except Exception as e:
                    print(f"无法读取文件 {file_path}: {e}")

    # 将结果写入 txt 文件
    with open(output_file, 'w', encoding='utf-8') as out_f:
        if results:
            out_f.write(f"共找到 {len(results)} 处匹配：\n\n")
            out_f.write("\n".join(results))
            print(f"扫描完成！结果已写入到 {output_file}，共找到 {len(results)} 处匹配。")
        else:
            out_f.write("未找到包含 'import android.' 的 .kt 文件。")
            print(f"扫描完成！未找到匹配项，结果已记录在 {output_file}。")

if __name__ == "__main__":
    scan_kt_files()