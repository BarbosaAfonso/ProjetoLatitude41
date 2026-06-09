import { Component, inject } from '@angular/core';
import { AuthService } from '../../services/auth';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';

@Component({
  selector: 'app-login',
  imports: [FormsModule],
  templateUrl: './login.html',
  styleUrl: './login.css'

})
export class Login {
  private authService = inject(AuthService);
  private router = inject(Router);

  // Controlo de estado do ecra
  isLoginMode = true;

  // Campos do formulario
  nome = '';
  email = '';
  password = '';

  // Alterna entre os modos de login e criar conta
  alterarModo() {
    this.isLoginMode = !this.isLoginMode;
  }

  // Funcao central submetida pelo botao principal
  submeter() {
    if (this.isLoginMode) {
      this.executarLogin();
    } else {
      this.executarRegisto();
    }
  }

  private executarLogin() {
    if (!this.email || !this.password) {
      alert('Preencha email e palavra-passe.');
      return;
    }

    const credenciais = { email: this.email.trim(), password: this.password };

    this.authService.login(credenciais).subscribe({
      next: (resposta) => {
        localStorage.setItem('user', JSON.stringify(resposta));
        this.router.navigate(['/home']);
      },
      error: (erro) => {
        alert(this.mensagemErroLogin(erro));
      }
    });
  }

  private executarRegisto() {
    if (!this.nome || !this.email || !this.password) {
      alert('Por favor, preencha todos os campos!');
      return;
    }

    const dadosRegisto = { nome: this.nome.trim(), email: this.email.trim(), password: this.password };

    this.authService.registar(dadosRegisto).subscribe({
      next: (resposta) => {
        alert('Conta criada com sucesso! Faca login para entrar.');
        this.isLoginMode = true;
        this.password = '';
      },
      error: (erro) => {
        alert(this.mensagemErroRegisto(erro));
      }
    });
  }

  private mensagemErroLogin(erro: unknown): string {
    if (erro instanceof HttpErrorResponse) {
      if (erro.status === 0) {
        return 'Nao foi possivel contactar a API em http://localhost:8080. Confirme que o backend latitude41-api esta a correr.';
      }

      if (erro.status === 401) {
        return 'Credenciais invalidas.';
      }
    }

    return 'Nao foi possivel validar o login. Tente novamente.';
  }

  private mensagemErroRegisto(erro: unknown): string {
    if (erro instanceof HttpErrorResponse && erro.status === 0) {
      return 'Nao foi possivel contactar a API em http://localhost:8080. Confirme que o backend latitude41-api esta a correr.';
    }

    return 'Erro ao criar conta. O email ja pode estar em uso.';
  }
}
