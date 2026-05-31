import os
import shutil

def replace_io_with_default():
    # 关键字设置
    old_text = "Dispatchers.IO"
    new_text = "Dispatchers.Default"
    
    # '.' 代表脚本当前所在的目录
    root_dir = "." 
    count = 0
    
    # os.walk 会自动递归遍历当前目录下的所有子文件夹
    for dirpath, dirnames, filenames in os.walk(root_dir):
        for filename in filenames:
            # 只处理 Kotlin 文件，同时排除备份文件本身
            if filename.endswith('.kt') and not filename.endswith('.bak'):
                file_path = os.path.join(dirpath, filename)
                
                try:
                    # 1. 读取文件内容
                    with open(file_path, 'r', encoding='utf-8') as f:
                        content = f.read()
                    
                    # 2. 检查并替换
                    if old_text in content:
                        print(f"正在处理: {file_path}")
                        
                        # 创建 .bak 备份
                        shutil.copy2(file_path, file_path + '.bak')
                        
                        # 替换并写入
                        new_content = content.replace(old_text, new_text)
                        with open(file_path, 'w', encoding='utf-8') as f:
                            f.write(new_content)
                        
                        count += 1
                except Exception as e:
                    print(f"处理文件失败 {file_path}: {e}")

    print(f"\n替换完成！共修改了 {count} 个 Kotlin 文件。")
    print("已自动生成 '.bak' 备份，编译通过后你可以放心删除它们。")

if __name__ == "__main__":
    replace_io_with_default()