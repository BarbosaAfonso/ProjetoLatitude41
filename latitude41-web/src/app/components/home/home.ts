import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { Router } from '@angular/router';

interface AuthenticatedUser {
  id: number;
  nome: string;
  email: string;
  tipo: string;
  estadoConta: string;
}

@Component({
  selector: 'app-home',
  imports: [CommonModule],
  templateUrl: './home.html',
  styleUrl: './home.css',
})
export class Home implements OnInit {
  private readonly router = inject(Router);

  public user: AuthenticatedUser | null = null;

  public readonly currentDate = new Intl.DateTimeFormat('pt-PT', {
    weekday: 'long',
    day: '2-digit',
    month: 'long',
    year: 'numeric',
  }).format(new Date());

  public ngOnInit(): void {
    const storedUser = localStorage.getItem('user');

    if (!storedUser) {
      this.router.navigate(['/login']);
      return;
    }

    try {
      const parsedUser = JSON.parse(storedUser) as Partial<AuthenticatedUser>;

      if (!this.isAuthenticatedUser(parsedUser)) {
        this.clearSessionAndRedirect();
        return;
      }

      this.user = parsedUser;
    } catch {
      this.clearSessionAndRedirect();
    }
  }

  public logout(): void {
    localStorage.clear();
    this.router.navigate(['/login']);
  }

  private isAuthenticatedUser(user: Partial<AuthenticatedUser>): user is AuthenticatedUser {
    return (
      typeof user.id === 'number' &&
      typeof user.nome === 'string' &&
      typeof user.email === 'string' &&
      typeof user.tipo === 'string' &&
      typeof user.estadoConta === 'string'
    );
  }

  private clearSessionAndRedirect(): void {
    localStorage.removeItem('user');
    this.router.navigate(['/login']);
  }
}
