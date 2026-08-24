import java.nio.file.*;
import java.util.regex.*;

public class CheckDivs {
    public static void main(String[] args) throws Exception {
        String text = new String(Files.readAllBytes(Paths.get("src/main/resources/templates/battle-arena.html")), "UTF-8");
        
        Matcher mOpen = Pattern.compile("<div\\b[^>]*>").matcher(text);
        int open = 0;
        while(mOpen.find()) open++;
        
        Matcher mClose = Pattern.compile("</div>").matcher(text);
        int close = 0;
        while(mClose.find()) close++;
        
        System.out.println("divs: " + open + " open, " + close + " close. Diff = " + (open - close));
    }
}
