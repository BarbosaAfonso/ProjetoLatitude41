import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Navbar } from '../navbar/navbar';

interface AuthenticatedUser {
  id: number;
  nome: string;
  email: string;
  tipo: string;
  estadoConta: string;
}

@Component({
  selector: 'app-perfil',
  standalone: true,
  imports: [CommonModule, FormsModule, Navbar],
  templateUrl: './perfil.html',
})
export class Perfil implements OnInit {
  private readonly router = inject(Router);

  public user: AuthenticatedUser | null = null;
  public isSaving = false;

  // Campos do formulário (ligados via ngModel)
  public nomeInput = '';
  public emailInput = '';

  public ngOnInit(): void {
    const storedUser = localStorage.getItem('user');

    if (!storedUser) {
      this.router.navigate(['/login']);
      return;
    }

    try {
      this.user = JSON.parse(storedUser) as AuthenticatedUser;

      // Inicializa os inputs com os dados guardados na sessão atual
      this.nomeInput = this.user.nome;
      this.emailInput = this.user.email;
    } catch {
      localStorage.removeItem('user');
      this.router.navigate(['/login']);
    }
  }

  public guardarAlteracoes(): void {
    if (!this.nomeInput.trim() || !this.emailInput.trim()) {
      alert('Por favor, preencha o seu nome e email.');
      return;
    }

    if (this.user) {
      this.isSaving = true;

      // 1. Cria o objeto atualizado mantendo o ID, Tipo e Estado originais
      const utilizadorAtualizado: AuthenticatedUser = {
        ...this.user,
        nome: this.nomeInput.trim(),
        email: this.emailInput.trim()
      };

      // 2. Simula o tempo de resposta do servidor (depois ligas isto com o HTTP PUT da tua API)
      setTimeout(() => {
        localStorage.setItem('user', JSON.stringify(utilizadorAtualizado));
        this.user = utilizadorAtualizado;
        this.isSaving = false;
        alert('Perfil atualizado com sucesso!');

        // Atualiza a página para que o novo nome apareça logo na Navbar lá em cima
        window.location.reload();
      }, 700);
    }
  }

  public logout(): void {
    localStorage.clear();
    this.router.navigate(['/login']);
  }
}
