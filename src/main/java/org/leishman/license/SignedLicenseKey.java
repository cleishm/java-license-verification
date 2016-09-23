package org.leishman.license;

import java.util.Arrays;
import java.util.Date;

public class SignedLicenseKey implements LicenseKey
{
    private final String identifier;
    private final String licensedName;
    private final Date issueDate;
    private final Date expiryDate;
    private final byte[] signature;

    public SignedLicenseKey( String identifier, String licensedName, Date issueDate, Date expiryDate, byte[] signature )
    {
        this.identifier = identifier;
        this.licensedName = licensedName;
        this.issueDate = issueDate;
        this.expiryDate = expiryDate;
        this.signature = signature;
    }

    public String getIdentifier()
    {
        return identifier;
    }

    public String getLicensedName()
    {
        return licensedName;
    }

    public Date getIssueDate()
    {
        return issueDate;
    }

    public Date getExpiryDate()
    {
        return expiryDate;
    }

    public byte[] signature()
    {
        return signature;
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o )
        {
            return true;
        }
        if ( !( o instanceof SignedLicenseKey ) )
        {
            return false;
        }

        SignedLicenseKey that = (SignedLicenseKey) o;

        if ( !expiryDate.equals( that.expiryDate ) )
        {
            return false;
        }
        if ( !identifier.equals( that.identifier ) )
        {
            return false;
        }
        if ( !issueDate.equals( that.issueDate ) )
        {
            return false;
        }
        if ( !licensedName.equals( that.licensedName ) )
        {
            return false;
        }
        if ( !Arrays.equals( signature, that.signature ) )
        {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode()
    {
        int result = identifier.hashCode();
        result = 31 * result + licensedName.hashCode();
        result = 31 * result + issueDate.hashCode();
        result = 31 * result + expiryDate.hashCode();
        result = 31 * result + Arrays.hashCode( signature );
        return result;
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append( getClass().getSimpleName() );
        builder.append( "(" );
        builder.append( "identifier=" );
        builder.append( identifier );
        builder.append( "," );
        builder.append( "licensedName=" );
        builder.append( licensedName );
        builder.append( "," );
        builder.append( "issueDate=" );
        builder.append( issueDate );
        builder.append( "," );
        builder.append( "expiryDate=" );
        builder.append( expiryDate );
        builder.append( ")" );
        return builder.toString();
    }
}
