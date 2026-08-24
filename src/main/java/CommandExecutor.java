import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.io.PrintStream;
public class CommandExecutor {
    public Path execute(String command, Path currentDirectory) throws Exception {

        List<String> commandSplit = Quoting.parseCommand(command);
    
        if (commandSplit.isEmpty()) {
            return currentDirectory;
        }
    
        PrintStream output = System.out;
        PrintStream errorOutput = System.err;
    
        int redirectIndex = -1;
        String redirectFile = null;
        int errorRedirectIndex = -1;
        String errorRedirectFile = null;

        int appendIndex = -1;
        int errorAppendIndex = -1;
    
        for (int i = 0; i < commandSplit.size(); i++) {
            String token = commandSplit.get(i);
            if (token.equals(">") || token.equals("1>")) {
               
                redirectIndex = i;
                redirectFile = commandSplit.get(i + 1);
            } else if (token.equals("2>")) {
                errorRedirectIndex = i;
                errorRedirectFile = commandSplit.get(i + 1);
            } else if(token.equals(">>") || token.equals("1>>")){
                appendIndex = i;
                redirectFile = commandSplit.get(i + 1);
            }else if(token.equals("2>>")){
                errorAppendIndex = i;
                errorRedirectFile = commandSplit.get(i+1);
            }
        }
    
        // Cut arguments at the first redirection symbol
        int firstRedirect = commandSplit.size();
        if (redirectIndex != -1) firstRedirect = Math.min(firstRedirect, redirectIndex);
        if (errorRedirectIndex != -1) firstRedirect = Math.min(firstRedirect, errorRedirectIndex);
        
    
        List<String> cleanArgs = commandSplit.subList(0, firstRedirect);
    
        // Ensure output redirect file is created (for both built-ins and external commands)
    if (redirectFile != null) {
    File outFile = new File(redirectFile);

    if (outFile.getParentFile() != null)
        outFile.getParentFile().mkdirs();

    if (appendIndex != -1) {
        output = new PrintStream(new FileOutputStream(outFile, true));
    } else {
        output = new PrintStream(outFile);
    }
}
    
        // Ensure error redirect file is created (for both built-ins and external commands)
        if (errorRedirectFile != null) {
            File errFile = new File(errorRedirectFile);

            if (errFile.getParentFile() != null) errFile.getParentFile().mkdirs();
            if(errorAppendIndex!= -1){
                errorOutput = new PrintStream(new FileOutputStream(errFile,true));
            }else{
            errorOutput = new PrintStream(errFile); 
            }
        }
    
        // Builtins handling
        switch (cleanArgs.get(0)) {
            case "echo":
                new Quoting().executeEcho(cleanArgs, output);
                return currentDirectory;
            case "type":
                executeType(command, output);
                return currentDirectory;
            case "pwd":
                output.println(currentDirectory);
                return currentDirectory;
            case "cd":
                Path path = executeCd(cleanArgs.get(1), currentDirectory);
                return path == null ? currentDirectory : path;
        }
    
        // External commands execution
        if (getCommandPath(cleanArgs.get(0)) != null) {
            ProcessBuilder processBuilder = new ProcessBuilder(cleanArgs);
    
            if (redirectFile != null) {
                processBuilder.redirectOutput(new File(redirectFile));
            } else {
                processBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
            }
    
            if (errorRedirectFile != null) {
                processBuilder.redirectError(new File(errorRedirectFile));
            } else {
                processBuilder.redirectError(ProcessBuilder.Redirect.INHERIT);
            }
    
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