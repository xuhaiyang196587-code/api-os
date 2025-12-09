package org.ssssssss.magicapi.utils;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ssssssss.script.annotation.Comment;

/**
 * IO工具包
 *
 * @author peter
 */
public class IoUtils {

	private static final Logger logger = LoggerFactory.getLogger(IoUtils.class);

	private static final Pattern FILE_NAME_PATTERN = Pattern.compile("^(?!\\.)[\\u4e00-\\u9fa5_a-zA-Z0-9.\\-()]+$");

	@Comment("压缩文件！")
	public void zipFiles(
			@Comment(name = "zipFilePath", value = "压缩后文件的路径（/xx/xx/name.zip）") String zipFilePath,
			@Comment(name = "toZipFilePath", value = "需要被压缩的问题件路径") String... toZipFilePath) throws IOException {
        try (ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(zipFilePath))) {
            for (String filePath : toZipFilePath) {
            	File file = new File(filePath);
                // 跳过不存在的文件和目录
                if (!file.exists() || file.isDirectory()) continue;
                
                try (FileInputStream fis = new FileInputStream(file)) {
                    // 创建ZIP条目（使用文件名）
                    ZipEntry zipEntry = new ZipEntry(file.getName());
                    zipOut.putNextEntry(zipEntry);

                    // 缓冲区读写数据
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = fis.read(buffer)) >= 0) {
                        zipOut.write(buffer, 0, length);
                    }
                    zipOut.closeEntry();
                }
            }
        }
    }
	
	
	@Comment("验证文件名是否合法\n\n"
			+ "@param name 要验证的文件名\n\n"
			+ "@return 如果文件名合法返回true，否则返回false\n\n"
			+ "@example boolean isValid = IoUtils.validateFileName(\"test.txt\");")
	public static boolean validateFileName(String name) {
		return FILE_NAME_PATTERN.matcher(name).matches();
	}
	@Comment("递归获取指定目录下所有指定后缀的文件\n\n"
			+ "@param file 要搜索的目录或文件\n\n"
			+ "@param suffix 文件后缀（如\".txt\"）\n\n"
			+ "@return 符合条件的文件列表\n\n"
			+ "@example List<File> files = IoUtils.files(new File(\"/path\"), \".txt\");")
	public static List<File> files(File file, String suffix) {
		List<File> list = new ArrayList<>();
		if (file.isDirectory()) {
			File[] files = file.listFiles((path) -> path.isDirectory() || path.getName().endsWith(suffix));
			if (files != null) {
				for (int i = files.length - 1; i >= 0; i--) {
					list.addAll(files(files[i], suffix));
				}
			}
		} else if (file.exists()) {
			list.add(file);
		}
		return list;
	}
	@Comment("获取指定目录下的所有子目录\n\n"
			+ "@param file 要搜索的目录\n\n"
			+ "@return 所有子目录列表\n\n"
			+ "@example List<File> dirs = IoUtils.dirs(new File(\"/path\"));")
	public static List<File> dirs(File file) {
		return subDirs(true, file);
	}
	@Comment("递归获取子目录\n\n"
			+ "@param isRoot 是否是根目录\n\n"
			+ "@param file 当前目录\n\n"
			+ "@return 子目录列表\n\n"
			+ "@example List<File> subDirs = IoUtils.subDirs(true, new File(\"/path\"));")
	private static List<File> subDirs(boolean isRoot, File file) {
		List<File> list = new ArrayList<>();
		if (file.isDirectory()) {
			File[] files = file.listFiles(File::isDirectory);
			if (files != null) {
				for (int i = files.length - 1; i >= 0; i--) {
					list.addAll(subDirs(false, files[i]));
				}
			}
			if (!isRoot) {
				list.add(file);
			}
		}
		return list;
	}
	@Comment("读取文件内容为字节数组\n\n"
			+ "@param file 要读取的文件\n\n"
			+ "@return 文件内容的字节数组，读取失败返回空数组\n\n"
			+ "@example byte[] data = IoUtils.bytes(new File(\"test.txt\"));")
	public static byte[] bytes(File file) {
		try {
			return Files.readAllBytes(file.toPath());
		} catch (IOException e) {
			logger.error("读取文件失败", e);
			return new byte[0];
		}
	}
	@Comment("读取文件内容为字符串（UTF-8编码）\n\n"
			+ "@param file 要读取的文件\n\n"
			+ "@return 文件内容的字符串，读取失败返回空字符串\n\n"
			+ "@example String content = IoUtils.string(new File(\"test.txt\"));")
	public static String string(File file) {
		return new String(bytes(file), StandardCharsets.UTF_8);
	}
	@Comment("读取输入流内容为字节数组\n\n"
			+ "@param inputStream 要读取的输入流\n\n"
			+ "@return 输入流内容的字节数组，读取失败返回空数组\n\n"
			+ "@example byte[] data = IoUtils.bytes(inputStream);")
	public static byte[] bytes(InputStream inputStream) {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			byte[] buf = new byte[4096];
			int len;
			while ((len = inputStream.read(buf, 0, buf.length)) != -1) {
				baos.write(buf, 0, len);
			}
			return baos.toByteArray();
		} catch (IOException e) {
			logger.error("读取InputStream失败", e);
			return new byte[0];
		}
	}
	@Comment("读取输入流内容为字符串（UTF-8编码）\n\n"
			+ "@param inputStream 要读取的输入流\n\n"
			+ "@return 输入流内容的字符串，读取失败返回空字符串\n\n"
			+ "@example String content = IoUtils.string(inputStream);")
	public static String string(InputStream inputStream) {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
			StringBuilder result = new StringBuilder();
			String line;
			boolean flag = false;
			while ((line = reader.readLine()) != null) {
				if (flag) {
					result.append("\r\n");
				}
				result.append(line);
				flag = true;
			}
			return result.toString();
		} catch (IOException e) {
			logger.error("读取InputStream失败", e);
			return "";
		}
	}
	@Comment("将字节数组写入文件\n\n"
			+ "@param file 目标文件\n\n"
			+ "@param bytes 要写入的字节数组\n\n"
			+ "@return 写入成功返回true，失败返回false\n\n"
			+ "@example boolean success = IoUtils.write(new File(\"test.txt\"), data);")
	public static boolean write(File file, byte[] bytes) {
		try {
			Files.write(file.toPath(), bytes);
			return true;
		} catch (IOException e) {
			logger.error("写文件失败", e);
			return false;
		}
	}
	@Comment("将字符串写入文件（UTF-8编码）\n\n"
			+ "@param file 目标文件\n\n"
			+ "@param content 要写入的字符串\n\n"
			+ "@return 写入成功返回true，失败返回false\n\n"
			+ "@example boolean success = IoUtils.write(new File(\"test.txt\"), \"content\");")
	public static boolean write(File file, String content) {
		if (content == null) {
			return false;
		}
		return write(file, content.getBytes(StandardCharsets.UTF_8));
	}
	@Comment("递归删除文件或目录\n\n"
			+ "@param file 要删除的文件或目录\n\n"
			+ "@return 删除成功返回true，失败返回false\n\n"
			+ "@example boolean success = IoUtils.delete(new File(\"test.txt\"));")
	public static boolean delete(File file) {
		if (file == null) {
			return true;
		}
		if (file.isDirectory()) {
			File[] files = file.listFiles();
			if (files != null) {
				for (int i = files.length - 1; i >= 0; i--) {
					if (!delete(files[i])) {
						return false;
					}
				}
			}
		}
		if (!file.exists()) {
			return true;
		}
		return file.delete();
	}
	@Comment("关闭数据源\n\n"
			+ "@param dataSource 要关闭的数据源\n\n"
			+ "@example IoUtils.closeDataSource(dataSource);")
	public static void closeDataSource(DataSource dataSource) {
		if (dataSource != null) {
			if (dataSource instanceof Closeable) {
				try {
					((Closeable) dataSource).close();
				} catch (Exception e) {
					logger.warn("Close DataSource error", e);
				}
			} else {
				logger.warn("DataSource can not close");
			}
		}
	}
	@Comment("关闭可关闭对象\n\n"
			+ "@param closeable 要关闭的对象\n\n"
			+ "@example IoUtils.close(closeable);")
	public static void close(Closeable closeable){
		try {
			if(closeable != null){
				closeable.close();
			}
		} catch (IOException ignored) {
		}
	}
}
