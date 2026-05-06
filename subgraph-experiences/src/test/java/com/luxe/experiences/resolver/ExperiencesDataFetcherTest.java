package com.luxe.experiences.resolver;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ExperiencesDataFetcherTest {

    @Autowired
    DgsQueryExecutor dgs;

    @Test
    void experiences_query_filters_by_hotel_and_category() {
        List<Map<String, Object>> exps = dgs.executeAndExtractJsonPath(
                """
                { experiences(hotelId: "prop-paris-001", category: SPA_WELLNESS) {
                    id name category pricePerPerson { amount currency }
                } }
                """,
                "data.experiences");
        assertThat(exps).isNotEmpty();
        assertThat(exps).allSatisfy(e -> assertThat(e.get("category")).isEqualTo("SPA_WELLNESS"));
    }

    @Test
    void spa_treatment_menu_for_hotel_returns_treatments() {
        List<Map<String, Object>> menu = dgs.executeAndExtractJsonPath(
                "{ spaTreatmentMenu(hotelId: \"prop-paris-001\") { id name durationMinutes } }",
                "data.spaTreatmentMenu");
        assertThat(menu).isNotEmpty();
    }

    @Test
    void federation_entities_resolves_hotel_extension_with_experiences() {
        // Router-style: pass a Hotel reference, ask for the experiences extension on it
        String query = """
                query($reps: [_Any!]!) {
                  _entities(representations: $reps) {
                    ... on Hotel { experiences(category: SPA_WELLNESS) { name } }
                  }
                }
                """;
        var rep = Map.of("__typename", "Hotel", "id", "prop-paris-001");
        var result = dgs.execute(query, Map.of("reps", List.of(rep)));
        assertThat(result.getErrors()).isEmpty();
    }
}
