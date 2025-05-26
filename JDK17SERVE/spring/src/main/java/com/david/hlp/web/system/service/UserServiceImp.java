package com.david.hlp.web.system.service;

import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.stereotype.Service;

import com.david.hlp.web.common.result.PageInfo;
import com.david.hlp.web.system.entity.user.DelUser;
import com.david.hlp.web.system.entity.user.User;
import com.david.hlp.web.system.mapper.UserMapper;

@Service
@RequiredArgsConstructor
public class UserServiceImp {
    private final UserMapper userMapper;
    private final PasswordService passwordService;

    public User getUserBaseInfo(Long userId) {
        User user = userMapper.getByUserIdToUser(userId);
        User res = User.builder()
                .id(user.getUserId())
                .name(user.getUsername())
                .email(user.getEmail())
                .roleId(user.getRoleId())
                .build();
        return res;
    }

    public PageInfo<User> getUserManageInfo(int pageNum, int pageSize, User query){
        List<User> users = userMapper.listByPage(pageNum-1, pageSize, query);
        Long total = userMapper.count(query); // 获取总记录数
        PageInfo<User> pageInfo = PageInfo.<User>builder()
                .content(users)
                .query(query)
                .number(pageNum)
                .size(pageSize)
                .totalElements(total)
                .totalPages((int)Math.ceil((double)total / pageSize))
                .build();
        return pageInfo;
    }

    public void deleteUser(DelUser user) {
        userMapper.deleteById(user.getId());
    }

    public void updateUser(User user) {
        userMapper.updateById(user);
    }

    public void addUser(User user) {
        user.setPassword(passwordService.encodePassword(user.getPassword()));
        userMapper.insert(user);
    }
}
