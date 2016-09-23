package org.leishman.license;

import java.util.Date;
import java.util.TimeZone;

public interface LicenseKey
{
    TimeZone TIME_ZONE = TimeZone.getTimeZone( "UTC" );

    String getIdentifier();

    String getLicensedName();

    Date getIssueDate();

    Date getExpiryDate();
}
