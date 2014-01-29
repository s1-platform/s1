package org.s1.misc;

import javax.servlet.*;
import java.io.IOException;

/**
 * s1v2
 * User: GPykhov
 * Date: 11.01.14
 * Time: 13:06
 */
public class CharsetEncodingFilter implements Filter {

    private String encoding = "UTF-8";

    public void doFilter(ServletRequest request,

                         ServletResponse response, FilterChain filterChain) throws IOException, ServletException {
        request.setCharacterEncoding(encoding);
        filterChain.doFilter(request, response);
    }

    public void init(FilterConfig filterConfig) throws ServletException {
        String encodingParam = filterConfig.getInitParameter("encoding");
        if (encodingParam != null) {
            encoding = encodingParam;
        }
    }

    public void destroy() {
        // nothing todo
    }
}
