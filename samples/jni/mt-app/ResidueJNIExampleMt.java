import com.muflihun.residue.Residue;
import com.muflihun.residue.Logger;

// Base code from https://docs.oracle.com/javase/tutorial/essential/concurrency/simple.html

public class ResidueJNIExampleMt {
    private static class MessageLoop implements Runnable {
        
        private Logger logger = new Logger("sample-app");
        
        public void run() {
            String importantInfo[] = {
                "Mares eat oats",
                "Does eat oats",
                "Little lambs eat ivy",
                "A kid will eat ivy too"
            };
            try {
                for (int i = 0; i < importantInfo.length; i++) {
                    // Pause for 4 seconds
                    Thread.sleep(400);
                    // Print a message
                    logger.info(importantInfo[i]);
                }
            } catch (InterruptedException e) {
                logger.info("I wasn't done!");
            }
        }
    }
    
   public static void main(String[] args) throws Exception {
       Residue r = Residue.getInstance();
       
       r.connect("app/conf.json");
       
       Logger logger = new Logger("sample-app");
       long patience = 1000 * 60 * 60;
       
       // If command line argument
       // present, gives patience
       // in seconds.
       if (args.length > 0) {
           try {
               patience = Long.parseLong(args[0]) * 1000;
           } catch (NumberFormatException e) {
               System.err.println("Argument must be an integer.");
               System.exit(1);
           }
       }
       
       logger.info("Starting MessageLoop thread");
       long startTime = System.currentTimeMillis();
       Thread t = new Thread(new MessageLoop());
       t.start();
       
       logger.info("Waiting for MessageLoop thread to finish");
       // loop until MessageLoop
       // thread exits
       while (t.isAlive()) {
           logger.info("Still waiting...");
           // Wait maximum of 1 second
           // for MessageLoop thread
           // to finish.
           t.join(1000);
           if (((System.currentTimeMillis() - startTime) > patience)
               && t.isAlive()) {
               logger.info("Tired of waiting!");
               t.interrupt();
               // Shouldn't be long now
               // -- wait indefinitely
               t.join();
           }
       }
       logger.info("Finally!");
       
       // wait (for flush) and disconnect
       r.disconnect();
   }
}
