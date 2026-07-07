package com.example.gestao_restaurante.Dtos;

import com.example.gestao_restaurante.Modules.Utilizador;

public class LoginResponse {
    private String token;
    private Utilizador utilizador;

    public LoginResponse(String token, Utilizador utilizador) {
        this.token = token;
        this.utilizador = utilizador;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Utilizador getUtilizador() {
        return utilizador;
    }

    public void setUtilizador(Utilizador utilizador) {
        this.utilizador = utilizador;
    }
}
