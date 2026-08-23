import java.util.ArrayList;
import java.util.List;

public class Quoting {

    public List<String> parser(String command) {
        List<String> args = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
    
        for (char c : command.toCharArray()) {
            if (c == '\'' && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote;
            } else if (c == '"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote;
            } else if (Character.isWhitespace(c) && !inSingleQuote && !inDoubleQuote) {
              
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

//    void func(String command){
//     List<String> args = new ArrayList<>();
//     StringBuilder current = new StringBuilder();

//     boolean isQuote = false;
//     boolean isDoubleQuote = false;
//     for(char c: command.toCharArray()){
//         if(c == '\"' && isQuote == false){
//           isDoubleQuote = true;
//         } else if(c == '\'' && isDoubleQuote == false){
//             isQuote = true;
//         }else if(Character.isWhitespace(c) && isQuote == false || Character.isWhitespace(c) && isDoubleQuote == false){
//             if (current.length() > 0) {
//                 args.add(current.toString());
//                 current.setLength(0);
//             }
//         }else{
//                 current.append(c);
//         }
//     }


   
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
