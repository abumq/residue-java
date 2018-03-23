public class ResidueJNIExample {
   public static void main(String[] args) throws Exception {
       Residue logger = Residue.getInstance();
       
       logger.connect("conf.json");
       
       logger.info("message from JNI");
       
       System.out.println("Disconnecting...");
       
       // wait (for flush) and disconnect
       logger.disconnect();
   }
}
