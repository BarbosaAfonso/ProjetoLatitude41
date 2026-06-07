import { Routes } from '@angular/router';
import { Login } from './components/login/login';
import { Home } from './components/home/home';
import { Reservas } from './components/reservas/reservas';
import { Menu } from './components/menu/menu';
import { Perfil } from './components/perfil/perfil';

export const routes: Routes = [
  { path: '', redirectTo: 'login', pathMatch: 'full' },
  { path: 'home', component: Home },
  { path: 'login', component: Login },
  { path: 'reservas', component: Reservas },
  { path: 'menu', component: Menu },

  { path: 'perfil', component: Perfil },

  { path: '**', redirectTo: 'home' }
];
