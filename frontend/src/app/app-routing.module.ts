import { NgModule }             from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { BeneficioListComponent }  from './pages/beneficio-list/beneficio-list.component';
import { BeneficioFormComponent }  from './pages/beneficio-form/beneficio-form.component';
import { TransferenciaComponent }  from './pages/transferencia/transferencia.component';

const routes: Routes = [
  { path: '',              redirectTo: 'beneficios', pathMatch: 'full' },
  { path: 'beneficios',   component: BeneficioListComponent },
  { path: 'beneficios/novo',     component: BeneficioFormComponent },
  { path: 'beneficios/:id/editar', component: BeneficioFormComponent },
  { path: 'transferencias', component: TransferenciaComponent },
  { path: '**',            redirectTo: 'beneficios' }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule {}
