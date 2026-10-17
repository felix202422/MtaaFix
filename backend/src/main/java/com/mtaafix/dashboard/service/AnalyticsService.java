package com.mtaafix.dashboard.service;

import com.mtaafix.common.geo.GeoBounds;
import com.mtaafix.dashboard.dto.AnalyticsSummaryDto;
import com.mtaafix.report.domain.Report.Status;
import com.mtaafix.user.domain.User;
import com.mtaafix.user.service.OrganisationMembershipService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * P23 analytics: date/category/status/organisation/geographic filters over
 * PostgreSQL (native SQL incl. PostGIS bbox). H2 executes nothing here —
 * parameter validation happens before any SQL is built, so the H2 suite
 * covers 400 validation only and the PG integration test covers execution.
 */
@Service
public class AnalyticsService {

    private static final String APP_TZ = "Africa/Dar_es_Salaam";

    @PersistenceContext
    private EntityManager entityManager;

    private final OrganisationMembershipService membershipService;

    public AnalyticsService(OrganisationMembershipService membershipService) {
        this.membershipService = membershipService;
    }

    @Transactional(readOnly = true)
    public AnalyticsSummaryDto summarize(Instant from, Instant to, String categoryId,
            String status, String orgId, String bbox, User user) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("'from' must not be after 'to'.");
        }
        Status statusEnum = null;
        if (status != null && !status.isBlank()) {
            try {
                statusEnum = Status.valueOf(status.trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("Unknown status filter: " + status);
            }
        }
        GeoBounds bounds = GeoBounds.parse(bbox);

        String scopedOrgId = orgId;
        if (user.getRole() == User.Role.ORGANIZATION_ADMIN) {
            String ownOrgId = membershipService.orgIdOfOrNull(user);
            if (ownOrgId == null) {
                throw new AccessDeniedException("You are not linked to an organisation.");
            }
            scopedOrgId = ownOrgId;
        }
        String safeCategoryId = blankToNull(categoryId);
        String safeOrgId = blankToNull(scopedOrgId);

        Map<String, Object> params = new HashMap<>();
        StringBuilder where = new StringBuilder(" where 1 = 1");
        if (from != null) {
            where.append(" and r.reported_at >= :from");
            params.put("from", Timestamp.from(from));
        }
        if (to != null) {
            where.append(" and r.reported_at <= :to");
            params.put("to", Timestamp.from(to));
        }
        if (safeCategoryId != null) {
            where.append(" and r.category_id = :categoryId");
            params.put("categoryId", safeCategoryId);
        }
        if (statusEnum != null) {
            where.append(" and r.status = :status");
            params.put("status", statusEnum.name());
        }
        if (safeOrgId != null) {
            where.append(" and r.organisation_id = :orgId");
            params.put("orgId", safeOrgId);
        }
        if (bounds != null) {
            where.append(" and ST_Intersects(r.location, "
                    + "ST_MakeEnvelope(:minLon, :minLat, :maxLon, :maxLat, 4326))");
            params.put("minLon", bounds.minLongitude());
            params.put("minLat", bounds.minLatitude());
            params.put("maxLon", bounds.maxLongitude());
            params.put("maxLat", bounds.maxLatitude());
        }

        Map<String, Long> byStatus = new TreeMap<>();
        for (Status s : Status.values()) {
            byStatus.put(s.name(), 0L);
        }
        for (Object[] row : rows("select r.status, count(*) from reports r" + where
                + " group by r.status", params)) {
            byStatus.put((String) row[0], number(row[1]).longValue());
        }

        Object[] totals = (Object[]) single("select count(*), "
                + "count(*) filter (where r.assignment_id is null) from reports r" + where,
                params);
        long total = number(totals[0]).longValue();
        long unassigned = number(totals[1]).longValue();

        List<AnalyticsSummaryDto.Breakdown> byCategory = new ArrayList<>();
        for (Object[] row : rows("select r.category_id, coalesce(c.name, 'Uncategorised'), "
                + "count(*) from reports r "
                + "left join report_categories c on c.id = r.category_id" + where
                + " group by r.category_id, c.name order by count(*) desc", params)) {
            byCategory.add(new AnalyticsSummaryDto.Breakdown((String) row[0], (String) row[1],
                    number(row[2]).longValue()));
        }

        List<AnalyticsSummaryDto.WeekCount> weekly = new ArrayList<>();
        for (Object[] row : rows("select date_trunc('week', "
                + "r.reported_at at time zone '" + APP_TZ + "'), count(*) from reports r"
                + where + " group by 1 order by 1", params)) {
            weekly.add(new AnalyticsSummaryDto.WeekCount(toLocalDate(row[0]),
                    number(row[1]).longValue()));
        }

        Object avgRow = single("select avg(extract(epoch from "
                + "(first_done.done_at - r.reported_at))) / 3600.0 from reports r "
                + "join (select report_id, min(created_at) as done_at from report_status_history "
                + "where to_status in ('RESOLVED', 'CLOSED') group by report_id) first_done "
                + "on first_done.report_id = r.id" + where, params);
        Double avgHours = avgRow == null ? null
                : Math.round(number(avgRow).doubleValue() * 10.0) / 10.0;

        List<AnalyticsSummaryDto.OrgRank> organisations = new ArrayList<>();
        for (Object[] row : rows("select r.organisation_id, coalesce(o.name, 'Unknown'), "
                + "count(*) from reports r "
                + "left join organisations o on o.id = r.organisation_id" + where
                + " and r.organisation_id is not null"
                + " group by r.organisation_id, o.name order by count(*) desc limit 10",
                params)) {
            organisations.add(new AnalyticsSummaryDto.OrgRank((String) row[0], (String) row[1],
                    number(row[2]).longValue()));
        }

        return new AnalyticsSummaryDto(from, to, safeOrgId, safeCategoryId,
                statusEnum != null ? statusEnum.name() : null, bbox, total, byStatus,
                byCategory, weekly, avgHours, unassigned, organisations);
    }

    private List<Object[]> rows(String sql, Map<String, Object> params) {
        Query query = entityManager.createNativeQuery(sql);
        params.forEach(query::setParameter);
        @SuppressWarnings("unchecked")
        List<Object[]> result = query.getResultList();
        return result;
    }

    private Object single(String sql, Map<String, Object> params) {
        Query query = entityManager.createNativeQuery(sql);
        params.forEach(query::setParameter);
        return query.getSingleResult();
    }

    private static Number number(Object value) {
        return value == null ? Double.valueOf(0) : (Number) value;
    }

    private static java.time.LocalDate toLocalDate(Object value) {
        if (value instanceof java.time.LocalDateTime dateTime) {
            return dateTime.toLocalDate();
        }
        if (value instanceof java.sql.Timestamp timestamp) {
            return timestamp.toLocalDateTime().toLocalDate();
        }
        if (value instanceof java.time.Instant instant) {
            return java.time.LocalDate.ofInstant(instant,
                    java.time.ZoneId.of(APP_TZ));
        }
        throw new IllegalStateException("Unexpected week bucket type: " + value);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
