import { Route } from '@angular/router';
import { Library } from './features/library/library/library';
import { Lector } from './pages/lector/lector';
import { EbookReaderComponent } from './components/ebook-reader/ebook-reader.component';
import { MainLayoutComponent } from './layouts/main-layout/main-layout.component';
import { EbookPageComponent } from './pages/ebook/ebook-page.component';
import { authGuard } from './core/auth/auth-guard';
import { Dashboard } from './features/progress/dashboard/dashboard';
import { Login } from './features/auth/login/login';

export const appRoutes: Route[] = [
    {
        path: 'login', component: Login
    },
    {
        path: '', component: MainLayoutComponent, 
            children: [
                { path: 'dashboard', component: Dashboard, canActivate: [authGuard] },
                { path: 'library', component: Library, canActivate: [authGuard] },
                { path: 'lector', component: Lector, canActivate: [authGuard] },
                { path: 'library/epub/:fileName', component: EbookReaderComponent, canActivate: [authGuard] },
                { path: 'library/ebook/:id', component: EbookPageComponent, canActivate: [authGuard] },
                { path: '', redirectTo: 'dashboard', pathMatch: 'full' }
            ]
    }
];
