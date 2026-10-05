import axios from 'axios';

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api';

const api = axios.create({
  baseURL: API_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;
      const refreshToken = localStorage.getItem('refreshToken');
      if (refreshToken) {
        try {
          const res = await axios.post(`${API_URL}/auth/refresh`, { refreshToken });
          const { accessToken } = res.data.data;
          localStorage.setItem('accessToken', accessToken);
          originalRequest.headers.Authorization = `Bearer ${accessToken}`;
          return api(originalRequest);
        } catch {
          localStorage.clear();
          window.location.href = '/login';
        }
      }
    }
    return Promise.reject(error);
  }
);

export default api;

export const authAPI = {
  login: (data) => api.post('/auth/login', data),
  logout: () => api.post('/auth/logout'),
  changePassword: (data) => api.post('/auth/change-password', data),
  forgotPassword: (data) => api.post('/auth/forgot-password', data),
  resetPassword: (data) => api.post('/auth/reset-password', data),
  refreshToken: (data) => api.post('/auth/refresh', data),
};

export const studentAPI = {
  getAll: (params) => api.get('/students', { params }),
  getById: (id) => api.get(`/students/${id}`),
  search: (params) => api.get('/students/search', { params }),
  create: (data) => api.post('/students', data),
  update: (id, data) => api.put(`/students/${id}`, data),
  delete: (id) => api.delete(`/students/${id}`),
  getByClass: (classId) => api.get(`/students/by-class/${classId}`),
  getCount: () => api.get('/students/count'),
};

export const teacherAPI = {
  getAll: (params) => api.get('/teachers', { params }),
  getById: (id) => api.get(`/teachers/${id}`),
  create: (data) => api.post('/teachers', data),
  update: (id, data) => api.put(`/teachers/${id}`, data),
  delete: (id) => api.delete(`/teachers/${id}`),
  getCount: () => api.get('/teachers/count'),
};

export const classAPI = {
  getAll: () => api.get('/classes'),
  getPaged: (params) => api.get('/classes/paged', { params }),
  getById: (id) => api.get(`/classes/${id}`),
  create: (data) => api.post('/classes', data),
  update: (id, data) => api.put(`/classes/${id}`, data),
  delete: (id) => api.delete(`/classes/${id}`),
};

export const attendanceAPI = {
  mark: (data) => api.post('/attendance', data),
  markBulk: (data) => api.post('/attendance/bulk', data),
  getByStudent: (studentId, params) => api.get(`/attendance/student/${studentId}`, { params }),
  getByClass: (classId, params) => api.get(`/attendance/class/${classId}`, { params }),
};

export const examAPI = {
  getAll: (params) => api.get('/examinations', { params }),
  getById: (id) => api.get(`/examinations/${id}`),
  create: (data) => api.post('/examinations', data),
  addResult: (data) => api.post('/examinations/results', data),
  getResults: (examId) => api.get(`/examinations/${examId}/results`),
  getStudentResults: (studentId) => api.get(`/examinations/results/student/${studentId}`),
  getByTerm: (termId) => api.get(`/examinations/term/${termId}`),
};

export const feeAPI = {
  getInvoices: (params) => api.get('/fees/invoices', { params }),
  getStudentInvoices: (studentId) => api.get(`/fees/invoices/student/${studentId}`),
  createInvoice: (data) => api.post('/fees/invoices', data),
  recordPayment: (data) => api.post('/fees/payments', data),
  getStudentPayments: (studentId) => api.get(`/fees/payments/student/${studentId}`),
};

export const libraryAPI = {
  getBooks: (params) => api.get('/library/books', { params }),
  searchBooks: (params) => api.get('/library/books/search', { params }),
  addBook: (data) => api.post('/library/books', data),
  updateBook: (id, data) => api.put(`/library/books/${id}`, data),
  deleteBook: (id) => api.delete(`/library/books/${id}`),
  borrowBook: (data) => api.post('/library/borrow', data),
  returnBook: (id) => api.put(`/library/return/${id}`),
  getActiveBorrows: () => api.get('/library/borrows/active'),
};

export const dashboardAPI = {
  getAdmin: () => api.get('/dashboard/admin'),
  getTeacher: () => api.get('/dashboard/teacher'),
  getStudent: () => api.get('/dashboard/student'),
  getParent: () => api.get('/dashboard/parent'),
};

export const userAPI = {
  getProfile: () => api.get('/users/profile'),
  updateProfile: (data) => api.put('/users/profile', data),
};

export const eventAPI = {
  getAll: () => api.get('/events'),
  create: (data) => api.post('/events', data),
  delete: (id) => api.delete(`/events/${id}`),
};

export const notificationAPI = {
  getByRole: (role) => api.get(`/notifications/${role}`),
  getUnreadCount: (role) => api.get(`/notifications/unread/${role}`),
  markRead: (id) => api.put(`/notifications/${id}/read`),
};

export const inventoryAPI = {
  getAll: (params) => api.get('/inventory', { params }),
  search: (params) => api.get('/inventory/search', { params }),
  getById: (id) => api.get(`/inventory/${id}`),
  getByCategory: (category) => api.get(`/inventory/category/${category}`),
  getByStatus: (status) => api.get(`/inventory/status/${status}`),
  create: (data) => api.post('/inventory', data),
  update: (id, data) => api.put(`/inventory/${id}`, data),
  adjustQuantity: (id, delta) => api.patch(`/inventory/${id}/quantity`, { delta }),
  delete: (id) => api.delete(`/inventory/${id}`),
};

