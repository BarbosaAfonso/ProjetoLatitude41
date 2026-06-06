import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class ReservaService {
  private readonly http = inject(HttpClient);
  private readonly reservasUrl = 'http://localhost:8080/reservas';

  public fazerReserva(reserva: any): Observable<any> {
    return this.http.post(this.reservasUrl, reserva);
  }
}
