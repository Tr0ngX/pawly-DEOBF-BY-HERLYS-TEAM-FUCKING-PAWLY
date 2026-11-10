package com.pawly.api;

public record Hook(
    String id,
    String owner,
    String method,
    String descriptor,
    Kind kind,
    String target,
    int ordinal,
    int argument,
    boolean optional,
    int[] parameters
) {
    public enum Kind {
        HEAD, TAIL, GUARD, RETURN_IF
    }
}
