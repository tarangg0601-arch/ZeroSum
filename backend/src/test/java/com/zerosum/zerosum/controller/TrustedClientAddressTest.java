package com.zerosum.zerosum.controller;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrustedClientAddressTest {
    @Test
    void acceptsOnlySingleIpLiteralsForTheConfiguredTrustedEdgeHeader() {
        assertTrue(TrustedClientAddress.isIpLiteral("203.0.113.7"));
        assertTrue(TrustedClientAddress.isIpLiteral("2001:db8::7"));
        assertFalse(TrustedClientAddress.isIpLiteral("spoof.example"));
        assertFalse(TrustedClientAddress.isIpLiteral("203.0.113.7, 10.0.0.1"));
        assertFalse(TrustedClientAddress.isIpLiteral("999.0.0.1"));
        assertFalse(TrustedClientAddress.isIpLiteral(null));
    }
}
