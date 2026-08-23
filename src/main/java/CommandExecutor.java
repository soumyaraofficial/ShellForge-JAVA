import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class CommandExecutor {
    public Path execute(String command, Path currentDirectory) throws Exception {

        List<String> commandSplit = new Quoting().parseCommand(command);
        switch (commandSplit.get(0)) {
            case "echo":
                new Quoting().executeEcho(commandSplit);
                return currentDirectory;
            case "type":
                executeType(command);
                return currentDirectory;
            case "pwd":
                System.out.println(currentDirectory);
                return currentDirectory;
            case "cd":
                Path path =  executeCd(commandSplit.get(1),currentDirectory);
                return path == null ? currentDirectory : path;

        }

        if (getCommandPath(commandSplit.get(0)) != null) {
            // "If the command exists somewhere in PATH, execute that command as a real
            // program
            // inheritIO It tells the child process: "Use the same input/output/error
            // streams as my Java shell."
            Process process = new ProcessBuilder(commandSplit).inheritIO().start();
            process.waitFor();

        } else {
            System.out.println(command + ": command not found");
        }
        return currentDirectory;
    }

    private Path executeCd(String command,Path currentDirectory) {
        if(command.equals("~")){
            Path path =Path.of(System.getenv("HOME"));
            return path;
        }


        Path path = Path.of(command);
        
        if(!path.isAbsolute()){ 
          path = currentDirectory.resolve(command).normalize();
        }
      
        if (!Files.isDirectory(path)) {
            System.out.println("cd: " + command + ": No such file or directory");
            return null;
        }
      
        return path;
    }

    // Echo command
    // private void executeEcho(String command) {
    //     System.out.println(command.substring(5, command.length()));
    // }

    private void executeType(String command) {
        String search = command.substring(5, command.length());
        System.out.println(search + type(search));
    }

    // checking builtin type or external command
    public static String type(String command) {
        String[] builin = { "exit", "echo", "type", "pwd" };
        for (String s : builin) {
            if (s.equalsIgnoreCase(command))
                return " is a shell builtin";
        }
        String path = getCommandPath(command);
        if (path != null)
            return " is " + path;

        return ": not found";
    }

    // getting command paths
    public static String getCommandPath(String command) {
        String paths = System.getenv("PATH");
        String pathDir[] = paths.split(File.pathSeparator);
        for (String dir : pathDir) {
            File file = new File(dir, command);
            if (file.exists() && file.canExecute())
                return file.getAbsolutePath();
        }
        return null;
    }

}