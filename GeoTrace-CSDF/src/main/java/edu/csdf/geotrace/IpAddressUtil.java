package edu.csdf.geotrace;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.regex.Pattern;

public final class IpAddressUtil {
    private static final Pattern IPV4 = Pattern.compile(
        "^(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}$");

    private IpAddressUtil() {}

    public static String normalizeAndValidate(String input) {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("Enter an IP address.");
        }
        String ip = input.trim();
        if (ip.length() > 45 || ip.contains("%")) {
            throw new IllegalArgumentException("Invalid IP address format.");
        }
        if (IPV4.matcher(ip).matches()) return ip;

        // Only pass colon-containing input to Java's IPv6 parser, preventing hostname DNS lookups.
        if (!ip.contains(":")) throw new IllegalArgumentException("Enter a valid IPv4 or IPv6 address.");
        try {
            InetAddress parsed = InetAddress.getByName(ip);
            if (parsed.getAddress().length != 16) {
                throw new IllegalArgumentException("Enter a valid IPv4 or IPv6 address.");
            }
            return parsed.getHostAddress();
        } catch (UnknownHostException ex) {
            throw new IllegalArgumentException("Enter a valid IPv4 or IPv6 address.");
        }
    }

    public static boolean isPublicCandidate(String ip) {
        try {
            InetAddress address = InetAddress.getByName(ip);
            byte[] b = address.getAddress();

            if (address.isAnyLocalAddress() || address.isLoopbackAddress()
                || address.isLinkLocalAddress() || address.isSiteLocalAddress()
                || address.isMulticastAddress()) return false;

            // Additional IPv4 special-use ranges commonly not suitable for public geolocation.
            if (b.length == 4) {
                int a = b[0] & 255, second = b[1] & 255;
                if (a == 0 || a == 10 || a == 127 || a >= 224) return false;
                if (a == 100 && second >= 64 && second <= 127) return false;
                if (a == 169 && second == 254) return false;
                if (a == 172 && second >= 16 && second <= 31) return false;
                if (a == 192 && (second == 0 || second == 168)) return false;
                if (a == 198 && (second == 18 || second == 19)) return false;
            }
            return true;
        } catch (UnknownHostException ex) {
            return false;
        }
    }
}
