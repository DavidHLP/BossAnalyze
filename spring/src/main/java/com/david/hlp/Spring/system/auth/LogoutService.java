package com.david.hlp.Spring.system.auth;

import com.david.hlp.Spring.system.mapper.TokenMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Service;
import java.util.List;
/**
 * 注销服务类。
 *
 * 该服务处理用户注销操作，将关联的 JWT 标记为失效并清除安全上下文。
 */
@Service
@RequiredArgsConstructor // 使用 Lombok 自动生成构造函数，注入依赖
public class LogoutService implements LogoutHandler {

  // 注入 TokenRepository，用于管理和查询令牌
  private final TokenMapper tokenMapper;

  /**
   * 处理用户注销逻辑。
   *
   * @param request        HTTP 请求对象，用于获取 Authorization 头中的 JWT。
   * @param response       HTTP 响应对象。
   * @param authentication 当前用户的认证信息。
   */
  @Override
  public void logout(
          HttpServletRequest request,
          HttpServletResponse response,
          Authentication authentication
  ) {
    final String authHeader = request.getHeader("Authorization");
    final String jwt;

    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      return;
    }

    jwt = authHeader.substring(7);

    var storedToken = tokenMapper.getByToken(jwt);

    if (storedToken != null) {
      storedToken.setExpired(true);
      storedToken.setRevoked(true);
      tokenMapper.updateBatch(List.of(storedToken));

      SecurityContextHolder.clearContext();
    }
  }
}