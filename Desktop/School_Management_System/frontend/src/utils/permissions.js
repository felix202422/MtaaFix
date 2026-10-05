import { ROLES } from './constants';

/**
 * Role-based access control for the frontend.
 * Each route lists the roles allowed to see it. Roles not listed are denied.
 * SUPER_ADMIN and SCHOOL_ADMIN implicitly get everything.
 */
const ADMINS = [ROLES.SUPER_ADMIN, ROLES.SCHOOL_ADMIN];

export const NAV_ITEMS = [
  { label: 'Dashboard', path: '/', icon: 'dashboard', roles: 'ALL' },
  { label: 'Students', path: '/students', icon: 'users', roles: [...ADMINS, ROLES.TEACHER, ROLES.REGISTRAR, ROLES.RECEPTIONIST] },
  { label: 'Teachers', path: '/teachers', icon: 'school', roles: [...ADMINS, ROLES.REGISTRAR] },
  { label: 'Classes', path: '/classes', icon: 'book', roles: [...ADMINS, ROLES.TEACHER, ROLES.REGISTRAR] },
  { label: 'Attendance', path: '/attendance', icon: 'calendar', roles: [...ADMINS, ROLES.TEACHER, ROLES.REGISTRAR] },
  { label: 'Examinations', path: '/examinations', icon: 'exam', roles: [...ADMINS, ROLES.TEACHER, ROLES.STUDENT, ROLES.PARENT] },
  { label: 'Assignments', path: '/assignments', icon: 'clipboard', roles: [...ADMINS, ROLES.TEACHER, ROLES.STUDENT, ROLES.PARENT] },
  { label: 'Timetable', path: '/timetable', icon: 'clock', roles: [...ADMINS, ROLES.TEACHER, ROLES.STUDENT, ROLES.PARENT] },
  { label: 'Subjects', path: '/subjects', icon: 'library', roles: [...ADMINS, ROLES.TEACHER, ROLES.STUDENT] },
  { label: 'Fees', path: '/fees', icon: 'wallet', roles: [...ADMINS, ROLES.ACCOUNTANT] },
  { label: 'Library', path: '/library', icon: 'bookOpen', roles: [...ADMINS, ROLES.LIBRARIAN, ROLES.TEACHER, ROLES.STUDENT] },
  { label: 'Inventory', path: '/inventory', icon: 'package', roles: [ROLES.SUPER_ADMIN, ROLES.SCHOOL_ADMIN, 'INVENTORY_MANAGER'] },
  { label: 'Transport', path: '/transport', icon: 'bus', roles: [ROLES.SUPER_ADMIN, ROLES.SCHOOL_ADMIN, 'TRANSPORT_MANAGER'] },
  { label: 'Hostel', path: '/hostel', icon: 'hotel', roles: [ROLES.SUPER_ADMIN, ROLES.SCHOOL_ADMIN, 'HOSTEL_MANAGER'] },
  { label: 'Events', path: '/events', icon: 'calendarCheck', roles: 'ALL' },
  { label: 'Settings', path: '/settings', icon: 'settings', roles: 'ALL' },
];

const ROLE_EXTRA = new Set(['INVENTORY_MANAGER', 'TRANSPORT_MANAGER', 'HOSTEL_MANAGER']);
const ALL = Object.values(ROLES).concat([...ROLE_EXTRA]);

/**
 * Roles allowed to access `path`. Returns null when open to everyone.
 */
export function rolesForPath(path) {
  const item = NAV_ITEMS.find((n) => n.path === path);
  if (!item) return ALL;
  if (item.roles === 'ALL') return null; // null = everyone
  return item.roles;
}

export function navItemsForRole(role) {
  return NAV_ITEMS.filter((item) => {
    if (item.roles === 'ALL') return true;
    if (role === ROLES.SUPER_ADMIN || role === ROLES.SCHOOL_ADMIN) return true;
    return item.roles.includes(role);
  });
}

export function canAccessRole(user, path) {
  if (!user) return false;
  const roles = rolesForPath(path);
  if (roles === null) return true;
  return roles.includes(user.role);
}
