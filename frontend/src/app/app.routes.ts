import { Routes } from '@angular/router';
import { HomeComponent } from './components/home/home.component';
import { VerificationComponent } from './components/verification/verification.component';

export const routes: Routes = [
  { path: '',             component: HomeComponent },
  { path: 'verification', component: VerificationComponent },
  { path: '**',           redirectTo: '' }
];
