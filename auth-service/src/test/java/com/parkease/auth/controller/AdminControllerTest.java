package com.parkease.auth.controller;

import com.parkease.auth.entity.AuthProvider;
import com.parkease.auth.entity.Role;
import com.parkease.auth.entity.User;
import com.parkease.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Mock private UserRepository userRepository;
    @InjectMocks private AdminController controller;

    private MockMvc mvc;
    private User user;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(controller).build();

        user = User.builder()
                .id(1L).fullName("Test User").email("test@test.com")
                .password("encoded").role(Role.DRIVER)
                .provider(AuthProvider.LOCAL).active(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void getAllUsers_shouldReturn200() throws Exception {
        when(userRepository.findAll()).thenReturn(List.of(user));

        mvc.perform(get("/api/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("test@test.com"));
    }

    @Test
    void getUserById_shouldReturn200() throws Exception {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        mvc.perform(get("/api/admin/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@test.com"));
    }


    @Test
    void suspendUser_shouldSetActiveToFalse() throws Exception {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenReturn(user);

        mvc.perform(put("/api/admin/users/1/suspend"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(userRepository).save(user);
    }

    @Test
    void activateUser_shouldSetActiveToTrue() throws Exception {
        user.setActive(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenReturn(user);

        mvc.perform(put("/api/admin/users/1/activate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(userRepository).save(user);
    }

    @Test
    void deleteUser_shouldReturn200() throws Exception {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        doNothing().when(userRepository).deleteById(1L);

        mvc.perform(delete("/api/admin/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(userRepository).deleteById(1L);
    }

    @Test
    void getUsersByRole_shouldReturn200() throws Exception {
        when(userRepository.findAllByRole(Role.DRIVER)).thenReturn(List.of(user));

        mvc.perform(get("/api/admin/users/role/DRIVER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].role").value("DRIVER"));
    }
}
