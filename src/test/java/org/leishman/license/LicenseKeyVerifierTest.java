package org.leishman.license;

import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

public class LicenseKeyVerifierTest
{
    // key signed with license-private-test.key
    public static final String validTestLicenseKeyText =
            "----BEGIN COMMERCIAL LICENSE KEY 01-23-4567----\n" +
                    "AAAhMDEtMjMtNDU2NwBBIFRlc3QgTGljZW5zZWUAD74JD77MIEJxX/krbFFpc2ZoeTy1ygUN5rXA\n" +
                    "HtsI+F+kkUf2dTprjHS8OObD7SeFbky2ELu6So7IdkRmuCybEu1W+ZGNHJYk1Di4ZZwsP9Iaw/Cd\n" +
                    "HO9gCxu+uMSc96+Acbju7JBgGlH/5KC5bz4sPvudpQ72kiXMc9eq2/I3hZH82joeecWcaNJdlZGZ\n" +
                    "NL2kFms4oetKc2nLY2QFu96X2AIp0ZincoDqf83+d7Mj26A9LOipfM+td+oNSR5E90HMl3zNM1Pz\n" +
                    "udlCqRIBa2MiarSpTUT3WnwEIZfizTZoDHm5CyyQ9XRWwNKBIh3zl+upbYxG08tjTgpvQ7F48Z+g\n" +
                    "LMpC3y+MZA==\n" +
                    "----END COMMERCIAL LICENSE KEY 01-23-4567----\n";

    private final SignedLicenseKey validTestLicenseKey = LicenseKeySerialization.deserializeLicenseKey( validTestLicenseKeyText );

    @Test
    public void shouldVerifyValidLicense() throws Throwable
    {
        // Given
        LicenseKeyVerifier licenseKeyVerifier = new LicenseKeyVerifier(
                () -> validTestLicenseKey.getIssueDate().getTime(),
                "license-public-test.key" );

        // When
        licenseKeyVerifier.verifySignature( validTestLicenseKey );
        licenseKeyVerifier.verifyExpiry( validTestLicenseKey );

        // Then no exception is thrown
    }

    @Test
    public void shouldThrowWhenSignatureIsInvalid() throws Throwable
    {
        SimpleDateFormat dateParser = new SimpleDateFormat( "yyyy-MM-dd ZZZZZ" );
        SignedLicenseKey licenseKey = new SignedLicenseKey(
                "01-23-4567",
                "A Test Licensee",
                dateParser.parse( "2014-01-01 +0000" ),
                dateParser.parse( "2015-07-23 +0000" ),
                new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9}
        );

        LicenseKeyVerifier licenseKeyVerifier = new LicenseKeyVerifier(
                () -> validTestLicenseKey.getIssueDate().getTime(),
                "license-public-test.key" );

        // When
        try
        {
            licenseKeyVerifier.verifySignature( licenseKey );
            fail( "Expected exception not thrown" );
        } catch ( InvalidLicenseKeyException e )
        {
            // Then
            assertThat( e.getMessage(), equalTo( "Signature on license key is invalid" ) );
        }
    }

    @Test
    public void shouldThrowWhenExpired() throws Throwable
    {
        Clock clock = mock( Clock.class );

        LicenseKeyVerifier licenseKeyVerifier = new LicenseKeyVerifier( clock, "license-public-test.key" );
        when( clock.currentTimeMillis() ).thenReturn( validTestLicenseKey.getIssueDate().getTime() );

        Calendar testDate = Calendar.getInstance( LicenseKey.TIME_ZONE );
        testDate.setTime( validTestLicenseKey.getExpiryDate() );
        testDate.add( Calendar.DAY_OF_MONTH, 1 );

        when( clock.currentTimeMillis() ).thenReturn( testDate.getTimeInMillis() );

        // When
        try
        {
            licenseKeyVerifier.verifyExpiry( validTestLicenseKey );
            fail( "Expected exception not thrown" );
        } catch ( InvalidLicenseKeyException e )
        {
            // Then
            assertThat( e.getMessage(), equalTo( "Commercial License key (01-23-4567) expired on 2015-07-23" ) );
        }
    }
}
