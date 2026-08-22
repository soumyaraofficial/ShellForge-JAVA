public class test {
     public static void main(String[] args) throws Exception {
        //we will run a video
        Runtime process = Runtime.getRuntime();
       
        String video ="/Users/soumyaranjanpanda/Downloads/The.Death.of.Robin.Hood.2026.1080p.10bit.WEB-DL.HIN-ENG.5.1.ESub-.mkv";
        process.exec(new String[]{"open","-a","vlc",video});
     }
}
