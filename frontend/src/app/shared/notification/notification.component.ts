import { Component, Input } from '@angular/core';

/**
 * Componente reutilizável para exibir notificações de sucesso e erro.
 */
@Component({
  selector: 'app-notification',
  template: `
    <div *ngIf="mensagemSucesso" class="alert alert-success" role="alert">
      ✅ {{ mensagemSucesso }}
    </div>
    <div *ngIf="mensagemErro" class="alert alert-error" role="alert">
      ❌ {{ mensagemErro }}
    </div>
  `
})
export class NotificationComponent {
  @Input() mensagemSucesso?: string | null;
  @Input() mensagemErro?:    string | null;
}
