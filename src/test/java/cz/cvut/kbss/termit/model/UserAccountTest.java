/**
 * TermIt
 * Copyright (C) 2019 Czech Technical University in Prague
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cz.cvut.kbss.termit.model;

import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.util.Vocabulary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class UserAccountTest {

    private UserAccount sut;

    @BeforeEach
    void setUp() {
        this.sut = Generator.generateUserAccount();
    }

    @Test
    void toUserReturnsUserWithIdenticalAttributes() {
        final UserAccount ua = Generator.generateUserAccount();
        ua.setTypes(Collections.singleton(Vocabulary.s_c_administrator_termitu));

        final User result = ua.toUser();
        assertAll(() -> assertEquals(ua.getUri(), result.getUri()),
                () -> assertEquals(ua.getFirstName(), result.getFirstName()),
                () -> assertEquals(ua.getLastName(), result.getLastName()),
                () -> assertEquals(ua.getUsername(), result.getUsername()),
                () -> assertEquals(ua.getTypes(), result.getTypes()));
    }

    @Test
    void removeTypeHandlesNullTypesAttribute() {
        sut.removeType(Vocabulary.s_c_administrator_termitu);
    }

    @Test
    void isAdminReturnsTrueForAdmin() {
        assertFalse(sut.isAdmin());
        sut.addType(Vocabulary.s_c_administrator_termitu);
        assertTrue(sut.isAdmin());
    }
}
