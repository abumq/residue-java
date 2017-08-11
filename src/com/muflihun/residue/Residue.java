/**
 * Residue.java
 *
 * Official Java client library for Residue logging server
 *
 * Copyright (C) 2017 Muflihun Labs
 *
 * https://muflihun.com
 * https://muflihun.github.io/residue
 * https://github.com/muflihun/residue-java
 *
 * See https://github.com/muflihun/residue-java/blob/master/LICENSE
 * for licensing information
 */

package com.muflihun.residue;

import javax.crypto.Cipher;
import javax.crypto.EncryptedPrivateKeyInfo;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.channels.NotYetConnectedException;
import java.nio.channels.ReadPendingException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.Key;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.zip.DeflaterOutputStream;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERNull;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;

/**
 * Provides residue interface for interacting with residue server seamlessly and sending the log messages
 * <p>
 * For the functions that do not have documentation, please refer to C++ library's documentation,
 * all the concepts and naming conventions are same for all the official residue client libraries
 *
 * @see <a href='https://muflihun.github.io/residue/'>C++ API Documentation </a>
 */
public class Residue {

    private static final Integer TOUCH_THRESHOLD = 120; // should always be min(client_age)
    private static final String DEFAULT_ACCESS_CODE = "default";
    private static final Integer ALLOCATION_BUFFER_SIZE = 4098;

    private final ResidueClient connectionClient = new ResidueClient();
    private final ResidueClient tokenClient = new ResidueClient();
    private final ResidueClient loggingClient = new ResidueClient();

    private final Deque<JsonObject> backlog = new ArrayDeque<>();
    private final Map<String, ResidueToken> tokens = new HashMap<>();
    private final Map<String, Logger> loggers = new HashMap<>();

    private String host;
    private Integer port;
    private Integer loggingPort;
    private Integer tokenPort;
    private String applicationName;
    private Integer rsaKeySize = 2048;
    private Integer keySize = 128;
    private Boolean utcTime = false;
    private Integer timeOffset = 0;
    private Boolean useTimeOffsetIfNotUtc = false;
    private Integer dispatchDelay = 1;
    private Boolean autoBulkParams = true;
    private Boolean plainRequest = false;
    private Boolean bulkDispatch = false;
    private Integer bulkSize = 0;

    private Map<String, String> accessCodeMap;

    private String privateKeySecret;
    private String privateKeyFilename;
    private PrivateKey privateKey;

    private String serverKeyFilename;

    private boolean connected = false;
    private boolean connecting = false;

    private String key;
    private String clientId;
    private Integer age;
    private Date dateCreated;
    private Integer serverFlags;
    private Integer maxBulkSize;
    private volatile String lastError;
    private PrintStream printStream = new ResiduePrintStream(System.out);

    private static Residue instance;

    private Boolean isConnecting() {
        return connecting;
    }

    public static Residue getInstance() {
        if (instance == null) {
            instance = new Residue();
        }
        return instance;
    }

    private Residue() {
    }

    public String getHost() {
        return host;
    }

