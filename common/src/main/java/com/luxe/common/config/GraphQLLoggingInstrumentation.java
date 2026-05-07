package com.luxe.common.config;

import graphql.ExecutionResult;
import graphql.execution.instrumentation.InstrumentationContext;
import graphql.execution.instrumentation.InstrumentationState;
import graphql.execution.instrumentation.SimpleInstrumentationContext;
import graphql.execution.instrumentation.SimplePerformantInstrumentation;
import graphql.execution.instrumentation.parameters.InstrumentationExecutionParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Logs every incoming GraphQL operation (name + variables + first 200 chars
 * of the query) and the response time in millis. Picked up automatically by
 * any Spring Boot subgraph that scans {@code com.luxe.common} (all of them).
 *
 * <p>Output goes to {@code com.luxe.common.GraphQL} at INFO level so it shows
 * up by default; toggle with {@code logging.level.com.luxe.common.GraphQL=WARN}
 * to silence in production.
 */
@Component
public class GraphQLLoggingInstrumentation extends SimplePerformantInstrumentation {

    private static final Logger LOG = LoggerFactory.getLogger("com.luxe.common.GraphQL");

    @Override
    public InstrumentationContext<ExecutionResult> beginExecution(
            InstrumentationExecutionParameters parameters,
            InstrumentationState state) {
        long start = System.currentTimeMillis();
        String op = parameters.getOperation() != null ? parameters.getOperation() : "(anonymous)";
        String query = parameters.getQuery();
        String preview = query == null ? "" : query.replaceAll("\\s+", " ").trim();
        if (preview.length() > 200) preview = preview.substring(0, 200) + "…";
        LOG.info(">> {} variables={} query=\"{}\"", op, parameters.getVariables(), preview);

        return SimpleInstrumentationContext.whenCompleted((result, t) -> {
            long elapsed = System.currentTimeMillis() - start;
            if (t != null) {
                LOG.error("<< {} FAILED in {}ms — {}", op, elapsed, t.getMessage());
            } else {
                int errorCount = result.getErrors() == null ? 0 : result.getErrors().size();
                if (errorCount > 0) {
                    LOG.warn("<< {} done in {}ms with {} error(s): {}", op, elapsed, errorCount, result.getErrors());
                } else {
                    LOG.info("<< {} done in {}ms", op, elapsed);
                }
            }
        });
    }
}
