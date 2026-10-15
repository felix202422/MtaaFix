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
  page?: number;
  size?: number;
}
