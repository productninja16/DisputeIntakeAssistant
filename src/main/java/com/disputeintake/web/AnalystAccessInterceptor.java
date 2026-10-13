package com.disputeintake.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

/** Sends anyone without an analyst session to the analyst sign-in page. */
class AnalystAccessInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if (AnalystSession.analyst(request.getSession()) == null) {
            response.sendRedirect(request.getContextPath() + "/analyst");
            return false;
        }
        return true;
    }
}
