package peerport.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class HostingUrlConfig {
    private static String hostingUrl;

    @Value("${server.hosting-url:}")
    public void setHostingUrl(String hostingUrl) {
        HostingUrlConfig.hostingUrl = hostingUrl;
    }

    public static String getHostingUrl() {
        return hostingUrl;
    }
}
