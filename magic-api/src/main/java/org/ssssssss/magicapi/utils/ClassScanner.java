package org.ssssssss.magicapi.utils;


import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.ssssssss.script.annotation.Comment;
import org.ssssssss.script.asm.ClassReader;
import org.ssssssss.script.functions.ObjectConvertExtension;


/**
 * Class扫描工具类，用于扫描类路径下的所有类
 *
 * @author peter
 */
public class ClassScanner {

	@Comment("扫描类路径下的所有类\n\n"
            + "@return 所有类的全限定名列表\n\n"
            + "@throws URISyntaxException\n\n"
            + "@throws IOException\n\n"
            + "@example List<String> classes = ClassScanner.scan();")
	public static List<String> scan() throws URISyntaxException, IOException {
		Set<String> classes = new HashSet<>();
		if (Double.parseDouble(System.getProperty("java.specification.version")) >= 11) {
			classes.addAll(latestJdkScan());
		} else {
			ClassLoader loader = Thread.currentThread().getContextClassLoader();
			do {
				if (loader instanceof URLClassLoader) {
					classes.addAll(scan(((URLClassLoader) loader).getURLs()));
				}
			} while ((loader = loader.getParent()) != null);
		}
		ClassLoader loader = Thread.currentThread().getContextClassLoader();
		do {
			if (loader instanceof URLClassLoader) {
				classes.addAll(scan(((URLClassLoader) loader).getURLs()));
			}
		} while ((loader = loader.getParent()) != null);
		classes.addAll(addJavaLibrary());
		return new ArrayList<>(classes);
	}

	 @Comment("JDK11+版本的类扫描方法\n\n"
	            + "@return 所有类的全限定名列表\n\n"
	            + "@throws IOException\n\n"
	            + "@example List<String> classes = ClassScanner.latestJdkScan();")
	public static List<String> latestJdkScan() throws IOException {
		ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();
		Resource[] resources = resourcePatternResolver.getResources("classpath*:**/**.class");
		return Arrays.asList(resources).parallelStream().map(it -> {
			try {
				if (isClass(it.getURL().getPath())) {
					if ("\"classes\"".contains(it.getURL().getPath())) {
						return it.getURL().getPath().split("classes")[1].substring(1).replace(".class", "").replaceAll("\\/", ".");
					} else if (it.getURL().getPath().split("!").length > 1) {
						return it.getURL().getPath().split("!")[1].substring(1).replace(".class", "").replaceAll("\\/", ".");
					} else {
						try (InputStream stream = it.getInputStream()) {
							return new ClassReader(stream).getClassName().replace("/", ".");
						} catch (Exception e) {
							return null;
						}
					}
				}
			} catch (Exception e) {
				return null;
			}
			return null;
		}).filter(Objects::nonNull).distinct().sorted(Comparator.comparing(Objects::toString)).collect(Collectors.toList());
	}
	 @Comment("压缩类名列表，将相同包下的类合并显示\n\n"
	            + "@param classes 类名列表\n\n"
	            + "@return 压缩后的字符串表示\n\n"
	            + "@example String compressed = ClassScanner.compress(classes);")
	public static String compress(List<String> classes) {
		Collections.sort(classes);
		String currentPackage = "";
		StringBuffer buf = new StringBuffer();
		int classCount = 0;
		for (String fullName : classes) {
			String packageName = "";
			String className = fullName;
			if (fullName.contains(".")) {
				int index = fullName.lastIndexOf(".");
				className = fullName.substring(index + 1);
				packageName = fullName.substring(0, index);
			}
			if (className.equals("package-info")) {
				continue;
			}
			if (currentPackage.equals(packageName)) {
				if (classCount > 0) {
					buf.append(",");
				}
				buf.append(className);
				classCount++;
			} else {
				currentPackage = packageName;
				if (buf.length() > 0) {
					buf.append("\n");
				}
				buf.append(packageName);
				buf.append(":");
				buf.append(className);
				classCount = 1;
			}
		}
		return buf.toString();
	}
	 @Comment("扫描指定URL数组对应的类\n\n"
	            + "@param urls URL数组\n\n"
	            + "@return 类名集合\n\n"
	            + "@throws URISyntaxException\n\n"
	            + "@example Set<String> classes = ClassScanner.scan(new URL[]{new URL(\"file:/path/to/jar\")});")
	private static Set<String> scan(URL[] urls) throws URISyntaxException {
		Set<String> classes = new HashSet<>();
		if (urls != null) {
			for (URL url : urls) {
				String protocol = url.getProtocol();
				if ("file".equalsIgnoreCase(protocol)) {
					String path = url.getPath();
					if (path.toLowerCase().endsWith(".jar")) {
						classes.addAll(scanJarFile(url));
					} else {
						classes.addAll(scanDirectory(new File(url.toURI()), null));
					}
				} else if ("jar".equalsIgnoreCase(protocol)) {
					classes.addAll(scanJarFile(url));
				}
			}
		}
		return classes;
	}
	 @Comment("添加Java标准库中的类\n\n"
	            + "@return 类名集合\n\n"
	            + "@example Set<String> jdkClasses = ClassScanner.addJavaLibrary();")
	private static Set<String> addJavaLibrary() {
		int version = checkJavaVersion();
		if (version >= 9) {
			return addJava9PlusLibrary();
		}
		return addJava8Library();
	}
	 @Comment("检查Java版本\n\n"
	            + "@return Java主版本号\n\n"
	            + "@example int version = ClassScanner.checkJavaVersion();")
	private static int checkJavaVersion() {
		String version = System.getProperty("java.version");
		int index = version.indexOf(".");
		if (index > -1) {
			String first = version.substring(0, index);
			if (!"1".equals(first)) {
				return ObjectConvertExtension.asInt(first, -1);
			} else {
				int endIndex = version.indexOf(".", index + 1);
				return ObjectConvertExtension.asInt(version.substring(index + 1, endIndex), -1);
			}
		}
		return -1;
	}

