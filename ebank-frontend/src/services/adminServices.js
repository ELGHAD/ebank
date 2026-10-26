import api from "../api/axiosClient";

// Platform administration (PLATFORM_ADMIN)
export const listOrganizations = (params) => api.get("/api/platform/organizations", { params });
export const createOrganization = (payload) => api.post("/api/platform/organizations", payload);
export const changeOrganizationStatus = (organizationId, status) =>
  api.patch(`/api/platform/organizations/${organizationId}/status`, { status });
/** Simulates the payment provider's notification (payment failed / payment received). */
export const changeSubscriptionStatus = (organizationId, status) =>
  api.patch(`/api/platform/organizations/${organizationId}/subscription-status`, { status });

// Organization administration (ORG_ADMIN)
export const getOverview = () => api.get("/api/org/overview");
export const listStaff = () => api.get("/api/org/staff");
export const createStaff = (payload) => api.post("/api/org/staff", payload);
export const setStaffEnabled = (userId, enabled) => api.patch(`/api/org/staff/${userId}/enabled`, { enabled });

// Plans and simulated billing (ORG_ADMIN)
export const listPlans = () => api.get("/api/org/plans");
export const getSubscription = () => api.get("/api/org/subscription");
export const changePlan = (planCode) => api.put("/api/org/subscription/plan", { planCode });
