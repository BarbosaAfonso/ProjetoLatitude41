import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';

export interface Produto {
  id?: number;
  nome: string;
  tipo: string;
  preco: number;
  disponivel: boolean;
  urlImagem: string | null;
  vegetariano: boolean;
  semGluten: boolean;
  semLactose: boolean;
}

export interface ProdutoPayload {
  nome: string;
  tipo: string;
  preco: number;
  disponivel: boolean;
  urlImagem: string | null;
  vegetariano: boolean;
  semGluten: boolean;
  semLactose: boolean;
}

interface ProdutoApi {
  id?: number;
  nome?: string;
  tipo?: string;
  preco?: number;
  disponivel?: boolean;
  urlImagem?: string | null;
  vegetariano?: boolean;
  semGluten?: boolean;
  semLactose?: boolean;
  url_imagem?: string | null;
  sem_gluten?: boolean;
  sem_lactose?: boolean;
}

@Injectable({
  providedIn: 'root',
})
export class ProdutoService {
  private readonly http = inject(HttpClient);
  private readonly produtosUrl = 'http://localhost:8080/produtos';

  public listarProdutos(): Observable<Produto[]> {
    return this.http.get<ProdutoApi[]>(this.produtosUrl).pipe(
      map((produtos) => produtos.map((produto) => this.normalizarProduto(produto))),
    );
  }

  public criarProduto(produto: ProdutoPayload): Observable<Produto> {
    return this.http
      .post<ProdutoApi>(this.produtosUrl, this.toRequest(produto))
      .pipe(map((produtoCriado) => this.normalizarProduto(produtoCriado)));
  }

  public atualizarProduto(id: number, produto: ProdutoPayload): Observable<Produto> {
    return this.http
      .put<ProdutoApi>(`${this.produtosUrl}/${id}`, this.toRequest(produto))
      .pipe(map((produtoAtualizado) => this.normalizarProduto(produtoAtualizado)));
  }

  public apagarProduto(id: number): Observable<void> {
    return this.http.delete<void>(`${this.produtosUrl}/${id}`);
  }

  private toRequest(produto: ProdutoPayload): ProdutoPayload {
    return {
      nome: produto.nome,
      tipo: produto.tipo,
      preco: produto.preco,
      disponivel: produto.disponivel,
      urlImagem: produto.urlImagem,
      vegetariano: produto.vegetariano,
      semGluten: produto.semGluten,
      semLactose: produto.semLactose,
    };
  }

  private normalizarProduto(produto: ProdutoApi): Produto {
    return {
      id: produto.id,
      nome: produto.nome ?? '',
      tipo: produto.tipo ?? 'Outros',
      preco: Number(produto.preco ?? 0),
      disponivel: produto.disponivel ?? true,
      urlImagem: produto.urlImagem ?? produto.url_imagem ?? null,
      semGluten: produto.semGluten ?? produto.sem_gluten ?? false,
      semLactose: produto.semLactose ?? produto.sem_lactose ?? false,
      vegetariano: produto.vegetariano ?? false,
    };
  }
}
