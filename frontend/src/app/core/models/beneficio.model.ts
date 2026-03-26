/**
 * Modelo de domínio do Benefício — espelha o DTO de resposta do backend.
 */
export interface Beneficio {
  id:          number;
  nome:        string;
  descricao?:  string;
  valor:       number;
  ativo:       boolean;
  createdAt?:  string;
  updatedAt?:  string;
}

/** DTO para criação/atualização de benefício */
export interface BeneficioRequest {
  nome:       string;
  descricao?: string;
  valor:      number;
  ativo:      boolean;
}

/** DTO para transferência de saldo */
export interface TransferenciaRequest {
  origemId:   number;
  destinoId:  number;
  valor:      number;
}
