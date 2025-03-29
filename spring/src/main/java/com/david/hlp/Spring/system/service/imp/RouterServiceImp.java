package com.david.hlp.Spring.system.service.imp;
import org.springframework.stereotype.Service;
import com.david.hlp.Spring.system.mapper.RouterMapper;
import com.david.hlp.Spring.system.entity.router.Router;
import lombok.RequiredArgsConstructor;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;
import com.david.hlp.Spring.system.mapper.PermissionMapper;
import org.springframework.transaction.annotation.Transactional;
@Service
@RequiredArgsConstructor
public class RouterServiceImp {
    private final RouterMapper routerMapper;
    private final PermissionMapper permissionMapper;
    public List<Router> getRouters() {
        List<Router> routers = routerMapper.listAll();
        return buildRouterTree(routers);
    }

    private List<Router> buildRouterTree(List<Router> routers) {
        routers.sort((o1, o2) -> {
            Long pid1 = o1.getPid() != null ? o1.getPid() : 0L;
            Long pid2 = o2.getPid() != null ? o2.getPid() : 0L;
            if (pid1.equals(pid2)) {
                return o1.getMenuOrder().compareTo(o2.getMenuOrder());
            }
            return pid1.compareTo(pid2);
        });

        List<Router> preResult = new ArrayList<>();
        for (Router router : routers) {
            for (Router r : preResult) {
                if (r.getId().equals(router.getPid())) {
                    r.getChildren().add(router);
                    break;
                }
            }
            preResult.add(router);
        }

        return preResult.stream()
                .filter(router -> router.getPid() == null)
                .collect(Collectors.toList());
    }
    private boolean checkPermissionIsHasNotInDB(String permission) {
        return permissionMapper.existsByPermissionName(permission);
    }

    private void insertPermissionHasNotInDB(String permission) {
        if (!checkPermissionIsHasNotInDB(permission)) {
            permissionMapper.insertPermission(permission);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void editRouter(Router router) {
        Router oldRouter = routerMapper.getById(router.getId());
        if (!oldRouter.getPermission().equals(router.getPermission())) {
            insertPermissionHasNotInDB(router.getPermission());
            routerMapper.update(router);
            permissionMapper.deleteByPermissionName(oldRouter.getPermission());
        } else {
            routerMapper.update(router);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void addRouter(Router router) {
        insertPermissionHasNotInDB(router.getPermission());
        routerMapper.save(router);
    }
    @Transactional(rollbackFor = Exception.class)
    public void deleteRouter(Router router) {
        routerMapper.deleteById(router.getId());
        permissionMapper.deleteByPermissionName(router.getPermission());
        for(Router r : router.getChildren()) {
            routerMapper.deleteById(r.getId());
            permissionMapper.deleteByPermissionName(r.getPermission());
        }
    }
}
