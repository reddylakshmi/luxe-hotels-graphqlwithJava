package com.luxe.common.pagination;

public record Edge<T>(T node, String cursor) {}
