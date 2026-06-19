package com.example.teamflow.infra.security;

import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TokenBlacklist {

    private final ConcurrentHashMap<String, Long> store = new ConcurrentHashMap<>();

    public void add(String jti, Date expiration) {
        store.put(jti, expiration.getTime());
    }

    public boolean contains(String jti) {
        Long expiry = store.get(jti);
        if (expiry == null) return false;
        if (System.currentTimeMillis() > expiry) {
            store.remove(jti);
            return false;
        }
        return true;
    }
}
