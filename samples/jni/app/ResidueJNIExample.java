import com.muflihun.residue.Residue;
import com.muflihun.residue.Logger;

public class ResidueJNIExample {
   public static void main(String[] args) throws Exception {
       Residue r = Residue.getInstance();
       
       r.connect("app/conf.json");
       
       Logger logger = new Logger("sample-app");

       logger.info("message from JNI");
       
       System.out.println("Disconnecting...");
       
       // wait (for flush) and disconnect
       r.disconnect();
   }
}
