package com.luxe.loyalty.schema.types;

import com.luxe.common.pagination.HasId;
import java.time.LocalDate;

public class Challenge implements HasId {
    private final String id, name, description;
    private final String status;
    private final LocalDate startDate, endDate;
    private final ChallengeReward reward;
    private ChallengeProgress progress;
    private boolean registered;

    public Challenge(String id, String name, String description, String status,
                     LocalDate startDate, LocalDate endDate, ChallengeReward reward,
                     ChallengeProgress progress, boolean registered) {
        this.id = id; this.name = name; this.description = description;
        this.status = status; this.startDate = startDate; this.endDate = endDate;
        this.reward = reward; this.progress = progress; this.registered = registered;
    }

    @Override public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getStatus() { return status; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public ChallengeReward getReward() { return reward; }
    public ChallengeProgress getProgress() { return progress; }
    public boolean isRegistered() { return registered; }

    public void setProgress(ChallengeProgress p) { this.progress = p; }
    public void setRegistered(boolean r) { this.registered = r; }

    public record ChallengeReward(Integer bonusPoints, Integer bonusNights,
                                   String certificateType, String description) {}

    public record ChallengeProgress(int current, int goal, double pct) {
        public static ChallengeProgress of(int current, int goal) {
            double p = goal == 0 ? 0.0 : Math.min(100.0, (current * 100.0) / goal);
            return new ChallengeProgress(current, goal, p);
        }
    }
}
