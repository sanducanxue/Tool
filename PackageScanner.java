import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class PackageScanner {
    /**
     * @param packName 要扫描的包
     */
    public static Set<Class<?>> findFileClass(String packName) {
        Set<Class<?>> clazzs = new LinkedHashSet<Class<?>>();
        String packageDirName = packName.replace('.', '/');
        Enumeration<URL> dirs;
        try {
            dirs = Thread.currentThread().getContextClassLoader()
                    .getResources(packageDirName);
            while (dirs.hasMoreElements()) {
                URL url = dirs.nextElement();
                String protocol = url.getProtocol();
                if ("file".equals(protocol)) {// 扫描file包中的类
                    String filePath = URLDecoder.decode(url.getFile(), "UTF-8");
                    getFileClass(packName, filePath, clazzs);
                } else if ("jar".equals(protocol)) {// 扫描jar包中的类
                    JarFile jarFile = ((JarURLConnection) url.openConnection())
                            .getJarFile();
                    getJarClass(jarFile, packageDirName, clazzs);
                }
            }
        } catch (Exception e) {
            e.getStackTrace();
        }
        return clazzs;
    }

    /**
     * 获取文件中的class
     */
    public static void getFileClass(String packName, String filePath,
                                    Set<Class<?>> clazzs) {
        File dir = new File(filePath);
        if (!dir.exists() || !dir.isDirectory()) {
            System.out.println("包目录不存在!");
            return;
        }
        File[] dirFiles = dir.listFiles(new FileFilter() {
            public boolean accept(File file) {
                boolean acceptDir = file.isDirectory();// 接受dir目录
                boolean acceptClass = file.getName().endsWith(".class");// 接受class文件
                return acceptDir || acceptClass;
            }
        });
        for (File file : dirFiles) {
            if (file.isDirectory()) {
                getFileClass(packName + "." + file.getName(),
                        file.getAbsolutePath(), clazzs);
            } else {
                String className = file.getName().substring(0,
                        file.getName().length() - 6);
                try {
                    Class<?> clazz = Thread.currentThread()
                            .getContextClassLoader()
                            .loadClass(packName + "." + className);
                    clazzs.add(clazz);
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 获取jar中的class
     *
     * @throws IOException
     */
    public static void getJarClass(JarFile jarFile, String filepath,
                                   Set<Class<?>> clazzs) throws IOException {
        List<JarEntry> jarEntryList = new ArrayList<JarEntry>();
        Enumeration<JarEntry> enumes = jarFile.entries();
        while (enumes.hasMoreElements()) {
            JarEntry entry = (JarEntry) enumes.nextElement();
            // 过滤出满足我们需求的东西
            if (entry.getName().startsWith(filepath)
                    && entry.getName().endsWith(".class")) {
                jarEntryList.add(entry);
            }
        }
        for (JarEntry entry : jarEntryList) {
            String className = entry.getName().replace('/', '.');
            className = className.substring(0, className.length() - 6);
            try {
                clazzs.add(Thread.currentThread().getContextClassLoader()
                        .loadClass(className));
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

}