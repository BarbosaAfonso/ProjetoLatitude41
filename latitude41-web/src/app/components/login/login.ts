import { Component, inject } from '@angular/core';
import { AuthService } from '../../services/auth';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';

@Component({
  selector: 'app-login',
  imports: [FormsModule],
  templateUrl: './login.html',
  styleUrl: './login.css'

})
export class Login {
  private authService = inject(AuthService);
  private router = inject(Router);

  // Controlo de estado do ecrã
  isLoginMode = true;

  // Campos do formulário
  nome = '';
  email = '';
  password = '';

  // Alterna entre os modos de Login e Criar Conta
  alterarModo() {
    this.isLoginMode = !this.isLoginMode;
  }

  // Função central submetida pelo botão principal
  submeter() {
    if (this.isLoginMode) {
      this.executarLogin();
    } else {
      this.executarRegisto();
    }
  }

  private executarLogin() {
    const credenciais = { email: this.email, password: this.password };

    this.authService.login(credenciais).subscribe({
      next: (resposta) => {
        localStorage.setItem('user', JSON.stringify(resposta));
        this.router.navigate(['/home']);
      },
      error: (erro) => {
        alert('Credenciais inválidas!');
      }
    });
  }

  private executarRegisto() {
    if (!this.nome || !this.email || !this.password) {
      alert('Por favor, preencha todos os campos!');
      return;
    }

    const dadosRegisto = { nome: this.nome, email: this.email, password: this.password };

    this.authService.registar(dadosRegisto).subscribe({
      next: (resposta) => {
        alert('Conta criada com sucesso! Faça login para entrar.');
        this.isLoginMode = true; // Volta ao modo login automaticamente
        this.password = '';      // Limpa a password por segurança
      },
      error: (erro) => {
        alert('Erro ao criar conta. O email já pode estar em uso.');
      }
    });
  }
}
