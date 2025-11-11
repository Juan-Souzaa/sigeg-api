package com.siseg.util;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Utilitário para operações relacionadas a requisições HTTP
 */
public final class HttpUtils {
    
    private static final String DEFAULT_IP = "127.0.0.1";
    private static final String HEADER_X_FORWARDED_FOR = "X-Forwarded-For";
    private static final String HEADER_X_REAL_IP = "X-Real-IP";
    private static final String HEADER_PROXY_CLIENT_IP = "Proxy-Client-IP";
    private static final String HEADER_WL_PROXY_CLIENT_IP = "WL-Proxy-Client-IP";
    private static final String UNKNOWN = "unknown";
    
    private HttpUtils() {
        throw new UnsupportedOperationException("Utility class");
    }
    
    /**
     * Obtém o endereço IP real do cliente a partir da requisição HTTP.
     * Considera headers de proxy e load balancer.
     * 
     * @param request Requisição HTTP
     * @return Endereço IP do cliente ou IP padrão se não encontrado
     */
    public static String getClientIpAddress(HttpServletRequest request) {
        String ip = obterIpDoHeader(request, HEADER_X_FORWARDED_FOR);
        
        if (isIpValido(ip)) {
            return extrairPrimeiroIp(ip);
        }
        
        ip = obterIpDoHeader(request, HEADER_X_REAL_IP);
        if (isIpValido(ip)) {
            return ip;
        }
        
        ip = obterIpDoHeader(request, HEADER_PROXY_CLIENT_IP);
        if (isIpValido(ip)) {
            return ip;
        }
        
        ip = obterIpDoHeader(request, HEADER_WL_PROXY_CLIENT_IP);
        if (isIpValido(ip)) {
            return ip;
        }
        
        ip = request.getRemoteAddr();
        return ip != null ? ip : DEFAULT_IP;
    }
    
    private static String obterIpDoHeader(HttpServletRequest request, String headerName) {
        return request.getHeader(headerName);
    }
    
    private static boolean isIpValido(String ip) {
        return ip != null && !ip.isEmpty() && !UNKNOWN.equalsIgnoreCase(ip);
    }
    
    private static String extrairPrimeiroIp(String ip) {
        if (ip.contains(",")) {
            return ip.split(",")[0].trim();
        }
        return ip;
    }
}

