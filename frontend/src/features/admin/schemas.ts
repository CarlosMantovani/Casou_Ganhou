import { z } from 'zod';

const optionalEmailSchema = z.preprocess(
  (value) => (typeof value === 'string' && value.trim() === '' ? undefined : value),
  z.string().email('Informe um e-mail valido.').optional(),
);

export const adminLoginSchema = z.object({
  username: z.string().min(1, 'Informe o usuario.'),
  password: z.string().min(1, 'Informe a senha.'),
});

export const cashPaymentSchema = z.object({
  name: z.string().trim().min(1, 'Informe o nome.'),
  phone: z
    .string()
    .trim()
    .min(1, 'Informe o telefone.')
    .refine((value) => {
      const digits = value.replace(/\D/g, '');
      return digits.length === 10 || digits.length === 11;
    }, 'Informe um telefone com DDD.'),
  email: optionalEmailSchema,
  quantity: z.coerce.number().int().min(1, 'Informe ao menos 1 numero.'),
});

export type AdminLoginFormData = z.infer<typeof adminLoginSchema>;
export type CashPaymentFormData = z.infer<typeof cashPaymentSchema>;
