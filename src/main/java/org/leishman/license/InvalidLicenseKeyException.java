package org.leishman.license;

public class InvalidLicenseKeyException extends Exception
{
    public InvalidLicenseKeyException( String message )
    {
        super(message);
    }
}
