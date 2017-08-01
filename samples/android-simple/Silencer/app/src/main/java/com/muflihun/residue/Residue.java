package com.muflihun.residue;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.muflihun.residue.thirdparty.android.util.Base64;

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERNull;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.channels.NotYetConnectedException;
import java.nio.channels.ReadPendingException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayDeque;
import java.util.Calendar;
import java.util.Date;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.TimeZone;
import java.util.concurrent.CountDownLatch;
import java.util.zip.DeflaterOutputStream;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class Residue {

    private static final Integer PING_THRESHOLD = 15; // minimum client_age
    private static final String DEFAULT_ACCESS_CODE = "default";
    private static final String JUST_CONNECTED = "CONNECTED";

    private final ResidueClient connectionClient = new ResidueClient();
    private final ResidueClient tokenClient = new ResidueClient();
    private final ResidueClient loggingClient = new ResidueClient();

    private String host;
    private Integer port;
    private Integer loggingPort;
    private Integer tokenPort;
    private String applicationName;
    private Integer rsaKeySize = 2048;
    private Boolean utcTime;
    private Integer timeOffset;

    private Map<String, String> accessCodeMap;
    private volatile Map<String, ResidueToken> tokens = new HashMap<>();
    private volatile Deque<JsonObject> backlog = new ArrayDeque<>();
    private volatile Map<String, Logger> loggers = new HashMap<>();

    private String privateKeyFilename;
    private PrivateKey privateKey;

    private String serverKeyFilename;

    private boolean connected;
    private boolean connecting;

    private String key;
    private String clientId;
    private Integer age;
    private Date dateCreated;
    private Integer serverFlags;
    private Integer maxBulkSize;
    private volatile String lastError;

    private static Residue instance;

    public static Residue getInstance() {
        if (instance == null) {
            instance = new Residue();
        }
        return instance;
    }

    protected Residue() {
        this.connected = false;
        this.utcTime = false;
        this.timeOffset = 0;
    }

    public String getLastError() {
        return lastError;
    }

    public Boolean isConnected() {
        return connected;
    }

    public void setAccessCodeMap(final Map<String, String> accessCodeMap) {
        this.accessCodeMap = accessCodeMap;
    }

    public void setTimeOffset(final Integer timeOffset) {
        this.timeOffset = timeOffset;
    }

    public void setClientId(final String clientId) {
        this.clientId = clientId;
    }

    public void setRsaKeySize(final Integer rsaKeySize) {
        this.rsaKeySize = rsaKeySize;
    }

    public void setPrivateKeyFilename(final String privateKeyFilename) {
        this.privateKeyFilename = privateKeyFilename;
    }

    public void setServerKeyFilename(final String serverKeyFilename) {
        this.serverKeyFilename = serverKeyFilename;
    }

    public void setApplicationName(final String applicationName) {
        this.applicationName = applicationName;
    }

    public void setUtcTime(Boolean utcTime) {
        this.utcTime = utcTime;
    }

    /**
     * Logger class to send log messages to the server
     */
    public static class Logger {
        private String id;

        private Logger(String id) {
            this.id = id;
        }

        public void info(String msg) {
            Residue.getInstance().log(id, msg, LoggingLevels.INFO);
        }

        public void warning(String msg) {
            Residue.getInstance().log(id, msg, LoggingLevels.WARNING);
        }

        public void error(String msg) {
            Residue.getInstance().log(id, msg, LoggingLevels.ERROR);
        }

        public void debug(String msg) {
            Residue.getInstance().log(id, msg, LoggingLevels.DEBUG);
        }

        public void fatal(String msg) {
            Residue.getInstance().log(id, msg, LoggingLevels.FATAL);
        }

        public void trace(String msg) {
            Residue.getInstance().log(id, msg, LoggingLevels.TRACE);
        }

        public void verbose(String msg) {
            Residue.getInstance().log(id, msg, LoggingLevels.VERBOSE);
        }

        public void verbose(String msg, Integer vlevel) {
            Residue.getInstance().log(id, msg, LoggingLevels.VERBOSE, vlevel);
        }
    }

    /**
     * Gets existing or new logger
     * @see Logger
     */
    public Logger getLogger(String id) {
        if (loggers.containsKey(id)) {
            return loggers.get(id);
        }
        Logger newLogger = new Logger(id);
        loggers.put(id, newLogger);
        return newLogger;
    }

    /**
     * Sets host and port
     */
    public void setHost(final String host, final Integer port) {
        this.host = host;
        this.port = port;
    }

    /**
     * Connects to previously set host and port
     * @see #setHost(String, Integer)
     * @see #connect(String, Integer)
     */
    public static boolean connect() throws Exception {
        return connect(getInstance().host, getInstance().port);
    }

    /**
     * Connects to the residue server and waits until connected or throws exception
     * @param host Server host
     * @param port Connection port
     * @return True if successfully connected, otherwise false
     * @throws Exception If any exception is thrown
     */
    public static boolean connect(final String host, final Integer port) throws Exception {
        ResidueUtils.log("connect()");
        getInstance().host = host;
        getInstance().port = port;
        getInstance().connecting = true;
        getInstance().connected = false;
        getInstance().connectionClient.destroy();
        getInstance().tokenClient.destroy();
        getInstance().loggingClient.destroy();

        final Map<String, String> accessCodeMap = getInstance().accessCodeMap;
        getInstance().tokens.clear();

        final CountDownLatch latch = new CountDownLatch(3); // 3 connection sockets
        if (getInstance().clientId != null && !getInstance().clientId.isEmpty()
                && getInstance().privateKeyFilename != null
                && !getInstance().privateKeyFilename.isEmpty()) {
            getInstance().privateKey = ResidueUtils.getPemPrivateKey(getInstance().privateKeyFilename);
        }

        getInstance().connectionClient.connect(getInstance().host, getInstance().port, new ResponseHandler("connectionClient.connect") {
            @Override
            public void handle(String data, boolean hasError) {
                logForDebugging();
                JsonObject j = new JsonObject();
                j.addProperty("_t", ResidueUtils.getTimestamp());
                j.addProperty("type", ConnectType.CONNECT.getValue());
                j.addProperty("key_size", 128);
                if (getInstance().clientId != null
                        && !getInstance().clientId.isEmpty()
                        && getInstance().privateKeyFilename != null
                        && !getInstance().privateKeyFilename.isEmpty()
                        && getInstance().rsaKeySize != null) {
                    j.addProperty("client_id", getInstance().clientId);
                } else {
                    ResidueUtils.log("Generating " + getInstance().rsaKeySize + "-bit key...");
                    KeyPair p = ResidueUtils.createNewKeyPair(getInstance().rsaKeySize);
                    getInstance().privateKey = p.getPrivate();
                    j.addProperty("rsa_public_key",  ResidueUtils.keyToPem(p.getPublic()));
                }

                String request = new Gson().toJson(j);

                if (getInstance().serverKeyFilename != null && !getInstance().serverKeyFilename.isEmpty()) {
                    try {
                        final PublicKey publicKey = ResidueUtils.getPemPublicKey(getInstance().serverKeyFilename);
                        request = ResidueUtils.base64Encode(ResidueUtils.encryptRSA(request, publicKey));
                    } catch (Exception e) {
                        ResidueUtils.log("Invalid server public key, ignoring and trying with plain connection! " + e.getMessage());
                    }
                }

                getInstance().connectionClient.send(request, new ResponseHandler("connectionClient.send") {

                    @Override
                    public void handle(String data, boolean hasError) {

                        logForDebugging();
                        try {
                            byte[] decoded = ResidueUtils.base64Decode(data);
                            String s2 = ResidueUtils.decryptRSA(decoded, getInstance().privateKey);
                            int pos = s2.indexOf("{\"ack\"");
                            if (pos == -1) {
                                ResidueUtils.log("Pos == -1");
                                return;
                            }
                            s2 = s2.substring(pos); // FIXME: Fix this decryption! // TODO: FOR_ANDROID
                            ResidueUtils.log("Recv (RSA): " + s2); // TODO: FOR_ANDROID
                            JsonObject nonAckResponse = new Gson().fromJson(s2, JsonObject.class);

                            getInstance().key = nonAckResponse.get("key").getAsString();
                            getInstance().clientId = nonAckResponse.get("client_id").getAsString();

                            JsonObject j = new JsonObject();
                            j.addProperty("_t", ResidueUtils.getTimestamp());
                            j.addProperty("type", ConnectType.ACKNOWLEGEMENT.getValue());
                            j.addProperty("client_id", getInstance().clientId);
                            String request = new Gson().toJson(j);
                            String r = ResidueUtils.encrypt(request, getInstance().key);
                            getInstance().connectionClient.send(r, new ResponseHandler("connectionClient.send-2") {
                                @Override
                                public void handle(String data, boolean hasError) {
                                    logForDebugging();
                                    getInstance().connecting = false;
                                    String finalConnectionStr = ResidueUtils.decrypt(data, getInstance().key);
                                    JsonObject finalConnection = new Gson().fromJson(finalConnectionStr, JsonObject.class);
                                    if (finalConnection.get("status").getAsInt() == 0) {
                                        getInstance().age = finalConnection.get("age").getAsInt();
                                        getInstance().loggingPort = finalConnection.get("logging_port").getAsInt();
                                        getInstance().tokenPort = finalConnection.get("token_port").getAsInt();
                                        getInstance().maxBulkSize = finalConnection.get("max_bulk_size").getAsInt();
                                        getInstance().serverFlags = finalConnection.get("flags").getAsInt();
                                        getInstance().dateCreated = new Date(finalConnection.get("date_created").getAsLong() * 1000);
                                        getInstance().connected = true;
                                        try {
                                            getInstance().tokenClient.connect(getInstance().host, getInstance().tokenPort, new ResponseHandler("tokenClient.connect") {
                                                @Override
                                                public void handle(String data, boolean hasError) {
                                                    logForDebugging();
                                                    if (JUST_CONNECTED.equals(data) && Residue.getInstance().tokens.isEmpty() && Residue.getInstance().accessCodeMap != null) {
                                                        for (String key : Residue.getInstance().accessCodeMap.keySet()) {
                                                            try {
                                                                getInstance().obtainToken(key, Residue.getInstance().accessCodeMap.get(key));
                                                            } catch (Exception e) {
                                                                e.printStackTrace();
                                                            }
                                                        }
                                                    } else if (!JUST_CONNECTED.equals(data) && data != null && !data.isEmpty() && !hasError) {
                                                        // We received new token

                                                    }
                                                    latch.countDown();
                                                }
                                            });
                                        } catch (IOException e) {
                                            latch.countDown();
                                        }
                                        try {
                                            getInstance().loggingClient.connect(getInstance().host, getInstance().loggingPort, new ResponseHandler("loggingClient.connect") {
                                                @Override
                                                public void handle(String data, boolean hasError) {
                                                    logForDebugging();
                                                    latch.countDown();
                                                }
                                            });
                                        } catch (IOException e) {
                                            latch.countDown();
                                        }
                                    } else {
                                        getInstance().lastError = finalConnection.get("error_text").getAsString();
                                    }
                                    latch.countDown();
                                }
                            });
                        } catch (Exception e) {
                            throw e;
                        }
                    }

                });
            }
        });

        latch.await();
        if (getInstance().connected) {
            try {
                if (!getInstance().dispatcher.isAlive()) {
                    getInstance().dispatcher.start();
                } else {
                    ResidueUtils.log("Dispatcher resumed!");
                }
            } catch (Exception e) {
                ResidueUtils.log("Unable to start dispatcher thread [" + e.getMessage() + "]");
            }
        }
        return getInstance().connected;
    }

    private abstract static class ResponseHandler {

        private String id;

        public ResponseHandler(String id) {
            this.id = id;
        }

        public abstract void handle(String data, boolean hasError);

        public void logForDebugging() {
            ResidueUtils.log("ResponseHandler::handle " + this.id);
        }

        public void logForDebugging(final String data) {
            logForDebugging();
            ResidueUtils.log("ResponseHandler::handle::data = " + data);
        }

        @Override
        public String toString() {
            return id;
        }
    }

    private enum ConnectType {
        CONNECT(1),
        ACKNOWLEGEMENT(2),
        PING(3);

        private Integer value;

        ConnectType(Integer value) {
            this.value = value;
        }

        public Integer getValue() {
            return value;
        }
    }

    private enum LoggingLevels {
        TRACE(2),
        DEBUG(4),
        FATAL(8),
        ERROR(16),
        WARNING(32),
        VERBOSE(64),
        INFO(128);

        private Integer value;

        LoggingLevels(Integer value) {
            this.value = value;
        }

        public Integer getValue() {
            return value;
        }
    }

    private enum Flag {
        NONE(0),
        ALLOW_UNKNOWN_LOGGERS(1),
        ALLOW_DEFAULT_ACCESS_CODE(4),
        ALLOW_PLAIN_LOG_REQUEST(8),
        ALLOW_BULK_LOG_REQUEST(16),
        COMPRESSION(256);

        private Integer value;

        Flag(Integer value) {
            this.value = value;
        }

        public boolean isSet() {
            if (!Residue.getInstance().connected) {
                return false;
            }
            return (Residue.getInstance().serverFlags & this.value) != 0;
        }
    }

    /**
     * Residue network client
     */
    private static class ResidueClient {
        private static final String PACKET_DELIMITER = "\r\n\r\n";
        private AsynchronousSocketChannel socketChannel;
        private Boolean isConnected;

        private ResidueClient() {
            isConnected = false;
        }

        private void destroy() {
            try {
                if (isConnected && socketChannel.isOpen()) {
                    isConnected = false;
                    socketChannel.close();
                }
            } catch (IOException e) {
                // ignore
            }
        }

        private void connect(String host, Integer port, final ResponseHandler responseHandler) throws IOException {
            socketChannel = AsynchronousSocketChannel.open();
            socketChannel.connect(new InetSocketAddress(host, port), socketChannel,
                    new CompletionHandler<Void, AsynchronousSocketChannel>() {
                        @Override
                        public void completed(Void result, AsynchronousSocketChannel channel) {
                            isConnected = true;
                            responseHandler.handle("CONNECTED", false);
                        }

                        @Override
                        public void failed(Throwable exc, AsynchronousSocketChannel channel) {
                            ResidueUtils.log("Failed to connect to the server " + exc);
                            responseHandler.handle("FAILED", true);
                        }
                    });
        }

        private void read(final ResponseHandler responseHandler) {
            final ByteBuffer buf = ByteBuffer.allocate(4098);
            try {
                socketChannel.read(buf, socketChannel,
                        new CompletionHandler<Integer, AsynchronousSocketChannel>() {
                            @Override
                            public void completed(Integer result, AsynchronousSocketChannel channel) {
                                try {
                                    responseHandler.handle(new String(buf.array(), "UTF-8"), false);
                                } catch (UnsupportedEncodingException e) {
                                    e.printStackTrace();
                                }
                            }

                            @Override
                            public void failed(Throwable exc, AsynchronousSocketChannel channel) {
                                ResidueUtils.log(exc.getMessage());
                                responseHandler.handle(exc.getMessage(), true);
                            }

                        });
            } catch (ReadPendingException e) {
                // We ignore this as we may send multiple requests without really waiting for
                // response
            } catch (NotYetConnectedException e) {
                // we may be in the middle of connecting but still log it
                e.printStackTrace();
            }
        }

        private void send(final String message, final ResponseHandler responseHandler) {
            ByteBuffer buf = ByteBuffer.allocate(4098);
            buf.put((message + PACKET_DELIMITER).getBytes());
            buf.flip();
            socketChannel.write(buf, socketChannel,
                    new CompletionHandler<Integer, AsynchronousSocketChannel>() {
                @Override
                public void completed(Integer result, AsynchronousSocketChannel channel) {
                    read(responseHandler);
                }

                @Override
                public void failed(Throwable exc, AsynchronousSocketChannel channel) {
                    read(responseHandler);
                }

            });
        }
    }

    private static class ResidueToken {
        private String data;
        private Date dateCreated;
        private Integer life;
    }

    /**
     * Residue utility functions
     */
    private static class ResidueUtils {

        private static void log(Object msg) {
            System.out.println(msg);
        }

        private static PrivateKey getPemPrivateKey(String filename) throws Exception {
            File f = new File(filename);
            FileInputStream fis = new FileInputStream(f);
            DataInputStream dis = new DataInputStream(fis);
            final ByteBuffer buf = ByteBuffer.allocate((int) f.length());
            dis.readFully(buf.array());
            dis.close();
            fis.close();

            String temp = new String(buf.array());
            String privKeyPEM = temp.replace("-----BEGIN RSA PRIVATE KEY-----", "");
            privKeyPEM = privKeyPEM.replace("-----END RSA PRIVATE KEY-----", "");
            privKeyPEM = privKeyPEM.trim();

            byte[] decoded = ResidueUtils.base64Decode(privKeyPEM);

            ASN1EncodableVector v = new ASN1EncodableVector();
            v.add(new ASN1Integer(0));
            ASN1EncodableVector v2 = new ASN1EncodableVector();
            v2.add(new ASN1ObjectIdentifier(PKCSObjectIdentifiers.rsaEncryption.getId()));
            v2.add(DERNull.INSTANCE);
            v.add(new DERSequence(v2));
            v.add(new DEROctetString(decoded));
            ASN1Sequence seq = new DERSequence(v);
            byte[] privKey = seq.getEncoded("DER");

            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(privKey);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return kf.generatePrivate(spec);
        }

        private static PublicKey getPemPublicKey(String filename) throws Exception {
            File f = new File(filename);
            FileInputStream fis = new FileInputStream(f);
            DataInputStream dis = new DataInputStream(fis);
            final ByteBuffer buf = ByteBuffer.allocate((int) f.length());
            dis.readFully(buf.array());
            dis.close();
            fis.close();

            String temp = new String(buf.array());
            String publicKeyPEM = temp.replace("-----BEGIN PUBLIC KEY-----\n", "");
            publicKeyPEM = publicKeyPEM.replace("-----END PUBLIC KEY-----", "");

            byte[] decoded = ResidueUtils.base64Decode(publicKeyPEM);

            X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return kf.generatePublic(spec);
        }

        private static KeyPair createNewKeyPair(Integer size) {
            try {
                KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
                SecureRandom random = SecureRandom.getInstance("SHA1PRNG");//, "SUN"); // Android!
                keyGen.initialize(size, random);
                return keyGen.generateKeyPair();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        private static String keyToPem(PublicKey pubKey) {
            String s = "-----BEGIN RSA PUBLIC KEY-----\n" +
                    ResidueUtils.base64Encode(pubKey.getEncoded()) +
                    "\n-----END RSA PUBLIC KEY-----\n";
            return ResidueUtils.base64Encode(s.getBytes());
        }

        private static byte[] encryptRSA(String Buffer, PublicKey key) {
            try {

                Cipher rsa = Cipher.getInstance("RSA");
                rsa.init(Cipher.ENCRYPT_MODE, key);
                return rsa.doFinal(Buffer.getBytes());
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }


        private static String decryptRSA(byte[] buffer, PrivateKey key) {
            try {
                Cipher rsa = Cipher.getInstance("RSA");
                rsa.init(Cipher.DECRYPT_MODE, key);
                byte[] utf8 = rsa.doFinal(buffer);
                return new String(utf8, "UTF-8");
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        private static String encrypt(String request, String keyHex) {
            try {
                byte[] initVector = new byte[16];
                new Random().nextBytes(initVector);

                String randomIV = ResidueUtils.hexEncode(initVector);
                byte[] k = ResidueUtils.hexDecode(keyHex);
                IvParameterSpec ivSpec = new IvParameterSpec(initVector);
                SecretKeySpec keySpec = new SecretKeySpec(k, "AES");

                Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
                cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);

                byte[] encrypted = cipher.doFinal(request.getBytes());
                return randomIV + ":" + Residue.getInstance().clientId + ":" + new String(ResidueUtils.base64Encode(encrypted));
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        private static String decrypt(String response, String keyHex) {
            try {
                String[] parts = response.split(":");
                if (parts.length >= 2) {
                    String iv = parts[0];
                    byte[] data = ResidueUtils.base64Decode(parts[1]);
                    IvParameterSpec ivSpec = new IvParameterSpec(ResidueUtils.hexDecode(iv));
                    SecretKeySpec keySpec = new SecretKeySpec(ResidueUtils.hexDecode(keyHex), "AES");

                    Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
                    cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);

                    byte[] decrypted = cipher.doFinal(data);
                    return new String(decrypted);
                }
                return null;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        private static String base64Encode(byte[] bytes) {
            return Base64.encodeToString(bytes, Base64.DEFAULT);
        }

        private static byte[] base64Decode(String str) {
            return Base64.decode(str.getBytes(), Base64.DEFAULT);
        }

        private final static char[] HEX_DIGITS = "0123456789ABCDEF".toCharArray();

        private static String hexEncode(byte[] bytes) {
            int l = bytes.length;

            char[] out = new char[l << 1];

            // two characters form the hex value.
            for (int i = 0, j = 0; i < l; i++) {
                out[j++] = HEX_DIGITS[(0xF0 & bytes[i]) >>> 4];
                out[j++] = HEX_DIGITS[0x0F & bytes[i]];
            }

            return new String(out);
        }

        private static byte[] hexDecode(String hex) throws RuntimeException {
            char[] data = hex.toCharArray();
            int len = data.length;

            if ((len & 0x01) != 0) {
                throw new RuntimeException("Odd number of characters.");
            }

            byte[] out = new byte[len >> 1];

            // two characters form the hex value.
            for (int i = 0, j = 0; j < len; i++) {
                int f = Character.digit(data[j++], 16) << 4;
                f = f | Character.digit(data[j++], 16);
                out[i] = (byte) (f & 0xFF);
            }

            return out;

        }

        private static long getTimestamp() {
            return System.currentTimeMillis() / 1000;
        }
    }

    private void obtainToken(final String loggerId, String accessCode) throws Exception {
        obtainToken(loggerId, accessCode, false);
    }

    private void obtainToken(final String loggerId, String accessCode, boolean recursiveEmptyAccessCode) throws Exception {

        if (tokenClient.isConnected) {
            if (accessCode == null) {
                if (accessCodeMap != null && accessCodeMap.containsKey(loggerId)) {
                    accessCode = accessCodeMap.get(loggerId);
                } else {
                    accessCode = DEFAULT_ACCESS_CODE;
                }
            }
            if (DEFAULT_ACCESS_CODE.equals(accessCode) && !Flag.ALLOW_DEFAULT_ACCESS_CODE.isSet()) {
                throw new Exception("ERROR: Access code for logger [" + loggerId + "] not provided. Loggers without access code are not allowed by the server.");
            } else if (DEFAULT_ACCESS_CODE.equals(accessCode) && Flag.ALLOW_DEFAULT_ACCESS_CODE.isSet() && !recursiveEmptyAccessCode) {
                // we don't need to get token, server will accept request
                // without tokens
                return;
            }
            final CountDownLatch latch = new CountDownLatch(1);
            JsonObject j = new JsonObject();
            j.addProperty("_t", ResidueUtils.getTimestamp());
            j.addProperty("logger_id", loggerId);
            j.addProperty("access_code", accessCode);
            String request = new Gson().toJson(j);
            ResidueUtils.log(request);

            String r = ResidueUtils.encrypt(request, key);
            tokenClient.send(r, new ResponseHandler("tokenClient.send") {
                @Override
                public void handle(String data, boolean hasError) {
                    logForDebugging();
                    if (!hasError && !data.isEmpty()) {
                        String finalConnectionStr = ResidueUtils.decrypt(data, key);
                        JsonObject tokenResponse = new Gson().fromJson(finalConnectionStr, JsonObject.class);
                        if (tokenResponse == null) {
                            latch.countDown();
                            return;
                        }
                        if (tokenResponse.get("status").getAsInt() == 0) {
                            ResidueToken token = new ResidueToken();
                            token.data = tokenResponse.get("token").getAsString();
                            token.life = tokenResponse.get("life").getAsInt();
                            token.dateCreated = new Date();
                            tokens.put(loggerId, token);
                        } else {
                            lastError = tokenResponse.get("error_text").getAsString();
                            ResidueUtils.log(getInstance().lastError);
                        }
                    } else {
                        ResidueUtils.log("Error while obtaining token");
                    }
                    latch.countDown();
                }
            });
            latch.await();
        }

    }

    private boolean hasValidToken(String loggerId) {
        if (!tokens.containsKey(loggerId)) {
            return false;
        }
        ResidueToken t = tokens.get(loggerId);
        return t != null && (t.life == 0 || (new Date().getTime() / 1000) - (t.dateCreated.getTime() / 1000) < t.life);
    }

    private boolean isClientValid() {
        if (!connected || dateCreated == null) {
            return false;
        }
        if (age == 0) {
            return true;
        }
        return new Date(((dateCreated.getTime() / 1000) + age) * 1000).after(new Date());
    }

    private boolean shouldSendPing() {
        if (!connected || connecting) {
            // Can't send ping
            return false;
        }
        if (age == 0) {
            // Always alive!
            return false;
        }
        if (dateCreated == null) {
            return true;
        }
        return age - ((new Date().getTime() / 1000) - (dateCreated.getTime() / 1000)) < PING_THRESHOLD;
    }

    private void sendPing() {
        final CountDownLatch latch = new CountDownLatch(1);
        JsonObject j = new JsonObject();
        j.addProperty("_t", ResidueUtils.getTimestamp());
        j.addProperty("type", ConnectType.PING.getValue());
        j.addProperty("client_id", clientId);
        String request = new Gson().toJson(j);

        String r = ResidueUtils.encrypt(request, key);

        connectionClient.send(r, new ResponseHandler("connectionClient.send-ping") {
            @Override
            public void handle(String data, boolean hasError) {
                logForDebugging(data);
                String pingResponseStr = ResidueUtils.decrypt(data, key);
                JsonObject pingResponse = new Gson().fromJson(pingResponseStr, JsonObject.class);
                if (pingResponse != null && pingResponse.get("status").getAsInt() == 0) {
                    ResidueUtils.log("Updating client age via ping!");
                    dateCreated = new Date(pingResponse.get("date_created").getAsLong() * 1000);
                }
                latch.countDown();
            }
        });
        try {
            latch.await();
        } catch (InterruptedException e) {
            // ignore
        }
    }

    private Thread dispatcher = new Thread(new Runnable() {
        public void run()
        {
            while (true) {
                if (!backlog.isEmpty()) {
                    if (!isClientValid()) {
                        try {
                            ResidueUtils.log("Reconnecting...");
                            connect(host, port);
                        } catch (Exception e) {
                            // Unable to connect
                            e.printStackTrace();
                        }
                    }
                    if (shouldSendPing()) {
                        ResidueUtils.log("Pinging...");
                        sendPing();
                    }
                    JsonObject j;
                    synchronized (backlog) {
                        j = backlog.pop();
                    }
                    if (j != null) {
                        String token = null;
                        String loggerId = j.get("logger").getAsString();
                        if (!hasValidToken(loggerId)) {
                            try {
                                ResidueUtils.log("Obtaining new token");
                                obtainToken(loggerId, null /* means read from map */);
                            } catch (Exception e) {
                                // Ignore
                                e.printStackTrace();
                            }
                        }
                        ResidueToken tokenObj = tokens.get(loggerId);
                        if (tokenObj == null && Flag.ALLOW_DEFAULT_ACCESS_CODE.isSet()) {
                            token = "";
                        } else if (tokenObj != null) {
                            token = tokenObj.data;
                        }
                        if (token == null) {
                            ResidueUtils.log("Failed to obtain token");
                        } else {
                            if (!token.isEmpty()) { // empty token means token is not needed
                                j.addProperty("token", token);
                            }

                            String request = new Gson().toJson(j);
                            if (Flag.COMPRESSION.isSet()) {
                                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                DeflaterOutputStream dos = new DeflaterOutputStream(baos);
                                try {
                                    dos.write(request.getBytes());
                                    dos.flush();
                                    dos.close();
                                    request = ResidueUtils.base64Encode(baos.toByteArray());
                                } catch (IOException e) {
                                    ResidueUtils.log(e);
                                }
                            }
                            String r = ResidueUtils.encrypt(request, key);
                            getInstance().loggingClient.send(r, new ResponseHandler("loggingClient.send") {
                                @Override
                                public void handle(String data, boolean hasError) {
                                }
                            });
                            try {
                                Thread.sleep(1);
                            } catch (InterruptedException e) {
                                // Ignore
                            }
                        }
                    }
                }
            }
        }
    });

    private void log(String loggerId, String msg, LoggingLevels level, Integer vlevel) {

        if (getInstance().loggingClient.isConnected) {
            int baseIdx = 5;
            final int stackItemIndex = level == LoggingLevels.VERBOSE && vlevel > 0 ? baseIdx : (baseIdx + 1);
            final StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            StackTraceElement stackItem = null;
            if (stackTrace != null && stackTrace.length > stackItemIndex) {
                stackItem = Thread.currentThread().getStackTrace()[stackItemIndex];
            }

            Calendar c = Calendar.getInstance();
            if (Boolean.TRUE.equals(utcTime)) {
                TimeZone timeZone = c.getTimeZone();
                int offset = timeZone.getRawOffset();
                if (timeZone.inDaylightTime(new Date())) {
                    offset = offset + timeZone.getDSTSavings();
                }
                int offsetHrs = offset / 1000 / 60 / 60;
                int offsetMins = offset / 1000 / 60 % 60;

                c.add(Calendar.HOUR_OF_DAY, -offsetHrs);
                c.add(Calendar.MINUTE, -offsetMins);
            }
            if (timeOffset != null) {
                c.add(Calendar.SECOND, timeOffset);
            }
            JsonObject j = new JsonObject();
            j.addProperty("datetime", c.getTime().getTime());
            j.addProperty("logger", loggerId);
            j.addProperty("msg", msg);
            j.addProperty("file", stackItem == null ? "" : stackItem.getFileName());
            j.addProperty("line", stackItem == null ? 0 : stackItem.getLineNumber());
            j.addProperty("app", applicationName);
            j.addProperty("level", level.getValue());
            j.addProperty("func", stackItem == null ? "" : stackItem.getMethodName());
            j.addProperty("thread", Thread.currentThread().getName());
            j.addProperty("vlevel", vlevel);

            synchronized (backlog) {
                backlog.add(j);
            }

        }
    }

    private void log(String loggerId, String msg, LoggingLevels level) {
        log(loggerId, msg, level, 0);
    }
}
