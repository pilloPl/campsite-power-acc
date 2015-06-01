package io.pillopl.user;

import io.pillopl.group.Group;
import io.pillopl.group.GroupService;
import io.pillopl.group.GroupType;
import io.pillopl.persistence.dao.UserinfoSummaryDao;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class UserinfoSummaryServiceTest {

    private final UserinfoSummaryDao userinfoSummaryDao = mock(UserinfoSummaryDao.class);
    private final GroupService groupService = mock(GroupService.class);
    private final UserinfoSummaryService service = new UserinfoSummaryService(userinfoSummaryDao, groupService);

    private final Pageable firstPage = PageRequest.of(0, 20);

    @Test
    void principalWithoutStaffGroupSeesNoSelfServiceUsers() {
        Userinfo ana = new Userinfo("ana", "Ana", "ana@example.org");

        Page<UserinfoSummary> users = service.findSelfServiceUsers(ana, null, null, firstPage);

        assertThat(users).isEmpty();
        verifyNoInteractions(groupService, userinfoSummaryDao);
    }

    @Test
    void selfServiceUsersAreSearchedInTheManageableStaffGroupsWithTheFlags() {
        Group hrStaff = new Group(GroupType.STAFF, "HR STAFF");
        Group selfService = new Group(GroupType.STAFF, "HR SELF SERVICE");
        Userinfo admin = new Userinfo("admin", "Admin", "admin@example.org");
        admin.getGroups().add(hrStaff);
        UserinfoSummary owner = new UserinfoSummary();
        when(groupService.findManageableGroups(hrStaff)).thenReturn(List.of(hrStaff, selfService));
        when(userinfoSummaryDao.findByGroups(any(), any(), any(), any())).thenReturn(new PageImpl<>(List.of(owner)));

        Page<UserinfoSummary> users = service.findSelfServiceUsers(admin, true, false, firstPage);

        assertThat(users).containsExactly(owner);
        verify(userinfoSummaryDao).findByGroups(List.of(hrStaff, selfService), true, false, firstPage);
    }
}
