import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Produto, ProdutoPayload, ProdutoService } from '../../services/produto.service';
import { Navbar } from '../navbar/navbar';

interface AuthenticatedUser {
  id: number;
  nome: string;
  email: string;
  tipo: string;
  estadoConta: string;
}

interface ProdutoForm {
  nome: string;
  tipo: string;
  preco: number | null;
  disponivel: boolean;
  urlImagem: string;
  vegetariano: boolean;
  semGluten: boolean;
  semLactose: boolean;
}

interface GrupoMenu {
  tipo: string;
  produtos: Produto[];
}

@Component({
  selector: 'app-menu',
  imports: [CommonModule, FormsModule, Navbar],
  templateUrl: './menu.html',
})
export class Menu implements OnInit {
  private readonly produtoService = inject(ProdutoService);
  private readonly router = inject(Router);

  public user: AuthenticatedUser | null = null;
  public produtos: Produto[] = [];
  public isLoading = true;
  public isSaving = false;
  public errorMessage = '';
  public successMessage = '';
  public editingProductId: number | null = null;

  // Estados dos filtros de preferencias e alergenios.
  public categoriaSelecionada = 'Todos';
  public filtroVegetariano = false;
  public filtroSemGluten = false;
  public filtroSemLactose = false;

  public readonly tiposProduto = [
    'Entrada',
    'Prato Principal',
    'Acompanhamento',
    'Sobremesa',
    'Bebida',
    'Menu Infantil',
  ];

  public form: ProdutoForm = this.emptyForm();

  public get isAdmin(): boolean {
    return this.user?.tipo.toUpperCase() === 'ADMIN';
  }

  public get isEditing(): boolean {
    return this.editingProductId !== null;
  }

  public get produtosVisiveis(): Produto[] {
    let produtosPermitidos = this.isAdmin ? this.produtos : this.produtos.filter((produto) => produto.disponivel !== false);

    // Filtro por Categoria Selecionada
    if (this.categoriaSelecionada !== 'Todos') {
      produtosPermitidos = produtosPermitidos.filter((p) => p.tipo === this.categoriaSelecionada);
    }

    // Filtros por preferencias e alergenios.
    if (this.filtroVegetariano || this.filtroSemGluten || this.filtroSemLactose) {
      produtosPermitidos = produtosPermitidos.filter((p) => {
        if (this.filtroVegetariano && p.vegetariano !== true) return false;
        if (this.filtroSemGluten && p.semGluten !== true) return false;
        if (this.filtroSemLactose && p.semLactose !== true) return false;
        return true;
      });
    }

    return this.deduplicarProdutosPorNome(produtosPermitidos);
  }

  public get gruposMenu(): GrupoMenu[] {
    const grupos = new Map<string, Produto[]>();

    for (const produto of this.produtosVisiveis) {
      const tipo = produto.tipo?.trim() || 'Outros';
      const lista = grupos.get(tipo) ?? [];
      lista.push(produto);
      grupos.set(tipo, lista);
    }

    return Array.from(grupos.entries())
      .map(([tipo, produtos]) => ({
        tipo,
        produtos: produtos.sort((a, b) => a.nome.localeCompare(b.nome, 'pt-PT')),
      }))
      .sort((a, b) => a.tipo.localeCompare(b.tipo, 'pt-PT'));
  }

