import java.util.ArrayList;
import java.util.List;

public class Quoting {

    public List<String> parser(String command) {
        List<String> args = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        boolean escaped = false;

        for (char c : command.toCharArray()) {
            if (escaped) {
                // Treat the current character as a literal character
                current.append(c);
                escaped = false;
            } else if (c == '\\' && !inSingleQuote && !inDoubleQuote) {
                // Enable escaped mode for the next character outside quotes
                escaped = true;
            } else if (c == '\'' && !inDoubleQuote) {
                // Toggle single quote mode
                inSingleQuote = !inSingleQuote;
            } else if (c == '"' && !inSingleQuote) {
                // Toggle double quote mode
                inDoubleQuote = !inDoubleQuote;
            } else if (Character.isWhitespace(c) && !inSingleQuote && !inDoubleQuote) {
                // Split arguments on unquoted, unescaped whitespace
                if (current.length() > 0) {
                    args.add(current.toString());
                    current.setLength(0);
                }
            } else {
                // Append regular character
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

    public void executeEcho(List<String> commandSplit) {
        StringBuilder string = new StringBuilder();

        for (int i = 1; i < commandSplit.size(); i++) {
            if (i > 1) {
                string.append(" ");
            }

            string.append(commandSplit.get(i));
        }

        System.out.println(string);
    }

}
