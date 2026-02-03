import { Route } from '@angular/router';
import { Login } from './views/login/login';
import { Dashboard } from './views/dashboard/dashboard';
import { Settings } from './views/settings/settings';

export const appRoutes: Route[] = [
    {path: '', component: Login},
    {path: 'login', component: Login},
    {path: 'dashboard', component: Dashboard},
    {path: 'settings', component: Settings},
];
