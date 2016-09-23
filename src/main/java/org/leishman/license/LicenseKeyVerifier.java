package org.leishman.license;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

public class LicenseKeyVerifier
{
    /* Key must be in the correct format. Key generation with openssl can be achieved as follows:
     * $ openssl genrsa -out license-private.pem 2048
     * $ Generating RSA private key, 2048 bit long modulus
     * ................+++
     * .....+++
     * e is 65537 (0x10001)
     * $ openssl rsa -in license-private.pem -inform pem -out license-public.key -outform der -pubout
     * writing RSA key
     * $ openssl pkcs8 -topk8 -inform pem -outform der -in license-private.pem -out license-private.key -nocrypt
     */
    private static final String PUBLIC_KEY_RESOURCE_NAME = "license-public.key";
    public static final TimeZone LAST_TIMEZONE = TimeZone.getTimeZone( "GMT-12" );

    private final Clock clock;
    private final PublicKey publicKey;

    public LicenseKeyVerifier()
    {
        this(PUBLIC_KEY_RESOURCE_NAME);
    }

    public LicenseKeyVerifier( String publicKeyFileName )
    {
        this( () -> System.currentTimeMillis(), publicKeyFileName );
    }

    public LicenseKeyVerifier( Clock clock )
    {
        this( clock, PUBLIC_KEY_RESOURCE_NAME );
    }

    public LicenseKeyVerifier( Clock clock, String publicKeyFileName )
    {
        this.clock = clock;
        try
        {
            this.publicKey = loadPublicKey( publicKeyFileName );
        } catch ( IOException | InvalidKeySpecException e )
        {
            throw new RuntimeException( e );
        }
    }

    public void verifySignature( SignedLicenseKey signedLicenseKey ) throws InvalidLicenseKeyException
    {
        if ( !isSignatureValid( signedLicenseKey ) )
        {
            throw new InvalidLicenseKeyException( "Signature on license key is invalid" );
        }
    }

    public boolean isSignatureValid( SignedLicenseKey signedLicenseKey )
    {
        byte[] keyDetailsBytes = LicenseKeySerialization.serializeLicenseKeyDetails( signedLicenseKey );
        byte[] signatureBytes = signedLicenseKey.signature();

        Signature signature = initSignature();
        try
        {
            signature.update( keyDetailsBytes, 0, keyDetailsBytes.length );
            return signature.verify( signatureBytes );
        } catch ( SignatureException e )
        {
            return false;
        }
    }

    public void verifyExpiry( LicenseKey licenseKey ) throws InvalidLicenseKeyException
    {
        if ( hasExpired( licenseKey ) )
        {
            SimpleDateFormat dateFormat = new SimpleDateFormat( "yyyy-MM-dd" );
            dateFormat.setTimeZone( LicenseKey.TIME_ZONE );
            String message = String.format( "Commercial License key (%s) expired on %s",
                    licenseKey.getIdentifier(),
                    dateFormat.format( licenseKey.getExpiryDate() ) );
            throw new InvalidLicenseKeyException( message );
        }
    }

    public boolean hasExpired( LicenseKey licenseKey )
    {
        Calendar now = Calendar.getInstance( LicenseKey.TIME_ZONE );
        now.setTimeInMillis( clock.currentTimeMillis() );

        Calendar expiryDate = Calendar.getInstance( LicenseKey.TIME_ZONE );
        expiryDate.setTime( licenseKey.getExpiryDate() );

        Calendar latestExpiryDate = Calendar.getInstance( LAST_TIMEZONE );
        latestExpiryDate.clear();
        latestExpiryDate.set( Calendar.YEAR, expiryDate.get( Calendar.YEAR ) );
        latestExpiryDate.set( Calendar.DAY_OF_YEAR, expiryDate.get( Calendar.DAY_OF_YEAR ) );

        return latestExpiryDate.before( now );
    }

    private PublicKey loadPublicKey( String keyResourceName ) throws IOException, InvalidKeySpecException
    {
        InputStream stream = ClassLoader.getSystemResourceAsStream( keyResourceName );
        if ( stream == null )
        {
            throw new IllegalArgumentException( "Key file resource not found: " + keyResourceName );
        }

        byte[] publicKey = toByteArray( stream );

        KeyFactory keyFactory;
        try
        {
            keyFactory = KeyFactory.getInstance( "RSA" );
        } catch ( NoSuchAlgorithmException e )
        {
            // not reachable - JVM does know RSA
            throw new RuntimeException( e );
        }
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec( publicKey );
        return keyFactory.generatePublic( keySpec );
    }

    private static byte[] toByteArray( InputStream source ) throws IOException
    {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[1024];

        while ( ( nRead = source.read( data, 0, data.length ) ) != -1 )
        {
            buffer.write( data, 0, nRead );
        }

        buffer.flush();
        return buffer.toByteArray();
    }

    private Signature initSignature()
    {
        try
        {
            Signature signature = Signature.getInstance( "SHA1withRSA" );
            signature.initVerify( publicKey );
            return signature;
        } catch ( NoSuchAlgorithmException e )
        {
            throw new IllegalStateException( "RSA Algorithm not supported by JVM", e );
        } catch ( InvalidKeyException e )
        {
            throw new IllegalStateException( "Invalid public key", e );
        }
    }
}
