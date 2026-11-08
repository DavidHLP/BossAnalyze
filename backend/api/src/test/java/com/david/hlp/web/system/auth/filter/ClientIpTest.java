package com.david.hlp.web.system.auth.filter;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ClientIpTest {
  @Test
  void onlyExplicitPeerAndDedicatedLiteralHeaderAreTrusted() {
    var request = new MockHttpServletRequest();
    request.setRemoteAddr("127.0.0.1");
    request.addHeader("X-Boss-Client-IP", "8.8.8.8");
    assertEquals("127.0.0.1", ClientIp.resolve(request, ""));
    assertEquals("127.0.0.1", ClientIp.resolve(request, "10.0.0.1"));
    assertEquals("8.8.8.8", ClientIp.resolve(request, "127.0.0.1"));
    for (String invalid : new String[]{"example.com", "127.1", "8.8.8.8, 1.1.1.1", "::1%lo"}) {
      request.removeHeader("X-Boss-Client-IP");
      request.addHeader("X-Boss-Client-IP", invalid);
      assertEquals("127.0.0.1", ClientIp.resolve(request, "127.0.0.1"));
    }
  }
}
