package com.example.url_shortener.service;


public class AliasUnavailableException extends RuntimeException {
    public AliasUnavailableException(String alias) {
        super("Alias is not available: " + alias);
    }
}
