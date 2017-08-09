package com.muflihun.residue;

import java.util.HashMap;

class Application {
    public static void main(String args[]){
        System.out.println("Connecting...");
        Residue r = Residue.getInstance();
        try {
            r.setAccessCodeMap(new HashMap<String, String>() {{
                put("sample-app", "eif89");
            }});
            //r.setClientId("muflihun00102030");
            //r.setPrivateKeyFilename("/Users/majid.khan/Projects/residue/samples/clients/netcat/client-256-private.pem");
            //r.setServerKeyFilename("/Users/majid.khan/Projects/residue/samples/clients/netcat/server-1024-public.pem");
            r.setApplicationName("Sample ResidueJ App");
            r.setUtcTime(true);
            r.setUseTimeOffsetIfNotUtc(true);
            r.setTimeOffset(36000);

            if (r.connect("localhost", 8777)) {
                System.out.println("successfully connected");


                final Residue.Logger l = r.getLogger("default");

                for (int i = 1; i <= 10; ++i) {
                    l.info("this is first log message " + i);
                }

                Thread t2 = new Thread(new Runnable() {
                    public void run() {
                        for (int i = 1; i <= 10; ++i) {
                            l.info("this is first log message " + i);
                        }
                    }
                });
                t2.setName("SampleThread-2");
                t2.start();
                Thread t3 = new Thread(new Runnable() {
                    public void run() {
                        for (int i = 1; i <= 10; ++i) {
                            l.info("this is first log message " + i);
                        }
                    }
                });
                t3.setName("SampleThread-3");
                t3.start();

				        l.debug("this is debug log");
/*
                System.out.println("waiting to expire the token");

                try {
                    Thread.sleep(60 * 1000); // expire token
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }


                System.out.println("Logging after token (is supposedly) expired");

                l.info("After long wait");

                System.out.println("waiting to expire the client");
                try {
                    Thread.sleep(300 * 1000); // expire client
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }


                System.out.println("Logging after client (is supposedly) expired");

                l.info("After long wait (client)");

                System.out.println("waiting to expire the client");
                try {
                    Thread.sleep(25 * 1000); // almost expire (ping) client
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }


                System.out.println("Logging after client (is supposedly) close to expire");

                l.info("After long wait (ping)");

                try {
                    Thread.sleep(5 * 1000); // if ping failed this would fail
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                l.info("After long wait (after expire)");*/

                System.out.println("end of application!");

                // TODO: r.disconnect() which means flush...
                try {
                    Thread.sleep(3590000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            System.out.println(r.getLastError());
        }
    }
}
