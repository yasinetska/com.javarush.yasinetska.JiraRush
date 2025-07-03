package com.javarush.jira.profile.internal.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.javarush.jira.common.error.ErrorMessageHandler;
import com.javarush.jira.common.internal.config.RestAuthenticationEntryPoint;
import com.javarush.jira.common.internal.config.SecurityConfig;
import com.javarush.jira.login.AuthUser;
import com.javarush.jira.login.internal.UserRepository;
import com.javarush.jira.login.internal.sociallogin.CustomOAuth2UserService;
import com.javarush.jira.profile.ContactTo;
import com.javarush.jira.profile.ProfileTo;
import com.javarush.jira.profile.internal.ProfileMapper;
import com.javarush.jira.profile.internal.ProfileRepository;
import com.javarush.jira.profile.internal.model.Profile;
import com.javarush.jira.ref.RefTo;
import com.javarush.jira.ref.RefType;
import com.javarush.jira.ref.ReferenceService;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.web.client.RestTemplateAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import java.util.Map;
import java.util.Set;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProfileRestController.class)
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ImportAutoConfiguration(RestTemplateAutoConfiguration.class)
@Import({SecurityConfig.class, ErrorMessageHandler.class})
class ProfileRestControllerTest {

    private static final String REST_URL = "/api/profile";
    private static final long USER_ID = 1L;
    private static MockedStatic<ReferenceService> refMock;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProfileRepository profileRepository;
    @MockBean
    private ProfileMapper profileMapper;
    @MockBean
    private UserRepository userRepository;
    @MockBean
    private CustomOAuth2UserService customOAuth2UserService;
    @MockBean
    private RestAuthenticationEntryPoint restAuthenticationEntryPoint;


    @BeforeAll
    void enableStaticMocks() {
        refMock = Mockito.mockStatic(ReferenceService.class);
    }

    // ────────── 1.  EntryPoint  ──────────
    @BeforeEach
    void setUpEntryPoint() throws Exception {
        doAnswer(inv -> {
            HttpServletResponse resp = inv.getArgument(1, HttpServletResponse.class);
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return null;
        }).when(restAuthenticationEntryPoint)
                .commence(any(), any(), any());
    }

    // ────────── 2.  ReferenceService ─────
    @BeforeEach
    void setUpRefs() {
        refMock.reset();                       // <-- сбрасываем предыдущие стабы

        Map<String, RefTo> contactRefs = Map.of(
                "skype", new RefTo(null, RefType.CONTACT, "skype", "Skype", null)
        );
        Map<String, RefTo> mailRefs = Map.of(
                "assigned", new RefTo(null, RefType.MAIL_NOTIFICATION, "assigned",
                        "Assigned", "1"),
                "three_days_before_deadline",
                new RefTo(null, RefType.MAIL_NOTIFICATION,
                        "three_days_before_deadline", "3 days before", "2")
        );

        refMock.when(() -> ReferenceService.getRefs(RefType.CONTACT))
                .thenReturn(contactRefs);
        refMock.when(() -> ReferenceService.getRefs(RefType.MAIL_NOTIFICATION))
                .thenReturn(mailRefs);
        refMock.when(() -> ReferenceService.getRefTo(eq(RefType.CONTACT), anyString()))
                .thenAnswer(inv -> contactRefs.get(inv.getArgument(1)));
        refMock.when(() -> ReferenceService.getRefTo(eq(RefType.MAIL_NOTIFICATION), anyString()))
                .thenAnswer(inv -> mailRefs.get(inv.getArgument(1)));
    }

    @AfterAll
    void closeStaticMock() {
        refMock.close();
    }

    @Test
    @DisplayName("GET /api/profile — 200 OK + тело")
    void get_authenticated_success() throws Exception {
        //   given
        ProfileTo dto = new ProfileTo(USER_ID, Set.of(), Set.of());
        when(profileMapper.toTo(any())).thenReturn(dto);
        AuthUser principal = mock(AuthUser.class);
        when(principal.id()).thenReturn(USER_ID);

        //   when / then
        mockMvc.perform(get(REST_URL).with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(USER_ID));
    }

    @Test
    @DisplayName("GET /api/profile — 403 Forbidden без логина")
    void get_unauthenticated_forbidden() throws Exception {
        mockMvc.perform(get(REST_URL))
                .andExpect(status().isForbidden());
    }


    @Test
    @DisplayName("PUT /api/profile — 204 No Content при валидных данных")
    void update_validData_success() throws Exception {
        ContactTo skype = new ContactTo("skype", "user_skype");
        ProfileTo body  = new ProfileTo(
                USER_ID,
                Set.of("assigned", "three_days_before_deadline"),
                Set.of(skype)
        );

        Profile stub = new Profile(USER_ID);
        when(profileRepository.getOrCreate(USER_ID)).thenReturn(stub);
        when(profileMapper.updateFromTo(any(Profile.class), any(ProfileTo.class))).thenReturn(stub);
        when(profileRepository.save(any(Profile.class))).thenReturn(stub);

        AuthUser principal = mock(AuthUser.class);
        when(principal.id()).thenReturn(USER_ID);

        mockMvc.perform(put(REST_URL)
                        .with(user(principal))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(body)))
                .andExpect(status().isNoContent());

        verify(profileRepository).save(stub);
    }

    @Test
    @DisplayName("PUT /api/profile --- 422 Unprocessable Entity: неразрешённый код контакта")
    void update_invalidContactCode_badRequest() throws Exception {
        ContactTo invalid = new ContactTo("bad_code", "val");
        ProfileTo body = new ProfileTo(
                USER_ID,
                Set.of("assigned"),
                Set.of(invalid));

        refMock.when(() -> ReferenceService.getRefTo(RefType.CONTACT, "bad_code"))
                .thenThrow(new IllegalArgumentException("Invalid contact code: bad_code"));

        AuthUser principal = mock(AuthUser.class);
        when(principal.id()).thenReturn(USER_ID);

        mockMvc.perform(put(REST_URL)
                        .with(user(principal))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(body)))
                .andExpect(status().isUnprocessableEntity());
    }



    @Test
    @DisplayName("PUT /api/profile — 422 BadRequest: пустое значение контакта")
    void update_emptyContactValue_badRequest() throws Exception {
        ContactTo empty = new ContactTo("skype", "");
        ProfileTo body  = new ProfileTo(
                USER_ID,
                Set.of("assigned"),
                Set.of(empty));

        AuthUser principal = mock(AuthUser.class);
        when(principal.id()).thenReturn(USER_ID);

        mockMvc.perform(put(REST_URL)
                        .with(user(principal))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(body)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("PUT /api/profile — 403 Forbidden без логина")
    void update_unauthenticated_forbidden() throws Exception {
        ProfileTo body = new ProfileTo(USER_ID, Set.of("assigned"), Set.of());

        mockMvc.perform(put(REST_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(body)))
                .andExpect(status().isForbidden());
    }
}

