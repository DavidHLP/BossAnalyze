package com.david.hlp.web.system.auth.filter;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.http.conn.util.InetAddressUtils;
import java.util.Arrays;

/** For access logging only; forwarded addresses never grant permissions. */
public final class ClientIp {
  private ClientIp() { }

  public static String resolve(HttpServletRequest request, String trustedPeers) {
    String peer = request.getRemoteAddr();
    String forwarded = request.getHeader("X-Boss-Client-IP");
    if (trustedPeers != null && Arrays.stream(trustedPeers.split(",")).map(String::trim).anyMatch(peer::equals)
        && forwarded != null && !forwarded.contains("%")
        && (InetAddressUtils.isIPv4Address(forwarded) || InetAddressUtils.isIPv6Address(forwarded))) return forwarded;
    return peer;
  }
}
