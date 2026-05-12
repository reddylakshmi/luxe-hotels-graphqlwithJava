package com.luxe.common.security;

import graphql.ErrorClassification;
import graphql.ErrorType;
import graphql.GraphQLError;
import graphql.execution.AbortExecutionException;
import graphql.execution.instrumentation.InstrumentationContext;
import graphql.execution.instrumentation.InstrumentationState;
import graphql.execution.instrumentation.SimpleInstrumentationContext;
import graphql.execution.instrumentation.SimplePerformantInstrumentation;
import graphql.execution.instrumentation.parameters.InstrumentationValidationParameters;
import graphql.language.SourceLocation;
import graphql.validation.ValidationError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * DGS-discoverable {@link graphql.execution.instrumentation.Instrumentation}
 * that rejects queries exceeding the configured complexity score or
 * nesting depth before any resolver runs. Scoring is delegated to the
 * pure {@link QueryComplexity} helper, so the threshold logic is
 * unit-testable in isolation.
 *
 * <p>Thresholds (per-subgraph, env-tunable):
 * <pre>
 * luxe.security.max-complexity: 1000
 * luxe.security.max-depth: 10
 * </pre>
 *
 * <p>The instrumentation hooks the validation phase. If parsing
 * already failed, the document is null and we no-op — graphql-java's
 * own validation errors carry the failure forward.
 *
 * <p>Rejections surface as {@link GraphQLError} entries with code
 * {@code QUERY_TOO_COMPLEX} or {@code QUERY_TOO_DEEP} in extensions —
 * dashboards can alert on those without parsing the message string.
 */
@Component
public class QueryGuardrails extends SimplePerformantInstrumentation {

    private static final Logger log = LoggerFactory.getLogger(QueryGuardrails.class);

    private final int maxComplexity;
    private final int maxDepth;

    public QueryGuardrails(
            // Production budget — the original 1000 baseline was too
            // tight for real product pages (brand portfolios + search
            // results legitimately score 1100–1500 with reasonable
            // field selections). 2000 still catches a query bomb (a
            // 100-rep _entities or `first: 200` trips it) while letting
            // the catalogue pages render unmolested. Tune per-env via
            // luxe.security.max-complexity if you change the workload.
            @Value("${luxe.security.max-complexity:2000}") int maxComplexity,
            @Value("${luxe.security.max-depth:10}") int maxDepth) {
        this.maxComplexity = maxComplexity;
        this.maxDepth = maxDepth;
    }

    @Override
    public InstrumentationContext<List<ValidationError>> beginValidation(
            InstrumentationValidationParameters parameters,
            InstrumentationState state) {
        if (parameters.getDocument() == null) {
            return SimpleInstrumentationContext.noOp();
        }
        QueryComplexity.Cost cost = QueryComplexity.calculate(parameters.getDocument());
        if (cost.score() > maxComplexity) {
            log.warn("rejecting query: complexity {} > limit {}", cost.score(), maxComplexity);
            throw new AbortExecutionException(List.<GraphQLError>of(new TooComplexError(
                    "Query complexity " + cost.score() + " exceeds limit " + maxComplexity,
                    "QUERY_TOO_COMPLEX", cost.score(), maxComplexity)));
        }
        if (cost.depth() > maxDepth) {
            log.warn("rejecting query: depth {} > limit {}", cost.depth(), maxDepth);
            throw new AbortExecutionException(List.<GraphQLError>of(new TooComplexError(
                    "Query depth " + cost.depth() + " exceeds limit " + maxDepth,
                    "QUERY_TOO_DEEP", cost.depth(), maxDepth)));
        }
        return SimpleInstrumentationContext.noOp();
    }

    public int maxComplexity() { return maxComplexity; }
    public int maxDepth() { return maxDepth; }

    /** Custom GraphQLError so the extensions.code is structured. */
    static final class TooComplexError extends RuntimeException implements GraphQLError {
        private final String code;
        private final int actual;
        private final int limit;

        TooComplexError(String message, String code, int actual, int limit) {
            super(message);
            this.code = code;
            this.actual = actual;
            this.limit = limit;
        }

        @Override public List<SourceLocation> getLocations() { return null; }
        @Override public ErrorClassification getErrorType() { return ErrorType.ValidationError; }
        @Override public Map<String, Object> getExtensions() {
            return Map.of("code", code, "actual", actual, "limit", limit);
        }
    }
}
