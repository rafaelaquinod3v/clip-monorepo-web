import { Route } from '@angular/router';
import { Dashboard } from './pages/dashboard/dashboard';
import { Login } from './pages/login/login';
import { authGuard } from './guards/auth-guard';
import { Library } from './pages/library/library';
import { Lector } from './pages/lector/lector';
import { Epub } from './components/epub/epub';
import { MainLayoutComponent } from './layouts/main-layout/main-layout.component';

export const appRoutes: Route[] = [
    {
        path: 'login', component: Login
    },
/*     {
        path: 'dashboard', component: Dashboard, canActivate: [authGuard]
    }, */
/*     {
        path: 'library', component: Library, canActivate: [authGuard]
    },
    {
        path: 'lector', component: Lector, canActivate: [authGuard]
    },
    {
        path: 'epub/:fileName', component: Epub, canActivate: [authGuard]
    },
    {
        path: '', redirectTo: '/login', pathMatch: 'full'
    } */
     {
    path: '',
    component: MainLayoutComponent, // This component has the Top App Bar
    children: [
      { path: 'dashboard', component: Dashboard },
      { path: 'library', component: Library },
      {
        path: 'epub/:fileName', component: Epub, canActivate: [authGuard]
      },
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' }
    ]
  }
];
