import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { ReservaService } from '../../services/reserva.service';
import { Navbar } from '../navbar/navbar';

interface AuthenticatedUser {
  id: number;
  nome: string;
  email: string;
  tipo: string;
  estadoConta: string;
}

interface ApiErrorResponse {
  detail?: unknown;
  message?: unknown;
  error?: unknown;
}

@Component({
  selector: 'app-reservas',
  imports: [CommonModule, FormsModule, Navbar],
  templateUrl: './reservas.html',
})
export class Reservas implements OnInit {
  private readonly reservaService = inject(ReservaService);
  private readonly router = inject(Router);

  public data = '';
  public hora = '';
  public quantidadePessoas: number | null = null;
  public isSubmitting = false;
  public user: AuthenticatedUser | null = null;

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
      const user = JSON.parse(storedUser) as Partial<AuthenticatedUser>;

      if (!this.isAuthenticatedUser(user)) {
        this.clearSessionAndRedirect();
        return;
      }

      this.user = user;
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

    const dataIso = this.converterDataParaIso(this.data);

    if (!dataIso) {
      alert('Use uma data valida no formato dia/mes/ano.');
      return;
    }

    const dataHoraSelecionada = this.criarDataHoraSelecionada(dataIso, this.hora);

    if (!dataHoraSelecionada || dataHoraSelecionada.getTime() < Date.now()) {
      alert('Nao pode criar reservas para datas ou horarios passados.');
      return;
    }

    if (!this.idUtilizador) {
      this.clearSessionAndRedirect();
      return;
    }

    const reserva = {
      dataHora: `${dataIso}T${this.hora}:00Z`,
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
      error: (error: HttpErrorResponse) => {
        alert(this.obterMensagemErroReserva(error));
        this.isSubmitting = false;
      },
      complete: () => {
        this.isSubmitting = false;
      },
    });
  }

  public formatarData(valor: string): void {
    const digitos = valor.replace(/\D/g, '').slice(0, 8);
    const partes = [digitos.slice(0, 2), digitos.slice(2, 4), digitos.slice(4, 8)].filter(Boolean);
    this.data = partes.join('/');
  }

  public logout(): void {
    localStorage.clear();
    this.router.navigate(['/login']);
  }

  private obterMesaPorQuantidade(): number {
    return this.quantidadePessoas && this.quantidadePessoas > 4 ? 2 : 1;
  }

  private criarDataHoraSelecionada(dataIso: string, hora: string): Date | null {
    const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(dataIso);
    const horaMatch = /^(\d{2}):(\d{2})$/.exec(hora);

    if (!match || !horaMatch) {
      return null;
    }

    const [, anoTexto, mesTexto, diaTexto] = match;
    const [, horasTexto, minutosTexto] = horaMatch;
    const ano = Number(anoTexto);
    const mes = Number(mesTexto);
    const dia = Number(diaTexto);
    const horas = Number(horasTexto);
    const minutos = Number(minutosTexto);
    const dataHora = new Date(ano, mes - 1, dia, horas, minutos, 0);

    const isValid =
      dataHora.getFullYear() === ano &&
      dataHora.getMonth() === mes - 1 &&
      dataHora.getDate() === dia &&
      dataHora.getHours() === horas &&
      dataHora.getMinutes() === minutos;

    return isValid ? dataHora : null;
  }

  private converterDataParaIso(data: string): string | null {
    const match = /^(\d{2})\/(\d{2})\/(\d{4})$/.exec(data.trim());

    if (!match) {
      return null;
    }

    const [, diaTexto, mesTexto, anoTexto] = match;
    const dia = Number(diaTexto);
    const mes = Number(mesTexto);
    const ano = Number(anoTexto);
    const dataUtc = new Date(Date.UTC(ano, mes - 1, dia));

    const isValid =
      dataUtc.getUTCFullYear() === ano &&
      dataUtc.getUTCMonth() === mes - 1 &&
      dataUtc.getUTCDate() === dia;

    if (!isValid) {
      return null;
    }

    return `${anoTexto}-${mesTexto}-${diaTexto}`;
  }

  private obterMensagemErroReserva(error: HttpErrorResponse): string {
    if (error.status === 0) {
      return 'Nao foi possivel contactar a API. Confirme se o servidor esta ligado.';
    }

    const apiError = error.error as ApiErrorResponse | string | null;

    if (typeof apiError === 'string' && apiError.trim()) {
      return apiError;
    }

    if (apiError && typeof apiError === 'object') {
      const mensagemApi = this.obterTextoErro(apiError.detail) ?? this.obterTextoErro(apiError.message);

      if (mensagemApi) {
        return mensagemApi;
      }
    }

    if (error.status === 409) {
      return 'Este horario ja nao esta disponivel para reserva.';
    }

    return 'Nao foi possivel confirmar a reserva. Tente novamente.';
  }

  private obterTextoErro(valor: unknown): string | null {
    return typeof valor === 'string' && valor.trim() ? valor : null;
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

  private isAuthenticatedUser(user: Partial<AuthenticatedUser>): user is AuthenticatedUser {
    return (
      typeof user.id === 'number' &&
      typeof user.nome === 'string' &&
      typeof user.email === 'string' &&
      typeof user.tipo === 'string' &&
      typeof user.estadoConta === 'string'
    );
  }
}
