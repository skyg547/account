import os

def fix_bom(directory):
    count = 0
    for root, dirs, files in os.walk(directory):
        for file in files:
            if file.endswith('.java'):
                file_path = os.path.join(root, file)
                try:
                    with open(file_path, 'rb') as f:
                        content = f.read()
                    
                    if content.startswith(b'\xef\xbb\xbf'):
                        print(f"BOM found in: {file_path}")
                        with open(file_path, 'wb') as f:
                            f.write(content[3:])
                        count += 1
                except Exception as e:
                    print(f"Error processing {file_path}: {e}")
    print(f"Fixed {count} files.")

if __name__ == "__main__":
    fix_bom('receivable/src/main/java')
    fix_bom('receivable/src/test/java')
