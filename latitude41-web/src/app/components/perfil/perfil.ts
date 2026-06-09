import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AtualizarPerfilPayload, UtilizadorService } from '../../services/utilizador.service';
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
  styleUrl: './perfil.css',
})
export class Perfil implements OnInit {
  private readonly router = inject(Router);
  private readonly utilizadorService = inject(UtilizadorService);

  public user: AuthenticatedUser | null = null;
  public isSaving = false;

  public nomeInput = '';
  public emailInput = '';
  public passwordInput = '';

  public ngOnInit(): void {
    const storedUser = localStorage.getItem('user');

    if (!storedUser) {
      this.router.navigate(['/login']);
      return;
    }

    try {
      this.user = JSON.parse(storedUser) as AuthenticatedUser;
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

    if (!this.user) {
      return;
    }

    this.isSaving = true;

    const payload: AtualizarPerfilPayload = {
      nome: this.nomeInput.trim(),
      email: this.emailInput.trim(),
      tipo: this.user.tipo,
      estadoConta: this.user.estadoConta,
    };

    if (this.passwordInput.trim()) {
      payload.password = this.passwordInput;
    }

    this.utilizadorService.atualizarPerfil(this.user.id, payload).subscribe({
      next: (utilizadorAtualizado) => {
        localStorage.setItem('user', JSON.stringify(utilizadorAtualizado));
        this.user = utilizadorAtualizado;
        this.passwordInput = '';
        this.isSaving = false;
        alert('Perfil atualizado com sucesso!');
      },
      error: (erro) => {
        this.isSaving = false;
        alert(this.mensagemErroGuardar(erro));
      },
    });
  }

  public logout(): void {
    localStorage.clear();
    this.router.navigate(['/login']);
  }

  public estadoPerfilLabel(): string {
    if (this.isAdmin()) {
      return 'Admin';
    }

    if (this.isFuncionario()) {
      return 'Conta ativa (funcion\u00e1rio)';
    }

    return 'Conta ativa (cliente)';
  }

  public estadoPerfilClass(): string {
    if (this.isAdmin()) {
      return 'status-dot--admin';
    }

    return this.isFuncionario() ? 'status-dot--employee' : 'status-dot--client';
  }

  public tipoUtilizadorLabel(): string {
    const tipo = this.normalizar(this.user?.tipo);

    if (tipo === 'ADMIN' || tipo === 'ADMINISTRADOR') {
      return 'Administrador';
    }

    if (tipo === 'FUNCIONARIO') {
      return 'Funcionario';
    }

    return 'Cliente';
  }

  private isAdmin(): boolean {
    const tipo = this.normalizar(this.user?.tipo);
    return tipo === 'ADMIN' || tipo === 'ADMINISTRADOR';
  }

  private isFuncionario(): boolean {
    return this.normalizar(this.user?.tipo) === 'FUNCIONARIO';
  }

  private mensagemErroGuardar(erro: unknown): string {
    if (erro instanceof HttpErrorResponse) {
      if (erro.status === 0) {
        return 'Nao foi possivel contactar a API em http://localhost:8080. Confirme que o backend latitude41-api esta a correr.';
      }

      if (erro.status === 404) {
        return 'Utilizador nao encontrado.';
      }
    }

    return 'Nao foi possivel atualizar o perfil. Tente novamente.';
  }

  private normalizar(valor: string | null | undefined): string {
    return (valor ?? '')
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .trim()
      .toUpperCase();
  }
}
