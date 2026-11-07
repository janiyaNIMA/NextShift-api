package com.unishift.unishift_backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class EndpointPrinter {

    private static final Logger log = LoggerFactory.getLogger(EndpointPrinter.class);

    private final ApplicationContext applicationContext;

    public EndpointPrinter(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        printEndpoints();
        printServerDetails();
    }

    public void printEndpoints() {
        Map<String, RequestMappingHandlerMapping> handlerMappings = applicationContext.getBeansOfType(RequestMappingHandlerMapping.class);
        List<EndpointEntry> endpoints = new ArrayList<>();

        for (RequestMappingHandlerMapping mapping : handlerMappings.values()) {
            for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : mapping.getHandlerMethods().entrySet()) {
                RequestMappingInfo info = entry.getKey();
                HandlerMethod handlerMethod = entry.getValue();

                Set<String> patterns = info.getPatternValues();
                if (patterns.isEmpty() && info.getPathPatternsCondition() != null) {
                    patterns = info.getPathPatternsCondition().getPatternValues();
                }

                Set<RequestMethod> methods = info.getMethodsCondition().getMethods();
                String methodStr = methods.isEmpty()
                        ? "ALL"
                        : methods.stream().map(Enum::name).sorted().collect(Collectors.joining(", "));

                String handlerStr = handlerMethod.getBeanType().getSimpleName() + "#" + handlerMethod.getMethod().getName();

                for (String path : patterns) {
                    endpoints.add(new EndpointEntry(methodStr, path, handlerStr));
                }
            }
        }

        if (endpoints.isEmpty()) {
            return;
        }

        endpoints.sort(Comparator.comparing(EndpointEntry::path).thenComparing(EndpointEntry::method));

        int maxMethodLen = Math.max("METHOD".length(), endpoints.stream().mapToInt(e -> e.method().length()).max().orElse(6));
        int maxPathLen = Math.max("PATH".length(), endpoints.stream().mapToInt(e -> e.path().length()).max().orElse(30));
        int maxHandlerLen = Math.max("HANDLER".length(), endpoints.stream().mapToInt(e -> e.handler().length()).max().orElse(25));

        int totalWidth = maxMethodLen + maxPathLen + maxHandlerLen + 6;
        totalWidth = Math.max(totalWidth, 80);

        String line = "=".repeat(totalWidth);
        String subLine = "-".repeat(totalWidth);

        String format = String.format(" %%-%ds | %%-%ds | %%-%ds%%n", maxMethodLen, maxPathLen, maxHandlerLen);

        StringBuilder sb = new StringBuilder();
        sb.append(System.lineSeparator());
        sb.append(line).append(System.lineSeparator());
        sb.append(centerText("REGISTERED BACKEND API ENDPOINTS", totalWidth)).append(System.lineSeparator());
        sb.append(line).append(System.lineSeparator());
        sb.append(String.format(format, "METHOD", "PATH", "HANDLER"));
        sb.append(subLine).append(System.lineSeparator());

        for (EndpointEntry endpoint : endpoints) {
            sb.append(String.format(format, endpoint.method(), endpoint.path(), endpoint.handler()));
        }

        sb.append(line).append(System.lineSeparator());
        sb.append(String.format(" Total Registered Endpoints: %d%n", endpoints.size()));
        sb.append(line).append(System.lineSeparator());

        System.out.println(sb);
        log.info("Registered API endpoints printed to console (total: {}).", endpoints.size());
    }

    public void printServerDetails() {
        int port = resolveServerPort();
        String protocol = resolveProtocol();
        String hostAddress = resolveHostAddress();
        String contextPath = resolveContextPath();
        String apiPrefix = resolveApiPrefix();
        String appName = resolveApplicationName();
        String environment = resolveEnvironmentName();
        String activeProfiles = resolveActiveProfiles();

        String localBackendUrl = String.format("%s://localhost:%d%s", protocol, port, contextPath);
        String networkBackendUrl = String.format("%s://%s:%d%s", protocol, hostAddress, port, contextPath);
        String healthUrl = localBackendUrl + (localBackendUrl.endsWith("/") ? "health" : "/health");
        String apiUrl = localBackendUrl + (localBackendUrl.endsWith("/") && apiPrefix.startsWith("/") ? apiPrefix.substring(1) : apiPrefix);

        int totalWidth = 80;
        String line = "=".repeat(totalWidth);

        StringBuilder sb = new StringBuilder();
        sb.append(System.lineSeparator());
        sb.append(line).append(System.lineSeparator());
        sb.append(centerText("BACKEND SERVER DETAILS & ACCESS LINKS", totalWidth)).append(System.lineSeparator());
        sb.append(line).append(System.lineSeparator());
        sb.append(String.format(" %-24s: %s%n", "Application Name", appName));
        sb.append(String.format(" %-24s: %s%n", "Environment", environment));
        sb.append(String.format(" %-24s: %s%n", "Active Profiles", "[" + activeProfiles + "]"));
        sb.append(String.format(" %-24s: %d%n", "Server Port", port));
        if (!contextPath.isEmpty()) {
            sb.append(String.format(" %-24s: %s%n", "Context Path", contextPath));
        }
        sb.append("-".repeat(totalWidth)).append(System.lineSeparator());
        sb.append(String.format(" %-24s: %s%n", "Backend Link (Local)", localBackendUrl));
        sb.append(String.format(" %-24s: %s%n", "Backend Link (Network)", networkBackendUrl));
        sb.append(String.format(" %-24s: %s%n", "Health Check Link", healthUrl));
        sb.append(String.format(" %-24s: %s%n", "API Base Link", apiUrl));
        sb.append(line).append(System.lineSeparator());

        System.out.println(sb);
        log.info("Backend server ready on port {} | Local: {} | Network: {}", port, localBackendUrl, networkBackendUrl);
    }

    private int resolveServerPort() {
        if (applicationContext instanceof WebServerApplicationContext webServerAppCtx) {
            try {
                if (webServerAppCtx.getWebServer() != null) {
                    int port = webServerAppCtx.getWebServer().getPort();
                    if (port > 0) {
                        return port;
                    }
                }
            } catch (Exception ignored) {
            }
        }

        String localServerPort = applicationContext.getEnvironment().getProperty("local.server.port");
        if (localServerPort != null && !localServerPort.isBlank()) {
            try {
                int port = Integer.parseInt(localServerPort.trim());
                if (port > 0) {
                    return port;
                }
            } catch (NumberFormatException ignored) {
            }
        }

        String serverPort = applicationContext.getEnvironment().getProperty("server.port");
        if (serverPort != null && !serverPort.isBlank()) {
            try {
                int port = Integer.parseInt(serverPort.trim());
                if (port > 0) {
                    return port;
                }
            } catch (NumberFormatException ignored) {
            }
        }

        return 8080;
    }

    private String resolveHostAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces != null && interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (iface.isLoopback() || !iface.isUp()) {
                    continue;
                }
                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (!addr.isLoopbackAddress() && addr instanceof Inet4Address) {
                        return addr.getHostAddress();
                    }
                }
            }
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            log.debug("Unable to determine network IP address: {}", e.getMessage());
        }
        return "127.0.0.1";
    }

    private String resolveProtocol() {
        Environment env = applicationContext.getEnvironment();
        boolean sslEnabled = Boolean.parseBoolean(env.getProperty("server.ssl.enabled", "false"))
                || env.getProperty("server.ssl.key-store") != null;
        return sslEnabled ? "https" : "http";
    }

    private String resolveContextPath() {
        String contextPath = applicationContext.getEnvironment().getProperty("server.servlet.context-path", "");
        if (contextPath == null || contextPath.isBlank() || contextPath.trim().equals("/")) {
            return "";
        }
        contextPath = contextPath.trim();
        if (!contextPath.startsWith("/")) {
            contextPath = "/" + contextPath;
        }
        if (contextPath.endsWith("/")) {
            contextPath = contextPath.substring(0, contextPath.length() - 1);
        }
        return contextPath;
    }

    private String resolveApiPrefix() {
        String apiPrefix = applicationContext.getEnvironment().getProperty("API_V1_STR", "/api/v1");
        if (apiPrefix == null || apiPrefix.isBlank()) {
            return "/api/v1";
        }
        apiPrefix = apiPrefix.trim();
        if (!apiPrefix.startsWith("/")) {
            apiPrefix = "/" + apiPrefix;
        }
        if (apiPrefix.length() > 1 && apiPrefix.endsWith("/")) {
            apiPrefix = apiPrefix.substring(0, apiPrefix.length() - 1);
        }
        return apiPrefix;
    }

    private String resolveApplicationName() {
        Environment env = applicationContext.getEnvironment();
        String appName = env.getProperty("PROJECT_NAME");
        if (appName != null && !appName.isBlank()) {
            return appName.trim();
        }
        appName = env.getProperty("spring.application.name");
        if (appName != null && !appName.isBlank()) {
            return appName.trim();
        }
        return "NextShift Backend";
    }

    private String resolveEnvironmentName() {
        Environment env = applicationContext.getEnvironment();
        String environment = env.getProperty("ENVIRONMENT");
        if (environment != null && !environment.isBlank()) {
            return environment.trim();
        }
        return "development";
    }

    private String resolveActiveProfiles() {
        String[] profiles = applicationContext.getEnvironment().getActiveProfiles();
        if (profiles.length == 0) {
            return "default";
        }
        return String.join(", ", profiles);
    }

    private String centerText(String text, int width) {
        if (text.length() >= width) {
            return text;
        }
        int padding = (width - text.length()) / 2;
        return " ".repeat(padding) + text;
    }

    private record EndpointEntry(String method, String path, String handler) {}
}