  public readonly gruposAbertos = new Set<string>();

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
      this.carregarProdutos();
    } catch {
      this.clearSessionAndRedirect();
    }
  }

  public carregarProdutos(): void {
    this.isLoading = true;
    this.errorMessage = '';

    this.produtoService.listarProdutos().subscribe({
      next: (produtos) => {
        this.produtos = produtos;
        this.abrirPrimeiroGrupo();
      },
      error: () => {
        this.errorMessage = 'Nao foi possivel carregar o menu. Confirme se a API esta ligada.';
      },
      complete: () => {
        this.isLoading = false;
      },
    });
  }

  public guardarProduto(): void {
    if (!this.isAdmin) {
      return;
    }

    const payload = this.buildPayload();
    if (!payload) {
      return;
    }

    this.isSaving = true;
    this.errorMessage = '';
    this.successMessage = '';

    const produtoIdEmEdicao = this.editingProductId;
    const request = produtoIdEmEdicao !== null
      ? this.produtoService.atualizarProduto(produtoIdEmEdicao, payload)
      : this.produtoService.criarProduto(payload);

    request.subscribe({
      next: (produtoGuardado) => {
        this.aplicarProdutoGuardado(produtoGuardado);
        this.successMessage = produtoIdEmEdicao !== null ? 'Produto atualizado no menu.' : 'Produto adicionado ao menu.';
        this.abrirGrupo(produtoGuardado.tipo);
        this.cancelarEdicao();
      },
      error: (error: unknown) => {
        this.errorMessage = this.getErrorMessage(error);
      },
      complete: () => {
        this.isSaving = false;
      },
    });
  }

  public editarProduto(produto: Produto): void {
    if (!this.isAdmin || produto.id == null) {
      return;
    }

    this.editingProductId = produto.id;
    this.successMessage = '';
    this.errorMessage = '';
    this.form = {
      nome: produto.nome,
      tipo: produto.tipo || this.tiposProduto[0],
      preco: produto.preco,
      disponivel: produto.disponivel !== false,
      urlImagem: produto.urlImagem ?? '',
      vegetariano: produto.vegetariano === true,
      semGluten: produto.semGluten === true,
      semLactose: produto.semLactose === true,
    };
  }

  public apagarProduto(produto: Produto): void {
    if (!this.isAdmin || produto.id == null) {
      return;
    }

    const confirmar = confirm(`Apagar "${produto.nome}" do menu?`);
    if (!confirmar) {
      return;
    }

    this.produtoService.apagarProduto(produto.id).subscribe({
      next: () => {
        this.successMessage = 'Produto removido do menu.';
        this.cancelarEdicao();
        this.produtos = this.produtos.filter((produtoAtual) => produtoAtual.id !== produto.id);
      },
      error: () => {
        this.errorMessage = 'Nao foi possivel remover o produto.';
      },
    });
  }

  public cancelarEdicao(): void {
    this.editingProductId = null;
    this.form = this.emptyForm();
  }

  public isGrupoAberto(tipo: string): boolean {
    return this.gruposAbertos.has(tipo);
  }

  public alternarGrupo(tipo: string): void {
    if (this.gruposAbertos.has(tipo)) {
      this.gruposAbertos.delete(tipo);
      return;
    }

    this.gruposAbertos.add(tipo);
  }

  public logout(): void {
    localStorage.clear();
    this.router.navigate(['/login']);
  }

  public formatPrice(preco: number): string {
    return new Intl.NumberFormat('pt-PT', {
      style: 'currency',
      currency: 'EUR',
    }).format(preco);
  }

  private buildPayload(): ProdutoPayload | null {
    const nome = this.form.nome.trim();
    const tipo = this.form.tipo.trim();
    const urlImagem = this.form.urlImagem.trim();

    if (!nome || !tipo || this.form.preco === null || this.form.preco <= 0) {
      this.errorMessage = 'Preencha nome, tipo e preco com valores validos.';
      return null;
    }

    return {
      nome,
      tipo,
      preco: Number(this.form.preco),
      disponivel: Boolean(this.form.disponivel),
      urlImagem: urlImagem || null,
      vegetariano: Boolean(this.form.vegetariano),
      semGluten: Boolean(this.form.semGluten),
      semLactose: Boolean(this.form.semLactose),
    };
  }

  private emptyForm(): ProdutoForm {
    return {
      nome: '',
      tipo: 'Prato Principal',
      preco: null,
      disponivel: true,
      urlImagem: '',
      vegetariano: false,
      semGluten: false,
      semLactose: false,
    };
  }

  private abrirPrimeiroGrupo(): void {
    if (this.gruposAbertos.size > 0) {
      return;
    }

    const [primeiroGrupo] = this.gruposMenu;
    if (primeiroGrupo) {
      this.abrirGrupo(primeiroGrupo.tipo);
    }
  }

  private abrirGrupo(tipo: string): void {
    this.gruposAbertos.add(tipo?.trim() || 'Outros');
  }

  private aplicarProdutoGuardado(produtoGuardado: Produto): void {
    if (produtoGuardado.id == null) {
      this.produtos = [produtoGuardado, ...this.produtos];
      return;
    }

    const index = this.produtos.findIndex((produto) => produto.id === produtoGuardado.id);
    if (index === -1) {
      this.produtos = [produtoGuardado, ...this.produtos];
      return;
    }

    this.produtos = this.produtos.map((produto, produtoIndex) =>
      produtoIndex === index ? produtoGuardado : produto
    );
  }

  private deduplicarProdutosPorNome(produtos: Produto[]): Produto[] {
    const nomesVistos = new Set<string>();

    return produtos.filter((produto) => {
      const nomeNormalizado = produto.nome.trim().toLocaleLowerCase('pt-PT');

      if (!nomeNormalizado) {
        return true;
      }

      if (nomesVistos.has(nomeNormalizado)) {
        return false;
      }

      nomesVistos.add(nomeNormalizado);
      return true;
    });
  }

  private getErrorMessage(error: unknown): string {
    if (
      typeof error === 'object' &&
      error !== null &&
      'error' in error &&
      typeof error.error === 'string'
    ) {
      return error.error;
    }

    return 'Nao foi possivel guardar o produto. Verifique os campos e tente novamente.';
  }

  // MÉTODOS PRIVADOS REPOSTOS CORRETAMENTE
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
