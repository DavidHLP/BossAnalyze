file://<WORKSPACE>/src/main/java/com/david/hlp/Spring/system/auth/JwtAuthenticationFilter.java
### java.util.NoSuchElementException: next on empty iterator

occurred in the presentation compiler.

presentation compiler configuration:


action parameters:
uri: file://<WORKSPACE>/src/main/java/com/david/hlp/Spring/system/auth/JwtAuthenticationFilter.java
text:
```scala
package com.david.hlp.Spring.system.auth;

import com.david.hlp.Spring.system.mapper.TokenMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;

/**
 * JWT 认证过滤器。
 *
 * 该过滤器会在每次请求时运行一次，用于验证 JWT 并设置用户的认证信息到 Spring Security 的上下文中。
 */
@Slf4j
@Component
@RequiredArgsConstructor // 自动生成包含所有必需依赖项的构造函数
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  // 用于处理 JWT 的服务类
  private final JwtService jwtService;

  // 用于加载用户详细信息
  private final UserDetailsService userDetailsService;

  // 用于检查 JWT 是否在数据库中有效
  private final TokenMapper tokenMapper;


  /**
   * 核心过滤逻辑。
   *
   * @param request     HTTP 请求对象
   * @param response    HTTP 响应对象
   * @param filterChain 过滤器链，用于继续执行后续过滤器
   * @throws ServletException 如果过滤过程中出现问题
   * @throws IOException      如果发生 I/O 错误
   */
  @Override
  protected void doFilterInternal(
          @NonNull HttpServletRequest request,
          @NonNull HttpServletResponse response,
          @NonNull FilterChain filterChain
  ) throws ServletException, IOException {
    // 直接放行的接口路径
    if (request.getServletPath().startsWith("/api/auth/demo/login")) {
      log.info("直接放行的接口路径: {}", request.getServletPath());
      filterChain.doFilter(request, response);
      return;
    }

    // 2. 从请求头中获取 Authorization 信息
    final String authHeader = request.getHeader("Authorization");
    final String jwt;
    final String userEmail;

    // 如果 Authorization 头为空或者不以 "Bearer " 开头，直接放行
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      filterChain.doFilter(request, response);
      return;
    }

    // 提取 JWT（移除 "Bearer " 前缀）并解析用户邮箱
    jwt = authHeader.substring(7);
    userEmail = jwtService.extractUsername(jwt);


    // 3. 验证用户邮箱和认证上下文
    if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
      // 从 UserDetailsService 加载用户信息
      UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

      // 检查 JWT 是否在数据库中有效，且未过期或撤销
      var isTokenValid = tokenMapper.isTokenValid(jwt);

      // 验证 JWT 是否有效
      if (jwtService.isTokenValid(jwt, userDetails) && isTokenValid) {
        // 直接从UserDetails获取权限
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );

        // 设置认证请求的详细信息
        authToken.setDetails(
                new WebAuthenticationDetailsSource().buildDetails(request)
        );

        // 确保在认证成功后设置SecurityContext
        SecurityContextHolder.getContext().setAuthentication(authToken);
      }
    }

    // 4. 放行请求到过滤器链的下一个过滤器
    filterChain.doFilter(request, response);
  }

  // @Override
  // protected boolean shouldNotFilter(HttpServletRequest request) {
  //   return request.getServletPath().startsWith("/api/auth/");
  // }
}
```



#### Error stacktrace:

```
scala.collection.Iterator$$anon$19.next(Iterator.scala:973)
	scala.collection.Iterator$$anon$19.next(Iterator.scala:971)
	scala.collection.mutable.MutationTracker$CheckedIterator.next(MutationTracker.scala:76)
	scala.collection.IterableOps.head(Iterable.scala:222)
	scala.collection.IterableOps.head$(Iterable.scala:222)
	scala.collection.AbstractIterable.head(Iterable.scala:935)
	dotty.tools.dotc.interactive.InteractiveDriver.run(InteractiveDriver.scala:164)
	dotty.tools.pc.MetalsDriver.run(MetalsDriver.scala:45)
	dotty.tools.pc.WithCompilationUnit.<init>(WithCompilationUnit.scala:31)
	dotty.tools.pc.SimpleCollector.<init>(PcCollector.scala:345)
	dotty.tools.pc.PcSemanticTokensProvider$Collector$.<init>(PcSemanticTokensProvider.scala:63)
	dotty.tools.pc.PcSemanticTokensProvider.Collector$lzyINIT1(PcSemanticTokensProvider.scala:63)
	dotty.tools.pc.PcSemanticTokensProvider.Collector(PcSemanticTokensProvider.scala:63)
	dotty.tools.pc.PcSemanticTokensProvider.provide(PcSemanticTokensProvider.scala:88)
	dotty.tools.pc.ScalaPresentationCompiler.semanticTokens$$anonfun$1(ScalaPresentationCompiler.scala:109)
```
#### Short summary: 

java.util.NoSuchElementException: next on empty iterator