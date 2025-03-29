package com.david.hlp.Spring.system.entity.user;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import com.david.hlp.Spring.common.util.annotation.emptystring.NotEmptyString;

/**
 * 删除用户实体类
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DelUser {
    /** 用户ID */
    private Long id;

    /** 用户密码 */
    @NotEmptyString(message = "密码不能为空")
    private String password;
}
