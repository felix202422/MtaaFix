export interface PagedResponse<T> {
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
  content: T[];
}

export type ReportStatus =
  | 'SUBMITTED'
  | 'UNDER_REVIEW'
  | 'NEEDS_INFORMATION'
  | 'VERIFIED'
  | 'ASSIGNED'
  | 'IN_PROGRESS'
  | 'RESOLUTION_REVIEW'
  | 'RESOLVED'
  | 'REJECTED'
  | 'DUPLICATE'
  | 'REOPENED'
  | 'CLOSED';

export interface GeoJsonPoint {
  type: 'Point';
  coordinates: [number, number]; // [longitude, latitude]
}

export interface PublicReportSummary {
  code: string;
  title: string;
  description: string | null;
  categoryId: string | null;
  categoryName: string | null;
  categorySlug: string | null;
  status: ReportStatus;
  reportedAt: string;
  latitude: number | null;
  longitude: number | null;
  geoJson?: GeoJsonPoint | null;
}

export interface PublicTimelineEntry {
  status: ReportStatus;
  timestamp: string | null;
}

export interface PublicReportDetail extends PublicReportSummary {
  resolvedAt: string | null;
  closedAt: string | null;
  duplicateOfCode: string | null;
  timeline: PublicTimelineEntry[];
}

export interface PublicCategory {
  id: string;
  name: string;
  slug: string;
  type: string;
}

export interface PublicStats {
  totalReports: number;
  submittedReports: number;
  resolvedReports: number;
  categories: number;
}

export interface PublicReportFilters {
  q?: string;
  status?: string;
  categoryId?: string;
  dateFrom?: string;
  dateTo?: string;
  /** Bounding box "minLon,minLat,maxLon,maxLat" (PostgreSQL spatial query). */
  bbox?: string;
  /** Radius search "lat,lon,radiusKm" (PostgreSQL spatial query). */
  near?: string;
  page?: number;
  size?: number;
}

export type UserRole =
  | 'CITIZEN'
  | 'MODERATOR'
  | 'ADMINISTRATOR'
  | 'FIELD_WORKER'
  | 'ORGANIZATION_ADMIN';

export interface User {
  id: string;
  email: string;
  fullName: string;
  mobile: string | null;
  active: boolean;
  role: UserRole;
  organisationId: string | null;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  accessTokenExpiresIn: number;
  user: User;
}

export interface UpdateProfileInput {
  fullName: string;
  mobile?: string;
}

export interface Category {
  id: string;
  name: string;
  slug: string;
  type: string;
  parentId?: string | null;
  active?: boolean;
  displayOrder?: number | null;
}

export interface Location {
  latitude: number;
  longitude: number;
  srid?: number;
}

export interface StatusHistoryEntry {
  fromStatus: ReportStatus | null;
  toStatus: ReportStatus;
  userId: string | null;
  timestamp: string;
  comment: string | null;
  reasonCode: string | null;
}

export interface ReportMedia {
  id: string;
  fileName: string;
  originalName: string;
  contentType: string;
  sizeBytes: number;
  url: string;
  mediaType: string;
}

export interface ReportComment {
  id: string;
  content: string;
  userId: string | null;
  timestamp: string;
}

export interface Report {
  id: string;
  code: string;
  title: string;
  description: string | null;
  location: Location | null;
  geoJson?: GeoJsonPoint | null;
  reportedAt: string;
  status: ReportStatus;
  score: number | null;
  categoryId: string;
  assignmentId: string | null;
  organisationId: string | null;
  assigneeId: string | null;
  duplicateOfId: string | null;
  statusHistory: StatusHistoryEntry[];
  media: ReportMedia[];
  comments: ReportComment[];
}

export type AssignmentStatus = 'PENDING' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';

export interface Assignment {
  id: string;
  assigneeId: string | null;
  organisationId: string | null;
  status: AssignmentStatus;
  reportId: string;
  reportCode: string;
  reportTitle: string;
  assignedAt: string | null;
  completedAt: string | null;
}

export interface AttentionReport {
  id: string;
  code: string;
  title: string;
  status: ReportStatus;
}

export interface Dashboard {
  initials: string;
  name: string;
  role: string;
  totalReports: number;
  submittedReports: number;
  verifiedReports: number;
  closedReports: number;
  needsInfoCount: number;
  resolvedAwaitingConfirmCount: number;
  activeCount: number;
  attentionReports: AttentionReport[];
}

