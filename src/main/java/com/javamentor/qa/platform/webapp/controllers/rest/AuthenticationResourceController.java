package com.javamentor.qa.platform.webapp.controllers.rest;

import com.javamentor.qa.platform.models.dto.AuthenticationRequest;
import com.javamentor.qa.platform.models.dto.AuthenticationResponse;
import com.javamentor.qa.platform.models.entity.user.User;
import com.javamentor.qa.platform.security.JwtUtil;
import com.javamentor.qa.platform.service.abstracts.model.RoleService;
import com.javamentor.qa.platform.webapp.converters.UserToUserDtoConverter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Аутентификация и авторизация пользователя")
public class AuthenticationResourceController {

    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtUtil jwtUtil;
    private final RoleService roleService;



    @PostMapping("token")
    @Operation(summary = "Получение JWT токена для учетных данных пользователя")
    @ApiResponse(responseCode = "200", description = "Пользователь авторизован", content = {
            @Content(mediaType = "application/json", schema = @Schema(implementation = AuthenticationResponse.class))
    })
    @ApiResponse(responseCode = "400", description = "Неверные учетные данные", content = {
            @Content(mediaType = "application/json")
    })
    public ResponseEntity<?> createToken(@RequestBody AuthenticationRequest authenticationRequest, HttpServletRequest request) {

        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                authenticationRequest.getUsername(), authenticationRequest.getPassword()));

        final UserDetails userDetails = userDetailsService.loadUserByUsername(authenticationRequest.getUsername());
        final String token = jwtUtil.generateToken((User) userDetails, authenticationRequest.getRememberMe());

        return ResponseEntity.ok(new AuthenticationResponse(token));
    }

    @GetMapping(
            value = "check",
            produces = "text/plain")
    @Operation(summary = "Проверяет авторизован ли пользователь")
    @ApiResponse(responseCode = "200", description = "Пользователь авторизован", content = {
            @Content(mediaType = "text/plain")
    })
    @ApiResponse(responseCode = "403", description = "Пользователь не авторизован", content = {
            @Content(mediaType = "text/plain")
    })
    public ResponseEntity<String> authorizationCheck(@AuthenticationPrincipal UserDetails userDetails){

        if (userDetails == null) {
            return new ResponseEntity<>("User is not authenticated", HttpStatus.TEMPORARY_REDIRECT);
        }

        if (!(userDetails.getAuthorities().stream().anyMatch(x -> x.getAuthority().equals("ROLE_USER"))
        || userDetails.getAuthorities().stream().anyMatch(x -> x.getAuthority().equals("ROLE_ADMIN")))){
            return new ResponseEntity<>("FORBIDDEN", HttpStatus.FORBIDDEN);
        }

        return new ResponseEntity<>("User is authenticated", HttpStatus.OK);
    }
    @GetMapping(path = "/currentUser")
    @Operation(summary = "Получение текущего юзера", responses = {
            @ApiResponse(description = "Мы получили текущего юзера", responseCode = "200",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = User.class))),
            @ApiResponse(responseCode = "403", description = "текущий юзер не получен")
    })
    public ResponseEntity <Object> getCurrentUserDto() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return (principal instanceof User)
                ? ResponseEntity.ok(((User) principal))
                : ResponseEntity.status(HttpStatus.FORBIDDEN).body("User is not authorized");
    }
}
