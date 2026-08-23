import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.io.PrintStream;
public class CommandExecutor {
    public Path execute(String command, Path currentDirectory) throws Exception {

        new Quoting();
        List<String> commandSplit = Quoting.parseCommand(command);

        PrintStream output = System.out;
        int redirectIndex = -1;
        String redirectFile = null;
        
        for (int i = 0; i < commandSplit.size(); i++) {
            if (commandSplit.get(i).equals(">") ||
                commandSplit.get(i).equals("1>")) {
        
                redirectIndex = i;
                redirectFile = commandSplit.get(i + 1);
                break;
            }
        }
        
        if (redirectIndex != -1) {
            output = new PrintStream(redirectFile);
            commandSplit = commandSplit.subList(0, redirectIndex);
        }


        switch (commandSplit.get(0)) {
            case "echo":
                new Quoting().executeEcho(commandSplit, output);
                return currentDirectory;
            case "type":
                executeType(command, output);
                return currentDirectory;
            case "pwd":
                output.println(currentDirectory);
                return currentDirectory;
            case "cd":
                Path path =  executeCd(commandSplit.get(1),currentDirectory);
                return path == null ? currentDirectory : path;

        }

        if (getCommandPath(commandSplit.get(0)) != null) {
            ProcessBuilder processBuilder = new ProcessBuilder(commandSplit);

            if (redirectFile != null) {
                processBuilder.redirectOutput(new File(redirectFile));
            } else {
                processBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
            }

            processBuilder.redirectError(ProcessBuilder.Redirect.INHERIT);

            Process process = processBuilder.start();
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

    private void executeType(String command, PrintStream output) {
        String search = command.substring(5, command.length());
        output.println(search+type(search));
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