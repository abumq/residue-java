package com.muflihun.residue;

import java.util.HashMap;
import java.io.Console;

class Minimal {
    public static void main(String args[]){
        System.out.println("Connecting...");
        Residue r = Residue.getInstance();
        try {
            r.setAccessCodeMap(new HashMap<String, String>() {{
                put("sample-app", "eif89");
            }});
            r.setApplicationName("Sample ResidueJ App");

            if (r.connect("localhost", 8777)) {
                System.out.println("successfully connected");


                final Residue.Logger logger = r.getLogger("sample-app");

				Console console = System.console();
				
				while (true) {
					String s = console.readLine();
					logger.info(s);
				}

            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            System.out.println(r.getLastError());
        }
    }
}
