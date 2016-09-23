package org.leishman.license;

import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.Calendar;
import java.util.Date;

public class LicenseKeySerialization
{
    private static final BASE64Encoder base64Encoder = new BASE64Encoder();
    private static final BASE64Decoder base64Decoder = new BASE64Decoder();

    public static String serializeLicenseKey( SignedLicenseKey licenseKey )
    {
        byte[] detailBytes = serializeLicenseKeyDetails( licenseKey );
        byte[] signatureBytes = licenseKey.signature();

        byte[] licenseBytes = new byte[1 + 2 + detailBytes.length + signatureBytes.length];
        ByteBuffer licenseBuffer = ByteBuffer.wrap( licenseBytes );
        licenseBuffer.put( (byte) 0 );
        licenseBuffer.putShort( (short) detailBytes.length );
        licenseBuffer.put( detailBytes );
        licenseBuffer.put( signatureBytes );

        String encodedLicense = base64Encoder.encodeBuffer( licenseBytes );

        return String.format("----BEGIN COMMERCIAL LICENSE KEY %s----\n%s----END COMMERCIAL LICENSE KEY %s----\n",
                licenseKey.getIdentifier(), encodedLicense, licenseKey.getIdentifier());
    }

    public static byte[] serializeLicenseKeyDetails( LicenseKey licenseKey )
    {
        byte[] identifierBytes;
        byte[] nameBytes;

        try
        {
            identifierBytes = licenseKey.getIdentifier().getBytes( "UTF-8" );
            nameBytes = licenseKey.getLicensedName().getBytes( "UTF-8" );
        } catch ( UnsupportedEncodingException e )
        {
            throw new IllegalStateException( "UTF-8 encoding not supported by JVM", e );
        }

        byte[] bytes = new byte[identifierBytes.length + 1 + nameBytes.length + 1 + 3 + 3];
        ByteBuffer buffer = ByteBuffer.wrap( bytes );

        buffer.put( identifierBytes );
        buffer.put( (byte) 0 );
        buffer.put( nameBytes );
        buffer.put( (byte) 0 );
        putDate( buffer, licenseKey.getIssueDate() );
        putDate( buffer, licenseKey.getExpiryDate() );

        return bytes;
    }

    public static SignedLicenseKey deserializeLicenseKey( String licenseText )
    {
        String encodedLicense = licenseText.replaceAll( "----.*----\r?\n?", "" ).replaceAll( "[ \t\n\r]", "" );

        byte[] licenseBytes;
        try
        {
            licenseBytes = base64Decoder.decodeBuffer( encodedLicense );
        } catch ( IOException e )
        {
            // Not reachable
            throw new RuntimeException( e );
        }

        ByteBuffer licenseBuffer = ByteBuffer.wrap( licenseBytes ).asReadOnlyBuffer();

        try
        {
            byte licenseVersion = licenseBuffer.get();
            if ( licenseVersion != 0 )
            {
                return null;
            }

            int detailLength = licenseBuffer.getShort();
            int signatureLength = licenseBuffer.remaining() - detailLength;
            if ( signatureLength <= 0 )
            {
                return null;
            }
            byte[] detailBytes = new byte[detailLength];
            licenseBuffer.get( detailBytes );
            byte[] signatureBytes = new byte[signatureLength];
            licenseBuffer.get( signatureBytes );

            return deserializeDetails( detailBytes, signatureBytes );
        } catch ( BufferUnderflowException e )
        {
            return null;
        }
    }

    private static SignedLicenseKey deserializeDetails( byte[] detailBytes, final byte[] signatureBytes )
    {
        final Date issueDate;
        final Date expiryDate;
        byte[] identifierBytes;
        byte[] nameBytes;

        try
        {
            ByteBuffer buffer = ByteBuffer.wrap( detailBytes ).asReadOnlyBuffer();

            identifierBytes = getUntilNull( buffer );
            if ( identifierBytes == null )
            {
                return null;
            }

            nameBytes = getUntilNull( buffer );
            if ( nameBytes == null )
            {
                return null;
            }

            issueDate = getDate( buffer );
            expiryDate = getDate( buffer );
        } catch ( BufferUnderflowException e )
        {
            return null;
        }

        final String identifier;
        final String licensedName;
        try
        {
            identifier = new String( identifierBytes, "UTF-8" );
            licensedName = new String( nameBytes, "UTF-8" );
        } catch ( UnsupportedEncodingException e )
        {
            throw new IllegalStateException( "UTF-8 encoding not supported by JVM", e );
        }

        return new SignedLicenseKey( identifier, licensedName, issueDate, expiryDate, signatureBytes );
    }

    private static void putDate( ByteBuffer buffer, Date date )
    {
        Calendar calendar = Calendar.getInstance( LicenseKey.TIME_ZONE );
        calendar.setTime( date );

        int year = calendar.get( Calendar.YEAR );
        int dayOfYear = calendar.get( Calendar.DAY_OF_YEAR );

        int packedDate = ( ( year & 0x7FFF ) << 9 ) | ( dayOfYear & 0x1FF );
        buffer.put( (byte) ( ( packedDate >> 16 ) & 0xFF ) );
        buffer.put( (byte) ( ( packedDate >> 8 ) & 0xFF ) );
        buffer.put( (byte) ( packedDate & 0xFF ) );
    }

    private static Date getDate( ByteBuffer buffer )
    {
        int packedDate = ( ( buffer.get() & 0xFF ) << 16 ) | ( ( buffer.get() & 0xFF ) << 8 ) | ( buffer.get() & 0xFF );
        int year = ( packedDate >> 9 ) & 0x7FFF;
        int dayOfYear = packedDate & 0x1FF;

        Calendar calendar = Calendar.getInstance( LicenseKey.TIME_ZONE );
        calendar.clear();
        calendar.set( Calendar.YEAR, year );
        calendar.set( Calendar.DAY_OF_YEAR, dayOfYear );
        return calendar.getTime();
    }

    private static byte[] getUntilNull( ByteBuffer buffer )
    {
        int identifierLength = findNextNull( buffer.duplicate() );
        if ( identifierLength == -1 )
        {
            return null;
        }
        byte[] bytes = new byte[identifierLength];
        buffer.get( bytes );
        byte nullByte = buffer.get();
        assert ( nullByte == 0 );
        return bytes;
    }

    private static int findNextNull( ByteBuffer buffer )
    {
        for ( int offset = 0; buffer.remaining() > 0; offset++ )
        {
            if ( buffer.get() == 0 )
            {
                return offset;
            }
        }
        return -1;
    }
}
