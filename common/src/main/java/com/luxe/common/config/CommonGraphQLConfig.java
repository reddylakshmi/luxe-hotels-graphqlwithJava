package com.luxe.common.config;

import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsRuntimeWiring;
import graphql.Scalars;
import graphql.scalars.ExtendedScalars;
import graphql.schema.GraphQLScalarType;
import graphql.schema.idl.RuntimeWiring;
import org.springframework.context.annotation.Configuration;

@Configuration
@DgsComponent
public class CommonGraphQLConfig {

    @DgsRuntimeWiring
    public RuntimeWiring.Builder addScalars(RuntimeWiring.Builder builder) {
        return builder
                .scalar(ExtendedScalars.DateTime)
                .scalar(ExtendedScalars.Date)
                .scalar(ExtendedScalars.Json)
                .scalar(ExtendedScalars.UUID)
                .scalar(ExtendedScalars.CountryCode)
                .scalar(URL_SCALAR)
                .scalar(CURRENCY_CODE_SCALAR)
                .scalar(LANGUAGE_CODE_SCALAR)
                .scalar(EMAIL_ADDRESS_SCALAR)
                .scalar(PHONE_NUMBER_SCALAR)
                .scalar(LATITUDE_SCALAR)
                .scalar(LONGITUDE_SCALAR);
    }

    // Schemas declare `scalar URL` (uppercase); ExtendedScalars.Url is named "Url".
    private static final GraphQLScalarType URL_SCALAR = ExtendedScalars
            .newAliasedScalar("URL")
            .description("URL string conforming to RFC 3986")
            .aliasedScalar(ExtendedScalars.Url)
            .build();

    // Schemas use `CurrencyCode`; library provides `Currency`.
    private static final GraphQLScalarType CURRENCY_CODE_SCALAR = ExtendedScalars
            .newAliasedScalar("CurrencyCode")
            .description("ISO 4217 currency code")
            .aliasedScalar(ExtendedScalars.Currency)
            .build();

    // Schemas use `LanguageCode`; library provides `Locale`.
    private static final GraphQLScalarType LANGUAGE_CODE_SCALAR = ExtendedScalars
            .newAliasedScalar("LanguageCode")
            .description("IETF BCP 47 language tag (e.g. 'en', 'fr-CA')")
            .aliasedScalar(ExtendedScalars.Locale)
            .build();

    // Library has no Email or PhoneNumber scalar in 21.0 — pass-through String.
    private static final GraphQLScalarType EMAIL_ADDRESS_SCALAR = GraphQLScalarType.newScalar()
            .name("EmailAddress")
            .description("RFC 5321 email address")
            .coercing(Scalars.GraphQLString.getCoercing())
            .build();

    private static final GraphQLScalarType PHONE_NUMBER_SCALAR = GraphQLScalarType.newScalar()
            .name("PhoneNumber")
            .description("E.164 phone number")
            .coercing(Scalars.GraphQLString.getCoercing())
            .build();

    private static final GraphQLScalarType LATITUDE_SCALAR = GraphQLScalarType.newScalar()
            .name("Latitude")
            .description("Latitude in decimal degrees, -90.0 to 90.0")
            .coercing(Scalars.GraphQLFloat.getCoercing())
            .build();

    private static final GraphQLScalarType LONGITUDE_SCALAR = GraphQLScalarType.newScalar()
            .name("Longitude")
            .description("Longitude in decimal degrees, -180.0 to 180.0")
            .coercing(Scalars.GraphQLFloat.getCoercing())
            .build();
}
