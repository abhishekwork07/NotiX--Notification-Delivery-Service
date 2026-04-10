package com.abhishek.notix.api_service.config;

import com.abhishek.notix.api_service.security.ApiKeyAuthFilter;
import com.abhishek.notix.api_service.security.RateLimitingFilter;
import com.abhishek.notix.api_service.v2.security.V2AuthFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SecurityConfig {

    @Bean
    public FilterRegistrationBean<ApiKeyAuthFilter> apiKeyFilter(ApiKeyAuthFilter filter) {
        FilterRegistrationBean<ApiKeyAuthFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(filter);
        registrationBean.addUrlPatterns("/notifications/*", "/test/*"); // secure these routes
        registrationBean.setOrder(1);
        return registrationBean;
    }

    @Bean
    public FilterRegistrationBean<RateLimitingFilter> rateLimiter(RateLimitingFilter filter) {
        FilterRegistrationBean<RateLimitingFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(filter);
        registrationBean.addUrlPatterns("/notifications/*", "/v2/notifications", "/v2/notifications/*", "/v2/schedules");
        registrationBean.setOrder(2); // after API key filter
        return registrationBean;
    }

    @Bean
    public FilterRegistrationBean<V2AuthFilter> v2AuthFilterRegistration(V2AuthFilter filter) {
        FilterRegistrationBean<V2AuthFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(filter);
        registrationBean.addUrlPatterns("/v2/*");
        registrationBean.setOrder(1);
        return registrationBean;
    }

}
