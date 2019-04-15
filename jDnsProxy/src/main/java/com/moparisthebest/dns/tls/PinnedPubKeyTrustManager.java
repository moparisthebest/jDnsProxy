package com.moparisthebest.dns.tls;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.KeyManagementException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Base64;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Given a MessageDigest algorithm name and a list of public key hashes, implements secure TLS certificate verification
 * via public key pinning.
 */
public class PinnedPubKeyTrustManager implements X509TrustManager {

    /**
     * Create TLS SSLContext for a single TrustManager
     *
     * @param trustManager TrustManager instance
     * @return SSLContext
     */
    public static SSLContext sslContext(final TrustManager trustManager) {
        try {
            final SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{trustManager}, new SecureRandom());
            return sslContext;
        } catch (KeyManagementException | NoSuchAlgorithmException e) {
            throw new RuntimeException("TLS not supported or ?", e);
        }
    }

    /**
     * Create pinned pubkey SSLContext
     *
     * @param sha256Pins HPKP RFC7469 style base64'd sha256 pins
     * @return SSLContext that only accepts servers with these pubkeys
     */
    public static SSLContext pinSha256SSLContext(final Stream<String> sha256Pins) {
        return sslContext(pinSha256TrustManager(sha256Pins));
    }

    /**
     * Create pinned pubkey SSLContext
     *
     * @param sha256Pins HPKP RFC7469 style base64'd sha256 pins
     * @return SSLContext that only accepts servers with these pubkeys
     */
    public static SSLContext pinSha256SSLContext(final String... sha256Pins) {
        return pinSha256SSLContext(Arrays.stream(sha256Pins));
    }

    /**
     * Create pinned pubkey TrustManager
     *
     * @param sha256Pins HPKP RFC7469 style base64'd sha256 pins
     * @return TrustManager that only accepts servers with these pubkeys
     */
    public static TrustManager pinSha256TrustManager(final Stream<String> sha256Pins) {
        return new PinnedPubKeyTrustManager("SHA-256", sha256Pins
                .map(sha256Pin -> Base64.getDecoder().decode(sha256Pin))
                .collect(Collectors.toList()));
    }

    private static final X509Certificate[] acceptedIssuers = new X509Certificate[0];

    private final String algorithm;
    private final Iterable<byte[]> pinnedPubKeys;

    /**
     * PinnedPubKeyTrustManager constructor
     *
     * @param algorithm Algorithm to create instance of MessageDigest to hash server public key with before comparing to pinnedPubKeys
     * @param pinnedPubKeys Hashes of public keys to trust, if any match the leaf certificate the connection is trusted
     */
    public PinnedPubKeyTrustManager(final String algorithm, final Iterable<byte[]> pinnedPubKeys) {
        this.algorithm = algorithm;
        this.pinnedPubKeys = pinnedPubKeys;
    }

    @Override
    public void checkClientTrusted(final X509Certificate[] chain, final String authType) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void checkServerTrusted(final X509Certificate[] chain, final String authType) throws CertificateException {
        if(chain.length < 1)
            throw new CertificateException("no certificates provided");
        // first cert is the leaf one????
        final byte[] hashedServerPubKey = getMessageDigest().digest(chain[0].getPublicKey().getEncoded());
        for(final byte[] pinnedPubKey : pinnedPubKeys) {
            if (Arrays.equals(hashedServerPubKey, pinnedPubKey)) {
                return; // success!
            }
        }
        throw new CertificateException("pinnedpubkey authentication failed, server pin (" + algorithm + "): " +
                Base64.getEncoder().encodeToString(hashedServerPubKey));
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return acceptedIssuers;
    }

    protected MessageDigest getMessageDigest() throws CertificateException {
        try {
            return MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new CertificateException(algorithm + " not supported", e);
        }
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public Iterable<byte[]> getPinnedPubKeys() {
        return pinnedPubKeys;
    }
}
