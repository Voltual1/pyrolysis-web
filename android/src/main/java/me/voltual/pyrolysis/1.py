import os


def replace_in_kt_files():
    old_string = "me.voltual.pyrolysis.KtorClient"
    new_string = "me.voltual.pyrolysis.network.KtorClient"

    # 获取当前工作目录
    current_dir = os.getcwd()
    print(f"开始扫描目录: {current_dir}\n" + "-" * 50)

    success_count = 0

    # 遍历当前目录及其子目录
    for root, dirs, files in os.walk(current_dir):
        for file in files:
            if file.endswith(".kt"):
                file_path = os.path.join(root, file)

                try:
                    # 1. 读取文件内容
                    with open(file_path, "r", encoding="utf-8") as f:
                        content = f.read()

                    # 2. 检查是否包含需要替换的字符串
                    if old_string in content:
                        print(f"匹配到目标: {file_path}")

                        # 创建备份文件 (例如: main.kt -> main.kt.bak)
                        backup_path = file_path + ".bak"
                        with open(backup_path, "w", encoding="utf-8") as b:
                            b.write(content)

                        # 3. 执行替换并写回原文件
                        new_content = content.replace(old_string, new_string)
                        with open(file_path, "w", encoding="utf-8") as f:
                            f.write(new_content)

                        print(f" -> 替换成功！备份已创建: {file}.bak")
                        success_count += 1

                except Exception as e:
                    print(f"处理文件 {file_path} 时出错: {e}")

    print("-" * 50)
    print(f"扫描完毕。共修改了 {success_count} 个文件。")


if __name__ == "__main__":
    replace_in_kt_files()