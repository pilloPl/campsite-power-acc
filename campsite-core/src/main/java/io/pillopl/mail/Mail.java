package io.pillopl.mail;

/**
 * A rendered plain text mail: comma separated recipients, subject and body.
 */
public record Mail(String to, String subject, String body) {

    public String[] recipients() {
        return to.split("\\s*,\\s*");
    }
}
