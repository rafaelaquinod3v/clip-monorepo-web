import { Route } from '@angular/router';
import { Dashboard } from './pages/dashboard/dashboard';
import { Login } from './pages/login/login';
import { authGuard } from './guards/auth-guard';

export const appRoutes: Route[] = [
    {
        path: 'login', component: Login
    },
    {
        path: 'dashboard', component: Dashboard, canActivate: [authGuard]
    },
    {
        path: '', redirectTo: '/login', pathMatch: 'full'
    }
];
