package com.unishift.unishift_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@SpringBootApplication
public class Main {

    public static void main(String[] args) {
        loadDotenv();
        SpringApplication.run(Main.class, args);
    }

    public static void loadDotenv() {
        Path envPath = Path.of(".env");
        if (!Files.exists(envPath)) {
            return;
        }

        try {
            for (String rawLine : Files.readAllLines(envPath)) {
                String line = rawLine.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                int separatorIndex = line.indexOf('=');
                if (separatorIndex <= 0) {
                    continue;
                }
                String key = line.substring(0, separatorIndex).trim();
                String value = line.substring(separatorIndex + 1).trim();
                if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
                    value = value.substring(1, value.length() - 1);
                }
                if ("DATABASE_URL".equals(key) || "DB_URL".equals(key)) {
                    ParsedDatabaseUrl parsed = parseDatabaseUrl(value);
                    value = parsed.url();
                    if (System.getProperty("DATABASE_USERNAME") == null && System.getenv("DATABASE_USERNAME") == null && parsed.username() != null) {
                        System.setProperty("DATABASE_USERNAME", parsed.username());
                    }
                    if (System.getProperty("DATABASE_PASSWORD") == null && System.getenv("DATABASE_PASSWORD") == null && parsed.password() != null) {
                        System.setProperty("DATABASE_PASSWORD", parsed.password());
                    }
                }
                if (System.getProperty(key) == null && System.getenv(key) == null) {
                    System.setProperty(key, value);
                }
            }
        } catch (IOException ignored) {
        }
    }

    private static ParsedDatabaseUrl parseDatabaseUrl(String value) {
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return new ParsedDatabaseUrl(trimmed, null, null);
        }

        String url = trimmed;
        if (url.startsWith("jdbc:")) {
            url = url.substring("jdbc:".length());
        }

        String username = null;
        String password = null;

        if (url.startsWith("postgresql://") || url.startsWith("postgres://")) {
            String withoutProtocol = url.substring(url.indexOf("://") + 3);
            int atIndex = withoutProtocol.lastIndexOf('@');
            if (atIndex >= 0) {
                String credentials = withoutProtocol.substring(0, atIndex);
                String rest = withoutProtocol.substring(atIndex + 1);
                int colonIndex = credentials.indexOf(':');
                if (colonIndex >= 0) {
                    username = credentials.substring(0, colonIndex);
                    password = credentials.substring(colonIndex + 1);
                } else {
                    username = credentials;
                }
                url = "jdbc:postgresql://" + rest;
            } else {
                url = "jdbc:postgresql://" + withoutProtocol;
            }
        }

        if (!url.startsWith("jdbc:")) {
            url = "jdbc:" + url;
        }

        if (url.startsWith("jdbc:postgresql://") && url.contains("?")) {
            String query = url.substring(url.indexOf('?') + 1);
            if (!query.contains("user=") && username != null) {
                url = url.substring(0, url.indexOf('?') + 1) + "user=" + username + "&password=" + password + "&" + query;
            }
        } else if (url.startsWith("jdbc:postgresql://") && username != null) {
            url = url + "?user=" + username + "&password=" + password;
        }

        return new ParsedDatabaseUrl(url, username, password);
    }

    private record ParsedDatabaseUrl(String url, String username, String password) {}
}
