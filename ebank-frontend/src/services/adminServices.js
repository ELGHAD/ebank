import api from "../api/axiosClient";

// Platform administration (PLATFORM_ADMIN)
export const listOrganizations = (params) => api.get("/api/platform/organizations", { params });
export const createOrganization = (payload) => api.post("/api/platform/organizations", payload);
export const changeOrganizationStatus = (organizationId, status) =>
  api.patch(`/api/platform/organizations/${organizationId}/status`, { status });

// Organization administration (ORG_ADMIN)
export const getOverview = () => api.get("/api/org/overview");
export const listStaff = () => api.get("/api/org/staff");
export const createStaff = (payload) => api.post("/api/org/staff", payload);
export const setStaffEnabled = (userId, enabled) => api.patch(`/api/org/staff/${userId}/enabled`, { enabled });
