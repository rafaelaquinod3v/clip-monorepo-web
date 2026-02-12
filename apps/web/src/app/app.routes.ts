import { Route } from '@angular/router';
import { Dashboard } from './pages/dashboard/dashboard';
import { Login } from './pages/login/login';
import { authGuard } from './guards/auth-guard';
import { Library } from './pages/library/library';
import { Lector } from './pages/lector/lector';

export const appRoutes: Route[] = [
    {
        path: 'login', component: Login
    },
    {
        path: 'dashboard', component: Dashboard, canActivate: [authGuard]
    },
    {
        path: 'library', component: Library, canActivate: [authGuard]
    },
    {
        path: 'lector', component: Lector, canActivate: [authGuard]
    },
    {
        path: '', redirectTo: '/login', pathMatch: 'full'
    }
];