export const transportAPI = {
  getRoutes: (params) => api.get('/transport/routes', { params }),
  getAllRoutes: () => api.get('/transport/routes/all'),
  getRoute: (id) => api.get(`/transport/routes/${id}`),
  createRoute: (data) => api.post('/transport/routes', data),
  updateRoute: (id, data) => api.put(`/transport/routes/${id}`, data),
  deleteRoute: (id) => api.delete(`/transport/routes/${id}`),
  assignStudent: (data) => api.post('/transport/assign', data),
  unassignStudent: (id) => api.delete(`/transport/assign/${id}`),
  getStudentsByRoute: (routeId) => api.get(`/transport/routes/${routeId}/students`),
  getRouteByStudent: (studentId) => api.get(`/transport/students/${studentId}/route`),
};

export const hostelAPI = {
  getHostels: (params) => api.get('/hostel/hostels', { params }),
  getAllHostels: () => api.get('/hostel/hostels/all'),
  getHostel: (id) => api.get(`/hostel/hostels/${id}`),
  createHostel: (data) => api.post('/hostel/hostels', data),
  updateHostel: (id, data) => api.put(`/hostel/hostels/${id}`, data),
  deleteHostel: (id) => api.delete(`/hostel/hostels/${id}`),
  getRoomsByHostel: (hostelId) => api.get(`/hostel/rooms/hostel/${hostelId}`),
  getRoom: (id) => api.get(`/hostel/rooms/${id}`),
  createRoom: (data) => api.post('/hostel/rooms', data),
  updateRoom: (id, data) => api.put(`/hostel/rooms/${id}`, data),
  deleteRoom: (id) => api.delete(`/hostel/rooms/${id}`),
  assignStudent: (data) => api.post('/hostel/assign', data),
  checkOutStudent: (id) => api.post(`/hostel/checkout/${id}`),
  getStudentsByRoom: (roomId) => api.get(`/hostel/rooms/${roomId}/students`),
  getRoomByStudent: (studentId) => api.get(`/hostel/students/${studentId}/room`),
};

export const payrollAPI = {
  getAll: (params) => api.get('/payroll', { params }),
  getById: (id) => api.get(`/payroll/${id}`),
  getByTeacher: (teacherId) => api.get(`/payroll/teacher/${teacherId}`),
  getByMonthAndYear: (month, year) => api.get(`/payroll/month/${month}/year/${year}`),
  create: (data) => api.post('/payroll', data),
  update: (id, data) => api.put(`/payroll/${id}`, data),
  process: (id) => api.post(`/payroll/${id}/process`),
  delete: (id) => api.delete(`/payroll/${id}`),
};

export const assignmentAPI = {
  getAll: (params) => api.get('/assignments', { params }),
  getById: (id) => api.get(`/assignments/${id}`),
  create: (data) => api.post('/assignments', data),
  update: (id, data) => api.put(`/assignments/${id}`, data),
  delete: (id) => api.delete(`/assignments/${id}`),
  submit: (id, data) => api.post(`/assignments/${id}/submit`, data),
  getSubmissions: (id) => api.get(`/assignments/${id}/submissions`),
  gradeSubmission: (submissionId, data) => api.post(`/assignments/submissions/${submissionId}/grade`, data),
};

export const timetableAPI = {
  getAll: (params) => api.get('/timetable', { params }),
  getById: (id) => api.get(`/timetable/${id}`),
  create: (data) => api.post('/timetable', data),
  update: (id, data) => api.put(`/timetable/${id}`, data),
  delete: (id) => api.delete(`/timetable/${id}`),
  getByClass: (classId, day) => api.get(`/timetable/class/${classId}${day ? `/day/${day}` : ''}`),
  getByTeacher: (teacherId, day) => api.get(`/timetable/teacher/${teacherId}${day ? `/day/${day}` : ''}`),
};

export const subjectAPI = {
  getAll: (params) => api.get('/subjects', { params }),
  getById: (id) => api.get(`/subjects/${id}`),
  create: (data) => api.post('/subjects', data),
  update: (id, data) => api.put(`/subjects/${id}`, data),
  delete: (id) => api.delete(`/subjects/${id}`),
  getByDepartment: (departmentId) => api.get(`/subjects/department/${departmentId}`),
};

export const reportAPI = {
  exportStudents: (format) => api.get('/reports/students/export', { params: { format }, responseType: 'blob' }),
  exportAttendance: (params) => api.get('/reports/attendance/export', { params: { ...params }, responseType: 'blob' }),
  exportPayments: (params) => api.get('/reports/payments/export', { params: { ...params }, responseType: 'blob' }),
  exportExamResults: (examinationId, format) => api.get(`/reports/examinations/${examinationId}/results/export`, { params: { format }, responseType: 'blob' }),
  getReportCard: (studentId, examinationId, format = 'pdf') =>
    api.get('/reports/report-card', { params: { studentId, examinationId, format }, responseType: format === 'json' ? undefined : 'blob' }),
  importStudents: (file) => {
    const formData = new FormData();
    formData.append('file', file);
    return api.post('/reports/students/import', formData, { headers: { 'Content-Type': 'multipart/form-data' } });
  },
};

export const auditAPI = {
  getAll: (params) => api.get('/audit', { params }),
  getByUser: (userId) => api.get(`/audit/user/${userId}`),
  getByAction: (action) => api.get(`/audit/action/${action}`),
};
