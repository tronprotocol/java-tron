/*
 * java-tron is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * java-tron is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.tron.command;

import org.tron.application.CliApplication;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;

import static org.fusesource.jansi.Ansi.ansi;

public class Cli {

  private final static String NEW_LINE = System.getProperty("line.separator");
  private final Map<String, Class<ExecutableCommand>> executableCommandClassMap;
  private String defaultUsage;

  public Cli() {
    executableCommandClassMap = new HashMap<>();

    try {
      Class[] classes = getClasses(getClass().getPackage().getName());

      for (Class c : classes) {
        if (c.isAnnotationPresent(CliCommand.class)) {
          CliCommand cliCommand = (CliCommand) c.getAnnotation(CliCommand.class);
          for (String command : cliCommand.commands()) {
            assert executableCommandClassMap.containsKey(command) == false;
            executableCommandClassMap.put(command, c);
          }
        }
      }

      defaultUsage = makeDefaultUsageString(classes);
    } catch (ClassNotFoundException | IOException e) {
      defaultUsage = null;
      e.printStackTrace();
    }
  }

  public void run(CliApplication app) {
    Scanner in = new Scanner(System.in);

    while (true) {
      String cmd = in.nextLine().trim();

      String[] cmdArray = cmd.split("\\s+");
      // split on trim() string will always return at the minimum: [""]
      if ("".equals(cmdArray[0])) {
        continue;
      }

      String[] cmdParameters = Arrays.copyOfRange(cmdArray, 1, cmdArray.length);

      if (executableCommandClassMap.containsKey(cmdArray[0])) {
        try {
          Class<ExecutableCommand> executableCommandClazz = executableCommandClassMap.get(cmdArray[0]);
          CliCommand cliCommand = executableCommandClazz.getAnnotation(CliCommand.class);

          if (cliCommand.needInjection()) {
            app.getInjector().getInstance(executableCommandClazz).execute(app, cmdParameters);
          } else {
            ExecutableCommand executableCommand = executableCommandClazz.newInstance(); // Creating new instance because previous code was so. I think it will be okay with singleton instances.
            executableCommand.execute(app, cmdParameters);
          }
        } catch (InstantiationException | IllegalAccessException e) {
          e.printStackTrace();
        }
      } else if(cmdArray[0].startsWith("help") && cmdArray.length > 1) {
        if (executableCommandClassMap.containsKey(cmdArray[1])) {
          try {
            Class<ExecutableCommand> executableCommandClazz = executableCommandClassMap.get(cmdArray[1]);
            CliCommand cliCommand = executableCommandClazz.getAnnotation(CliCommand.class);
            if (cliCommand.needInjection()) {
              app.getInjector().getInstance(executableCommandClazz).usage();
            } else {
              executableCommandClazz.newInstance().usage();
            }
          } catch (InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
          }
        }
      } else {
        // show available commands
        System.out.print(defaultUsage);
      }
    }
  }

  private static Class[] getClasses(String packageName)
          throws ClassNotFoundException, IOException {
    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    assert classLoader != null;
    String path = packageName.replace('.', '/');
    Enumeration<URL> resources = classLoader.getResources(path);
    List<File> dirs = new ArrayList<>();
    while (resources.hasMoreElements()) {
      URL resource = resources.nextElement();
      dirs.add(new File(resource.getFile()));
    }
    ArrayList<Class> classes = new ArrayList<>();
    for (File directory : dirs) {
      classes.addAll(findClasses(directory, packageName));
    }
    return classes.toArray(new Class[classes.size()]);
  }

  private static List<Class> findClasses(File directory, String packageName) throws ClassNotFoundException {
    List<Class> classes = new ArrayList<>();
    if (!directory.exists()) {
      return classes;
    }
    File[] files = directory.listFiles();
    for (File file : files) {
      if (file.isDirectory()) {
        assert !file.getName().contains(".");
        classes.addAll(findClasses(file, packageName + "." + file.getName()));
      } else if (file.getName().endsWith(".class")) {
        classes.add(Class.forName(packageName + '.' + file.getName().substring(0, file.getName().length() - 6)));
      }
    }
    return classes;
  }

  private String makeDefaultUsageString(Class[] classes) {

    StringBuilder sbDefaultUsage = new StringBuilder();
    sbDefaultUsage
            .append(NEW_LINE)
            .append(ansi().eraseScreen().render("@|magenta,bold USAGE|@\n\t@|bold help [arguments]|@"))
            .append(NEW_LINE)
            .append(ansi().eraseScreen().render("@|magenta,bold AVAILABLE COMMANDS|@"))
            .append(NEW_LINE);

    for (Class c : classes) {
      if (c.isAnnotationPresent(CliCommand.class)) {
        CliCommand cliCommand = (CliCommand) c.getAnnotation(CliCommand.class);
        sbDefaultUsage.append(ansi().eraseScreen().render(
                String.format("\t@|bold %-20s\t%s|@",  cliCommand.commands()[0], cliCommand.description())
        )).append(NEW_LINE);

      }
    }

    sbDefaultUsage.append(ansi().eraseScreen().render("Use @|bold help [topic] for more information about that topic.|@")).append(NEW_LINE);
    return sbDefaultUsage.toString();
  }
}
