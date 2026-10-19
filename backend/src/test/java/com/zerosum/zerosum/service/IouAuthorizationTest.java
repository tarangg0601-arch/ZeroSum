package com.zerosum.zerosum.service;

import com.zerosum.zerosum.dto.CreateIouRequest;
import com.zerosum.zerosum.repository.GroupMemberRepository;
import com.zerosum.zerosum.repository.GroupRepository;
import com.zerosum.zerosum.repository.IouRepository;
import com.zerosum.zerosum.repository.UserRepository;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class IouAuthorizationTest {
    @Test
    void preventsAnAccountFromCreatingAnIouBetweenOtherUsers() {
        IouRepository ious = mock(IouRepository.class);
        UserRepository users = mock(UserRepository.class);
        GroupRepository groups = mock(GroupRepository.class);
        GroupMemberRepository members = mock(GroupMemberRepository.class);
        IouService service = new IouService(ious, users, groups, members);

        assertThrows(IllegalArgumentException.class, () -> service.create(new CreateIouRequest(1L, 2L, 100L, null, null), 3L));
        verifyNoInteractions(ious, users, groups, members);
    }
}
