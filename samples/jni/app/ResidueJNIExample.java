import com.abumq.residue.Residue;
import com.abumq.residue.Logger;

public class ResidueJNIExample {
   public static void main(String[] args) throws Exception {
       Residue r = Residue.getInstance();
       
       r.connect("app/conf.json");
       
       Logger logger = new Logger("sample-app");

       logger.info("message from JNI");
       
       Thread.currentThread().setName("ThreadSimple");

       logger.info("message from JNI with thread ID");
       
       System.out.println("Disconnecting...");
       
       // wait (for flush) and disconnect
       r.disconnect();
   }
}