export type NotificationType =
  | 'REPORT_SUBMITTED'
  | 'REPORT_VERIFIED'
  | 'REPORT_ASSIGNED'
  | 'REPORT_RESOLVED'
  | 'REPORT_CLOSED'
  | 'REPORT_REJECTED'
  | 'REPORT_NEEDS_INFO'
  | 'REPORT_RESUBMITTED'
  | 'REPORT_DUPLICATE'
  | 'REPORT_COMPLETED'
  | 'REPORT_RETURNED'
  | 'REPORT_REOPENED'
  | 'REPORT_DISPUTED'
  | 'REPORT_IN_PROGRESS'
  | 'REPORT_COMMENTED';

export interface Notification {
  id: string;
  title: string;
  body: string;
  type: NotificationType;
  isRead: boolean;
  timestamp: string;
  reportId: string | null;
}

export interface NotificationPreference {
  emailEnabled: boolean;
  pushEnabled: boolean;
  pushAvailable: boolean;
}

export interface UnreadNotificationCount {
  count: number;
}

export interface SimpleMessage {
  success: boolean;
  message: string | null;
}

// --- Phase 9: administration and moderation (P14-P24) ---

export interface AdminRecentReport {
  id: string;
  code: string;
  title: string;
  status: ReportStatus;
  categoryId: string | null;
  organisationId: string | null;
  reportedAt: string;
  latitude: number | null;
  longitude: number | null;
}

export interface AdminDashboard {
  totalReports: number;
  byStatus: Record<string, number>;
  newLast7Days: number;
  activity14d: { date: string; count: number }[];
  byCategory: { categoryId: string | null; name: string; count: number }[];
  moderationQueue: {
    submitted: number;
    underReview: number;
    needsInformation: number;
    resolutionReview: number;
    items: AdminRecentReport[];
  };
  workload: {
    unassigned: number;
    assigned: number;
    byOrganisation: { organisationId: string | null; name: string; assignedCount: number }[];
    byWorker: { userId: string | null; name: string | null; activeAssignments: number }[];
  };
  attention: {
    needsInformation: number;
    resolvedAwaitingConfirm: number;
    needsInfoItems: AdminRecentReport[];
  };
  recent: AdminRecentReport[];
}

export interface AnalyticsSummary {
  from: string | null;
  to: string | null;
  organisationId: string | null;
  categoryId: string | null;
  status: string | null;
  bbox: string | null;
  total: number;
  byStatus: Record<string, number>;
  byCategory: { categoryId: string | null; name: string; count: number }[];
  weekly: { weekStart: string; count: number }[];
  avgResolutionHours: number | null;
  unassigned: number;
  organisations: { organisationId: string | null; name: string; assignedReports: number }[];
}

export interface AuditLogEntry {
  id: string;
  action: string;
  actorId: string | null;
  actorEmail: string | null;
  entityType: string | null;
  entityId: string | null;
  outcome: string | null;
  details: string | null;
  createdAt: string;
}

export interface AuditLogFilters {
  action?: string;
  actorId?: string;
  entityType?: string;
  entityId?: string;
  outcome?: string;
  from?: string;
  to?: string;
  page?: number;
  size?: number;
}

export interface Organisation {
  id: string;
  name: string;
  slug: string;
  email: string | null;
  phone: string | null;
  address: string | null;
  description: string | null;
  logoUrl: string | null;
  active: boolean;
  memberCount: number;
}

export interface OrganisationMember {
  userId: string;
  email: string;
  fullName: string;
  role: UserRole;
  active: boolean;
  joinedAt: string | null;
}

export interface SystemSettings {
  general: { applicationName: string; applicationTimeZone: string };
  reporting: {
    maxUploadBytes: number;
    multipartMaxFileSize: string;
    allowedMediaTypes: string[];
    notifications: { inAppEnabled: boolean; emailEnabled: boolean; emailNote: string };
  };
  security: {
    accessTokenTtlSeconds: number;
    refreshTokenTtlSeconds: number;
    corsAllowedOrigins: string;
  };
  privacy: { locationFuzzing: string; rolesSeeingPreciseLocations: string[] };
  geographic: {
    fuzzMinMeters: number;
    fuzzMaxMeters: number;
    fuzzSecretConfigured: boolean;
    fuzzSecretIsDefault: boolean;
  };
  integrations: { mapTiles: string; email: string; push: string };
}

export interface AdminReportFilters {
  status?: string;
  categoryId?: string;
  orgId?: string;
  q?: string;
  assignment?: 'assigned' | 'unassigned';
  page?: number;
  size?: number;
}

/** Staff user row (FR-12.1; includes the account status flag). */
export type AdminUser = User;
