import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  // Injeta o cliente HTTP de forma moderna
  private http = inject(HttpClient);

  // A URL base da tua API Spring Boot que configurámos
  private baseUrl = 'http://localhost:8080';

  // Método isolado que o teu ecrã de Login vai chamar
  login(dadosLogin: any): Observable<any> {
    return this.http.post(`${this.baseUrl}/auth/login`, dadosLogin);
  }

  registar(dadosRegisto: any): Observable<any> {
    return this.http.post(`${this.baseUrl}/auth/registar`, dadosRegisto);
  }
}
