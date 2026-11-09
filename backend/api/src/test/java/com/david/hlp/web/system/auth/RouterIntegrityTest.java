package com.david.hlp.web.system.auth;

import com.david.hlp.web.common.exception.BusinessException;
import com.david.hlp.web.system.auth.entity.router.Router;
import com.david.hlp.web.system.auth.mapper.PermissionMapper;
import com.david.hlp.web.system.auth.mapper.RouterMapper;
import com.david.hlp.web.system.auth.service.imp.RouterServiceImp;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RouterIntegrityTest {
    private final RouterMapper routers = mock(RouterMapper.class);
    private final PermissionMapper permissions = mock(PermissionMapper.class);
    private final RouterServiceImp service = new RouterServiceImp(routers, permissions);

    @Test
    void permissionRenameAndParentChangeFailBeforeWrites() {
        Router stored = Router.builder().id(1L).pid(2L).permission("shared").build();
        when(routers.getById(1L)).thenReturn(stored);
        Router edit = Router.builder().id(1L).pid(2L).permission("renamed").build();
        assertThrows(BusinessException.class, () -> service.editRouter(edit));
        edit.setPermission("shared"); edit.setPid(3L);
        assertThrows(BusinessException.class, () -> service.editRouter(edit));
        verify(routers, never()).update(any(Router.class));
        verifyNoInteractions(permissions);
        edit.setPid(2L); edit.setName("new display name");
        service.editRouter(edit);
        verify(routers).update(edit);
    }

    @Test
    void storedChildrenAndSharedOrRoleReferencesBlockDeletion() {
        Router stored = Router.builder().id(1L).permission("stored").build();
        when(routers.lockById(1L)).thenReturn(stored);
        Router request = Router.builder().id(1L).permission("forged").build();
        when(routers.countChildren(1L)).thenReturn(1L);
        assertThrows(BusinessException.class, () -> service.deleteRouter(request));
        when(routers.countChildren(1L)).thenReturn(0L);
        when(routers.countOtherPermissionReferences("stored", 1L)).thenReturn(1L);
        assertThrows(BusinessException.class, () -> service.deleteRouter(request));
        when(routers.countOtherPermissionReferences("stored", 1L)).thenReturn(0L);
        when(permissions.countRoleReferences("stored")).thenReturn(1L);
        assertThrows(BusinessException.class, () -> service.deleteRouter(request));
        verify(routers, never()).deleteById(anyLong());
        verify(permissions, never()).deleteByPermissionName(anyString());
    }

    @Test
    void forgedChildrenAndPermissionCannotDeleteUnrelatedRows() {
        when(routers.lockById(1L)).thenReturn(Router.builder().id(1L).permission("stored").build());
        Router request = Router.builder().id(1L).permission("victim")
                .children(List.of(Router.builder().id(99L).permission("victim").build())).build();
        service.deleteRouter(request);
        verify(routers).deleteById(1L);
        verify(routers, never()).deleteById(99L);
        verify(permissions).deleteByPermissionName("stored");
        verify(permissions, never()).deleteByPermissionName("victim");
    }
}
