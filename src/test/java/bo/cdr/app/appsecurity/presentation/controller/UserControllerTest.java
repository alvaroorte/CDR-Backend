package bo.cdr.app.appsecurity.presentation.controller;

import bo.cdr.app.appsecurity.domain.service.interfaces.UserService;
import bo.cdr.app.appsecurity.presentation.response.pojo.RolePojo;
import bo.cdr.app.appsecurity.presentation.response.pojo.UserInfoPojo;
import bo.cdr.app.exception.EntityNotFoundException;
import bo.cdr.app.util.MockMvcTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    UserService userService;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcTestSupport.buildMockMvc(new UserController(userService));
    }

    @Test
    void getMe_returns200_withRoles() throws Exception {
        UserInfoPojo info = new UserInfoPojo(7, "maria.caja", "María Caja", null);
        info.setRoles(List.of(new RolePojo(2, "Cajero", "CAJERO")));
        when(userService.getCurrentUserInfo()).thenReturn(info);

        mockMvc.perform(get("/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("maria.caja"))
                .andExpect(jsonPath("$.roles[0].code").value("CAJERO"));
    }

    @Test
    void getMe_returns404_whenUserNoLongerExists() throws Exception {
        when(userService.getCurrentUserInfo()).thenThrow(new EntityNotFoundException("Usuario", 7));

        mockMvc.perform(get("/users/me")).andExpect(status().isNotFound());
    }
}
