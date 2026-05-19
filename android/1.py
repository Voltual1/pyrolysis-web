import re
from pathlib import Path

# ==================== 配置区域 ====================
# 将这里替换为你 Kotlin 项目的源代码目录（例如：'./app/src'）
TARGET_DIR = "./src"
# ==================================================

# 定义替换规则：(正则表达式, 替换后的文本)
REPLACEMENT_RULES = [
    # 1. 替换包名和导入语句
    (r"import okio\.FileSystem", "import kotlinx.io.files.SystemFileSystem"),
    (r"import okio\.Path", "import kotlinx.io.files.Path"),
    (r"import okio\.Path\.Companion\.toPath", "import kotlinx.io.files.Path.Companion.toPath"),
    (r"import okio\.buffer", ""), # kotlinx.io 通常不需要单独导入 buffer 扩展
    (r"import okio\.source", ""), # 移除旧的 source 导入
    
    # 2. 替换核心 API 的使用
    (r"FileSystem\.SYSTEM", "SystemFileSystem"),
    
    # 3. 替换常用的读取/写入方法名
    (r"\.readUtf8\(\)", ".readString()"),
    (r"\.writeUtf8\((.*?)\)", r".writeString(\1)"),
    
    # 4. 清理可能遗留的连续空行（因为删除了部分 import）
    (r"\n{3,}", "\n\n")
]

def migrate_file(file_path):
    try:
        with open(file_path, "r", encoding="utf-8") as f:
            content = f.read()

        original_content = content
        
        # 逐条应用替换规则
        for pattern, replacement in REPLACEMENT_RULES:
            content = re.sub(pattern, replacement, content)

        # 如果内容发生变化，写回文件
        if content != original_content:
            with open(file_path, "w", encoding="utf-8") as f:
                f.write(content)
            print(r"[已修改] {file_path}")
        else:
            print(r"[未变化] {file_path}")

    except Exception as e:
        print(r"[错误] 无法处理文件 {file_path}: {e}")

def main():
    path = Path(TARGET_DIR)
    if not path.exists():
        print(r"错误：找不到目录 '{TARGET_DIR}'，请检查配置。")
        return

    print("开始扫描并迁移 Kotlin 文件...")
    # 递归查找所有 .kt 文件
    kt_files = list(path.rglob("*.kt"))
    
    if not kt_files:
        print("未找到任何 .kt 文件。")
        return

    for file_path in kt_files:
        migrate_file(file_path)
        
    print("\n迁移完成！请在 IDE 中重新构建项目并检查编译情况。")

if __name__ == "__main__":
    main()