	 @Comment("JDK8版本的Java标准库扫描\n\n"
	            + "@return 类名集合\n\n"
	            + "@example Set<String> classes = ClassScanner.addJava8Library();")
	private static Set<String> addJava8Library() {
		try {
			// 直接反射调用..
			Object classpath = Class.forName("sun.misc.Launcher").getMethod("getBootstrapClassPath").invoke(null);
			return scan((URL[]) classpath.getClass().getMethod("getURLs").invoke(classpath));
		} catch (Exception ignored) {
		}
		return Collections.emptySet();
	}

	 @Comment("JDK9+版本的Java标准库扫描\n\n"
	            + "@return 类名集合\n\n"
	            + "@example Set<String> classes = ClassScanner.addJava9PlusLibrary();")
	private static Set<String> addJava9PlusLibrary() {
		Set<String> classes = new HashSet<>();
		try {
			Class<?> moduleLayer = Class.forName("java.lang.ModuleLayer");
			Object boot = moduleLayer.getMethod("boot").invoke(null);
			Object configuration = moduleLayer.getMethod("configuration").invoke(boot);
			//Set<ResolvedModule>
			Set<?> modules = (Set<?>) Class.forName("java.lang.module.Configuration").getMethod("modules").invoke(configuration);
			Method reference = Class.forName("java.lang.module.ResolvedModule").getMethod("reference");
			Method open = Class.forName("java.lang.module.ModuleReference").getMethod("open");
			Method list = Class.forName("java.lang.module.ModuleReader").getMethod("list");
			modules.forEach(module -> {
			});
			for (Object module : modules) {
				Object ref = reference.invoke(module);
				try (Closeable reader = (Closeable) open.invoke(ref)) {
					@SuppressWarnings("unchecked")
					Stream<String> stream = (Stream<String>) list.invoke(reader);
					stream.filter(ClassScanner::isClass).forEach(className -> classes.add(className.substring(0, className.length() - 6).replace("/", ".")));
				} catch (IOException ignored) {
				}
			}
		} catch (Exception ignored) {
		}
		return classes;
	}
	  @Comment("扫描目录下的类文件\n\n"
	            + "@param dir 目录\n\n"
	            + "@param packageName 包名\n\n"
	            + "@return 类名列表\n\n"
	            + "@example List<String> classes = ClassScanner.scanDirectory(new File(\"target/classes\"), \"com.example\");")
	private static List<String> scanDirectory(File dir, String packageName) {
		File[] files = dir.listFiles();
		List<String> classes = new ArrayList<>();
		if (files != null) {
			for (File file : files) {
				String name = file.getName();
				if (file.isDirectory()) {
					classes.addAll(scanDirectory(file, packageName == null ? name : packageName + "." + name));
				} else if (name.endsWith(".class") && !name.contains("$")) {
					classes.add(filterFullName(packageName + "." + name.substring(0, name.length() - 6)));
				}
			}
		}
		return classes;
	}
	  @Comment("过滤全限定名中的特殊前缀\n\n"
	            + "@param fullName 全限定名\n\n"
	            + "@return 过滤后的全限定名\n\n"
	            + "@example String name = ClassScanner.filterFullName(\"BOOT-INF.classes.com.example.Test\");")
	private static String filterFullName(String fullName) {
		if (fullName.startsWith("BOOT-INF.classes.")) {
			fullName = fullName.substring(17);
		}
		return fullName;
	}
	  @Comment("扫描JAR文件中的类\n\n"
	            + "@param url JAR文件URL\n\n"
	            + "@return 类名列表\n\n"
	            + "@example List<String> classes = ClassScanner.scanJarFile(new URL(\"file:/path/to/lib.jar\"));")
	private static List<String> scanJarFile(URL url) {
		List<String> classes = new ArrayList<>();
		try (ZipInputStream zis = new ZipInputStream(url.openStream())) {
			ZipEntry entry;
			while ((entry = zis.getNextEntry()) != null) {
				if (!entry.getName().contains("META-INF")) {
					String className = entry.getName();
					if (isClass(className)) {
						classes.add(filterFullName(className.substring(0, className.length() - 6).replace("/", ".")));
					}
				}
			}
		} catch (IOException ignored) {

		}
		return classes;
	}
	  @Comment("判断是否是有效的类文件\n\n"
	            + "@param className 类文件名\n\n"
	            + "@return 是否是有效的类文件\n\n"
	            + "@example boolean isClass = ClassScanner.isClass(\"com/example/Test.class\");")
	private static boolean isClass(String className) {
		return className.endsWith(".class") && !className.contains("$") && !className.contains("module-info");
	}
}
