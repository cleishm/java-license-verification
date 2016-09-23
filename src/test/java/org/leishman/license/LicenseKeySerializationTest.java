package org.leishman.license;

import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.Date;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.*;

public class LicenseKeySerializationTest
{
    @Test
    public void testLicenseKeySerialization() throws Exception
    {
        // GIVEN
        SimpleDateFormat dateParser = new SimpleDateFormat( "yyyy-MM-dd ZZZZZ" );
        Date issueDate = dateParser.parse( "2014-01-01 +0000" );
        Date expiryDate = dateParser.parse( "2015-07-23 +0000" );
        byte[] signature = new byte[]{1, 2, 3, 4, 5, 6, 7};

        SignedLicenseKey licenseKey = new SignedLicenseKey( "01-23-4567", "A Test Licensee", issueDate, expiryDate, signature );

        // WHEN
        String keyText = LicenseKeySerialization.serializeLicenseKey( licenseKey );

        // THEN
        assertThat( keyText, is(
                "----BEGIN COMMERCIAL LICENSE KEY 01-23-4567----\n" +
                "AAAhMDEtMjMtNDU2NwBBIFRlc3QgTGljZW5zZWUAD7wBD77MAQIDBAUGBw==\n" +
                "----END COMMERCIAL LICENSE KEY 01-23-4567----\n"
        ) );
    }

    @Test
    public void testLicenseKeyDeserialize() throws Exception
    {
        // GIVEN
        String keyText =
                "----BEGIN COMMERCIAL LICENSE KEY 01-23-4567----\n" +
                "AAAhMDEtMjMtNDU2NwBBIFRlc3QgTGljZW5zZWUAD7wBD77MAQIDBAUGBw==\n" +
                "----END COMMERCIAL LICENSE KEY 01-23-4567----\n";

        // WHEN
        SignedLicenseKey licenseKey = LicenseKeySerialization.deserializeLicenseKey( keyText );

        // THEN
        SimpleDateFormat dateParser = new SimpleDateFormat( "yyyy-MM-dd ZZZZZ" );
        Date issueDate = dateParser.parse( "2014-01-01 +0000" );
        Date expiryDate = dateParser.parse( "2015-07-23 +0000" );
        byte[] signature = new byte[]{1, 2, 3, 4, 5, 6, 7};

        SignedLicenseKey expectedKey = new SignedLicenseKey( "01-23-4567", "A Test Licensee", issueDate, expiryDate, signature );
        assertThat( licenseKey, equalTo( expectedKey ) );
    }

    @Test
    public void testLicenseKeyDeserializeWithoutTrailingNewline() throws Exception
    {
        // GIVEN
        String keyText =
                "----BEGIN COMMERCIAL LICENSE KEY 01-23-4567----\n" +
                "AAAhMDEtMjMtNDU2NwBBIFRlc3QgTGljZW5zZWUAD7wBD77MAQIDBAUGBw==\n" +
                "----END COMMERCIAL LICENSE KEY 01-23-4567----";

        // WHEN
        SignedLicenseKey licenseKey = LicenseKeySerialization.deserializeLicenseKey( keyText );

        // THEN
        SimpleDateFormat dateParser = new SimpleDateFormat( "yyyy-MM-dd ZZZZZ" );
        Date issueDate = dateParser.parse( "2014-01-01 +0000" );
        Date expiryDate = dateParser.parse( "2015-07-23 +0000" );
        byte[] signature = new byte[]{1, 2, 3, 4, 5, 6, 7};

        SignedLicenseKey expectedKey = new SignedLicenseKey( "01-23-4567", "A Test Licensee", issueDate, expiryDate, signature );
        assertThat( licenseKey, equalTo( expectedKey ) );
    }

        @Test
    public void testLicenseKeyDeserializeWithAdditionalSpaces() throws Exception
    {
        // GIVEN
        String keyText =
                "   ----BEGIN COMMERCIAL LICENSE KEY 01-23-4567----   \n" +
                "  AAAhMDEtMjMtNDU2NwBBIFRlc3QgTGljZW5zb3IAD7wBD77MAQIDBAUGBw==   \r\n" +
                "----END COMMERCIAL LICENSE KEY 01-23-4567----\r\n";

        // WHEN
        SignedLicenseKey licenseKey = LicenseKeySerialization.deserializeLicenseKey( keyText );

        // THEN
        SimpleDateFormat dateParser = new SimpleDateFormat( "yyyy-MM-dd ZZZZZ" );
        Date issueDate = dateParser.parse( "2014-01-01 +0000" );
        Date expiryDate = dateParser.parse( "2015-07-23 +0000" );
        byte[] signature = new byte[]{1, 2, 3, 4, 5, 6, 7};

        SignedLicenseKey expectedKey = new SignedLicenseKey( "01-23-4567", "A Test Licensor", issueDate, expiryDate, signature );
        assertThat( licenseKey, equalTo( expectedKey ) );
    }

    @Test
    public void testInvalidLicenseKeyDeserialize() throws Exception
    {
        // GIVEN
        String keyText =
                "----BEGIN COMMERCIAL LICENSE KEY 01-23-4567----\n" +
                "NOTVALIDtMjMtNDU2NwBBIFRlc3QgTGljZW5zb3IAD7wBD77MAQIDBAUGBw==\n" +
                "----END COMMERCIAL LICENSE KEY 01-23-4567----";

        // WHEN
        SignedLicenseKey licenseKey = LicenseKeySerialization.deserializeLicenseKey( keyText );

        // THEN
        assertThat( licenseKey, is( nullValue() ) );
    }
}
