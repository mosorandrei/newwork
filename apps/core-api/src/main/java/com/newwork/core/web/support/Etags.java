package com.newwork.core.web.support;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.stereotype.Component;

@Component
public class Etags {
    public String toEtag(Integer version) { return "\"" + nullToZero(version) + "\""; }

    public int requireAndParse(String ifMatch) {
        if (ifMatch == null || ifMatch.isBlank()) {
            throw new ResponseStatusException(HttpStatus.PRECONDITION_REQUIRED, "if_match_required");
        }
        try {
            return Integer.parseInt(ifMatch.replace("\"","").trim());
        } catch (NumberFormatException nfe) {
            throw new ResponseStatusException(HttpStatus.PRECONDITION_FAILED, "bad_if_match");
        }
    }

    public void assertMatches(Integer currentVersion, String ifMatch) {
        int current = nullToZero(currentVersion);
        int expected = requireAndParse(ifMatch);
        if (current != expected) {
            // 409 with currentVersion for client to retry
            throw new VersionMismatchException(current);
        }
    }

    private static int nullToZero(Integer v) { return v == null ? 0 : v; }

    public static class VersionMismatchException extends RuntimeException {
        public final int current;
        public VersionMismatchException(int current) { this.current = current; }
    }
}