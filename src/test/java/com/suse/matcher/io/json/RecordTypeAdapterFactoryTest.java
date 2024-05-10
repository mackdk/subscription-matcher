/**
 * Copyright (c) 2026 SUSE LLC
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *    * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *    * Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the
 * distribution.
 *    * Neither the name of SUSE LLC nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.suse.matcher.io.json;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.junit.jupiter.api.Test;

class RecordTypeAdapterFactoryTest {

    @Test
    void canSerializeSimpleRecord() {
        Gson gson = new GsonBuilder()
            .registerTypeAdapterFactory(new RecordTypeAdapterFactory())
            .serializeNulls()
            .create();

        assertAll(
            () -> assertEquals(
                "{\"name\":\"Charlie\",\"age\":35,\"nationality\":\"Canadian\"}",
                gson.toJson(new SimpleTestRecord("Charlie", 35, "Canadian"))
            ),
            () -> assertEquals(
                "{\"name\":\"Diana\",\"age\":28,\"nationality\":null}",
                gson.toJson(new SimpleTestRecord("Diana", 28, null))
            ),
            () -> assertEquals(
                "{\"name\":\"\",\"age\":0,\"nationality\":null}",
                gson.toJson(new SimpleTestRecord("", 0, null))
            )
        );
    }

    @Test
    void canDeserializeSimpleRecord() {
        Gson gson = new GsonBuilder()
            .registerTypeAdapterFactory(new RecordTypeAdapterFactory())
            .serializeNulls()
            .create();

        assertAll(
            () -> assertEquals(
                new SimpleTestRecord("Eve", 40, "Japanese"),
                gson.fromJson("{\"name\":\"Eve\",\"age\":40,\"nationality\":\"Japanese\"}", SimpleTestRecord.class)
            ),
            () -> assertEquals(
                new SimpleTestRecord("Alice", 30, null),
                gson.fromJson("{\"name\":\"Alice\",\"age\":30,\"nationality\":null}", SimpleTestRecord.class)
            ),
            () -> assertEquals(
                new SimpleTestRecord("", 21, null),
                gson.fromJson("{\"name\":\"\",\"age\":21,\"nationality\":null}", SimpleTestRecord.class)
            )
        );
    }

    @Test
    void canSerializeRecordWithLowerCaseWithUnderscoresNamingPolicy() {
        Gson gson = new GsonBuilder()
            .registerTypeAdapterFactory(new RecordTypeAdapterFactory())
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .serializeNulls()
            .create();

        assertAll(
            () -> assertEquals(
                "{\"first_name\":\"Charlie\",\"last_name\":\"Brown\",\"years_of_experience\":10}",
                gson.toJson(new NamingPolicyTestRecord("Charlie", "Brown", 10))
            ),
            () -> assertEquals(
                "{\"first_name\":\"Diana\",\"last_name\":null,\"years_of_experience\":0}",
                gson.toJson(new NamingPolicyTestRecord("Diana", null, 0))
            )
        );
    }

    @Test
    void canDeserializeRecordWithLowerCaseWithUnderscoresNamingPolicy() {
        Gson gson = new GsonBuilder()
            .registerTypeAdapterFactory(new RecordTypeAdapterFactory())
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .serializeNulls()
            .create();

        assertAll(
            () -> assertEquals(
                new NamingPolicyTestRecord("Eve", "Adams", 5),
                gson.fromJson(
                    "{\"first_name\":\"Eve\",\"last_name\":\"Adams\",\"years_of_experience\":5}",
                    NamingPolicyTestRecord.class
                )
            ),
            () -> assertEquals(
                new NamingPolicyTestRecord("Alice", null, 0),
                gson.fromJson(
                    "{\"first_name\":\"Alice\",\"last_name\":null,\"years_of_experience\":0}",
                    NamingPolicyTestRecord.class
                )
            )
        );
    }

    record SimpleTestRecord(String name, int age, String nationality) { }

    record NamingPolicyTestRecord(String firstName, String lastName, int yearsOfExperience) { }
}
