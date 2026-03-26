import { NgModule }             from '@angular/core';
import { BrowserModule }         from '@angular/platform-browser';
import { HttpClientModule }      from '@angular/common/http';
import { ReactiveFormsModule }   from '@angular/forms';

import { AppRoutingModule }      from './app-routing.module';
import { AppComponent }          from './app.component';
import { BeneficioListComponent }     from './pages/beneficio-list/beneficio-list.component';
import { BeneficioFormComponent }     from './pages/beneficio-form/beneficio-form.component';
import { TransferenciaComponent }     from './pages/transferencia/transferencia.component';
import { NotificationComponent }      from './shared/notification/notification.component';

@NgModule({
  declarations: [
    AppComponent,
    BeneficioListComponent,
    BeneficioFormComponent,
    TransferenciaComponent,
    NotificationComponent
  ],
  imports: [
    BrowserModule,
    HttpClientModule,
    ReactiveFormsModule,
    AppRoutingModule
  ],
  bootstrap: [AppComponent]
})
export class AppModule {}
