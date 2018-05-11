package com.cosoros.www.datastructure;

public class Version {
    protected int _major;
    protected int _minor;
    protected int _revision;

    public enum COMPARE {
        HIGHER,
        SAME,
        LOWER,
    }

    public Version(int major, int minor, int revision)
    {
        _major = major;
        _minor = minor;
        _revision = revision;
    }

    public String toString()
    {
        return Integer.toString(_major) + "." +
                Integer.toString(_major) + "." +
                Integer.toString(_revision);
    }

    public COMPARE compare(Version ver)
    {
        return (_major > ver._major) ? COMPARE.HIGHER :
                (_major < ver._major) ? COMPARE.LOWER :
                (_minor > ver._minor) ? COMPARE.HIGHER :
                (_minor < ver._minor) ? COMPARE.LOWER :
                (_revision > ver._revision) ? COMPARE.HIGHER :
                (_revision < ver._revision) ? COMPARE.LOWER : COMPARE.SAME;
    }

    public boolean isValidVersion()
    {
        return (_major != 0 || _minor != 0 || _revision != 0);
    }
}
