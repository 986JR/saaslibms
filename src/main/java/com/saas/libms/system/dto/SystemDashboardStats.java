package com.saas.libms.system.dto;

public record SystemDashboardStats(
        long totalInstitutions,
        long activeInstitutions,
        long suspendedInstitutions,
        long totalUsers,
        long systemAdmins
) {}
