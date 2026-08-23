import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

public class Quoting {

    public static List<String> parseCommand(String command) {
        List<String> args = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        boolean escaped = false;

        char[] chars = command.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];

            if (escaped) {
                // Unquoted backslash: append next char as a literal
                current.append(c);
                escaped = false;
            } else if (inDoubleQuote) {
                if (c == '\\') {
                    // Peek at the next character inside double quotes
                    if (i + 1 < chars.length) {
                        char next = chars[i + 1];
                        if (next == '"' || next == '\\') {
                            current.append(next); // Append the escaped character directly
                            i++; // Consume the escaped character
                            continue;
                        }
                    }
                    // Backslash followed by anything else inside double quotes remains literal
                    current.append(c);
                } else if (c == '"') {
                    inDoubleQuote = false;
                } else {
                    current.append(c);
                }
            } else if (inSingleQuote) {

                if (c == '\'') {

                    inSingleQuote = false;

                } else {
                    current.append(c);
                }
            } else if (c == '\\') {
                escaped = true;

            } else if (c == '\'') {
                inSingleQuote = true;
            } else if (c == '"') {
                inDoubleQuote = true;
            } else if (Character.isWhitespace(c)) {
                if (current.length() > 0) {
                    args.add(current.toString());
                    current.setLength(0);
                }
            } else {
                current.append(c);
            }
        }

        if (current.length() > 0) {
            args.add(current.toString());
        }

        return args;
    }

    // void func(String command){
    // List<String> args = new ArrayList<>();
    // StringBuilder current = new StringBuilder();

    // boolean isQuote = false;
    // boolean isDoubleQuote = false;
    // for(char c: command.toCharArray()){
    // if(c == '\"' && isQuote == false){
    // isDoubleQuote = true;
    // } else if(c == '\'' && isDoubleQuote == false){
    // isQuote = true;
    // }else if(Character.isWhitespace(c) && isQuote == false ||
    // Character.isWhitespace(c) && isDoubleQuote == false){
    // if (current.length() > 0) {
    // args.add(current.toString());
    // current.setLength(0);
    // }
    // }else{
    // current.append(c);
    // }
    // }

    public void executeEcho(List<String> commandSplit, PrintStream output) {
        StringBuilder string = new StringBuilder();
    
        for (int i = 1; i < commandSplit.size(); i++) {
    
            if (commandSplit.get(i).equals(">")) {
                break;
            }
    
            if (i > 1) {
                string.append(" ");
            }
    
            string.append(commandSplit.get(i));
        }
    
       output.println(string);
    }

}
