package com.wildcodeschool.myProjectWithSecurity.config;

import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

@EnableWebSecurity
public class MySecurityConfig extends WebSecurityConfigurerAdapter {

	@Override
	protected void configure(HttpSecurity http) throws Exception {
	    http
	        .authorizeRequests()
	            .antMatchers("/avengers/assemble").hasRole("HERO")
	            .antMatchers("/secret-bases").hasRole("DIRECTOR")
	            .and()
	        .formLogin()
	            .and()
	        .httpBasic();
	}
	
	@Override
	protected void configure(AuthenticationManagerBuilder auth) throws Exception {
		PasswordEncoder encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
		auth.inMemoryAuthentication()
	    .withUser("steve")
	        .password(encoder.encode("guy"))
	        .roles("HERO")
	        .and()
	    .withUser("tony")
	        .password(encoder.encode("stark"))
	        .roles("HERO")
	        .and()
	    .withUser("nick")
	        .password(encoder.encode("fury"))
	        .roles("DIRECTOR");

	}

}

