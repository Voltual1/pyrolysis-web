import os
import re


def find_path_blocks(content):
    """使用状态机/花括号计数法，精准提取所有 path(...) { ... } 块的起止位置"""
    matches = []
    # 寻找 path( 或 path    ( 这样的起点
    start_indices = [m.start() for m in re.finditer(r"\bpath\s*\(", content)]

    for start_idx in start_indices:
        # 从 path 开始往后找第一个左花括号 '{'
        brace_idx = content.find("{", start_idx)
        if brace_idx == -1:
            continue

        # 验证 path( 到 { 之间是否包含了右括号 ')', 确保这是个带闭包的 path 调用
        header_zone = content[start_idx:brace_idx]
        if ")" not in header_zone:
            continue

        # 开始数花括号，寻找匹配的闭合右花括号 '}'
        brace_count = 1
        end_idx = -1
        for i in range(brace_idx + 1, len(content)):
            if content[i] == "{":
                brace_count += 1
            elif content[i] == "}":
                brace_count -= 1

            if brace_count == 0:
                end_idx = i + 1
                break

        if end_idx != -1:
            # 成功捕获一个完整的闭包段
            header = content[start_idx:brace_idx] + "{"
            body = content[brace_idx + 1 : end_idx - 1]
            footer = "}"
            matches.append(
                {
                    "start": start_idx,
                    "end": end_idx,
                    "header": header,
                    "body": body,
                    "footer": footer,
                }
            )

    return matches


def split_large_kt_icon_content(content, max_lines_per_path=5):
    """通用图标转换逻辑"""
    content = content.replace("\r\n", "\n")

    # 1. 提取图标变量名（允许极其宽松的空格和换行）
    name_match = re.search(r"val\s+(\w+)\s*:\s*ImageVector", content)
    if not name_match:
        # 放宽政策：有时候可能没有 : ImageVector 而是直接 val Fire get()
        name_match = re.search(r"val\s+(\w+)\s*", content)
        if not name_match:
            return None, 0, "无法识别任何 val 变量声明，跳过。"

    icon_name = name_match.group(1)
    func_prefix = f"_{icon_name[0].lower()}{icon_name[1:]}"

    # 2. 通过花括号计数器获取所有 path 块
    blocks = find_path_blocks(content)
    if not blocks:
        return None, 0, "未在文件中找到任何标准的 path(...) { ... } 闭包结构。"

    new_functions = []
    sub_func_count = 0
    total_lines_found = 0

    # 倒序替换，防止索引漂移
    for block in reversed(blocks):
        path_header = block["header"]
        path_body = block["body"]
        path_footer = block["footer"]

        raw_lines = path_body.split("\n")
        body_lines = [line for line in raw_lines if line.strip()]
        total_lines_found += len(body_lines)

        if len(body_lines) <= max_lines_per_path:
            continue

        chunked_invocations = []
        for i in range(0, len(body_lines), max_lines_per_path):
            chunk = body_lines[i : i + max_lines_per_path]
            sub_func_name = f"{func_prefix}Path{sub_func_count}"
            sub_func_count += 1

            chunk_content = "\n".join(chunk)
            new_func_code = (
                f"private fun androidx.compose.ui.graphics.vector.PathBuilder.{sub_func_name}() {{\n"
                f"{chunk_content}\n"
                f"}}\n"
            )
            new_functions.append(new_func_code)

            indent_match = re.match(r"^(\s*)", chunk[0])
            indent = indent_match.group(1) if indent_match else "                "
            chunked_invocations.append(f"{indent}{sub_func_name}()")

        new_path_body = "\n".join(chunked_invocations) + "\n"

        # 组装替换
        # 保留 path 关键字前面的原本缩进
        orig_start = block["start"]
        indent_before_path = ""
        idx = orig_start - 1
        while idx >= 0 and content[idx] in (" ", "\t"):
            indent_before_path = content[idx] + indent_before_path
            idx -= 1

        replacement = f"{path_header}\n{new_path_body}{indent_before_path}{path_footer}"
        content = content[:orig_start] + replacement + content[block["end"] :]

    if new_functions:
        new_functions.reverse()
        content += "\n\n" + "\n".join(new_functions)
        return content, sub_func_count, "成功拆分"

    return (
        None,
        0,
        f"虽找到了 path 块，但有效绘图指令仅 {total_lines_found} 行，未达到拆分阈值 {max_lines_per_path} 行。",
    )


def batch_process_current_dir(max_lines_per_path=5, backup=True):
    """批量扫描"""
    current_dir = os.getcwd()
    print(f"📂 开始扫描当前目录: {current_dir}")
    print("-" * 60)

    success_count = 0
    kt_files_found = 0

    for root, dirs, files in os.walk(current_dir):
        for file in files:
            if not file.endswith(".kt"):
                continue

            kt_files_found += 1
            file_path = os.path.join(root, file)
            rel_path = os.path.relpath(file_path, current_dir)

            try:
                with open(file_path, "r", encoding="utf-8") as f:
                    content = f.read()

                new_content, count, message = split_large_kt_icon_content(
                    content, max_lines_per_path
                )

                if new_content and count > 0:
                    if backup:
                        with open(file_path + ".bak", "w", encoding="utf-8") as fb:
                            fb.write(content)

                    with open(file_path, "w", encoding="utf-8") as f_out:
                        f_out.write(new_content)

                    print(f"✅ 处理成功: {rel_path} -> 拆分了 {count} 个方法")
                    success_count += 1
                else:
                    print(f"ℹ️ 跳过文件: {rel_path} -> {message}")

            except Exception as e:
                print(f"❌ 处理失败: {rel_path} -> 报错: {e}")

    print("-" * 60)
    print(f"📊 扫描结束：共发现 {kt_files_found} 个 .kt 文件，成功优化了 {success_count} 个。")


if __name__ == "__main__":
    # 强制设为 5 行做冒烟测试，确保能动它
    batch_process_current_dir(max_lines_per_path=5, backup=True)