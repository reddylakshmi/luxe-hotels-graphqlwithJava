package com.luxe.meetings.resolver;

import com.luxe.common.auth.AuthContext;
import com.luxe.common.auth.AuthContextResolver;
import com.luxe.common.auth.AuthRole;
import com.netflix.graphql.dgs.DgsQueryExecutor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Locks the alignment between {@code MeetingsMockDataSource} demo
 * RFPs for guest-001 (Sophia Chen, Pinnacle Ventures) and the
 * resolver's email-derivation rule {@code guestId + "@example.com"}.
 *
 * Without these RFPs the {@code /account/events} surface on the web
 * app would render an empty state on first sign-in for the demo
 * guest — the test guarantees first-impression freshness.
 */
@SpringBootTest
@Import(MeetingsGuest001RfpsTest.AuthOverrideConfig.class)
class MeetingsGuest001RfpsTest {

    @TestConfiguration
    static class AuthOverrideConfig {
        @Bean @Primary
        AuthContextResolver mockAuth() {
            // Resolver does {guestId}@example.com → guest-001@example.com
            return dfe -> AuthContext.of("guest-001", null, AuthRole.GUEST);
        }
    }

    @Autowired
    DgsQueryExecutor dgs;

    @Test
    void my_rfps_lists_seeded_rfps_for_guest_001() {
        Integer total = dgs.executeAndExtractJsonPath(
                "{ myRFPs(first: 10) { totalCount edges { node { id rfpNumber status } } } }",
                "data.myRFPs.totalCount");
        assertThat(total)
                .as("guest-001 should see at least the two seeded demo RFPs")
                .isGreaterThanOrEqualTo(2);
    }

    @Test
    void seeded_rfps_carry_expected_status_mix() {
        List<Map<String, Object>> nodes = dgs.executeAndExtractJsonPath(
                "{ myRFPs(first: 10) { edges { node { rfpNumber status eventName } } } }",
                "data.myRFPs.edges[*].node");
        assertThat(nodes).extracting(n -> (String) n.get("status"))
                .as("at least one PROPOSAL_SENT (timeline demo) and one SUBMITTED (early-stage demo)")
                .contains("PROPOSAL_SENT", "SUBMITTED");
    }

    @Test
    void proposal_sent_rfp_carries_two_responses() {
        // The Pinnacle Ventures Annual Partner Offsite RFP shows the
        // multi-hotel proposal experience — Paris + London respond.
        Integer count = dgs.executeAndExtractJsonPath(
                "{ rfp(id: \"rfp-004\") { responses { id status hotelId } } }",
                "data.rfp.responses.length()");
        assertThat(count).isEqualTo(2);
    }
}
