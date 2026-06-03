package com.example.gestao_restaurante.Views;

import com.example.gestao_restaurante.GestaoRestauranteApplication;
import javafx.application.Application;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

public class JavaFXLauncher {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = null;
        try {
            context = SpringApplication.run(GestaoRestauranteApplication.class, args);
            JavaFXApp.setOnStopCallback(context::close);
        } catch (Exception e) {
            System.err.println("Nao foi possivel iniciar automaticamente o backend Spring Boot.");
            System.err.println("Inicia o backend manualmente e volta a tentar login. Detalhe: " + e.getMessage());
        }

        Application.launch(JavaFXApp.class, args);
    }
}
