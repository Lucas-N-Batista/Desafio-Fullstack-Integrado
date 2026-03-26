import { Component } from '@angular/core';

/**
 * Componente raiz da aplicação.
 */
@Component({
  selector:    'app-root',
  templateUrl: './app.component.html',
  styleUrls:   ['./app.component.scss']
})
export class AppComponent {
  readonly titulo = 'Desafio Fullstack – Benefícios';
}
