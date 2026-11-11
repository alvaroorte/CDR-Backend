package bo.cdr.app.appsecurity.presentation.controller;

import bo.cdr.app.appsecurity.domain.service.interfaces.PermissionService;
import bo.cdr.app.appsecurity.domain.service.interfaces.RoleService;
import bo.cdr.app.appsecurity.domain.service.interfaces.UserService;
import bo.cdr.app.appsecurity.presentation.request.dto.RoleDto;
import bo.cdr.app.appsecurity.presentation.request.dto.UserAppDto;
import bo.cdr.app.appsecurity.presentation.response.pojo.PermissionPojo;
import bo.cdr.app.appsecurity.presentation.response.pojo.RolePojo;
import bo.cdr.app.appsecurity.presentation.response.pojo.UserAppPojo;
import bo.cdr.app.exception.DuplicateException;
import bo.cdr.app.exception.NotAllowedException;
import bo.cdr.app.util.MockMvcTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class SecurityAdminControllersTest {

    @Nested
    class UserControllerCrudTest {
        @Mock UserService userService;
        MockMvc mockMvc;

        private static final String VALID_JSON = """
                { "username": "maria.caja", "fullName": "María Caja", "password": "Caja2026!", "roleIds": [2], "active": true }
                """;

        @BeforeEach
        void setUp() { mockMvc = MockMvcTestSupport.buildMockMvc(new UserController(userService)); }

        @Test
        void getAll_returns200() throws Exception {
            when(userService.getAllUserAppPojo()).thenReturn(List.of(new UserAppPojo()));
            mockMvc.perform(get("/users")).andExpect(status().isOk());
        }

        @Test
        void create_returns201() throws Exception {
            when(userService.create(any(UserAppDto.class))).thenReturn(new UserAppPojo());
            mockMvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON).content(VALID_JSON))
                    .andExpect(status().isCreated());
        }

        @Test
        void create_returns400_whenUsernameHasSpaces() throws Exception {
            mockMvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON)
                    .content(VALID_JSON.replace("maria.caja", "maria caja"))).andExpect(status().isBadRequest());
        }

        @Test
        void create_returns400_whenNoRoles() throws Exception {
            mockMvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON)
                    .content(VALID_JSON.replace("[2]", "[]"))).andExpect(status().isBadRequest());
        }

        @Test
        void create_returns400_whenPasswordTooShort() throws Exception {
            mockMvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON)
                    .content(VALID_JSON.replace("Caja2026!", "corta"))).andExpect(status().isBadRequest());
        }

        @Test
        void create_returns409_whenUsernameTaken() throws Exception {
            when(userService.create(any(UserAppDto.class))).thenThrow(new DuplicateException("Usuario", "Nombre de usuario", "maria.caja"));
            mockMvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON).content(VALID_JSON))
                    .andExpect(status().isConflict());
        }

        @Test
        void update_returns200() throws Exception {
            when(userService.update(eq(2), any(UserAppDto.class))).thenReturn(new UserAppPojo());
            mockMvc.perform(put("/users/2").contentType(MediaType.APPLICATION_JSON).content(VALID_JSON))
                    .andExpect(status().isOk());
        }

        @Test
        void resetPassword_returns204() throws Exception {
            mockMvc.perform(patch("/users/2/password").contentType(MediaType.APPLICATION_JSON)
                    .content("{ \"password\": \"NuevaClave1\" }")).andExpect(status().isNoContent());
            verify(userService).resetPassword(2, "NuevaClave1");
        }

        @Test
        void delete_returns400_whenDeactivatingSelf() throws Exception {
            doThrow(new NotAllowedException("No puede desactivar su propio usuario.")).when(userService).softDelete(1);
            mockMvc.perform(delete("/users/1")).andExpect(status().isBadRequest());
        }
    }

    @Nested
    class RoleControllerTest {
        @Mock RoleService roleService;
        MockMvc mockMvc;

        @BeforeEach
        void setUp() { mockMvc = MockMvcTestSupport.buildMockMvc(new RoleController(roleService)); }

        @Test
        void getAll_returns200() throws Exception {
            when(roleService.getAllRolePojo()).thenReturn(List.of(new RolePojo(1, "Administrador", "ADMIN")));
            mockMvc.perform(get("/roles")).andExpect(status().isOk()).andExpect(jsonPath("$[0].code").value("ADMIN"));
        }

        @Test
        void create_returns201() throws Exception {
            when(roleService.create(any(RoleDto.class))).thenReturn(new RolePojo());
            mockMvc.perform(post("/roles").contentType(MediaType.APPLICATION_JSON)
                    .content("{ \"name\": \"Mesero\", \"code\": \"MESERO\", \"active\": true }")).andExpect(status().isCreated());
        }

        @Test
        void create_returns400_whenCodeInvalid() throws Exception {
            mockMvc.perform(post("/roles").contentType(MediaType.APPLICATION_JSON)
                    .content("{ \"name\": \"Mesero\", \"code\": \"ME-1\", \"active\": true }")).andExpect(status().isBadRequest());
        }

        @Test
        void delete_returns400_whenSystemRole() throws Exception {
            doThrow(new NotAllowedException("Rol del sistema")).when(roleService).softDelete(2);
            mockMvc.perform(delete("/roles/2")).andExpect(status().isBadRequest());
        }

        @Test
        void replacePermissions_returns200() throws Exception {
            when(roleService.replacePermissions(eq(2), any())).thenReturn(List.of(5, 6));
            mockMvc.perform(put("/roles/2/permissions").contentType(MediaType.APPLICATION_JSON)
                    .content("{ \"permissionIds\": [5, 6] }"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$[1]").value(6));
        }

        @Test
        void replacePermissions_returns400_whenListMissing() throws Exception {
            mockMvc.perform(put("/roles/2/permissions").contentType(MediaType.APPLICATION_JSON)
                    .content("{ \"permissionIds\": null }")).andExpect(status().isBadRequest());
        }
    }

    @Nested
    class PermissionControllerTest {
        @Mock PermissionService permissionService;
        MockMvc mockMvc;

        @BeforeEach
        void setUp() { mockMvc = MockMvcTestSupport.buildMockMvc(new PermissionController(permissionService)); }

        @Test
        void getAll_returns200() throws Exception {
            when(permissionService.getAllActive()).thenReturn(List.of(
                    new PermissionPojo(1, "GET__PRODUCT", "GET /product", "Permiso", "GET", "/product")));
            mockMvc.perform(get("/permission")).andExpect(status().isOk()).andExpect(jsonPath("$[0].code").value("GET__PRODUCT"));
        }
    }
}
