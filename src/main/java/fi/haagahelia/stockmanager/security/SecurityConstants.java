package fi.haagahelia.stockmanager.security;

import java.util.Calendar;

public class SecurityConstants {
    public static final int jwtExpiration = 2;
    public static final int jwtExpirationUnit = Calendar.HOUR;
    public static final String jwtSecret = "SECRET";
}
