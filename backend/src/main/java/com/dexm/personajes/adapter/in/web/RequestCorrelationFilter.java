package com.dexm.personajes.adapter.in.web;
import jakarta.servlet.*; import jakarta.servlet.http.*; import org.slf4j.MDC; import org.springframework.web.filter.OncePerRequestFilter; import java.io.IOException; import java.util.UUID;
public final class RequestCorrelationFilter extends OncePerRequestFilter {
 public static final String HEADER="X-Request-Id", MDC_KEY="requestId", ATTRIBUTE="dexm.requestId";
 protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)throws ServletException,IOException { String id=request.getHeader(HEADER); if(id==null||!id.matches("[A-Za-z0-9._:-]{1,128}"))id=UUID.randomUUID().toString(); request.setAttribute(ATTRIBUTE,id); response.setHeader(HEADER,id); try(MDC.MDCCloseable ignored=MDC.putCloseable(MDC_KEY,id)){chain.doFilter(request,response);} finally{MDC.remove(MDC_KEY);} }
}
