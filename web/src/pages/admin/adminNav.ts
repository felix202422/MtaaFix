import type { UserRole } from '../../lib/types';

export interface AdminNavLink {
  to: string;
  label: string;
  roles: UserRole[];
}

export const ADMIN_LINKS: AdminNavLink[] = [
  { to: '/admin', label: 'Dashboard', roles: ['MODERATOR', 'ADMINISTRATOR'] },
  {
    to: '/admin/reports',
    label: 'Reports',
    roles: ['MODERATOR', 'ADMINISTRATOR', 'ORGANIZATION_ADMIN', 'FIELD_WORKER'],
  },
  { to: '/admin/moderation', label: 'Moderation', roles: ['MODERATOR', 'ADMINISTRATOR'] },
  {
    to: '/admin/assignments',
    label: 'Assignments',
    roles: ['ADMINISTRATOR', 'ORGANIZATION_ADMIN'],
  },
  { to: '/admin/users', label: 'Users', roles: ['ADMINISTRATOR'] },
  { to: '/admin/categories', label: 'Categories', roles: ['ADMINISTRATOR'] },
  {
    to: '/admin/organizations',
    label: 'Organizations',
    roles: ['ADMINISTRATOR', 'ORGANIZATION_ADMIN'],
  },
  { to: '/admin/audit', label: 'Audit logs', roles: ['ADMINISTRATOR', 'ORGANIZATION_ADMIN'] },
  {
    to: '/admin/analytics',
    label: 'Analytics',
    roles: ['MODERATOR', 'ADMINISTRATOR', 'ORGANIZATION_ADMIN'],
  },
  { to: '/admin/settings', label: 'Settings', roles: ['ADMINISTRATOR'] },
];

export function adminLinksForRole(role: UserRole): AdminNavLink[] {
  return ADMIN_LINKS.filter((link) => link.roles.includes(role));
}
