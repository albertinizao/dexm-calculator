package com.dexm.personajes.application;
import org.springframework.context.annotation.*;
@Configuration public class JacksonConfiguration { @Bean public com.fasterxml.jackson.databind.ObjectMapper legacyObjectMapper(){return new com.fasterxml.jackson.databind.ObjectMapper();} }
