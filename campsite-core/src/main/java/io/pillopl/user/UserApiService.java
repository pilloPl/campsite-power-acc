package io.pillopl.user;

import io.pillopl.api.UserApi;
import io.pillopl.api.exception.AccessNotAllowedException;
import io.pillopl.api.exception.InvalidInputException;
import io.pillopl.commons.ListPage;
import io.pillopl.commons.Pagination;
import io.pillopl.dto.user.CurrentUserDTO;
import io.pillopl.dto.user.UserinfoDTO;
import io.pillopl.dto.user.UserinfoSummaryDTO;
import io.pillopl.audit.AuditActionType;
import io.pillopl.audit.AuditService;
import io.pillopl.event.EventService;
import io.pillopl.event.UserinfoChangedEvent;
import io.pillopl.group.Market;
import io.pillopl.persistence.dao.UserroleDao;
import io.pillopl.util.ApiExceptionTranslator;
import io.pillopl.util.ApiValues;
import io.pillopl.value.EmailAddress;
import io.pillopl.value.Username;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * User calls of the console on top of {@link UserinfoService}. Managing other users needs USER_ADMIN and the user in
 * the STAFF scope of the caller; every user may read and change its own name and email. Changes are announced as
 * events (the self-service list is refreshed from them).
 */
@Service
@Transactional
public class UserApiService implements UserApi {

    private final UserinfoService userinfoService;

    private final UserinfoSummaryService userinfoSummaryService;

    private final UserroleDao userroleDao;

    private final EventService eventService;

    private final PrincipalResolver principalResolver;

    private final PasswordEncoder passwordEncoder;

    private final AuditService auditService;

    public UserApiService(UserinfoService userinfoService, UserinfoSummaryService userinfoSummaryService,
                          UserroleDao userroleDao, EventService eventService, PrincipalResolver principalResolver,
                          PasswordEncoder passwordEncoder, AuditService auditService) {
        this.userinfoService = userinfoService;
        this.userinfoSummaryService = userinfoSummaryService;
        this.userroleDao = userroleDao;
        this.eventService = eventService;
        this.principalResolver = principalResolver;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
        this.noUserHash = passwordEncoder.encode(java.util.UUID.randomUUID().toString());
    }

    /**
     * Unknown users are checked against this hash too, so that the answer takes as long as for a known user.
     */
    private final String noUserHash;


    private static final String LOGIN_REFUSED = "Wrong username or password";

    @Override
    @Transactional(noRollbackFor = AccessNotAllowedException.class)
    public CurrentUserDTO authenticate(Username username, String password) {
        Userinfo userinfo = userinfoService.findByUsername(username.value());
        String hash = userinfo != null && userinfo.getPasswordHash() != null ? userinfo.getPasswordHash()
                : noUserHash;
        boolean passwordMatches = password != null && passwordEncoder.matches(password, hash);
        if (userinfo == null) {
            auditService.auditAction(AuditActionType.LOGIN, username.value(), "login", null, null, "unknown user");
            throw new AccessNotAllowedException(LOGIN_REFUSED);
        }
        if (!passwordMatches) {
            boolean locked = userinfoService.recordFailedLogin(userinfo.getUsername());
            auditService.auditAction(AuditActionType.LOGIN, userinfo.getUsername(), "login", groupIdOf(userinfo),
                    locked ? "user locked" : null, "wrong password");
            throw new AccessNotAllowedException(LOGIN_REFUSED);
        }
        if (!userinfo.canLogIn()) {
            auditService.auditAction(AuditActionType.LOGIN, userinfo.getUsername(), "login", groupIdOf(userinfo),
                    null, "user is " + (userinfo.isDeleted() ? "deleted" : userinfo.getStatus()));
            throw new AccessNotAllowedException(LOGIN_REFUSED);
        }
        userinfoService.recordSuccessfulLogin(userinfo.getUsername());
        auditService.auditAction(AuditActionType.LOGIN, userinfo.getUsername(), "login", groupIdOf(userinfo), null,
                null);
        return UserinfoMapper.toCurrentUserDTO(userinfo);
    }

    private static String groupIdOf(Userinfo userinfo) {
        return userinfo.getGroup() != null ? String.valueOf(userinfo.getGroup().getId()) : null;
    }