    public Integer getPort() {
        return port;
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

    /**
     * Enables automatic setting of bulk parameters depending on
     * what's most efficient. It essentially enables bulk if server supports
     * it and sets bulk size and dispatch delay accordingly.
     *
     * note: You need re-connect using <pre>connect()</pre> helper method
     * note: By default it is enabled
     */
    public void setAutoBulkParams(final Boolean autoBulkParams) {
        this.autoBulkParams = autoBulkParams;
    }

    public void setDispatchDelay(final Integer dispatchDelay) {
        this.dispatchDelay = dispatchDelay;
    }

    public void setClientId(final String clientId) {
        this.clientId = clientId;
    }

    /**
     * Applicable to unknown clients only. Known clients already have public key on the server
     * <p>
     * Accepted values are 1024, 2048, 4096
     *
     * @throws IllegalArgumentException if invalid key size is provided
     */
    public void setRsaKeySize(final Integer rsaKeySize) {
        if (rsaKeySize == 0 || rsaKeySize % 1024 != 0) {
            throw new IllegalArgumentException("Accepted RSA key sizes are 1024, 2048, 4096");
        }
        this.rsaKeySize = rsaKeySize;
    }

    /**
     * Only change this if you know the client can support any bigger keys
     * Accepted values are 128, 192 and 256
     *
     * @throws IllegalArgumentException if invalid key size is provided
     */
    public void setKeySize(final Integer keySize) throws IllegalArgumentException {
        if (keySize != 128 && keySize != 192 && keySize != 256) {
            throw new IllegalArgumentException("Accepted key sizes are 128, 192 and 256");
        }
        this.keySize = keySize;
    }

    public void setPrivateKeyFilename(final String privateKeyFilename) {
        this.privateKeyFilename = privateKeyFilename;
    }

    public void setPrivateKeySecret(final String privateKeySecret) {
        this.privateKeySecret = privateKeySecret;
    }

    public void setPlainRequest(final Boolean plainRequest) {
        this.plainRequest = plainRequest;
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
	 * If this is true the timeOffset will only take affect if the
	 * current timezone is NOT UTC.
     *
     * You must enable utcTime with this
     */
    public void setUseTimeOffsetIfNotUtc(Boolean useTimeOffsetIfNotUtc) {
        this.useTimeOffsetIfNotUtc = useTimeOffsetIfNotUtc;
    }

    public synchronized void loadConfigurations(final String jsonFilename) throws Exception {
        byte[] encoded = Files.readAllBytes(Paths.get(jsonFilename));
        String json = new String(encoded, Charset.defaultCharset());
        loadConfigurationsFromJson(json);
    }

    public synchronized void loadConfigurationsFromJson(final String json) throws Exception {
        JsonObject jsonObject = new Gson().fromJson(json, JsonObject.class);
        if (jsonObject.has("url")) {
            String[] parts = jsonObject.get("url").getAsString().split(":");
            if (parts.length == 2) {
                Integer port = Integer.parseInt(parts[1]);
                setHost(parts[0], port);
            }
        } else {
            throw new Exception("URL should be in format of <host>:<port>");
        }

        if (jsonObject.has("application_id")) {
            setApplicationName(jsonObject.get("application_id").getAsString());
        }

        if (jsonObject.has("utc_time")) {
            setUtcTime(jsonObject.get("utc_time").getAsBoolean());
        }

        if (jsonObject.has("use_timeoffset_if_not_utc")) {
            setUseTimeOffsetIfNotUtc(jsonObject.get("use_timeoffset_if_not_utc").getAsBoolean());
        }

        if (jsonObject.has("time_offset")) {
            setTimeOffset(jsonObject.get("time_offset").getAsInt());
        }

        if (jsonObject.has("rsa_key_size")) {
            setRsaKeySize(jsonObject.get("rsa_key_size").getAsInt());
        }

        if (jsonObject.has("dispatch_delay")) {
            setDispatchDelay(jsonObject.get("dispatch_delay").getAsInt());
        }

        if (jsonObject.has("key_size")) {
            setKeySize(jsonObject.get("key_size").getAsInt());
        }

        if (jsonObject.has("main_thread_id")) {
            Thread.currentThread().setName(jsonObject.get("main_thread_id").getAsString());
        }

        if (jsonObject.has("plain_request")) {
            setPlainRequest(jsonObject.get("plain_request").getAsBoolean());
        }

        if (jsonObject.has("access_codes")) {
            Type type = new TypeToken<List<Map<String, String>>>(){}.getType();

            Map<String, String> newMap = new HashMap<>();
            List<Map<String, String>> accessCodes = new Gson().fromJson(jsonObject.getAsJsonArray("access_codes"), type);
            for (Map<String, String> accessCodeMap : accessCodes) {
                String loggerId = "";
                String code = "";
                for (String key : accessCodeMap.keySet()) {
                    if ("logger_id".equals(key)) {
                        loggerId = accessCodeMap.get(key);
                    } else if ("code".equals(key)) {
                        code = accessCodeMap.get(key);
                    }
                }
                if (!loggerId.isEmpty() && !code.isEmpty()) {
                    newMap.put(loggerId, code);
                }
            }
            setAccessCodeMap(newMap);

        }

        if (jsonObject.has("server_public_key")) {
            setServerKeyFilename(jsonObject.get("server_public_key").getAsString());
        }

        if (jsonObject.has("client_id")) {
            setClientId(jsonObject.get("client_id").getAsString());
        }

        if (jsonObject.has("client_private_key")) {

            setPrivateKeyFilename(jsonObject.get("client_private_key").getAsString());

            if (jsonObject.has("client_key_secret")) {
                setPrivateKeySecret(jsonObject.get("client_key_secret").getAsString());
            }
        }
    }

    /**
     * Sets number of log messages to be bulked together
     * This depends on what server accepts. The configuration value on the server is
     * <a href='https://github.com/muflihun/residue/blob/master/docs/CONFIGURATION.md#max_items_in_bulk'>max_items_in_bulk</a>.
     *
     * @param bulkSize
     * @throws IllegalArgumentException If already connected and you try to set size more than
     * maximum allowed
     * @see #setAutoBulkParams(Boolean)
     */
    public void setBulkSize(Integer bulkSize) throws IllegalArgumentException {
        if (isConnected() && bulkSize > maxBulkSize) {
            throw new IllegalArgumentException("Invalid bulk dispatch size. Maximum allowed: " + maxBulkSize);
        }
        this.bulkSize = bulkSize;
    }

    /**
     * Logger class to send log messages to the server
     */
    public static class Logger {

        private String id;

        private Logger(String id) {
            this.id = id;
        }

        public boolean isDebugEnabled() {
            return true;
        }

        public boolean isInfoEnabled() {
            return true;
        }

        public boolean isWarnEnabled() {
            return true;
        }

        public boolean isErrorEnabled() {
            return true;
        }

        public void debug(Object obj) {
            log(obj, LoggingLevels.DEBUG);
        }

        public void info(Object obj) {
            log(obj, LoggingLevels.INFO);
        }

        public void error(Object obj) {
            log(obj, LoggingLevels.ERROR);
        }

        public void warn(Object obj) {
            log(obj, LoggingLevels.WARNING);
        }

        public void fatal(Object obj) {
            log(obj, LoggingLevels.FATAL);
        }

        public void trace(Object obj) {
            log(obj, LoggingLevels.TRACE);
        }

        public void debug(String format, Object... args) {
            if (isDebugEnabled()) {
                String message = String.format(format, args);

                log(message, LoggingLevels.DEBUG);
            }
        }

        public void debug(Throwable t, String format, Object... args) {
            if (isDebugEnabled()) {
                String message = String.format(format, args);

                log(message, t, LoggingLevels.DEBUG);
            }
        }

        public void debug(String message, Throwable throwable) {
            if (isDebugEnabled()) {
                log(message, throwable, LoggingLevels.DEBUG);
            }

        }

        public void info(String format, Object... args) {
            if (isInfoEnabled()) {
                String message = String.format(format, args);

                log(message, LoggingLevels.INFO);
            }
        }

        public void info(Throwable t, String format, Object... args) {
            if (isInfoEnabled()) {
                String message = String.format(format, args);

                info(message, t);
            }
        }

        public void info(String message, Throwable throwable) {
            if (isInfoEnabled()) {
                log(message, throwable, LoggingLevels.INFO);
            }
        }

        public void warn(String format, Object... args) {
            if (isWarnEnabled()) {
                String message = String.format(format, args);

                log(message, LoggingLevels.WARNING);
            }
        }

        public void warn(Throwable t, String format, Object... args) {
            if (isWarnEnabled()) {
                String message = String.format(format, args);

                warn(message, t);
            }
        }

        public void warn(String message, Throwable throwable) {
            if (isWarnEnabled()) {
                log(message, throwable, LoggingLevels.WARNING);
            }
        }

        public void error(String format, Object... args) {
            if (isErrorEnabled()) {
                String message = String.format(format, args);

                log(message, LoggingLevels.ERROR);
            }
        }

        public void error(Throwable t, String format, Object... args) {
            if (isErrorEnabled()) {
                String message = String.format(format, args);

                error(message, t);
            }
        }

        public void error(String message, Throwable throwable) {
            if (isErrorEnabled()) {
                log(message, throwable, LoggingLevels.ERROR);
            }
        }

        public void trace(String format, Object... args) {
            if (isErrorEnabled()) {
                String message = String.format(format, args);

                log(message, LoggingLevels.TRACE);
            }
        }

        public void trace(Throwable t, String format, Object... args) {
            if (isErrorEnabled()) {
                String message = String.format(format, args);

                trace(message, t);
            }
        }

        public void trace(String message, Throwable throwable) {
            if (isErrorEnabled()) {
                log(message, throwable, LoggingLevels.TRACE);
            }
        }

        public void fatal(String format, Object... args) {
            if (isErrorEnabled()) {
                String message = String.format(format, args);

                log(message, LoggingLevels.FATAL);
            }
        }

        public void fatal(Throwable t, String format, Object... args) {
            if (isErrorEnabled()) {
                String message = String.format(format, args);

                fatal(message, t);
            }
        }

        public void fatal(String message, Throwable throwable) {
            if (isErrorEnabled()) {
                log(message, throwable, LoggingLevels.FATAL);
            }
        }

        public void log(Object msg, Throwable t, LoggingLevels level) {
            if (t != null) {
                t.printStackTrace(Residue.getInstance().printStream);
            }
            Residue.getInstance().log(id, msg, level);
        }

        public void log(Object msg, LoggingLevels level) {
            Residue.getInstance().log(id, msg, level);
        }
    }

    class ResiduePrintStream extends PrintStream {

        final String STREAM_LOGGER_ID = "default";

        private ResiduePrintStream(PrintStream org) {
            super(org);
        }

        @Override
        public void println(String line) {
            Residue.getInstance().getLogger(STREAM_LOGGER_ID).info(line);
            super.println(line);
        }

        public void println(int line) {
            Residue.getInstance().getLogger(STREAM_LOGGER_ID).info(String.valueOf(line));
            this.println(String.valueOf(line));
        }

        public void println(double line) {
            Residue.getInstance().getLogger(STREAM_LOGGER_ID).info(String.valueOf(line));
            this.println(String.valueOf(line));
        }

        // implement more later ...
    }

    /**
     * Gets existing or new logger
     *
     * @see Logger
     */
    public synchronized Logger getLogger(String id) {
        if (loggers.containsKey(id)) {
            return loggers.get(id);
        }
        Logger newLogger = new Logger(id);
        loggers.put(id, newLogger);
        return newLogger;
    }

    public static Logger getClassLogger() {
        StackTraceElement[] stacktrace = Thread.currentThread().getStackTrace();
        StackTraceElement element = stacktrace[2];
        String name = element.getClassName();
        return getInstance().getLogger(name);
    }

    /**
     * Sets host and port
     */
    public void setHost(final String host, final Integer port) {
        this.host = host;
        this.port = port;
    }

    /**
     * Connects to previously set host and port.
     * <p>
     * You will need to make sure host and port is already set
     *
     * @see #setHost(String, Integer)
     * @see #connect(String, Integer)
     */
    public static boolean reconnect() throws Exception {
        return connect(getInstance().host, getInstance().port);
    }

    /**
     * Connects to the residue server and waits until connected or throws exception
     *
     * @param host Server host
     * @param port Connection port
     * @return True if successfully connected, otherwise false
     * @throws Exception If any exception is thrown
     */
    public static boolean connect(final String host, final Integer port) throws Exception {
        ResidueUtils.debugLog("reconnect()");
        getInstance().host = host;
        getInstance().port = port;
        getInstance().connecting = true;
        getInstance().connected = false;
        getInstance().connectionClient.destroy();
        getInstance().tokenClient.destroy();
        getInstance().loggingClient.destroy();
        synchronized (getInstance().tokens) {
            getInstance().tokens.clear();
        }

        final CountDownLatch latch = new CountDownLatch(3); // 3 connection sockets
        if (getInstance().clientId != null && !getInstance().clientId.isEmpty()
                && getInstance().privateKeyFilename != null
                && !getInstance().privateKeyFilename.isEmpty()) {
            getInstance().privateKey = ResidueUtils.getPemPrivateKey(getInstance().privateKeyFilename, getInstance().privateKeySecret);
        }

        getInstance().connectionClient.connect(getInstance().host, getInstance().port, new ResponseHandler("connectionClient.reconnect") {
            @Override
            public void handle(String data, boolean hasError) {
                logForDebugging();
                JsonObject j = new JsonObject();
                j.addProperty("_t", ResidueUtils.getTimestamp());
                j.addProperty("type", ConnectType.CONNECT.getValue());
                j.addProperty("key_size", getInstance().keySize);
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
                    j.addProperty("rsa_public_key", ResidueUtils.keyToPem(p.getPublic()));
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
                        if (hasError) {
                            ResidueUtils.log("Failed to connect, connection refused");
                            getInstance().connecting = false;
                            getInstance().connected = false;
                            latch.countDown();
                            latch.countDown();
                            latch.countDown();
                            return;
                        }
                        try {
                            if (!data.isEmpty() && "{".equals(data.substring(1, 1))) {
                                // error?

                                ResidueUtils.log("Failed to connect. Error response: " + data);
                                getInstance().connecting = false;
                                getInstance().connected = false;
                                latch.countDown();
                                latch.countDown();
                                latch.countDown();
                            }

                            byte[] decoded = ResidueUtils.base64Decode(data);
                            String s2 = ResidueUtils.decryptRSA(decoded, getInstance().privateKey);
                            if (s2 != null) {
                                int pos = s2.indexOf("{\"ack\""); // decryption issue on android
                                if (pos == -1) {
                                    ResidueUtils.log("Pos == -1");
                                    return;
                                }
                                s2 = s2.substring(pos);
                            }
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
                                        System.setOut(getInstance().printStream);
                                        if (Boolean.TRUE.equals(getInstance().autoBulkParams) && Flag.ALLOW_BULK_LOG_REQUEST.isSet()) {
                                            getInstance().bulkSize = Math.min(getInstance().maxBulkSize, 40);
                                            getInstance().bulkDispatch = true;
                                        }
                                        if (Boolean.TRUE.equals(getInstance().bulkDispatch) && Flag.ALLOW_BULK_LOG_REQUEST.isSet() && getInstance().bulkSize > getInstance().maxBulkSize) {
                                            getInstance().bulkSize = getInstance().maxBulkSize;
                                        } else if (Boolean.TRUE.equals(getInstance().bulkDispatch) && !Flag.ALLOW_BULK_LOG_REQUEST.isSet()) {
                                            getInstance().bulkDispatch = false;
                                        }
                                        getInstance().connected = true;
                                        try {
                                            getInstance().tokenClient.connect(getInstance().host, getInstance().tokenPort, new ResponseHandler("tokenClient.reconnect") {
                                                @Override
                                                public void handle(String data, boolean hasError) {
                                                    logForDebugging();
                                                    if (Flag.REQUIRES_TOKEN.isSet() && Residue.getInstance().accessCodeMap != null) {
                                                        for (String key : Residue.getInstance().accessCodeMap.keySet()) {
                                                            try {
                                                                getInstance().obtainToken(key, Residue.getInstance().accessCodeMap.get(key));
                                                            } catch (Exception e) {
                                                                e.printStackTrace();
                                                            }
                                                        }
                                                    }
                                                    latch.countDown();
                                                }
                                            });
                                        } catch (IOException e) {
                                            latch.countDown();
                                        }
                                        try {
                                            getInstance().loggingClient.connect(getInstance().host, getInstance().loggingPort, new ResponseHandler("loggingClient.reconnect") {
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
                            ResidueUtils.log(e.getMessage());
                            throw e;
                        }
                    }

                });
            }
        });

        latch.await(10L, TimeUnit.SECONDS);

        if (getInstance().connected) {
            try {
                if (!getInstance().dispatcher.isAlive()) {
                    getInstance().dispatcher.start();
                } else {
                    ResidueUtils.debugLog("Dispatcher resumed!");
                }
            } catch (Exception e) {
                ResidueUtils.log("ERROR: Unable to start dispatcher thread [" + e.getMessage() + "]");
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
            //ResidueUtils.debugLog("ResponseHandler::handle " + this.id);
        }

        public void logForDebugging(final String data) {
            logForDebugging();
            //ResidueUtils.debugLog("ResponseHandler::handle::data = " + data);
        }

        @Override
        public String toString() {
            return id;
        }
    }

    private enum ConnectType {
        CONNECT(1),
        ACKNOWLEGEMENT(2),
        TOUCH(3);

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
        REQUIRES_TOKEN(2),
        ALLOW_DEFAULT_ACCESS_CODE(4),
        ALLOW_PLAIN_LOG_REQUEST(8),
        ALLOW_BULK_LOG_REQUEST(16),
        COMPRESSION(256);

        private Integer value;

        Flag(Integer value) {
            this.value = value;
        }

        public boolean isSet() {
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
                            ResidueUtils.log("Failed to reconnect to the server " + exc);
                            responseHandler.handle("FAILED", true);
                        }
                    });
        }

        private void read(final ResponseHandler responseHandler) {
            final ByteBuffer buf = ByteBuffer.allocate(ALLOCATION_BUFFER_SIZE);
            try {
                socketChannel.read(buf, socketChannel,
                        new CompletionHandler<Integer, AsynchronousSocketChannel>() {
                            @Override
                            public void completed(Integer result, AsynchronousSocketChannel channel) {
                                try {
                                    responseHandler.handle(new String(buf.array(), "UTF-8").substring(0, ALLOCATION_BUFFER_SIZE - buf.remaining()), false);
                                } catch (UnsupportedEncodingException e) {
                                    e.printStackTrace();
                                }
                            }

                            @Override
                            public void failed(Throwable exc, AsynchronousSocketChannel channel) {
                                ResidueUtils.log("Thrown exception while reading: " + exc.getMessage());
                                if ("Connection reset by peer".equals(exc.getMessage())) {
                                    isConnected = false;
                                    getInstance().connected = false;
                                }
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
            ByteBuffer buf = ByteBuffer.allocate(ALLOCATION_BUFFER_SIZE);
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
                            exc.printStackTrace();
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
            synchronized (System.out) {
                System.out.println(msg);
            }
        }

        private static void debugLog(Object msg) {
            /*synchronized (System.out) {
                System.out.println(msg);
            }*/
        }

        private static PrivateKey getPemPrivateKey(String filename, String secret) throws Exception {
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
            ASN1ObjectIdentifier objectIdentifier = new ASN1ObjectIdentifier(PKCSObjectIdentifiers.rsaEncryption.getId());
            v2.add(objectIdentifier);
            v2.add(DERNull.INSTANCE);
            v.add(new DERSequence(v2));
            v.add(new DEROctetString(decoded));
            ASN1Sequence seq = new DERSequence(v);
            byte[] privKey = seq.getEncoded("DER");

            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privKey);
            KeyFactory kf = KeyFactory.getInstance("RSA");

            if (secret != null && !secret.isEmpty()) {
                // FIXME: Encrypted private keys not working
                PBEKeySpec pbeSpec = new PBEKeySpec(secret.toCharArray());
                EncryptedPrivateKeyInfo pkinfo = new EncryptedPrivateKeyInfo(keySpec.getEncoded());
                SecretKeyFactory skf = SecretKeyFactory.getInstance(pkinfo.getAlgName());
                Key secretKey = skf.generateSecret(pbeSpec);
                keySpec = pkinfo.getKeySpec(secretKey);
            }
            return kf.generatePrivate(keySpec);
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
            } else if (DEFAULT_ACCESS_CODE.equals(accessCode) && Flag.ALLOW_DEFAULT_ACCESS_CODE.isSet()) {
                // we don't need to get token, server will accept request
                // without tokens
                return;
            }
            ResidueUtils.debugLog("Obtaining new token for [" + loggerId + "]");
            final CountDownLatch latch = new CountDownLatch(1);
            JsonObject j = new JsonObject();
            j.addProperty("_t", ResidueUtils.getTimestamp());
            j.addProperty("logger_id", loggerId);
            j.addProperty("access_code", accessCode);
            String request = new Gson().toJson(j);
            ResidueUtils.debugLog("Token request: " + request);

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
                            synchronized (tokens) {
                                tokens.put(loggerId, token);
                            }
                        } else {
                            lastError = tokenResponse.get("error_text").getAsString();
                            ResidueUtils.debugLog("Error: " + getInstance().lastError);
                        }
                    } else {
                        ResidueUtils.debugLog("Error while obtaining token");
                    }
                    latch.countDown();
                }
            });
            latch.await(10L, TimeUnit.SECONDS);
        }

    }

    private synchronized boolean hasValidToken(String loggerId) {
        if (!Flag.REQUIRES_TOKEN.isSet()) {
            return true;
        }
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

    private boolean shouldTouch() {
        if (!connected || connecting) {
            // Can't send touch 
            return false;
        }
        if (age == 0) {
            // Always alive!
            return false;
        }
        if (dateCreated == null) {
            return true;
        }
        return age - ((new Date().getTime() / 1000) - (dateCreated.getTime() / 1000)) < TOUCH_THRESHOLD;
    }

    private void touch() {
        final CountDownLatch latch = new CountDownLatch(1);
        JsonObject j = new JsonObject();
        j.addProperty("_t", ResidueUtils.getTimestamp());
        j.addProperty("type", ConnectType.TOUCH.getValue());
        j.addProperty("client_id", clientId);
        String request = new Gson().toJson(j);

        String r = ResidueUtils.encrypt(request, key);

        connectionClient.send(r, new ResponseHandler("connectionClient.touch") {
            @Override
            public void handle(String data, boolean hasError) {
                logForDebugging(data);
                String touchResponseStr = ResidueUtils.decrypt(data, key);
                JsonObject touchResponse = new Gson().fromJson(touchResponseStr, JsonObject.class);
                if (touchResponse != null && touchResponse.get("status").getAsInt() == 0) {
                    ResidueUtils.log("Updating client age via touch!");
                    dateCreated = new Date(touchResponse.get("date_created").getAsLong() * 1000);
                }
                latch.countDown();
            }
        });
        try {
            latch.await(10L, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            // ignore
        }
    }

    private Thread dispatcher = new Thread(new Runnable() {
        public void run() {
            Integer reconnectingAttempts = 0; // don't use Timer as we want to schedule it once
            while (true) {
                if (!backlog.isEmpty()) {
                    if (isConnecting()) {
                        ResidueUtils.debugLog("Still connecting...");
                        if (reconnectingAttempts >= 20) { // 10 seconds
                            connecting = false;
                            connected = false;
                            reconnectingAttempts = 0; // reset
                            // fallthrough to next condition to ensure we start from scratch the re-connection
                        } else {
                            try {
                                Thread.sleep(500);
                                reconnectingAttempts++;
                            } catch (InterruptedException e) {
                                // Ignore
                            }
                        }
                        continue;
                    }

                    if (!isConnected()) {
                        try {
                            ResidueUtils.log("Trying to reconnect...");
                            connect(host, port);
                        } catch (Exception e) {
                            ResidueUtils.log("Unable to connect, " + e.getMessage() + "\nRetrying in 500ms");
                        }
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            // Ignore
                        }
                        continue;
                    }

                    if (!isClientValid()) {
                        try {
                            ResidueUtils.log("Client expired, reconnecting...");
                            connect(host, port);
                        } catch (Exception e) {
                            // Unable to reconnect
                            e.printStackTrace();
                        }
                    }

                    if (shouldTouch()) {
                        ResidueUtils.log("Touching...");
                        touch();
                    }

                    Integer totalRequests = Boolean.TRUE.equals(bulkDispatch) ? bulkSize : 1;
                    totalRequests = Math.min(totalRequests, backlog.size());

                    final JsonArray bulkJ = new JsonArray();
                    final Set<String> loggerIds = new HashSet<>();
                    final Map<String, String> tokenList = new HashMap<>();

                    // build up bulk request
                    synchronized (backlog) {
                        for (Integer i = 0; i < totalRequests; ++i) {
                            JsonObject j;
                            j = backlog.pop();
                            if (j != null) {
                                loggerIds.add(j.get("logger").getAsString());
                                if (Boolean.TRUE.equals(plainRequest)
                                        && Flag.ALLOW_PLAIN_LOG_REQUEST.isSet()) {
                                    j.addProperty("client_id", clientId);
                                }
                                bulkJ.add(j);
                            }
                        }
                    }

                    // Obtain tokens for all the loggers (unique)
                    for (String loggerId : loggerIds) {
                        if (!hasValidToken(loggerId)) {
                            try {
                                obtainToken(loggerId, null /* means read from map */);
                            } catch (Exception e) {
                                // Ignore
                                e.printStackTrace();
                            }
                        }
                        ResidueToken tokenObj = null;
                        synchronized (tokens) {
                            tokenObj = tokens.get(loggerId);
                        }
                        if (tokenObj == null
                                && (Flag.ALLOW_DEFAULT_ACCESS_CODE.isSet() || !Flag.REQUIRES_TOKEN.isSet())) {
                            tokenList.put(loggerId, "");
                        } else if (tokenObj != null) {
                            tokenList.put(loggerId, tokenObj.data);
                        } else {
                            tokenList.put(loggerId, null); // token not found
                        }
                    }

                    // Integrate token to the request
                    for (JsonElement jElem : bulkJ) {
                        JsonObject j = jElem.getAsJsonObject();
                        String loggerId = j.get("logger").getAsString();
                        String foundToken = tokenList.get(loggerId);
                        if (foundToken == null) {
                            ResidueUtils.log("ERROR: Failed to obtain token [" + loggerId + "]");
                            continue;
                        }
                        if (!foundToken.isEmpty()) {
                            j.addProperty("token", foundToken);
                        }
                    }

                    String request = new Gson().toJson(Boolean.TRUE.equals(bulkDispatch) ? bulkJ : bulkJ.get(0));
                    if (Flag.COMPRESSION.isSet()) {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        DeflaterOutputStream dos = new DeflaterOutputStream(baos);
                        try {
                            dos.write(request.getBytes());
                            dos.flush();
                            dos.close();
                            request = ResidueUtils.base64Encode(baos.toByteArray());
                        } catch (IOException e) {
                            ResidueUtils.log("Failed to compress: " + e.getMessage());
                        }
                    }
                    String r = Boolean.TRUE.equals(plainRequest)
                            && Flag.ALLOW_PLAIN_LOG_REQUEST.isSet() ? request : ResidueUtils.encrypt(request, key);
                    getInstance().loggingClient.send(r, new ResponseHandler("loggingClient.send") {
                        @Override
                        public void handle(String data, boolean hasError) {
                            if (data.isEmpty()) {
                                // Not connected
                                getInstance().connected = false;
                                getInstance().loggingClient.isConnected = false;
                            } else {
                                ResidueUtils.debugLog("loggingClient response: " + data);
                            }
                        }
                    });
                }
                try {
                    Thread.sleep(dispatchDelay);
                } catch (InterruptedException e) {
                    // Ignore
                }
            }
        }
    });

    private void log(String loggerId, String msg, LoggingLevels level, Integer vlevel) {
        String sourceFilename = "";
        int baseIdx = 4;
        StackTraceElement stackItem = null;
        while (sourceFilename.isEmpty() || "Residue.java".equals(sourceFilename)) {
            final int stackItemIndex = level == LoggingLevels.VERBOSE && vlevel > 0 ? baseIdx : (baseIdx + 1);
            final StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            if (stackTrace != null && stackTrace.length > stackItemIndex) {
                stackItem = Thread.currentThread().getStackTrace()[stackItemIndex];
                sourceFilename = stackItem == null ? "" : stackItem.getFileName();
            }
            baseIdx++;
            if (baseIdx >= 10) {
                // too much effort, leave it!
                // technically it should be resolved when baseIdx == 4 or 5 or max 6
                break;
            }
        }
		Boolean isNonUTC = false;
        Calendar c = Calendar.getInstance();
        if (Boolean.TRUE.equals(utcTime)) {
            TimeZone timeZone = c.getTimeZone();
            int offset = timeZone.getRawOffset();
						
            if (timeZone.inDaylightTime(new Date())) {
                offset = offset + timeZone.getDSTSavings();
            }
			
            int offsetHrs = offset / 1000 / 60 / 60;
            int offsetMins = offset / 1000 / 60 % 60;
			
			if (offsetHrs != 0 || offsetMins != 0) { // already utc
	            c.add(Calendar.HOUR_OF_DAY, -offsetHrs);
	            c.add(Calendar.MINUTE, -offsetMins);
				isNonUTC = true;
			}
        }
        if (timeOffset != null) {
			if (useTimeOffsetIfNotUtc && isNonUTC) {
				c.add(Calendar.SECOND, timeOffset);
			} else if (!useTimeOffsetIfNotUtc) {
            	c.add(Calendar.SECOND, timeOffset);
			}
        }
        JsonObject j = new JsonObject();
        j.addProperty("datetime", c.getTime().getTime());
        j.addProperty("logger", loggerId);
        j.addProperty("msg", msg);
        j.addProperty("file", sourceFilename);
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

    private void log(String loggerId, Object msg, LoggingLevels level) {
        log(loggerId, msg == null ? "NULL" : msg.toString(), level, 0);
    }
}
