/**
 * TermIt Copyright (C) 2019 Czech Technical University in Prague
 * <p>
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cz.cvut.kbss.termit.rest;

import com.fasterxml.jackson.core.type.TypeReference;
import cz.cvut.kbss.termit.environment.Environment;
import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.model.UserAccount;
import cz.cvut.kbss.termit.service.business.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserControllerTest extends BaseControllerTestRunner {

    private static final String BASE_URL = "/users";

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController sut;

    @BeforeEach
    void setUp() {
        super.setUp(sut);
        UserAccount user = Generator.generateUserAccount();
        Environment.setCurrentUser(user);
    }

    @Test
    void getAllReturnsAllUsers() throws Exception {
        final List<UserAccount> users = IntStream.range(0, 5).mapToObj(i -> Generator.generateUserAccount())
                .collect(Collectors.toList());
        when(userService.findAll()).thenReturn(users);

        final MvcResult mvcResult = mockMvc.perform(get(BASE_URL).accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk()).andReturn();
        final List<UserAccount> result = readValue(mvcResult, new TypeReference<List<UserAccount>>() {
        });
        assertEquals(users, result);
    }
}
