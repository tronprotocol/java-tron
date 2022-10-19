package org.tron.program.design.factory;

import org.tron.program.generate.TransactionCreator;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * @author liukai
 * @since 2022/9/9.
 */
public class GeneratorFactory {

  public static Map<String, TransactionCreator> creators = new HashMap<>();

  static {
    try {
      ArrayList<Class> creatorsClass = new ArrayList<>();
      ArrayList<File> classFiles = new ArrayList<>();
      Class<?> interfaceClass = Class.forName("org.tron.program.generate.TransactionCreator");
      String packageName = interfaceClass.getPackage().getName();
      ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
      String path = packageName.replace(".", "/");
      Enumeration<URL> resources = contextClassLoader.getResources(path);
      while (resources.hasMoreElements()) {
        URL url = resources.nextElement();
        classFiles.add(new File(url.getFile()));
      }
      for (File file : classFiles) {
        creatorsClass.addAll(findClass(file, packageName));
      }

      for (Class clazz : creatorsClass) {
        Creator declaredAnnotation = (Creator) clazz.getAnnotation(Creator.class);
        if (declaredAnnotation != null) {
          creators.put(declaredAnnotation.type(), (TransactionCreator) clazz.newInstance());
        }
      }

    } catch (ClassNotFoundException | IOException | IllegalAccessException | InstantiationException e) {
      e.printStackTrace();
    }
  }


  public static TransactionCreator getGenerator(String type) {
    return creators.get(type);
  }

  private static ArrayList<Class> findClass(File file, String packagename) {
    ArrayList<Class> list = new ArrayList<>();
    if (!file.exists()) {
      return list;
    }
    File[] files = file.listFiles();
    for (File file2 : files) {
      if (file2.isDirectory()) {
        assert !file2.getName().contains(".");
        ArrayList<Class> arrayList = findClass(file2, packagename + "." + file2.getName());
        list.addAll(arrayList);
      } else if (file2.getName().endsWith(".class")) {
        try {
          list.add(Class.forName(packagename + '.' + file2.getName().substring(0,
                  file2.getName().length() - 6)));
        } catch (ClassNotFoundException e) {
          e.printStackTrace();
        }
      }
    }
    return list;
  }

}
