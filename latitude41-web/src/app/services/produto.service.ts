import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

export interface Produto {
  id?: number;
  nome: string;
  tipo: string;
  preco: number;
  disponivel?: boolean;
}

export type ProdutoPayload = Omit<Produto, 'id'>;

@Injectable({
  providedIn: 'root',
})
export class ProdutoService {
  private readonly http = inject(HttpClient);
  private readonly produtosUrl = 'http://localhost:8080/produtos';

  public listarProdutos(): Observable<Produto[]> {
    return this.http.get<Produto[]>(this.produtosUrl);
  }

  public criarProduto(produto: ProdutoPayload): Observable<Produto> {
    return this.http.post<Produto>(this.produtosUrl, produto);
  }

  public atualizarProduto(id: number, produto: ProdutoPayload): Observable<Produto> {
    return this.http.put<Produto>(`${this.produtosUrl}/${id}`, produto);
  }

  public apagarProduto(id: number): Observable<void> {
    return this.http.delete<void>(`${this.produtosUrl}/${id}`);
  }
}
