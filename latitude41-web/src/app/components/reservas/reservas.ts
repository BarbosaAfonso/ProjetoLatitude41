import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ReservaService } from '../../services/reserva.service';

interface StoredUser {
  id: number;
}

@Component({
  selector: 'app-reservas',
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './reservas.html',
})
export class Reservas implements OnInit {
  private readonly reservaService = inject(ReservaService);
  private readonly router = inject(Router);

  public data = '';
  public hora = '';
  public quantidadePessoas: number | null = null;
  public isSubmitting = false;

  public readonly horarios = ['12:30', '13:30', '20:00', '21:30'];
  public readonly opcoesPessoas = [1, 2, 3, 4, 5, 6, 7, 8];

  private idUtilizador: number | null = null;

  public ngOnInit(): void {
    const storedUser = localStorage.getItem('user');

    if (!storedUser) {
      this.router.navigate(['/login']);
      return;
    }

    try {
      const user = JSON.parse(storedUser) as Partial<StoredUser>;

      if (typeof user.id !== 'number') {
        this.clearSessionAndRedirect();
        return;
      }

      this.idUtilizador = user.id;
    } catch {
      this.clearSessionAndRedirect();
    }
  }

  public submeterReserva(): void {
    if (!this.data || !this.hora || !this.quantidadePessoas) {
      alert('Por favor, preencha todos os campos da reserva.');
      return;
    }

    if (!this.idUtilizador) {
      this.clearSessionAndRedirect();
      return;
    }

    const reserva = {
      dataHora: `${this.data}T${this.hora}:00Z`,
      estado: 'CONFIRMADA',
      numMesa: { id: this.obterMesaPorQuantidade() },
      idUtilizador: { id: this.idUtilizador },
    };

    this.isSubmitting = true;

    this.reservaService.fazerReserva(reserva).subscribe({
      next: () => {
        alert('Reserva confirmada com sucesso!');
        this.limparFormulario();
      },
      error: () => {
        alert('Não foi possível confirmar a reserva. Tente novamente.');
        this.isSubmitting = false;
      },
      complete: () => {
        this.isSubmitting = false;
      },
    });
  }

  private obterMesaPorQuantidade(): number {
    return this.quantidadePessoas && this.quantidadePessoas > 4 ? 2 : 1;
  }

  private limparFormulario(): void {
    this.data = '';
    this.hora = '';
    this.quantidadePessoas = null;
  }

  private clearSessionAndRedirect(): void {
    localStorage.removeItem('user');
    this.router.navigate(['/login']);
  }
}
