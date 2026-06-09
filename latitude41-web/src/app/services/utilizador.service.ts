import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';

export interface UtilizadorPerfil {
  id: number;
  nome: string;
  email: string;
  tipo: string;
  estadoConta: string;
}

export interface AtualizarPerfilPayload {
  nome: string;
  email: string;
  tipo: string;
  estadoConta: string;
  password?: string;
}

interface UtilizadorApi {
  id?: number;
  nome?: string;
  email?: string;
  tipo?: string;
  estadoConta?: string;
  estado_conta?: string;
}

@Injectable({
  providedIn: 'root',
})
export class UtilizadorService {
  private readonly http = inject(HttpClient);
  private readonly utilizadoresUrl = 'http://localhost:8080/utilizadores';

  public atualizarPerfil(id: number, payload: AtualizarPerfilPayload): Observable<UtilizadorPerfil> {
    return this.http
      .put<UtilizadorApi>(`${this.utilizadoresUrl}/${id}`, payload)
      .pipe(map((utilizador) => this.normalizarUtilizador(utilizador, id, payload)));
  }

  private normalizarUtilizador(
    utilizador: UtilizadorApi,
    idFallback: number,
    payloadFallback: AtualizarPerfilPayload,
  ): UtilizadorPerfil {
    return {
      id: utilizador.id ?? idFallback,
      nome: utilizador.nome ?? payloadFallback.nome,
      email: utilizador.email ?? payloadFallback.email,
      tipo: utilizador.tipo ?? payloadFallback.tipo,
      estadoConta: utilizador.estadoConta ?? utilizador.estado_conta ?? payloadFallback.estadoConta,
    };
  }
}
