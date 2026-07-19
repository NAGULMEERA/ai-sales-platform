package com.aisales.gateway.support;

import com.aisales.gateway.config.GatewayRateLimitProperties;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;

/**
 * Resolves the originating client IP for gateway rate limiting.
 * {@code X-Forwarded-For} is honored only when the direct peer is a configured trusted proxy;
 * otherwise the remote address is used (prevents client spoofing of XFF).
 */
@Slf4j
@Component
public class ClientIpResolver {

    private final List<Cidr> trustedProxies;

    public ClientIpResolver(GatewayRateLimitProperties properties) {
        this.trustedProxies = parseCidrs(properties.getTrustedProxyCidrs());
    }

    public String resolve(ServerWebExchange exchange) {
        InetAddress remote = remoteAddress(exchange);
        String forwardedFor = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");

        if (remote != null && isTrustedProxy(remote) && StringUtils.hasText(forwardedFor)) {
            String client = clientFromForwardedFor(forwardedFor);
            if (StringUtils.hasText(client)) {
                return client;
            }
        }

        if (remote != null) {
            return remote.getHostAddress();
        }
        return "unknown";
    }

    /**
     * XFF is typically {@code client, proxy1, proxy2}. Walk from the right, skip trusted proxies,
     * return the first untrusted hop (the client).
     */
    String clientFromForwardedFor(String forwardedFor) {
        String[] hops = forwardedFor.split(",");
        for (int i = hops.length - 1; i >= 0; i--) {
            String hop = hops[i].trim();
            if (!StringUtils.hasText(hop)) {
                continue;
            }
            try {
                InetAddress address = InetAddress.getByName(hop);
                if (!isTrustedProxy(address)) {
                    return address.getHostAddress();
                }
            } catch (UnknownHostException ex) {
                log.debug("Ignoring invalid X-Forwarded-For hop: {}", hop);
            }
        }
        return hops[0].trim();
    }

    private boolean isTrustedProxy(InetAddress address) {
        for (Cidr cidr : trustedProxies) {
            if (cidr.contains(address)) {
                return true;
            }
        }
        return false;
    }

    private static InetAddress remoteAddress(ServerWebExchange exchange) {
        if (exchange.getRequest().getRemoteAddress() == null
                || exchange.getRequest().getRemoteAddress().getAddress() == null) {
            return null;
        }
        return exchange.getRequest().getRemoteAddress().getAddress();
    }

    private static List<Cidr> parseCidrs(List<String> raw) {
        List<Cidr> parsed = new ArrayList<>();
        if (raw == null) {
            return parsed;
        }
        for (String entry : raw) {
            if (!StringUtils.hasText(entry)) {
                continue;
            }
            try {
                parsed.add(Cidr.parse(entry.trim()));
            } catch (IllegalArgumentException | UnknownHostException ex) {
                throw new IllegalStateException("Invalid trusted-proxy CIDR: " + entry, ex);
            }
        }
        return List.copyOf(parsed);
    }

    private record Cidr(byte[] network, int prefixLength) {

        static Cidr parse(String cidr) throws UnknownHostException {
            String networkPart;
            int prefix;
            if (cidr.contains("/")) {
                String[] parts = cidr.split("/", 2);
                networkPart = parts[0];
                prefix = Integer.parseInt(parts[1]);
            } else {
                networkPart = cidr;
                InetAddress addr = InetAddress.getByName(networkPart);
                prefix = addr.getAddress().length * 8;
            }
            byte[] network = InetAddress.getByName(networkPart).getAddress();
            int max = network.length * 8;
            if (prefix < 0 || prefix > max) {
                throw new IllegalArgumentException("Invalid prefix length: " + prefix);
            }
            return new Cidr(network, prefix);
        }

        boolean contains(InetAddress address) {
            byte[] candidate = address.getAddress();
            if (candidate.length != network.length) {
                return false;
            }
            int fullBytes = prefixLength / 8;
            int remainingBits = prefixLength % 8;
            for (int i = 0; i < fullBytes; i++) {
                if (network[i] != candidate[i]) {
                    return false;
                }
            }
            if (remainingBits == 0) {
                return true;
            }
            int mask = 0xFF << (8 - remainingBits);
            return (network[fullBytes] & mask) == (candidate[fullBytes] & mask);
        }
    }
}
