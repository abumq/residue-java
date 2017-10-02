package com.muflihun.residue;

import java.util.HashMap;
import java.util.logging.Handler;
import java.util.logging.LogManager;
import java.util.logging.Logger;


public class JLogger {

  public static class AppClass {
    private static final Logger LOGGER = Logger.getLogger(AppClass.class.getName());

    public void doSomething(){
        LOGGER.info("in AppClass");
    }
  }

  public static void main(String[] args) {
      //reset() will remove all default handlers
      LogManager.getLogManager().reset();
      Logger rootLogger = LogManager.getLogManager().getLogger("");

      rootLogger.addHandler(new Residue.ResidueLogger());

      Residue r = Residue.getInstance();
      try {
          r.setAccessCodeMap(new HashMap<String, String>() {{
              put("sample-app", "eif89");
          }});
          if (r.connect("localhost", 8777)) {
              System.out.println("successfully connected");
              AppClass c = new AppClass();
              c.doSomething();
              rootLogger.info("from rootLogger");
          }
      } catch (Exception e) {
          System.out.println(e.getMessage());
          System.out.println(r.getLastError());
      }
  }
}
