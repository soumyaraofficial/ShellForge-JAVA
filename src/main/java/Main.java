import java.io.File;
import java.nio.file.Path;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws Exception {
        // TODO: Uncomment the code below to pass the first stage

        Scanner sc = new Scanner(System.in);
        while (true) {
            System.out.print("$ ");
            String command = sc.nextLine();
            if(command.isEmpty())continue;
            String commandSplit[] = command.split(" ");

            if (command.equals("exit")) {
                break;
            } else if (command.startsWith("echo ")) {

                System.out.println(command.substring(5, command.length()));
                
            } else if (command.startsWith("type ")) {
                String search = command.substring(5, command.length());
                System.out.println(search + type(search));
            } else if (getCommandPath(commandSplit[0])!=null) {
                Process process = new ProcessBuilder(commandSplit).inheritIO().start();
                process.waitFor();

            } else {
                System.out.println(command + ": command not found");
            }
        }
    }

    //will search the directories if the command is present or not. 

    public static String type(String command){
      
        String[] builin = {"exit","echo", "type"};
        String allPath = System.getenv("PATH");
        String[] pathDir = allPath.split(File.pathSeparator);
        for(String s : builin){
            if(s.equals(command))return " is a shell builtin";
        }

        String path = getCommandPath(command);
        if(path!=null) return " is " + path;

        return ": not found";
    }

    public static String getCommandPath(String command){
     String paths = System.getenv("PATH");
     String pathDir[] = paths.split(File.pathSeparator);
     for(String dir : pathDir){
       File file = new File(dir,command);
       if(file.exists() && file.canExecute())return file.getAbsolutePath();
     }
     return null;
    }
    
}
