package com.cmpt276.project;

import com.cmpt276.project.owner.controller.PublicSalonController;
import com.cmpt276.project.owner.model.Salon;
import com.cmpt276.project.owner.service.OwnerService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class PublicSalonControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OwnerService ownerService;

    @Test
    void shouldShowPublicSalonPage() throws Exception {

        Salon salon = mock(Salon.class);

        when(ownerService.findPublicSalonBySlug("magiccuts-salon"))
                .thenReturn(salon);

        mockMvc.perform(get("/salons/magiccuts-salon"))
                .andExpect(status().isOk())
                .andExpect(view().name("customer/public-salon"))
                .andExpect(model().attributeExists("salon"))
                .andExpect(model().attributeExists("days"));
    }
}