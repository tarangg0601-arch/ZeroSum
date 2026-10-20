package com.zerosum.zerosum.controller;

import java.net.InetAddress;

final class TrustedClientAddress {
    private TrustedClientAddress() {}

    static boolean isIpLiteral(String value) {
        if (value == null || value.isBlank()) return false;
        String address = value.trim();
        if (address.contains(",") || !address.matches("[0-9a-fA-F:.]+")) return false;
        if (!address.contains(":") && !address.matches("(?:[0-9]{1,3}\\.){3}[0-9]{1,3}")) return false;
        try {
            return InetAddress.getByName(address).getHostAddress() != null;
        } catch (Exception invalidAddress) {
            return false;
        }
    }
}
