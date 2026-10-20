package com.example.is.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.NoHandlerFoundException;

@Controller
public class SpaController {
    @GetMapping(value = {"/{path:[^\\.]*}", "/**/{path:[^\\.]*}"})
    public String forward(HttpServletRequest request) throws NoHandlerFoundException {
        String uri = request.getRequestURI();
        if (uri.startsWith("/api/")) {
            throw new NoHandlerFoundException("GET", uri, null);
        }
        return "forward:/index.html";
    }
}