package com.luxe.common.auth;

import graphql.ErrorClassification;
import graphql.GraphQLError;
import graphql.language.SourceLocation;

import java.util.List;
import java.util.Map;

public class UnauthorizedException extends RuntimeException implements GraphQLError {

    public UnauthorizedException(String message) {
        super(message);
    }

    @Override
    public List<SourceLocation> getLocations() {
        return null;
    }

    @Override
    public ErrorClassification getErrorType() {
        return graphql.ErrorType.ValidationError;
    }

    @Override
    public Map<String, Object> getExtensions() {
        return Map.of("code", "UNAUTHORIZED");
    }
}
