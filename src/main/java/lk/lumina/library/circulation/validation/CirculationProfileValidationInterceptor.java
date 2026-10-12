package lk.lumina.library.circulation.validation;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lk.lumina.library.model.Role;
import lk.lumina.library.repository.UserAccountRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.support.RequestContextUtils;

/** Adds validation without changing the shared profile controller used by other roles. */
final class CirculationProfileValidationInterceptor implements HandlerInterceptor {
    private final UserAccountRepository users;

    CirculationProfileValidationInterceptor(UserAccountRepository users) {
        this.users = users;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if (!"POST".equalsIgnoreCase(request.getMethod())) return true;

        if (!isCirculationStaff()) return true;

        String error = CirculationProfileValidator.validate(
                request.getParameter("firstName"),
                request.getParameter("lastName"),
                request.getParameter("email"),
                request.getParameter("phone"),
                request.getParameter("password"),
                request.getParameter("confirmPassword"));
        if (error == null) return true;

        FlashMap flash = RequestContextUtils.getOutputFlashMap(request);
        flash.put("error", error);
        RequestContextUtils.saveOutputFlashMap(request.getContextPath() + "/profile", request, response);
        response.sendRedirect(request.getContextPath() + "/profile");
        return false;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
            ModelAndView modelAndView) {
        if (!"GET".equalsIgnoreCase(request.getMethod()) || modelAndView == null) return;
        if ("profile".equals(modelAndView.getViewName()) && isCirculationStaff()) {
            modelAndView.setViewName("circulation/profile");
        } else if ("dashboard".equals(modelAndView.getViewName()) && isCirculationStaff()) {
            modelAndView.setViewName("circulation/dashboard");
        }
    }

    private boolean isCirculationStaff() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) return false;
        return users.findByEmailIgnoreCase(authentication.getName())
                .map(user -> user.getRole() == Role.CIRCULATION_STAFF)
                .orElse(false);
    }
}