    @Override
    @Transactional(readOnly = true)
    public UserinfoDTO findUser(Username authUsername, Username username) {
        try {
            Userinfo principal = principalResolver.resolve(authUsername);
            Userinfo user = principalResolver.findUser(username);
            userinfoService.checkIsManageable(principal, user, true);
            return UserinfoMapper.toDTO(user);
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    @Override
    public UserinfoDTO createUser(Username authUsername, EmailAddress email, String name, String market) {
        try {
            Userinfo principal = principalResolver.resolve(authUsername, UserrightName.USER_ADMIN);
            Market userMarket = ApiValues.parseEnumOrNull(Market.class, market, "market");
            Userinfo created = userinfoService.createUser(principal, email.value(), name, userMarket);
            eventService.send(new UserinfoChangedEvent(created.getId(), created.getUsername(),
                    UserinfoChangeType.CREATED));
            return UserinfoMapper.toDTO(created);
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    @Override
    public UserinfoDTO updateUser(Username authUsername, Username username, EmailAddress email, String name) {
        try {
            Userinfo principal = principalResolver.resolve(authUsername);
            Userinfo user = principalResolver.findUser(username);
            boolean self = principal.getUsername().equals(user.getUsername());
            if (!self && !principal.hasRight(UserrightName.USER_ADMIN.name())) {
                throw new AccessNotAllowedException(String.format("User '%s' lacks right %s", authUsername,
                        UserrightName.USER_ADMIN));
            }
            userinfoService.checkIsManageable(principal, user, true);
            if (email != null && !email.value().equalsIgnoreCase(user.getEmail())) {
                user = userinfoService.changeEmail(user.getUsername(), email.value());
                eventService.send(new UserinfoChangedEvent(user.getId(), user.getUsername(),
                        UserinfoChangeType.EMAIL_CHANGED));
            }
            if (name != null && !name.equals(user.getName())) {
                user = userinfoService.changeName(user.getUsername(), name);
                eventService.send(new UserinfoChangedEvent(user.getId(), user.getUsername(),
                        UserinfoChangeType.NAME_CHANGED));
            }
            return UserinfoMapper.toDTO(user);
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    @Override
    public UserinfoDTO setupAffiliateOwner(Username authUsername, Username ownerUsername) {
        try {
            Userinfo principal = principalResolver.resolve(authUsername, UserrightName.USER_ADMIN);
            Userinfo owner = principalResolver.findUser(ownerUsername);
            userinfoService.checkIsManageable(principal, owner, false);
            Userinfo result = userinfoService.setupAffiliateOwner(owner);
            eventService.send(new UserinfoChangedEvent(result.getId(), result.getUsername(),
                    UserinfoChangeType.AFFILIATE_OWNER_SETUP));
            return UserinfoMapper.toDTO(result);
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ListPage<UserinfoSummaryDTO> findSelfServiceUsers(Username authUsername, Boolean affiliateOwner,
                                                             Boolean seasonalResident, Pagination pagination) {
        try {
            Userinfo principal = principalResolver.resolve(authUsername, UserrightName.USER_ADMIN);
            Pagination p = pagination != null ? pagination : Pagination.FIRST_PAGE;
            Page<UserinfoSummary> page = userinfoSummaryService.findSelfServiceUsers(principal, affiliateOwner,
                    seasonalResident, PageRequest.of(p.page(), p.rowsPerPage()));
            List<UserinfoSummaryDTO> items = new ArrayList<>();
            for (UserinfoSummary summary : page.getContent()) {
                items.add(UserinfoMapper.toDTO(summary));
            }
            return ListPage.of(items, page.getTotalElements(), p);
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserinfoDTO> findUsersByRoles(Username authUsername, List<String> roleNames) {
        try {
            Userinfo principal = principalResolver.resolve(authUsername, UserrightName.USER_ADMIN);
            if (roleNames == null || roleNames.isEmpty()) {
                throw new InvalidInputException("No roles given");
            }
            List<Userrole> roles = new ArrayList<>();
            for (String roleName : roleNames) {
                Userrole role = roleName != null ? userroleDao.findByName(roleName.trim()) : null;
                if (role == null) {
                    throw new InvalidInputException("Unknown role: " + roleName);
                }
                roles.add(role);
            }
            return UserinfoMapper.toDTOs(userinfoService.findByRoles(principal, roles));
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public CurrentUserDTO getCurrentUser(Username authUsername) {
        try {
            return UserinfoMapper.toCurrentUserDTO(principalResolver.resolve(authUsername));
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserinfoDTO> findUsers(Username authUsername, String search) {
        try {
            Userinfo principal = principalResolver.resolve(authUsername, UserrightName.USER_ADMIN);
            return UserinfoMapper.toDTOs(userinfoService.findManageableUsers(principal, search));
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    @Override
    public UserinfoDTO setPassword(Username authUsername, Username username, String password) {
        try {
            Userinfo principal = principalResolver.resolve(authUsername);
            boolean self = principal.getUsername().equals(username.value());
            if (!self && !principal.hasRight(UserrightName.USER_ADMIN.name())) {
                throw new AccessNotAllowedException(String.format("User '%s' lacks right %s", authUsername,
                        UserrightName.USER_ADMIN));
            }
            Userinfo user = userinfoService.setPassword(principal, username.value(), password);
            eventService.send(new UserinfoChangedEvent(user.getId(), user.getUsername(),
                    UserinfoChangeType.PASSWORD_CHANGED));
            return UserinfoMapper.toDTO(user);
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    @Override
    public UserinfoDTO changeUserStatus(Username authUsername, Username username, String status) {
        try {
            Userinfo principal = principalResolver.resolve(authUsername, UserrightName.USER_ADMIN);
            UserinfoStatus newStatus = ApiValues.parseEnum(UserinfoStatus.class, status, "status");
            Userinfo user = userinfoService.changeStatus(principal, username.value(), newStatus);
            eventService.send(new UserinfoChangedEvent(user.getId(), user.getUsername(),
                    UserinfoChangeType.STATUS_CHANGED));
            return UserinfoMapper.toDTO(user);
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    @Override
    public UserinfoDTO replaceUserRoles(Username authUsername, Username username, List<String> roleNames) {
        try {
            Userinfo principal = principalResolver.resolve(authUsername, UserrightName.USER_ADMIN);
            Userinfo user = userinfoService.replaceRoles(principal, username.value(), roleNames);
            eventService.send(new UserinfoChangedEvent(user.getId(), user.getUsername(),
                    UserinfoChangeType.ROLES_CHANGED));
            return UserinfoMapper.toDTO(user);
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }
}
