package io.pillopl.admin;

import io.pillopl.api.AdministrationApi;
import io.pillopl.api.exception.InvalidInputException;
import io.pillopl.audit.AuditActionType;
import io.pillopl.audit.AuditCategory;
import io.pillopl.audit.AuditService;
import io.pillopl.commons.SortOrder;
import io.pillopl.config.SystemPropertyKey;
import io.pillopl.config.SystemPropertyService;
import io.pillopl.dto.admin.AuditEntryDTO;
import io.pillopl.dto.admin.AuditFilterDTO;
import io.pillopl.dto.admin.RightsMatrixDTO;
import io.pillopl.dto.admin.SecurityGroupDTO;
import io.pillopl.dto.admin.SystemPropertyDTO;
import io.pillopl.dto.user.UserroleDTO;
import io.pillopl.persistence.dao.SecurityGroupDao;
import io.pillopl.persistence.dao.SystemPropertyDao;
import io.pillopl.persistence.dao.UserinfoDao;
import io.pillopl.persistence.dao.UserroleDao;
import io.pillopl.user.PrincipalResolver;
import io.pillopl.user.Userinfo;
import io.pillopl.user.UserinfoSummaryService;
import io.pillopl.user.UserrightName;
import io.pillopl.util.ApiExceptionTranslator;
import io.pillopl.util.ApiValues;
import io.pillopl.value.Username;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Administration calls: audit search (AUDIT_VIEW), configuration (CONFIG_EDIT, every change audited), roles and the
 * refresh of the self-service user view (USER_ADMIN).
 */
@Service
@Transactional
public class AdministrationApiService implements AdministrationApi {

    private final AuditService auditService;

    private final SystemPropertyService systemPropertyService;

    private final SystemPropertyDao systemPropertyDao;

    private final UserroleDao userroleDao;

    private final UserinfoSummaryService userinfoSummaryService;

    private final PrincipalResolver principalResolver;

    private final UserinfoDao userinfoDao;

    private final SecurityGroupDao securityGroupDao;

    public AdministrationApiService(AuditService auditService, SystemPropertyService systemPropertyService,
                                    SystemPropertyDao systemPropertyDao, UserroleDao userroleDao,
                                    UserinfoSummaryService userinfoSummaryService,
                                    PrincipalResolver principalResolver, UserinfoDao userinfoDao,
                                    SecurityGroupDao securityGroupDao) {
        this.userinfoDao = userinfoDao;
        this.securityGroupDao = securityGroupDao;
        this.auditService = auditService;
        this.systemPropertyService = systemPropertyService;
        this.systemPropertyDao = systemPropertyDao;
        this.userroleDao = userroleDao;
        this.userinfoSummaryService = userinfoSummaryService;
        this.principalResolver = principalResolver;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditEntryDTO> findAuditEntries(Username authUsername, AuditFilterDTO filter, SortOrder sortOrder) {
        try {
            principalResolver.resolve(authUsername, UserrightName.AUDIT_VIEW);
            AuditFilterDTO f = filter != null ? filter : new AuditFilterDTO(null, null, null, null, null, null);
            SortOrder sort = sortOrder != null ? sortOrder : SortOrder.UNSORTED;
            AuditCategory category = ApiValues.parseEnumOrNull(AuditCategory.class, f.category(), "audit category");
            return AdministrationMapper.toAuditDTOs(auditService.getAuditEntries(f.name(), f.type(), f.status(),
                    category, f.period().from(), f.period().to(), f.freeSearch(), sort.column(),
                    sort.direction().name()));
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<SystemPropertyDTO> getSystemProperties(Username authUsername) {
        try {
            principalResolver.resolve(authUsername, UserrightName.CONFIG_EDIT);
            return AdministrationMapper.toSystemPropertyDTOs(systemPropertyDao.findAll());
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    @Override
    public SystemPropertyDTO setSystemProperty(Username authUsername, String key, String value, String description) {
        try {
            Userinfo principal = principalResolver.resolve(authUsername, UserrightName.CONFIG_EDIT);
            SystemPropertyKey propertyKey = parseKey(key);
            String oldValue = systemPropertyService.getSystemProperties().get(propertyKey);
            systemPropertyService.setProperty(propertyKey, value, description);
            auditService.auditAction(AuditActionType.CONFIG_CHANGE, principal.getUsername(), propertyKey.name(), null,
                    String.format("'%s' -> '%s'", oldValue, value), null);
            return AdministrationMapper.toDTO(systemPropertyDao.findByKey(propertyKey).orElseThrow());
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserroleDTO> findRoles(Username authUsername) {
        try {
            principalResolver.resolve(authUsername, UserrightName.USER_ADMIN);
            return AdministrationMapper.toRoleDTOs(userroleDao.findAllOrdered());
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    @Override
    public void refreshUserSummary(Username authUsername) {
        try {
            principalResolver.resolve(authUsername, UserrightName.USER_ADMIN);
            userinfoSummaryService.refresh();
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public RightsMatrixDTO getRightsMatrix(Username authUsername) {
        try {
            principalResolver.resolve(authUsername, UserrightName.USER_ADMIN);
            List<SecurityGroupDTO> securityGroups = securityGroupDao.findAll(Sort.by("name")).stream()
                    .map(AdministrationMapper::toDTO).toList();
            return new RightsMatrixDTO(userroleDao.findAllRightNames(),
                    AdministrationMapper.toRoleDTOs(userroleDao.findAllOrdered()), securityGroups);
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<SystemPropertyDTO> reloadConfiguration(Username authUsername) {
        try {
            Userinfo principal = principalResolver.resolve(authUsername, UserrightName.CONFIG_EDIT);
            systemPropertyService.reloadSystemProperties();
            auditService.auditAction(AuditActionType.CONFIG_CHANGE, principal.getUsername(), "reload", null,
                    "Configuration reloaded from SYSTEM_PROPERTY", null);
            return AdministrationMapper.toSystemPropertyDTOs(systemPropertyDao.findAll());
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void auditConsoleAction(String username, String operation, String request, String errorReason) {
        String groupId = null;
        if (username != null) {
            Userinfo userinfo = userinfoDao.findByUsername(username);
            if (userinfo != null && userinfo.getGroup() != null) {
                groupId = String.valueOf(userinfo.getGroup().getId());
            }
        }
        auditService.auditAction(AuditActionType.CONSOLE_ACTION, username, operation, groupId, request, errorReason);
    }

    /**
     * Keys are the enum names as stored in PROPERTY_KEY (case sensitive, e.g. StaleSessionTimeoutInHours).
     */
    static SystemPropertyKey parseKey(String key) {
        if (key == null || key.isBlank()) {
            throw new InvalidInputException("Property key is missing");
        }
        try {
            return SystemPropertyKey.valueOf(key.trim());
        } catch (IllegalArgumentException e) {
            throw new InvalidInputException("Unknown property key: " + key, e);
        }
    }
}
