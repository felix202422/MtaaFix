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
