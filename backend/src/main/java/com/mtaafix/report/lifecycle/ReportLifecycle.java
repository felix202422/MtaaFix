package com.mtaafix.report.lifecycle;

import com.mtaafix.report.domain.Report;
import com.mtaafix.report.domain.ReportStatusHistory;
import com.mtaafix.user.domain.User;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/**
 * Central state machine for the report lifecycle (REPORT_LIFECYCLE.md §2, T1-T17).
 * Any (action, from-status) pair not listed is forbidden and yields 409
 * INVALID_STATUS_TRANSITION via the IllegalStateException handler; forbidden
 * actor roles yield 403. T1 (creation) is handled by ReportService.create.
 */
@Component
public class ReportLifecycle {

    public record Rule(Report.Status from, Report.Status to, Set<User.Role> actors) {
    }

    private static final Map<LifecycleAction, List<Rule>> RULES = new EnumMap<>(LifecycleAction.class);

    static {
        RULES.put(LifecycleAction.CLAIM_REVIEW, List.of(
                new Rule(Report.Status.SUBMITTED, Report.Status.UNDER_REVIEW,
                        EnumSet.of(User.Role.MODERATOR, User.Role.ADMINISTRATOR)),
                new Rule(Report.Status.REOPENED, Report.Status.UNDER_REVIEW,
                        EnumSet.of(User.Role.MODERATOR, User.Role.ADMINISTRATOR))));
        RULES.put(LifecycleAction.REQUEST_INFO, List.of(
                new Rule(Report.Status.UNDER_REVIEW, Report.Status.NEEDS_INFORMATION,
                        EnumSet.of(User.Role.MODERATOR, User.Role.ADMINISTRATOR))));
        RULES.put(LifecycleAction.RESUBMIT, List.of(
                new Rule(Report.Status.NEEDS_INFORMATION, Report.Status.UNDER_REVIEW,
                        EnumSet.of(User.Role.CITIZEN))));
        RULES.put(LifecycleAction.VERIFY, List.of(
                new Rule(Report.Status.UNDER_REVIEW, Report.Status.VERIFIED,
                        EnumSet.of(User.Role.MODERATOR, User.Role.ADMINISTRATOR))));
        RULES.put(LifecycleAction.REJECT, List.of(
                new Rule(Report.Status.UNDER_REVIEW, Report.Status.REJECTED,
                        EnumSet.of(User.Role.MODERATOR, User.Role.ADMINISTRATOR))));
        RULES.put(LifecycleAction.CANCEL, List.of(
                new Rule(Report.Status.SUBMITTED, Report.Status.REJECTED,
                        EnumSet.of(User.Role.CITIZEN))));
        RULES.put(LifecycleAction.MARK_DUPLICATE, List.of(
                new Rule(Report.Status.UNDER_REVIEW, Report.Status.DUPLICATE,
                        EnumSet.of(User.Role.MODERATOR, User.Role.ADMINISTRATOR))));
        RULES.put(LifecycleAction.ASSIGN, List.of(
                new Rule(Report.Status.VERIFIED, Report.Status.ASSIGNED,
                        EnumSet.of(User.Role.ADMINISTRATOR, User.Role.ORGANIZATION_ADMIN)),
                new Rule(Report.Status.ASSIGNED, Report.Status.ASSIGNED,
                        EnumSet.of(User.Role.ADMINISTRATOR, User.Role.ORGANIZATION_ADMIN))));
        RULES.put(LifecycleAction.START_WORK, List.of(
                new Rule(Report.Status.ASSIGNED, Report.Status.IN_PROGRESS,
                        EnumSet.of(User.Role.FIELD_WORKER, User.Role.ORGANIZATION_ADMIN))));
        RULES.put(LifecycleAction.SUBMIT_COMPLETION, List.of(
                new Rule(Report.Status.IN_PROGRESS, Report.Status.RESOLUTION_REVIEW,
                        EnumSet.of(User.Role.FIELD_WORKER, User.Role.ORGANIZATION_ADMIN))));
        RULES.put(LifecycleAction.RETURN_TO_WORK, List.of(
                new Rule(Report.Status.RESOLUTION_REVIEW, Report.Status.IN_PROGRESS,
                        EnumSet.of(User.Role.MODERATOR, User.Role.ADMINISTRATOR,
                                User.Role.ORGANIZATION_ADMIN))));
        RULES.put(LifecycleAction.RESOLVE, List.of(
                new Rule(Report.Status.RESOLUTION_REVIEW, Report.Status.RESOLVED,
                        EnumSet.of(User.Role.MODERATOR, User.Role.ADMINISTRATOR))));
        RULES.put(LifecycleAction.CLOSE, List.of(
                new Rule(Report.Status.RESOLVED, Report.Status.CLOSED,
                        EnumSet.of(User.Role.ADMINISTRATOR))));
        RULES.put(LifecycleAction.CONFIRM, List.of(
                new Rule(Report.Status.RESOLVED, Report.Status.CLOSED,
                        EnumSet.of(User.Role.CITIZEN, User.Role.ADMINISTRATOR))));
        RULES.put(LifecycleAction.DISPUTE, List.of(
                new Rule(Report.Status.RESOLVED, Report.Status.IN_PROGRESS,
                        EnumSet.of(User.Role.CITIZEN, User.Role.MODERATOR,
                                User.Role.ADMINISTRATOR))));
        RULES.put(LifecycleAction.REOPEN, List.of(
                new Rule(Report.Status.CLOSED, Report.Status.REOPENED,
                        EnumSet.of(User.Role.ADMINISTRATOR))));
    }

    public List<Rule> rulesFor(LifecycleAction action) {
        return RULES.getOrDefault(action, List.of());
    }

    private Rule ruleFor(Report report, LifecycleAction action) {
        Report.Status from = report.getStatus();
        return rulesFor(action).stream()
                .filter(r -> r.from() == from)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Invalid status transition: cannot perform " + action + " while report is "
                                + from + " (report " + report.getId() + ")."));
    }

    /**
     * Actor-role gate (403) evaluated before the state gate (409) so that
     * authorization failures always win over conflict errors.
     */
    public void validate(Report report, LifecycleAction action, User actor) {
        boolean everAllowed = rulesFor(action).stream()
                .anyMatch(r -> r.actors().contains(actor.getRole()));
        if (!everAllowed) {
            throw new AccessDeniedException(
                    "Role " + actor.getRole() + " may not perform " + action + " on report "
                            + report.getId() + ".");
        }
        Rule rule = ruleFor(report, action);
        if (!rule.actors().contains(actor.getRole())) {
            throw new AccessDeniedException(
                    "Role " + actor.getRole() + " may not perform " + action + " on report "
                            + report.getId() + ".");
        }
    }

    public Report.Status apply(Report report, LifecycleAction action, User actor, String comment,
            String reasonCode) {
        validate(report, action, actor);
        Rule rule = ruleFor(report, action);
        Report.Status from = report.getStatus();
        report.setStatus(rule.to());
        report.getStatusHistory()
                .add(new ReportStatusHistory(report, from, rule.to(), actor, comment, reasonCode));
        return rule.to();
    }
}
